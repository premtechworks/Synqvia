# Synqvia — Wire Protocol v1 (RFCOMM/SPP)

Transport: **Bluetooth Classic RFCOMM/SPP**, persistent bidirectional stream.
No BLE. No cloud. Devices must already be OS-level bonded. No in-app pairing UI.

## 1. Roles

| Side | Role | Behavior |
|------|------|----------|
| Linux PC | **RFCOMM server (listener)** | Binds channel, advertises SPP UUID via SDP, `accept()` loop. Optionally also attempts outbound connect to `REMOTE_MAC` if configured (helps when Android also listens). |
| Android | **RFCOMM client (initiator)** | `createRfcommSocketToServiceRecord(MAC, UUID)` to Linux MAC, retry with backoff. Cancels discovery before connect. |

This single-connection model avoids duplicate sockets. If both sides ever listen,
the Android-client → Linux-server direction wins; any second inbound socket is
closed with a `bye` reason `duplicate`.

## 2. Service UUID

Custom 128-bit UUID reserved for this app (do NOT reuse generic `00001101-...`):

```
7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002
```

Both sides must use exactly this UUID for SDP lookup / socket creation.
Linux advertises it; Android connects to it.

Configuration needed per install:
- Linux: own adapter MAC (auto), peer `REMOTE_MAC` optional (for status display only in server-only mode).
- Android: peer Linux MAC stored in SharedPreferences (`pc_mac`, uppercase `AA:BB:CC:DD:EE:FF`), entered once in app UI. UUID is hardcoded.

HOW TO GET MAC:
- Linux: `bluetoothctl show | grep Controller` or `hciconfig`.
- Android: Settings → About phone → Bluetooth address (or `bluetoothctl devices` on Linux after pairing shows phone MAC).

## 3. Framing

Stream is TCP-like with no message boundaries → **4-byte length prefix**.

```
+------------------+---------------------------+
| LEN (4B, BE u32) | PAYLOAD (LEN bytes, UTF-8 JSON) |
+------------------+---------------------------+
```

- `LEN` = number of bytes of JSON payload, `struct.pack('>I', len)`, max 2 MiB (text-only v1).
- Sender MUST send full `LEN + PAYLOAD` in one logical write (loop `sendall`).
- Receiver MUST loop `recv` until 4 bytes, decode LEN, then loop until LEN bytes, then parse.
- Back-to-back messages on one socket are allowed. Partial reads must be buffered.
- If LEN > MAX (2 MiB), receiver MUST drop connection and log (protects memory).

## 4. Message schema (JSON, UTF-8)

All messages: `{"v":1,"type":..., ...}`. `v` = protocol version (int, currently 1).

### 4.1 `hello` — handshake, first message both directions after connect

```json
{"v":1,"type":"hello","id":"<uuid4>","src":"<device_id>","name":"<human name>","ts":1690000000000}
```

- `src`: stable device id, e.g. `linux-<hostname>` or `android-<ANDROID_ID>` (string, ≤64 chars).
- `ts`: unix millis (wall clock). Used only for logging, NOT for conflict resolution of clips.
- On receipt, peer records `peer_id`, `peer_name`. No `ack` required. If `v` mismatch → send `bye` + close.

### 4.2 `clip` — clipboard content (the only v1 data message)

```json
{"v":1,"type":"clip","id":"<uuid4>","src":"<device_id>","ts":1690000000123,"text":"...","mime":"text/plain"}
```

- `id`: uuid4 per copy event, used for dedup.
- `ts`: unix millis at moment of local copy (source wall clock). Used for last-write-wins.
- `text`: clipboard UTF-8 string. Empty string is valid (clipboard cleared) and IS synced.
- `mime`: always `text/plain` in v1. Future: `image/png`, etc. (receivers MUST ignore unknown mime, still ack).

### 4.3 `ack` — optional receipt (reliability / no-loss on reconnect)

```json
{"v":1,"type":"ack","id":"<uuid4>","src":"<device_id>","ts":1690000000456,"for":"<clip id>"}
```

- Sender of `clip` keeps it in an outbox queue until `ack` or socket drop. On reconnect, unsent/unacked clips are re-sent in ts order (dedup by `id` on receiver).
- Receiver sends `ack` immediately after persisting + applying to local clipboard.

