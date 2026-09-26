"""Synqvia main window (ClipCascade/KDE-Connect inspired).

A real, visible GUI — the tray icon alone was effectively invisible
(StatusIcon with no window; left-click did nothing). Layout:

  Header: status dot + peer  |  [Sync now] [Send test] [Clear]
  Pairing row: PC adapter MAC (read-only) + channel + device name
  Search entry
  History list (direction arrow, time, preview; click = re-copy/broadcast)
  Settings row: history cap + save
  Log view (collapsible) fed by logging handler
"""
import logging
import time

import gi
gi.require_version("Gtk", "3.0")
from gi.repository import Gtk, GLib, Gdk

log = logging.getLogger("synqvia.ui")


class LogHandler(logging.Handler):
    def __init__(self, textview):
        super().__init__(level=logging.DEBUG)
        self._view = textview
        self.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(name)s: %(message)s",
                                            datefmt="%H:%M:%S"))

    def emit(self, record):
        try:
            msg = self.format(record)
            GLib.idle_add(self._append, msg)
        except Exception:
            pass

    def _append(self, msg):
        try:
            buf = self._view.get_buffer()
            end = buf.get_end_iter()
            buf.insert(end, msg + "\n")
            # autoscroll
            mark = buf.create_mark(None, buf.get_end_iter(), False)
            self._view.scroll_to_mark(mark, 0.0, False, 0.0, 0.0)
        except Exception:
            pass
        return False


