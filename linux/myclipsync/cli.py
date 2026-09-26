"""myclipsync CLI (KDE-Connect `kdeconnect-cli` inspired).

Works against the local history DB even when the GUI/daemon isn't running:

  myclipsync status [--json]        connection state + counts
  myclipsync history [-n N] [-q Q]  list recent clips
  myclipsync send "text"            queue a clip for the peer (sent on connect)
  myclipsync clear                  clear history
  myclipsync log [-n N]             tail the daemon log
"""
import json
import os
import sqlite3
import sys
import time

from .main import APP_DIR, DB_PATH, CONFIG_PATH, load_config


def _db_rows(limit, query):
    if not os.path.exists(DB_PATH):
        return []
    db = sqlite3.connect(DB_PATH, timeout=10.0, isolation_level=None)
    db.execute("PRAGMA busy_timeout=5000;")
    try:
        if query:
            q = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            cur = db.execute(
                "SELECT id,text,ts,src,direction,conflict_loser FROM clips "
                "WHERE text LIKE ? ESCAPE '\\' ORDER BY ts DESC, rowid DESC LIMIT ?",
                (f"%{q}%", limit))
        else:
            cur = db.execute(
                "SELECT id,text,ts,src,direction,conflict_loser FROM clips "
                "ORDER BY ts DESC, rowid DESC LIMIT ?", (limit,))
        return cur.fetchall()
    finally:
        db.close()


def cmd_status(args):
    cfg = load_config()
    logf = os.path.join(APP_DIR, "myclipsync.log")
    logging_alive = False
    if os.path.exists(logf):
        try:
            logging_alive = time.time() - os.path.getmtime(logf) < 120
        except Exception:
            pass
    n = 0
    if os.path.exists(DB_PATH):
        try:
            db = sqlite3.connect(DB_PATH, timeout=10.0, isolation_level=None)
            db.execute("PRAGMA busy_timeout=5000;")
            n = db.execute("SELECT COUNT(*) FROM clips").fetchone()[0]
            db.close()
        except Exception:
            pass
    status_file = os.path.join(APP_DIR, "status.json")
    bt_status = "Offline (listening)"
    if os.path.exists(status_file):
        try:
            with open(status_file) as f:
                st = json.load(f)
            if time.time() - st.get("ts", 0) < 60:
                peer = st.get("peer", "")
                bt_status = f"Connected ({peer})" if st.get("connected") else f"Offline ({peer or 'listening'})"
        except Exception:
            pass

    out = {"device": cfg.get("device_name"), "channel": cfg.get("channel"),
           "history_cap": cfg.get("history_cap"), "history_rows": n,
           "log_recent": logging_alive, "bt_status": bt_status, "db": DB_PATH}
    if getattr(args, "json", False):
        print(json.dumps(out, indent=2))
    else:
        print(f"device:      {out['device']}")
        print(f"channel:     {out['channel']}")
        print(f"history:     {out['history_rows']} rows (cap {out['history_cap']})")
        print(f"db:          {out['db']}")
        print(f"log recent:  {'yes' if out['log_recent'] else 'no (<120s activity)'}")
        print(f"BT server:   {out['bt_status']}")


def cmd_history(args):
    rows = _db_rows(args.n, args.q or "")
    for mid, text, ts, src, direction, loser in rows:
        t = time.strftime("%m-%d %H:%M", time.localtime(ts / 1000))
        arrow = "◀" if direction == "remote" else "▶"
        flag = " [loser]" if loser else ""
        print(f"{arrow} [{t}] ({src}/{direction}{flag}) {(text[:200].replace(chr(10), ' ⏎ ') or '(empty)')}")


def cmd_send(args):
    # Queue into the outbox table? v1 has no shared outbox file, so insert as
    # a local clip row AND drop a spool file the daemon flushes on connect.
    from . import protocol
    cfg = load_config()
    src = ("linux-" + str(cfg.get("device_name", "pc")))[:64]
    msg = protocol.make_clip(src, args.text)
    spool = os.path.join(APP_DIR, "spool")
    os.makedirs(spool, exist_ok=True)
    with open(os.path.join(spool, msg["id"] + ".json"), "w") as f:
        json.dump(msg, f)
    # also record locally so it shows in history immediately
    try:
        db = sqlite3.connect(DB_PATH, timeout=10.0, isolation_level=None)
        db.execute("PRAGMA busy_timeout=5000;")
        db.execute("INSERT OR IGNORE INTO clips(id,text,ts,src,direction) VALUES(?,?,?,?,?)",
                   (msg["id"], args.text, msg["ts"], src, "local"))
        db.close()
    except Exception as e:
        print(f"warning: history insert failed: {e}", file=sys.stderr)
    print(f"queued {msg['id']} (delivered when peer connects / daemon flushes spool)")


def cmd_clear(_args):
    if os.path.exists(DB_PATH):
        db = sqlite3.connect(DB_PATH, timeout=10.0, isolation_level=None)
        db.execute("PRAGMA busy_timeout=5000;")
        db.execute("DELETE FROM clips")
        db.close()
    print("history cleared")


def cmd_log(args):
    logf = os.path.join(APP_DIR, "myclipsync.log")
    if not os.path.exists(logf):
        print("(no log yet — start the app once)")
        return
    with open(logf, errors="replace") as f:
        lines = f.readlines()
    for ln in lines[-args.n:]:
        print(ln, end="")


def main(argv=None):
    import argparse
    ap = argparse.ArgumentParser(prog="myclipsync", description="MyClipSync CLI")
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("status"); p.add_argument("--json", action="store_true")
    p.set_defaults(fn=cmd_status)
    p = sub.add_parser("history"); p.add_argument("-n", type=int, default=20)
    p.add_argument("-q", default=""); p.set_defaults(fn=cmd_history)
    p = sub.add_parser("send"); p.add_argument("text"); p.set_defaults(fn=cmd_send)
    p = sub.add_parser("clear"); p.set_defaults(fn=cmd_clear)
    p = sub.add_parser("log"); p.add_argument("-n", type=int, default=50)
    p.set_defaults(fn=cmd_log)
    args = ap.parse_args(argv)
    args.fn(args)


if __name__ == "__main__":
    main()

