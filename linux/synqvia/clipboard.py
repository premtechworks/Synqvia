"""GTK clipboard monitor with suppress-on-apply (loop prevention)."""
import threading
import time
import hashlib

from . import protocol


class ClipboardMonitor:
    def __init__(self, on_local_change, poll_fallback_ms: int = 500):
        self.on_local_change = on_local_change
        self._suppress_hash: str | None = None
        self._suppress_until_ms = 0
        self._last_sent_hash: str | None = None
        self._last_sent_ts = 0
        self._last_text = ""
        self._lock = threading.Lock()
        self._clip = None
        self._poll_ms = poll_fallback_ms
        self._stop_poll = threading.Event()
        try:
            import gi
            gi.require_version("Gtk", "3.0")
            from gi.repository import Gtk, Gdk
            disp = Gdk.Display.get_default()
            if disp is not None:
                self._clip = Gtk.Clipboard.get(Gdk.SELECTION_CLIPBOARD)
            else:
                self._clip = None
            if self._clip is not None:
                self._clip.connect("owner-change", self._on_owner_change)
        except Exception:
            self._clip = None  # headless/test: use polling or manual feed
        if self._clip is None and poll_fallback_ms > 0:
            t = threading.Thread(target=self._poll_loop, daemon=True)
            t.start()

    def _hash(self, t: str) -> str:
        return hashlib.sha256(t.encode("utf-8")).hexdigest()

    def apply_remote(self, text: str):
        """Write remote text locally; next echo of same text is suppressed."""
        with self._lock:
            self._suppress_hash = self._hash(text)
            self._suppress_until_ms = protocol.now_ms() + protocol.SUPPRESS_WINDOW_MS
            self._last_text = text

        def _write():
            try:
                if self._clip is not None:
                    self._clip.set_text(text, -1)
                    self._clip.store()
            except Exception:
                pass
            return False

        try:
            from gi.repository import GLib
            GLib.idle_add(_write)
        except Exception:
            _write()

    def set_current(self, text: str):
        with self._lock:
            self._last_text = text

    def get_current(self) -> str:
        with self._lock:
            return self._last_text

    def _on_owner_change(self, *args):
        # Every owner-change is a genuine new copy event — even if the text
        # value is identical to the previous one (user re-copied same text).
        # Do NOT compare against _last_text here; suppress window handles echo.
        try:
            text = self._clip.wait_for_text()
            if text is None:
                text = ""
        except Exception:
            return
        self.feed_local(text)

    def _poll_loop(self):
        while not self._stop_poll.is_set():
            try:
                text = None
                if self._clip is not None:
                    try:
                        text = self._clip.wait_for_text()
                    except Exception:
                        text = None
                if text is not None:
                    with self._lock:
                        changed = (text != self._last_text)
                    if changed:
                        self.feed_local(text)
            except Exception:
                pass
            time.sleep(max(self._poll_ms, 100) / 1000.0)

    def stop(self):
        self._stop_poll.set()

    def feed_local(self, text: str):
        """Entry point for local clipboard changes. Returns True if broadcast.

        Suppress-only (no equality block): each call is treated as a new copy
        event with a fresh uuid, unless it matches the suppress hash inside
        the suppress window (echo of a just-applied remote clip).

        The suppress stays armed for the whole window: Gtk can emit
        owner-change TWICE for one programmatic set_text+store(), and the
        first echo must not disarm the second. Likewise, identical text
        re-broadcast within the conflict window is coalesced (multiple
        capture paths — listener + accessibility + share — can report one
        user copy); human-speed recopies (>1.5 s apart) still broadcast.
        """
        now = protocol.now_ms()
        h = self._hash(text)
        with self._lock:
            if now > self._suppress_until_ms:
                self._suppress_hash = None  # expired
            if (self._suppress_hash is not None
                    and h == self._suppress_hash):
                self._last_text = text  # echo swallowed, suppress stays armed
                return False
            self._suppress_hash = None
            if h == self._last_sent_hash and (now - self._last_sent_ts) < protocol.CONFLICT_WINDOW_MS:
                self._last_text = text  # duplicate capture path, coalesce
                return False
            self._last_text = text
            self._last_sent_hash = h
            self._last_sent_ts = now
        self.on_local_change(text)
        return True
