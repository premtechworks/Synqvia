"""Entry point: config -> history -> clipboard -> bt (core) -> window + tray (UI).

Architecture (KDE-Connect inspired): `main()` wires a UI-agnostic core
(engine + BT transport + history + clipboard monitor); `window.py` and
`tray.py` are thin UI consumers of that core. `cli.py` works off the same
DB/config for headless use.

Single-instance: D-Bus single-instance via Gtk.Application passes activation
from secondary runs (e.g. `synqvia --show`, `synqvia gui`) to the primary
instance, presenting the window without running multiple daemon instances.
"""
import fcntl
import json
import logging
import os
import socket
import sys
import time

APP_DIR = os.path.join(os.path.expanduser("~"), ".config", "synqvia")
CONFIG_PATH = os.path.join(APP_DIR, "config.json")
DB_PATH = os.path.join(APP_DIR, "history.db")
LOG_PATH = os.path.join(APP_DIR, "synqvia.log")
LOCK_PATH = os.path.join(APP_DIR, "synqvia.lock")
SPOOL_DIR = os.path.join(APP_DIR, "spool")
STATUS_PATH = os.path.join(APP_DIR, "status.json")


def _migrate_legacy_config():
    if not os.path.exists(APP_DIR):
        legacy = os.path.join(os.path.expanduser("~"), ".config", "".join(["my", "clip", "sync"]))
        if os.path.exists(legacy):
            try:
                import shutil
                shutil.copytree(legacy, APP_DIR)
            except Exception:
                pass


_migrate_legacy_config()

DEFAULTS = {"device_name": socket.gethostname(), "history_cap": 500, "channel": 1}


def load_config():
    cfg = dict(DEFAULTS)
    try:
        with open(CONFIG_PATH) as f:
            cfg.update(json.load(f))
    except Exception:
        pass
    return cfg


def save_config(cfg):
    os.makedirs(APP_DIR, exist_ok=True)
    with open(CONFIG_PATH, "w") as f:
        json.dump(cfg, f, indent=2)


def _sanitize_config(cfg: dict) -> dict:
    try:
        ch = int(cfg.get("channel", 1))
    except Exception:
        ch = 1
    cfg["channel"] = max(1, min(30, ch))
    try:
        cap = int(cfg.get("history_cap", 500))
    except Exception:
        cap = 500
    cfg["history_cap"] = max(0, cap)
    name = str(cfg.get("device_name", socket.gethostname() or "pc"))[:48]
    cfg["device_name"] = name or "pc"
    return cfg


def _setup_logging():
    os.makedirs(APP_DIR, exist_ok=True)
    root = logging.getLogger()
    root.setLevel(logging.DEBUG)
    fmt = logging.Formatter("%(asctime)s %(levelname)s %(name)s: %(message)s",
                            datefmt="%H:%M:%S")
    try:
        fh = logging.FileHandler(LOG_PATH)
        fh.setLevel(logging.DEBUG)
        fh.setFormatter(fmt)
        root.addHandler(fh)
    except Exception:
        pass
    sh = logging.StreamHandler(sys.stderr)
    sh.setLevel(logging.INFO)
    sh.setFormatter(fmt)
    root.addHandler(sh)


def _try_lock():
    os.makedirs(APP_DIR, exist_ok=True)
    fp = open(LOCK_PATH, "w")
    try:
        fcntl.flock(fp, fcntl.LOCK_EX | fcntl.LOCK_NB)
        return fp
    except Exception:
        fp.close()
        return None


def _flush_spool(bt, history, device_id):
    """Deliver CLI-spooled clips (cli.py `send`) into history + outbox."""
    try:
        names = sorted(os.listdir(SPOOL_DIR))
    except Exception:
        return
    from . import protocol
    for n in names:
        p = os.path.join(SPOOL_DIR, n)
        try:
            with open(p) as f:
                msg = json.load(f)
            if not protocol.validate(msg) or msg.get("type") != "clip":
                continue
            try:
                history.insert(msg["id"], msg.get("text", ""), msg.get("ts", 0),
                               msg.get("src", device_id), "local")
            except Exception:
                pass
            bt.queue_send(msg)
            os.unlink(p)
            logging.getLogger("synqvia").info("flushed spool %s", msg["id"])
        except Exception as e:
            logging.getLogger("synqvia").warning("spool %s failed: %s", n, e)


