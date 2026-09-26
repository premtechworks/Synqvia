"""RFCOMM server (Linux side). stdlib AF_BLUETOOTH + optional PyBluez SDP advertise."""
import logging
import socket
import threading
import time

from . import protocol

log = logging.getLogger("myclipsync.bt")
OUTBOX_CAP = 1000

HAVE_PYBLUEZ = False
try:
    import bluetooth  # type: ignore
    HAVE_PYBLUEZ = True
except Exception:
    HAVE_PYBLUEZ = False


class BtServer:
    def __init__(self, device_id: str, device_name: str, on_clip, on_status,
                 channel: int = 1):
        self.device_id = device_id
        self.device_name = device_name
        self.on_clip = on_clip          # fn(msg dict)
        self.on_status = on_status      # fn(connected: bool, peer: str)
        self.channel = channel
        self._stop = threading.Event()
        self._conn = None
        self._conn_lock = threading.Lock()
        self._outbox: list[dict] = []   # unacked clips
        self._outbox_lock = threading.Lock()
        self._server_sock = None

    # ---- sending ----
    def queue_send(self, msg: dict):
        with self._outbox_lock:
            if msg.get("type") == "clip":
                self._outbox.append(msg)
                # Bound memory: drop oldest unacked on overflow.
                if len(self._outbox) > OUTBOX_CAP:
                    del self._outbox[:len(self._outbox) - OUTBOX_CAP]
        self._try_send(msg)

    def send_msg(self, msg: dict):
        """Public send wrapper (engine uses this)."""
        self._try_send(msg)

    def _try_send(self, msg: dict):
        with self._conn_lock:
            conn = self._conn
        if conn is None:
            return
        try:
            conn.sendall(protocol.encode(msg))
        except Exception as e:
            log.debug("send failed: %s", e)

    def ack_received(self, for_id: str):
        with self._outbox_lock:
            self._outbox = [m for m in self._outbox if m.get("id") != for_id]

    def _flush_outbox(self):
        with self._outbox_lock:
            pending = sorted(list(self._outbox), key=lambda m: m.get("ts", 0))
        for m in pending:
            self._try_send(m)

    # ---- main loop ----
    def start(self):
        threading.Thread(target=self._serve_forever, daemon=True).start()

    def stop(self):
        self._stop.set()
        try:
            with self._conn_lock:
                if self._conn:
                    try:
                        self._conn.sendall(
                            protocol.encode(protocol.make_bye(self.device_id, "shutdown")))
                    except Exception:
                        pass
                    self._conn.close()
        except Exception:
            pass
        try:
            if self._server_sock:
                self._server_sock.close()
        except Exception:
            pass

    def _make_server_socket(self):
        if HAVE_PYBLUEZ:
            s = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
            s.bind(("", self.channel))
            s.listen(1)
            try:
                bluetooth.advertise_service(
                    s, "MyClipSync",
                    service_id=protocol.SERVICE_UUID,
                    service_classes=[protocol.SERVICE_UUID],
                    profiles=[bluetooth.SERIAL_PORT_PROFILE])
            except Exception:
                pass
            return s
        # stdlib fallback (SDP advertise NOT done — document: install pybluez)
        s = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM,
                          socket.BTPROTO_RFCOMM)
        s.bind((getattr(socket, "BDADDR_ANY", "00:00:00:00:00:00"), self.channel))
        s.listen(1)
        return s

    def _serve_forever(self):
        try:
            self._server_sock = self._make_server_socket()
        except Exception as e:
            self.on_status(False, f"BT bind failed: {e}")
            return
        self.on_status(False, "listening")
        while not self._stop.is_set():
            try:
                self._server_sock.settimeout(2.0)
                try:
                    conn, _ = self._server_sock.accept()
                except socket.timeout:
                    continue
                self._handle_conn(conn)
            except Exception:
                time.sleep(1)
                continue

    def _handle_conn(self, conn):
        with self._conn_lock:
            if self._conn is not None:  # duplicate: keep first
                try:
                    conn.sendall(protocol.encode(
                        protocol.make_bye(self.device_id, "duplicate")))
                    conn.close()
                except Exception:
                    pass
                return
            self._conn = conn
        try:
            conn.sendall(protocol.encode(
                protocol.make_hello(self.device_id, self.device_name)))
        except Exception:
            pass
        self.on_status(True, "connected")
        self._flush_outbox()
        buf = bytearray()
        peer = "connected"
        try:
            while not self._stop.is_set():
                chunk = conn.recv(4096)
                if not chunk:
                    break
                buf += chunk
                while True:
                    try:
                        msg, _ = protocol.decode_one(buf)
                    except ValueError as ve:
                        # Oversize/corrupt frame per PROTOCOL.md: drop connection.
                        log.warning("bad frame (%s), dropping connection", ve)
                        try:
                            conn.sendall(protocol.encode(
                                protocol.make_bye(self.device_id, "version")))
                        except Exception:
                            pass
                        raise ConnectionError("bad frame")
                    if msg is None:
                        break
                    if not protocol.validate(msg):
                        log.debug("ignoring invalid message: %r", str(msg)[:200])
                        continue
                    if msg.get("v") != protocol.PROTOCOL_VERSION:
                        try:
                            conn.sendall(protocol.encode(
                                protocol.make_bye(self.device_id, "version")))
                        except Exception:
                            pass
                        raise ConnectionError("version mismatch")
                    if msg.get("type") == "hello":
                        peer = str(msg.get("name", msg.get("src", "connected")))
                        self.on_status(True, peer)
                    self.on_clip(msg)
                    # invalid messages ignored
        except Exception as e:
            log.debug("conn reader ended: %s", e)
        finally:
            with self._conn_lock:
                if self._conn is conn:
                    self._conn = None
            try:
                conn.close()
            except Exception:
                pass
            self.on_status(False, "listening")
