"""MyClipSync shared protocol v1: framing + message schema. Identical logic in Android Protocol.kt."""
import json
import struct
import time
import uuid
import hashlib

PROTOCOL_VERSION = 1
SERVICE_UUID = "7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002"
MAX_PAYLOAD = 2 * 1024 * 1024  # 2 MiB, text-only v1
HEADER_FMT = ">I"
HEADER_LEN = 4
CONFLICT_WINDOW_MS = 1500
SUPPRESS_WINDOW_MS = 2000

VALID_TYPES = {"hello", "clip", "ack", "bye"}


def now_ms() -> int:
    return int(time.time() * 1000)


def content_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def make_hello(src: str, name: str) -> dict:
    return {"v": 1, "type": "hello", "id": str(uuid.uuid4()),
            "src": src, "name": name, "ts": now_ms()}


def make_clip(src: str, text: str, ts: int | None = None) -> dict:
    return {"v": 1, "type": "clip", "id": str(uuid.uuid4()),
            "src": src, "ts": ts if ts is not None else now_ms(),
            "text": text, "mime": "text/plain"}


def make_ack(src: str, for_id: str) -> dict:
    return {"v": 1, "type": "ack", "id": str(uuid.uuid4()),
            "src": src, "ts": now_ms(), "for": for_id}


def make_bye(src: str, reason: str = "shutdown") -> dict:
    return {"v": 1, "type": "bye", "id": str(uuid.uuid4()),
            "src": src, "ts": now_ms(), "reason": reason}


def encode(msg: dict) -> bytes:
    payload = json.dumps(msg, ensure_ascii=False).encode("utf-8")
    if len(payload) > MAX_PAYLOAD:
        raise ValueError(f"payload {len(payload)} exceeds MAX {MAX_PAYLOAD}")
    return struct.pack(HEADER_FMT, len(payload)) + payload


def decode_one(buf: bytearray):
    """Try to pop one message from buf. Returns (msg|None, bytes_consumed)."""
    if len(buf) < HEADER_LEN:
        return None, 0
    (length,) = struct.unpack(HEADER_FMT, bytes(buf[:HEADER_LEN]))
    if length > MAX_PAYLOAD:
        raise ValueError(f"frame length {length} exceeds MAX")
    if len(buf) < HEADER_LEN + length:
        return None, 0
    payload = bytes(buf[HEADER_LEN:HEADER_LEN + length])
    msg = json.loads(payload.decode("utf-8"))
    del buf[:HEADER_LEN + length]
    return msg, HEADER_LEN + length


def validate(msg: dict) -> bool:
    if not isinstance(msg, dict):
        return False
    if msg.get("v") != 1:
        return False
    t = msg.get("type")
    if t not in VALID_TYPES:
        return False
    if not isinstance(msg.get("src"), str) or not isinstance(msg.get("id"), str):
        return False
    if len(msg.get("src", "")) > 64:
        return False
    if t == "clip":
        # mime required per schema; unknown mime still validates (receiver acks but ignores)
        return isinstance(msg.get("text"), str) and isinstance(msg.get("ts"), int) \
            and not isinstance(msg.get("ts"), bool) \
            and isinstance(msg.get("mime"), str) and isinstance(msg.get("id"), str)
    if t == "ack":
        return isinstance(msg.get("for"), str)
    if t == "bye":
        return isinstance(msg.get("reason"), str)
    if t == "hello":
        return isinstance(msg.get("name"), str)
    return True


def last_write_wins(local_ts: int, local_src: str, remote_ts: int, remote_src: str) -> str:
    """Return 'local' or 'remote' winner. Higher ts wins; tie -> larger src wins."""
    if remote_ts != local_ts:
        return "remote" if remote_ts > local_ts else "local"
    return "remote" if remote_src > local_src else "local"