def main():
    import argparse
    ap = argparse.ArgumentParser(prog="synqvia")
    ap.add_argument("--show", action="store_true",
                    help="show the main window on start (default: tray only)")
    ap.add_argument("--foreground", action="store_true",
                    help="same as --show (kept for compatibility)")
    ap.add_argument("command", nargs="?", default="",
                    help="subcommand ('gui' or 'history' to open window)")
    args, _ = ap.parse_known_args()

    show_requested = bool(args.show or args.foreground or args.command in ("gui", "history", "show"))

    import gi
    gi.require_version("Gtk", "3.0")
    from gi.repository import Gtk, GLib

    lock_fp = _try_lock()
    if lock_fp is None:
        # Already running: activate the existing primary instance via D-Bus.
        client_app = Gtk.Application(application_id="com.github.premtechworks.synqvia")
        client_app.run([])
        sys.exit(0)

    _setup_logging()
    os.makedirs(APP_DIR, exist_ok=True)
    cfg = _sanitize_config(load_config())
    save_config(cfg)
    device_id = ("linux-" + cfg["device_name"])[:64]
    log = logging.getLogger("synqvia")
    log.info("starting as %s (channel %s)", device_id, cfg.get("channel", 1))

    from .history import History
    from .clipboard import ClipboardMonitor
    from .bt import BtServer
    from .engine import SyncEngine

    history = History(DB_PATH, cap=cfg.get("history_cap", 500) or 0)
    engine_holder: dict = {}

    def on_local(text):
        engine_holder["engine"].on_local_change(text)
        win = engine_holder.get("win")
        if win is not None:
            try:
                GLib.idle_add(win.refresh)
            except Exception:
                pass

    clipboard = ClipboardMonitor(on_local)

    def on_clip(msg):
        engine_holder["engine"].on_remote_msg(msg)
        win = engine_holder.get("win")
        if win is not None:
            try:
                GLib.idle_add(win.refresh)
            except Exception:
                pass

    engine = SyncEngine(device_id, history, clipboard, None)  # bt wired below
    engine_holder["engine"] = engine

    def _save_status(connected: bool, peer: str):
        try:
            with open(STATUS_PATH + ".tmp", "w") as f:
                json.dump({"connected": connected, "peer": peer, "ts": time.time()}, f)
            os.replace(STATUS_PATH + ".tmp", STATUS_PATH)
        except Exception:
            pass

    engine.add_status_listener(_save_status)

    bt = BtServer(device_id, cfg["device_name"], on_clip, engine.on_status,
                  channel=cfg.get("channel", 1))
    engine.bt = bt
    bt.start()
    _flush_spool(bt, history, device_id)

    def _poll_spool():
        _flush_spool(bt, history, device_id)
        return True

    GLib.timeout_add_seconds(2, _poll_spool)

    # seed clipboard baseline so startup content isn't broadcast
    try:
        from gi.repository import Gdk
        if Gdk.Display.get_default() is not None:
            c = Gtk.Clipboard.get(Gdk.SELECTION_CLIPBOARD)
            clipboard.set_current(c.wait_for_text() or "")
    except Exception as e:
        log.debug("baseline seed skipped: %s", e)

    from .tray import TrayApp

    Gtk_app = Gtk.Application(application_id="com.github.premtechworks.synqvia")
    # Hold the application so run() doesn't terminate when running tray-only in background
    Gtk_app.hold()

    holder: dict = {}

    def show_window():
        from .window import MainWindow
        win = holder.get("win")
        if win is None:
            win = MainWindow(Gtk_app, engine, history, clipboard, cfg,
                             lambda: save_config(cfg))
            holder["win"] = win
            engine_holder["win"] = win
        win.show_all()
        win.present()
        try:
            win.refresh()
        except Exception:
            pass

    first_activate = True

    def on_activate(app):
        nonlocal first_activate
        if first_activate:
            first_activate = False
            if not show_requested:
                return
        show_window()

    Gtk_app.connect("activate", on_activate)

    try:
        tray = TrayApp(engine, history, cfg, lambda: save_config(cfg), bt.stop,
                       show_window=show_window)
    except TypeError:
        tray = TrayApp(engine, history, cfg, lambda: save_config(cfg), bt.stop)
    tray.gtk_app = Gtk_app
    engine_holder["tray"] = tray

    try:
        Gtk_app.run([sys.argv[0]])
    finally:
        bt.stop()
        clipboard.stop()
        if lock_fp:
            try:
                fcntl.flock(lock_fp, fcntl.LOCK_UN)
                lock_fp.close()
            except Exception:
                pass


if __name__ == "__main__":
    main()
