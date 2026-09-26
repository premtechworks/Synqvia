"""RFCOMM server (Linux side). stdlib AF_BLUETOOTH + optional PyBluez SDP advertise.
Supports multiple concurrent connected Bluetooth devices.
"""
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


class ClientConnection:
    """Represents an active client Bluetooth RFCOMM connection."""

    def __init__(self, conn, addr):
        self.conn = conn
        self.addr = addr
        self.peer_id: str | None = None
        self.peer_name: str | None = None
        self.lock = threading.Lock()
        self.connected = True
        self.connected_at = time.time()

    def send_frame(self, data: bytes) -> bool:
        if not self.connected:
            return False
        with self.lock:
            try:
                self.conn.sendall(data)
                return True
            except Exception as e:
                log.debug("send_frame failed to %s: %s", self.display_name(), e)
                self.connected = False
                return False

    def close(self, bye_msg: dict | None = None):
        self.connected = False
        if bye_msg:
            try:
                with self.lock:
                    self.conn.sendall(protocol.encode(bye_msg))
            except Exception:
                pass
        try:
            self.conn.close()
        except Exception:
            pass

    def display_name(self) -> str:
        return self.peer_name or self.peer_id or str(self.addr)


class BtServer:
    def __init__(self, device_id: str, device_name: str, on_clip, on_status,
                 channel: int = 1):
        self.device_id = device_id
        self.device_name = device_name
        self.on_clip = on_clip          # fn(msg dict)
        self.on_status = on_status      # fn(connected: bool, peer: str)
        self.channel = channel
        self._stop = threading.Event()
        self._clients: dict[int, ClientConnection] = {}
        self._clients_lock = threading.Lock()
        self._outbox: list[dict] = []   # unacked clips
        self._outbox_lock = threading.Lock()
        self._server_sock = None

    # ---- multi-client accessors ----
    def get_connected_clients(self) -> list[ClientConnection]:
        with self._clients_lock:
            return [c for c in self._clients.values() if c.connected]

    def _notify_status(self):
        clients = self.get_connected_clients()
        if not clients:
            self.on_status(False, "listening")
            return

        names = [c.peer_name or c.peer_id or str(c.addr) for c in clients]
        if len(clients) == 1:
            self.on_status(True, names[0])
        else:
            self.on_status(True, f"{len(clients)} devices: {', '.join(names)}")

    # ---- sending ----
    def queue_send(self, msg: dict):
        with self._outbox_lock:
            if msg.get("type") == "clip":
                self._outbox.append(msg)
                # Bound memory: drop oldest unacked on overflow.
                if len(self._outbox) > OUTBOX_CAP:
                    del self._outbox[:len(self._outbox) - OUTBOX_CAP]
        self.broadcast_msg(msg)

    def send_msg(self, msg: dict, target_peer_id: str | None = None):
        """Public send wrapper (engine uses this for acks, bye, etc.)."""
        if target_peer_id:
            data = protocol.encode(msg)
            with self._clients_lock:
                targets = [c for c in self._clients.values() if c.peer_id == target_peer_id and c.connected]
            if targets:
                for t in targets:
                    t.send_frame(data)
                return
        # If no specific target or target not found, broadcast to all active clients
        self.broadcast_msg(msg)

    def _try_send(self, msg: dict):
        """Backwards compatibility alias for send_msg."""
        self.send_msg(msg)

    def broadcast_clip(self, msg: dict, exclude_peer_id: str | None = None):
        """Relay remote clip to all other connected Bluetooth clients."""
        self.broadcast_msg(msg, exclude_peer_id=exclude_peer_id)

    def broadcast_msg(self, msg: dict, exclude_peer_id: str | None = None):
        try:
            data = protocol.encode(msg)
        except Exception as e:
            log.warning("failed to encode message: %s", e)
            return

        with self._clients_lock:
            clients = list(self._clients.values())

        for client in clients:
            if exclude_peer_id and client.peer_id == exclude_peer_id:
                continue
            client.send_frame(data)

    def ack_received(self, for_id: str):
        with self._outbox_lock:
            self._outbox = [m for m in self._outbox if m.get("id") != for_id]

    def _flush_outbox_for_client(self, client: ClientConnection):
        with self._outbox_lock:
            pending = sorted(list(self._outbox), key=lambda m: m.get("ts", 0))
        for m in pending:
            try:
                client.send_frame(protocol.encode(m))
            except Exception:
                pass

    # ---- main loop ----
    def start(self):
        threading.Thread(target=self._serve_forever, daemon=True, name="BtServer").start()

    def stop(self):
        self._stop.set()
        bye_msg = protocol.make_bye(self.device_id, "shutdown")
        with self._clients_lock:
            clients = list(self._clients.values())
            self._clients.clear()
        for client in clients:
            client.close(bye_msg)
        try:
            if self._server_sock:
                self._server_sock.close()
        except Exception:
            pass
        self._notify_status()

    def _make_server_socket(self):
        if HAVE_PYBLUEZ:
            s = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
            s.bind(("", self.channel))
            s.listen(10)
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
        s.listen(10)
        return s

    def _serve_forever(self):
        try:
            self._server_sock = self._make_server_socket()
        except Exception as e:
            log.error("BT bind failed on channel %s: %s", self.channel, e)
            self.on_status(False, f"BT bind failed: {e}")
            return
        self.on_status(False, "listening")
        log.info("BT server listening on channel %s (multi-device enabled)", self.channel)
        while not self._stop.is_set():
            try:
                self._server_sock.settimeout(2.0)
                try:
                    conn, addr = self._server_sock.accept()
                except socket.timeout:
                    continue
                except OSError:
                    if self._stop.is_set():
                        break
                    time.sleep(0.5)
                    continue

                log.info("accepted connection from %s", addr)
                client = ClientConnection(conn, addr)
                t = threading.Thread(
                    target=self._handle_client,
                    args=(client,),
                    daemon=True,
                    name=f"BtClient-{addr}"
                )
                t.start()
            except Exception as e:
                if not self._stop.is_set():
                    log.warning("accept loop error: %s", e)
                    time.sleep(1)
                continue

    def _handle_client(self, client: ClientConnection):
        client_key = id(client)
        with self._clients_lock:
            self._clients[client_key] = client
        self._notify_status()

        # Send our Hello to the newly connected peer
        try:
            client.send_frame(protocol.encode(
                protocol.make_hello(self.device_id, self.device_name)))
        except Exception as e:
            log.debug("initial hello send failed: %s", e)

        # Flush pending outbox to this peer
        self._flush_outbox_for_client(client)

        buf = bytearray()
        try:
            while not self._stop.is_set() and client.connected:
                chunk = client.conn.recv(4096)
                if not chunk:
                    break
                buf += chunk
                while True:
                    try:
                        msg, _ = protocol.decode_one(buf)
                    except ValueError as ve:
                        log.warning("bad frame (%s) from %s, dropping connection", ve, client.display_name())
                        client.close(protocol.make_bye(self.device_id, "version"))
                        raise ConnectionError("bad frame")
                    if msg is None:
                        break
                    if not protocol.validate(msg):
                        log.debug("ignoring invalid message: %r", str(msg)[:200])
                        continue
                    if msg.get("v") != protocol.PROTOCOL_VERSION:
                        client.close(protocol.make_bye(self.device_id, "version"))
                        raise ConnectionError("version mismatch")

                    msg_type = msg.get("type")
                    if msg_type == "hello":
                        peer_id = str(msg.get("src", ""))
                        peer_name = str(msg.get("name", peer_id))
                        client.peer_id = peer_id
                        client.peer_name = peer_name
                        log.info("client identified: %s (%s)", peer_name, peer_id)

                        # Check if another stale connection exists for this exact peer_id
                        with self._clients_lock:
                            stale_clients = [
                                c for c in self._clients.values()
                                if c is not client and c.peer_id == peer_id
                            ]
                        for stale in stale_clients:
                            log.info("closing stale duplicate connection for %s", peer_id)
                            stale.close(protocol.make_bye(self.device_id, "duplicate"))

                        self._notify_status()
                        self._flush_outbox_for_client(client)

                    elif msg_type == "ack":
                        self.ack_received(msg.get("for", ""))

                    # Pass message to engine (clips, acks, etc.)
                    self.on_clip(msg)

        except Exception as e:
            log.debug("conn reader ended (%s): %s", client.display_name(), e)
        finally:
            with self._clients_lock:
                self._clients.pop(client_key, None)
            client.close()
            log.info("client disconnected: %s", client.display_name())
            self._notify_status()
