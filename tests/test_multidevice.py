"""Unit tests for multi-device Bluetooth server and sync engine."""
import os
import sys
import threading
import time

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "linux"))
from synqvia import protocol
from synqvia.bt import BtServer, ClientConnection
from synqvia.engine import SyncEngine


class DummySocket:
    def __init__(self):
        self.sent = bytearray()
        self.closed = False
        self.lock = threading.Lock()

    def sendall(self, data):
        if self.closed:
            raise OSError("Socket closed")
        with self.lock:
            self.sent += data

    def close(self):
        self.closed = True

    def get_sent_messages(self):
        with self.lock:
            buf = bytearray(self.sent)
        msgs = []
        while True:
            msg, _ = protocol.decode_one(buf)
            if msg is None:
                break
            msgs.append(msg)
        return msgs


class DummyHistory:
    def __init__(self):
        self.records = []

    def insert(self, mid, text, ts, src, direction):
        self.records.append((mid, text, ts, src, direction))
        return True

    def exists(self, mid):
        return any(r[0] == mid for r in self.records)


class DummyClipboard:
    def __init__(self):
        self.applied = []

    def apply_remote(self, text):
        self.applied.append(text)


def test_client_connection_basic():
    sock = DummySocket()
    client = ClientConnection(sock, ("11:22:33:44:55:66", 1))
    client.peer_id = "android-dev1"
    client.peer_name = "OnePlus 7"

    assert client.display_name() == "OnePlus 7"
    hello = protocol.make_hello("linux-pc", "Linux PC")
    assert client.send_frame(protocol.encode(hello)) is True
    sent_msgs = sock.get_sent_messages()
    assert len(sent_msgs) == 1
    assert sent_msgs[0]["type"] == "hello"

    client.close()
    assert sock.closed is True
    assert client.send_frame(b"test") is False


def test_multi_device_broadcast_and_status():
    status_updates = []
    received_clips = []

    server = BtServer(
        device_id="linux-pc",
        device_name="Linux PC",
        on_clip=received_clips.append,
        on_status=lambda connected, peer: status_updates.append((connected, peer)),
        channel=1,
    )

    # Initially 0 clients
    server._notify_status()
    assert status_updates[-1] == (False, "listening")

    # Client 1 connects (OnePlus 7)
    sock1 = DummySocket()
    c1 = ClientConnection(sock1, ("AA:BB:CC:DD:EE:01", 1))
    c1.peer_id = "android-op7"
    c1.peer_name = "OnePlus 7"
    server._clients[id(c1)] = c1
    server._notify_status()
    assert status_updates[-1] == (True, "OnePlus 7")

    # Client 2 connects (Galaxy Tab A7)
    sock2 = DummySocket()
    c2 = ClientConnection(sock2, ("AA:BB:CC:DD:EE:02", 1))
    c2.peer_id = "android-tab"
    c2.peer_name = "Galaxy Tab A7"
    server._clients[id(c2)] = c2
    server._notify_status()
    assert status_updates[-1] == (True, "2 devices: OnePlus 7, Galaxy Tab A7")

    # Local copy on Linux broadcasts to BOTH devices
    local_clip = protocol.make_clip("linux-pc", "Hello both phones!")
    server.queue_send(local_clip)

    msgs1 = sock1.get_sent_messages()
    msgs2 = sock2.get_sent_messages()
    assert any(m.get("text") == "Hello both phones!" for m in msgs1)
    assert any(m.get("text") == "Hello both phones!" for m in msgs2)

    # Disconnect Client 1
    c1.close()
    server._clients.pop(id(c1))
    server._notify_status()
    assert status_updates[-1] == (True, "Galaxy Tab A7")

    # Disconnect Client 2
    c2.close()
    server._clients.pop(id(c2))
    server._notify_status()
    assert status_updates[-1] == (False, "listening")


def test_cross_device_relay():
    history = DummyHistory()
    clipboard = DummyClipboard()
    status_updates = []

    server = BtServer(
        device_id="linux-pc",
        device_name="Linux PC",
        on_clip=lambda msg: None,
        on_status=lambda connected, peer: status_updates.append((connected, peer)),
        channel=1,
    )
    engine = SyncEngine("linux-pc", history, clipboard, server)
    server.on_clip = engine.on_remote_msg

    sock1 = DummySocket()
    c1 = ClientConnection(sock1, ("AA:BB:CC:DD:EE:01", 1))
    c1.peer_id = "android-op7"
    c1.peer_name = "OnePlus 7"
    server._clients[id(c1)] = c1

    sock2 = DummySocket()
    c2 = ClientConnection(sock2, ("AA:BB:CC:DD:EE:02", 1))
    c2.peer_id = "android-tab"
    c2.peer_name = "Galaxy Tab A7"
    server._clients[id(c2)] = c2

    clip_from_op7 = protocol.make_clip("android-op7", "Copied on OnePlus 7")
    engine.on_remote_msg(clip_from_op7)

    assert "Copied on OnePlus 7" in clipboard.applied
    assert any(r[1] == "Copied on OnePlus 7" and r[3] == "android-op7" for r in history.records)

    msgs1 = sock1.get_sent_messages()
    acks_for_c1 = [m for m in msgs1 if m.get("type") == "ack" and m.get("for") == clip_from_op7["id"]]
    assert len(acks_for_c1) == 1

    msgs2 = sock2.get_sent_messages()
    relayed_to_c2 = [m for m in msgs2 if m.get("type") == "clip" and m.get("text") == "Copied on OnePlus 7"]
    assert len(relayed_to_c2) == 1
    assert relayed_to_c2[0]["src"] == "android-op7"

    relayed_to_c1 = [m for m in msgs1 if m.get("type") == "clip" and m.get("text") == "Copied on OnePlus 7"]
    assert len(relayed_to_c1) == 0


def test_reconnection_cleans_stale_connection():
    server = BtServer(
        device_id="linux-pc",
        device_name="Linux PC",
        on_clip=lambda msg: None,
        on_status=lambda c, p: None,
        channel=1,
    )

    sock_old = DummySocket()
    c_old = ClientConnection(sock_old, ("AA:BB:CC:DD:EE:01", 1))
    c_old.peer_id = "android-op7"
    c_old.peer_name = "OnePlus 7"
    server._clients[id(c_old)] = c_old

    sock_new = DummySocket()
    c_new = ClientConnection(sock_new, ("AA:BB:CC:DD:EE:01", 2))
    server._clients[id(c_new)] = c_new

    with server._clients_lock:
        stale_clients = [c for c in server._clients.values() if c is not c_new and c.peer_id == "android-op7"]
    for stale in stale_clients:
        stale.close(protocol.make_bye(server.device_id, "duplicate"))

    assert sock_old.closed is True
    assert c_new.connected is True
    old_msgs = sock_old.get_sent_messages()
    assert any(m.get("type") == "bye" and m.get("reason") == "duplicate" for m in old_msgs)


if __name__ == "__main__":
    test_client_connection_basic()
    test_multi_device_broadcast_and_status()
    test_cross_device_relay()
    test_reconnection_cleans_stale_connection()
    print("All multi-device tests passed successfully!")