### 4.4 `bye` — graceful close

```json
{"v":1,"type":"bye","id":"<uuid4>","src":"<device_id>","ts":1690000000999,"reason":"shutdown|duplicate|version"}
```

## 5. Connection lifecycle

- Linux server: `bind(channel 1..30, auto) → advertise UUID → accept()` in loop. On client drop, log, close, re-accept immediately.
- Android client: connect with backoff 2s → 5s → 10s → 30s (cap), reset on success. Cancel discovery before connect. Hold socket in foreground service thread; reader loop blocking on framing.
- Keepalive: none in v1 (RFCOMM is reliable; socket drop = disconnect). Either side may close idle socket after 24h and reconnect — not required.
- Offline: server keeps listening; client keeps retrying. Both show "Offline — waiting for peer". No data loss: unsent clips stay in outbox + history; flushed on reconnect.

## 6. Conflict handling: last-write-wins + loop prevention

Problem: A device must not re-broadcast content it just received (ping-pong loop).

Rules (identical on both apps):

1. **Suppress-on-apply**: when applying a remote `clip` to the local clipboard, set `suppress_next = hash(text)` (plus `suppress_until = now + 2s`). The clipboard monitor, on next local change, if `hash(new_text) == suppress_next` and within window, treats it as echo of remote apply → does NOT broadcast, clears flag, still records in history as `remote` (dedup by msg `id`).
2. **Self-echo dedup**: never send a `clip` whose `hash(text)` equals the last received hash within 2s AND whose source was remote.
3. **Seen-id cache**: keep last 500 msg `id`s; drop duplicates without applying or rebroadcasting (handles retransmit after reconnect).
4. **Last-write-wins (LWW)**: conflict window `CONFLICT_WINDOW_MS = 1500`. If local clipboard changes while a remote `clip` arrived within the last 1500 ms (i.e. near-simultaneous copies on both devices):
   - Compare `ts` (source wall-clock millis). Larger `ts` wins and is the surviving clipboard on BOTH devices (loser overwrites its local clipboard with winner when winner's message arrives/applies).
   - Tie-break: lexicographically larger `src` wins (deterministic).
   - Loser's text is NOT lost: it remains in local history (flagged `conflict_loser=1`) but is not rebroadcast.
   - Clock skew note: wall clocks may differ by seconds. LWW is best-effort; tie-break keeps determinism. Future: exchange hello timestamps for skew estimate (not in v1).
5. **Outbox**: local clips queued with `ack` tracking; on reconnect resend unacked in `ts` order. Receiver dedups by `id`.

Pseudo (both sides):

```
on_local_clipboard(text):
  if suppress_active and hash(text)==suppress_hash: clear_suppress(); return  # echo, don't send
  if msg_id_seen(hash): return
  entry = history.insert(text, dir=local, ts=now())
  enqueue_outbox(entry); try_send(entry)

on_remote_clip(msg):
  if msg.id in seen: send_ack(msg); return
  mark_seen(msg.id)
  history.insert(text, dir=remote, ts=msg.ts, id=msg.id)
  if now()-last_local_ts < CONFLICT_WINDOW and pending_local:
     winner = lww(local, msg)  # by (ts, src)
     if winner is local: keep local clipboard, still ack remote, do NOT overwrite local
     else: apply_to_clipboard(msg.text, suppress=True)
  else: apply_to_clipboard(msg.text, suppress=True)
  send_ack(msg)
```

## 7. Example byte stream

```
HELLO linux→android: 00 00 00 7B {"v":1,"type":"hello",...}
HELLO android→linux: 00 00 00 7B {"v":1,...}
CLIP  linux→android: 00 00 00 A4 {"v":1,"type":"clip","id":"...","src":"linux-x1","ts":...,"text":"hello","mime":"text/plain"}
ACK   android→linux: 00 00 00 5C {"v":1,"type":"ack",...,"for":"..."}
```

## 8. Future extension (NOT in v1)

Images/files: new `mime` values + chunked `clip_chunk {id, seq, total, bytes_b64}` messages reassembled by `id`, same framing (raise MAX to 25 MB), same LWW/suppress logic keyed on content hash. No protocol version bump needed if `mime` handling already ignores unknown types.
