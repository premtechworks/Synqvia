"""Sync engine: clipboard <-> history <-> BT, with LWW + loop prevention."""
import threading
import uuid
from collections import deque

from . import protocol


class SyncEngine:
    def __init__(self, device_id: str, history, clipboard, bt_server):
        self.device_id = device_id
        self.history = history
        self.clipboard = clipboard
        self.bt = bt_server
        self._seen: deque = deque(maxlen=500)
        self._seen_set: set = set()
        self._lock = threading.Lock()
        self._last_local_ts = 0
        self._pending_local: dict | None = None
        self.connected = False
        self.peer = ""

    def _mark_seen(self, mid: str) -> bool:
        with self._lock:
            if mid in self._seen_set:
                return False
            self._seen.append(mid)
            self._seen_set.add(mid)
            while len(self._seen_set) > 500:
                old = self._seen.popleft()
                self._seen_set.discard(old)
            return True

    # called by ClipboardMonitor
    def on_local_change(self, text: str):
        ts = protocol.now_ms()
        msg = protocol.make_clip(self.device_id, text, ts)
        self._mark_seen(msg["id"])
        with self._lock:
            self._last_local_ts = ts
            self._pending_local = msg
        self.history.insert(msg["id"], text, ts, self.device_id, "local")
        if self.bt is not None:
            self.bt.queue_send(msg)

    def _send_ack(self, for_id: str):
        if self.bt is None:
            return
        try:
            send = getattr(self.bt, "send_msg", None) or getattr(self.bt, "_try_send", None)
            if send is not None:
                send(protocol.make_ack(self.device_id, for_id))
        except Exception:
            pass

    # called by BtServer reader
    def on_remote_msg(self, msg: dict):
        if not protocol.validate(msg):
            return
        if msg.get("v") != protocol.PROTOCOL_VERSION:
            if self.bt is not None:
                try:
                    send = getattr(self.bt, "send_msg", None) or getattr(self.bt, "_try_send", None)
                    if send is not None:
                        send(protocol.make_bye(self.device_id, "version"))
                except Exception:
                    pass
            return
        t = msg.get("type")
        if t == "hello":
            peer = msg.get("name", msg.get("src", ""))
            self.on_status(True, str(peer))
            return
        if t == "ack":
            if self.bt is not None:
                try:
                    self.bt.ack_received(msg.get("for", ""))
                except Exception:
                    pass
            return
        if t == "bye":
            return
        if t != "clip":
            return
        mid = msg.get("id", "")
        if not self._mark_seen(mid):
            # duplicate (e.g. retransmit): ack again, don't apply
            self._send_ack(mid)
            return
        # Restart-proof dedup: DB outlives in-memory seen cache.
        try:
            if hasattr(self.history, "exists") and self.history.exists(mid):
                self._send_ack(mid)
                return
        except Exception:
            pass
        text = msg.get("text", "")
        if msg.get("mime", "text/plain") != "text/plain":
            # Unknown mime: ack per spec but ignore (no apply, no history).
            self._send_ack(mid)
            return
        rts, rsrc = msg.get("ts", 0), msg.get("src", "")
        try:
            inserted = self.history.insert(mid, text, rts, rsrc, "remote")
        except Exception:
            inserted = True
        if inserted is False:
            self._send_ack(mid)
            return
        # LWW: near-simultaneous local edit?
        with self._lock:
            local = self._pending_local
            now = protocol.now_ms()
            recent = (now - self._last_local_ts) < protocol.CONFLICT_WINDOW_MS
            if not recent:
                self._pending_local = None
        if recent and local is not None:
            winner = protocol.last_write_wins(
                local["ts"], local["src"], rts, rsrc)
            if winner == "local":
                # keep local clipboard; mark remote as loser (already in history)
                try:
                    if hasattr(self.history, "mark_loser"):
                        self.history.mark_loser(mid)
                except Exception:
                    pass
                self._send_ack(mid)
                return
            else:
                # remote wins: local loser stays in history flagged, not rebroadcast
                try:
                    if hasattr(self.history, "mark_loser"):
                        self.history.mark_loser(local.get("id", ""))
                except Exception:
                    pass
        # remote wins (or no conflict): apply + suppress echo
        try:
            self.clipboard.apply_remote(text)
        except Exception:
            pass
        self._send_ack(mid)

    def add_status_listener(self, cb):
        """Multiple UI consumers (tray + window) can all listen (KDE-style)."""
        lst = getattr(self, "_status_listeners", None)
        if lst is None:
            lst = self._status_listeners = []
        if cb not in lst:
            lst.append(cb)

    def on_status(self, connected: bool, peer: str):
        self.connected = connected
        self.peer = peer
        for cb in list(getattr(self, "_status_listeners", None) or []):
            try:
                cb(connected, peer)
            except Exception:
                pass
        cb = getattr(self, "status_cb", None)
        if cb:
            try:
                cb(connected, peer)
            except Exception:
                pass

    def repaste(self, text: str):
        """User clicked a history item: treat as fresh local copy (broadcast)."""
        try:
            self.clipboard.feed_local(text)
        except Exception:
            pass