class MainWindow(Gtk.ApplicationWindow):
    def __init__(self, app, engine, history, clipboard, config, save_config):
        super().__init__(application=app, title="Synqvia")
        self.set_default_size(680, 520)
        self.set_position(Gtk.WindowPosition.CENTER)
        self._engine = engine
        self._history = history
        self._clipboard = clipboard
        self._config = config
        self._save_config = save_config
        self._connected = False
        self._peer = ""
        try:
            engine.add_status_listener(self._on_status)
        except Exception:
            engine.status_cb = self._on_status

        root = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=6)
        root.set_border_width(10)
        self.add(root)

        # ---- status header (KDE-Connect style device card) ----
        hdr = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        self._dot = Gtk.Label()
        self._dot.set_markup('<span font="24">○</span>')
        self._status = Gtk.Label(label="Starting…")
        self._status.set_halign(Gtk.Align.START)
        self._status.set_hexpand(True)
        btn_sync = Gtk.Button(label="Sync now")
        btn_sync.set_tooltip_text("Read the current Linux clipboard and broadcast it")
        btn_sync.connect("clicked", self._on_sync_now)
        btn_test = Gtk.Button(label="Send test")
        btn_test.set_tooltip_text("Send a timestamped test clip to the peer")
        btn_test.connect("clicked", self._on_send_test)
        btn_clear = Gtk.Button(label="Clear")
        btn_clear.connect("clicked", self._on_clear)
        for b in (btn_sync, btn_test, btn_clear):
            hdr.pack_start(b, False, False, 0)
        hdr.pack_start(self._dot, False, False, 0)
        hdr.pack_start(self._status, True, True, 0)
        hdr.reorder_child(self._dot, 0)
        hdr.reorder_child(self._status, 1)
        root.pack_start(hdr, False, False, 0)

        # ---- pairing / adapter row ----
        pair = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        mac = self._adapter_mac()
        lbl = Gtk.Label(label=f"This PC adapter: {mac}   •   channel {config.get('channel', 1)}")
        lbl.set_halign(Gtk.Align.START)
        lbl.set_hexpand(True)
        lbl.set_selectable(True)
        pair.pack_start(lbl, True, True, 0)
        root.pack_start(pair, False, False, 0)
        tip = Gtk.Label()
        tip.set_markup('<small>Pair first with <tt>bluetoothctl</tt>, then enter the PC MAC above in the Android app.</small>')
        tip.set_halign(Gtk.Align.START)
        root.pack_start(tip, False, False, 0)

        # ---- search ----
        self._search = Gtk.SearchEntry()
        self._search.set_placeholder_text("Search history…")
        self._search.connect("changed", lambda *a: self.refresh())
        root.pack_start(self._search, False, False, 0)

        # ---- history list ----
        scr = Gtk.ScrolledWindow()
        scr.set_hexpand(True)
        scr.set_vexpand(True)
        self._list = Gtk.ListBox()
        self._list.set_selection_mode(Gtk.SelectionMode.SINGLE)
        self._list.connect("row-activated", self._on_row)
        scr.add(self._list)
        root.pack_start(scr, True, True, 0)

        # ---- settings row ----
        cfg = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=8)
        cfg.pack_start(Gtk.Label(label="Device name:"), False, False, 0)
        self._name = Gtk.Entry(text=str(config.get("device_name", "")))
        self._name.set_hexpand(True)
        cfg.pack_start(self._name, True, True, 0)
        cfg.pack_start(Gtk.Label(label="Cap (0=∞):"), False, False, 0)
        adj = Gtk.Adjustment(value=float(config.get("history_cap", 500) or 0),
                             lower=0, upper=100000, step_increment=50)
        self._cap = Gtk.SpinButton(adjustment=adj)
        cfg.pack_start(self._cap, False, False, 0)
        btn_save = Gtk.Button(label="Save")
        btn_save.connect("clicked", self._on_save_cfg)
        cfg.pack_start(btn_save, False, False, 0)
        root.pack_start(cfg, False, False, 0)

        # ---- log expander ----
        exp = Gtk.Expander(label="Log")
        logscr = Gtk.ScrolledWindow()
        logscr.set_min_content_height(120)
        self._logview = Gtk.TextView()
        self._logview.set_editable(False)
        self._logview.set_monospace(True)
        logscr.add(self._logview)
        exp.add(logscr)
        root.pack_start(exp, False, False, 0)
        logging.getLogger().addHandler(LogHandler(self._logview))

        self.connect("delete-event", self._on_close)
        self.refresh()
        self._update_status()
        self.show_all()

    # ---------- helpers ----------
    @staticmethod
    def _adapter_mac():
        try:
            import re, subprocess
            out = subprocess.check_output(["bluetoothctl", "show"],
                                          stderr=subprocess.STDOUT, timeout=5).decode()
            m = re.search(r"Controller\s+([0-9A-F:]{17})", out)
            if m:
                return m.group(1)
        except Exception as e:
            log.debug("adapter mac lookup failed: %s", e)
        return "(unknown — run: bluetoothctl show)"

    # ---------- status ----------
    def _on_status(self, connected, peer):
        self._connected = connected
        self._peer = peer or ""
        GLib.idle_add(self._update_status)

    def _update_status(self):
        if self._connected:
            self._dot.set_markup('<span font="24" fgcolor="green">●</span>')
            self._status.set_text(f"Connected ({self._peer})" if self._peer else "Connected")
        else:
            self._dot.set_markup('<span font="24" fgcolor="red">○</span>')
            self._status.set_text(f"Offline — listening ({self._peer})" if self._peer else "Offline — listening")
        return False

    # ---------- history ----------
    def refresh(self):
        q = self._search.get_text()
        try:
            rows = self._history.recent(limit=200, query=q)
        except Exception as e:
            log.warning("history read failed: %s", e)
            rows = []
        for ch in self._list.get_children():
            self._list.remove(ch)
        if not rows:
            lbl = Gtk.Label(label="(no matches)" if q else "(empty — copy something to begin)")
            row = Gtk.ListBoxRow()
            row.add(lbl)
            self._list.add(row)
        for r in rows:
            mid, text, ts, src, direction = r[0], r[1], r[2], r[3], r[4]
            loser = len(r) > 5 and r[5]
            t = time.strftime("%m-%d %H:%M", time.localtime(ts / 1000))
            arrow = "◀" if direction == "remote" else "▶"
            preview = (text[:120].replace("\n", " ⏎ ") or "(empty)")
            if loser:
                preview += "  [conflict loser]"
            lbl = Gtk.Label()
            lbl.set_markup(
                f"<b>{arrow}</b> <small>{GLib.markup_escape_text(t)} · "
                f"{GLib.markup_escape_text(str(src))}</small>\n"
                f"{GLib.markup_escape_text(preview)}")
            lbl.set_halign(Gtk.Align.START)
            lbl.set_line_wrap(True)
            row = Gtk.ListBoxRow()
            row.add(lbl)
            row._clip_text = text
            self._list.add(row)
        self._list.show_all()

    def _on_row(self, _box, row):
        text = getattr(row, "_clip_text", None)
        if text is not None:
            try:
                self._engine.repaste(text)
            except Exception as e:
                log.warning("repaste failed: %s", e)

    # ---------- buttons ----------
    def _on_sync_now(self, _b):
        """Foreground read of the Linux clipboard + broadcast (always allowed on Linux)."""
        try:
            disp = Gdk.Display.get_default()
            text = None
            if disp is not None:
                c = Gtk.Clipboard.get(Gdk.SELECTION_CLIPBOARD)
                text = c.wait_for_text()
            if text is None:
                text = self._clipboard.get_current()
            self._clipboard.feed_local(text or "")
            self.refresh()
        except Exception as e:
            log.warning("sync-now failed: %s", e)

    def _on_send_test(self, _b):
        try:
            import time as _t
            self._clipboard.feed_local(f"Synqvia test {_t.strftime('%H:%M:%S')}")
            self.refresh()
        except Exception as e:
            log.warning("send-test failed: %s", e)

    def _on_clear(self, _b):
        try:
            self._history.clear()
            self.refresh()
        except Exception as e:
            log.warning("clear failed: %s", e)

    def _on_save_cfg(self, _b):
        try:
            self._config["device_name"] = self._name.get_text().strip()[:48] or "pc"
            self._config["history_cap"] = max(0, int(self._cap.get_value()))
            self._history.set_cap(self._config["history_cap"])
            self._save_config()
            self.refresh()
        except Exception as e:
            log.warning("save config failed: %s", e)

    def _on_close(self, *a):
        # Hide to tray instead of quitting (window close ≠ quit).
        try:
            self.hide()
            return True
        except Exception:
            return False
