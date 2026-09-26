"""Framing + LWW unit tests (no Bluetooth hardware needed)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "linux"))
from myclipsync import protocol


def test_roundtrip():
    m = protocol.make_clip("linux-x", "hello ✓")
    raw = protocol.encode(m)
    buf = bytearray(raw)
    out, _ = protocol.decode_one(buf)
    assert out["text"] == "hello ✓" and len(buf) == 0


def test_split_frames():
    a = protocol.encode(protocol.make_clip("a", "one"))
    b = protocol.encode(protocol.make_clip("a", "two"))
    stream = a + b
    buf = bytearray(stream[:len(a) + 2])  # partial second frame
    m1, _ = protocol.decode_one(buf)
    assert m1["text"] == "one"
    assert protocol.decode_one(buf)[0] is None  # incomplete
    buf += stream[len(a) + 2:]
    m2, _ = protocol.decode_one(buf)
    assert m2["text"] == "two"


def test_lww():
    assert protocol.last_write_wins(100, "a", 200, "b") == "remote"
    assert protocol.last_write_wins(200, "a", 100, "b") == "local"
    assert protocol.last_write_wins(100, "a", 100, "b") == "remote"  # tie: larger src
    assert protocol.last_write_wins(100, "b", 100, "a") == "local"


def test_suppress_echo():
    from myclipsync.clipboard import ClipboardMonitor
    sent = []
    mon = ClipboardMonitor(sent.append)
    mon.apply_remote("from-phone")   # writes clipboard, arms suppress
    assert mon.feed_local("from-phone") is False  # echo swallowed
    assert sent == []
    assert mon.feed_local("fresh text") is True   # genuinely new -> broadcast
    assert sent == ["fresh text"]


def test_dedup_ids():
    from myclipsync.engine import SyncEngine
    seen = set()
    e = SyncEngine.__new__(SyncEngine)
    import collections, threading
    e._seen = collections.deque(maxlen=500); e._seen_set = set(); e._lock = threading.Lock()
    assert SyncEngine._mark_seen(e, "x") is True
    assert SyncEngine._mark_seen(e, "x") is False
