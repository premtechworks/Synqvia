# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Synqvia syncs the clipboard between an Android phone and a Linux PC over **Bluetooth Classic RFCOMM/SPP** — no cloud, no BLE, no LAN. Two independent implementations (Kotlin app, Python daemon) must stay byte-compatible across the wire protocol. `PROTOCOL.md` is the normative spec; treat it as the source of truth whenever the two sides disagree.

Roles are asymmetric: **Linux is the RFCOMM server/listener**, **Android is the client/initiator**. Custom 128-bit UUID `7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002` (hardcoded in both `Protocol.kt` and `protocol.py` — change both together). Devices must already be OS-bluetooth-paired; there is no in-app pairing.

## Commands

```bash
# Android (JDK 17 required; Gradle wrapper 9.3.1, AGP 9.1.1, compileSdk 36 / target 34 / min 26)
cd android
./gradlew test                     # all Robolectric + JVM unit tests
./gradlew testDebugUnitTest        # debug variant only
./gradlew testDebugUnitTest --tests "com.github.premtechworks.synqvia.ProtocolUnitTest"
./gradlew testDebugUnitTest --tests "*RetryPolicyTest.*backoff*"
./gradlew assembleDebug            # debug APK (signs with android/debug.keystore)
./gradlew assembleRelease bundleRelease   # needs signing config, see below
./gradlew lint                     # lint is configured non-fatal (abortOnError = false)

# Linux daemon (Python 3.10+)
python3 -m pytest tests/ -v                                  # from repo root
python3 -m pytest tests/test_multidevice.py::test_cross_device_relay -v
python3 tests/test_multidevice.py                             # file also has a __main__ runner

# Linux daemon runtime
python3 -m synqvia --show            # from linux/; GUI daemon, --show opens window at start
python3 -m synqvia-cli status --json  # headless CLI over the same SQLite DB + config
python3 -m synqvia-cli send "text"     # spools a clip for the running daemon to flush
```

`tests/` at repo root holds the real Python tests. `linux/tests/test_protocol.py` and `linux/tests/test_history.py` are **empty (0-byte) placeholders** — don't assume coverage there.

### Android signing
The `release` signing config resolves keystore path/passwords from env vars (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) falling back to `android/keystore.properties`, then to `android/synqvia-release.jks`. All of these are gitignored. `.env` feeds the Secrets Gradle plugin; `googleServices` runs with `MissingGoogleServicesStrategy.WARN` so `google-services.json` is optional. The app declares Firebase/Retrofit/OkHttp/Moshi deps but no source uses them — the clipboard path is pure Android framework + Room.

## Architecture

### Android (`android/app/src/main/java/com/github/premtechworks/synqvia/`)

Capture is **normalized through one funnel**: every input pathway calls `ClipboardCaptureManager`, which is the only thing that touches `ClipboardManager`, dedupes, and writes to the Room database.

```
IME / Share / ProcessText / Trampoline / Tile / Notification / Accessibility
        → ClipboardCaptureManager   (echo suppression, LWW gate, sensitive flag)
        → ClipRepository (Room, prunes to historyCap)
        → ClipSyncService (foreground; RFCOMM client + outbox)
```

- **`clipboard/ClipboardCaptureManager.kt`** — the funnel. Arms SHA-256 echo suppression *before* writing a remote clip to the clipboard (`SUPPRESS_WINDOW_MS` 2000ms), coalesces the same text arriving via two pathways within `CONFLICT_WINDOW_MS` (1500ms), and fires an outbound callback only when `isExplicit || syncPreferences.autoSync`.
- **`service/ClipSyncService.kt`** — foreground service (channel `synqvia_channel`, NOTIFICATION_ID 1001, type `connectedDevice|dataSync`). Holds the reconnect loop, the in-memory outbox (cap `MAX_OUTBOX_SIZE` 1000, dropped oldest-first), and `Protocol.StreamFramer`. Its connection state is a **companion-object static `StateFlow<SyncConnectionState>`** — the UI and IME read it without binding (`onBind` returns null). Backoff lives in `RetryPolicy.kt` (2s → 30s, ×2, 5 attempts, ±20% jitter) and only failed *connect attempts* increment the counter.
- **`ime/`** — this is a **full QWERTY keyboard**, not a paste-only IME. `SynqviaKeyboardView` is a hand-rolled `View` (three layers built programmatically: letters / symbols1 / symbols2) with key previews, hold-to-repeat backspace, and haptics. `KeyboardState.kt` holds all pure logic (shift/caps-lock, layer, enter-action resolution, grapheme-aware backspace, double-space period) with zero Android View imports — that is what makes the IME unit-testable in plain JUnit. `KeyboardActionListener.kt` is the view→service seam (9 callbacks). `SynqviaImeService` owns the `InputConnection`; 1-tap paste is `commitText(clip.text, 1)`, falling back to a clipboard write + toast when there is no connection.
- **The IME privilege is load-bearing, not cosmetic.** Android 10+ blocks background clipboard reads. `DefaultImeDetector` (reads `Settings.Secure.DEFAULT_INPUT_METHOD`) gates `ClipboardManager.OnPrimaryClipChangedListener` registration. **Continuous capture only works while Synqvia is the default IME** — when it isn't, the app must report that as an IME-privilege limitation, not a sync failure, and the explicit pathways stay functional. `ClipSyncService` holds a `ContentObserver` on the secure IME setting so switching keyboards live starts/stops monitoring.
- **`data/`** — Room (`clips`, v2 with `pinned` + `sensitive`) and `SyncPreferences`, a SharedPreferences wrapper where **every key is mirrored into a `StateFlow`** for Compose. `deviceId` is derived, not stored: `"android-" + ANDROID_ID.take(16)`. `ime_expiry_hours` defaults to 1; `0` means effectively infinite for the IME drawer.
- **`di/AppContainer.kt`** — hand-rolled DI (no Hilt). `by lazy` singletons on `DefaultAppContainer`, set on `SynqviaApp.onCreate`. Components reached outside the app process (IME, ProcessText, Trampoline) defensively fall back to constructing their own container.
- **`ui/`** — Compose Material3 with **no navigation library** (`navigationCompose` is commented out in the version catalog). `MainViewModel` hand-rolls a route stack (`AppRoutes`, `resolveStartDestination`, `navigateTo`/`navigateBack`) and `MainActivity` swaps screens via `AnimatedContent`. `ui/LiquidGlass.kt` is now just three flat design-system composables — the translucency/blur was removed.
- **Input pathway entry points** (one line each): `ShareActivity` (ACTION_SEND), `ProcessTextActivity` (PROCESS_TEXT), `TrampolineActivity` (invisible focus-grab so the QS tile may legally read the clipboard), `SyncTileService` (QS tile, prefers a fresh `SelectionCache` hit), `NotificationActionReceiver` (STOP / RECONNECT / SYNC_NOW PendingIntents), `BootReceiver` (restart unless `isUserStopped`), `ClipAccessService` (accessibility → `SelectionCache`).

### Linux (`linux/synqvia/`)

```
GTK owner-change → ClipboardMonitor → SyncEngine → History (SQLite) + BtServer.queue_send → N clients
inbound socket → BtServer._handle_client → protocol.decode_one → SyncEngine.on_remote_msg → ack + relay
```

- **`main.py`** is the composition root *and* the process-lifecycle owner: it wires `History` → `ClipboardMonitor` → `SyncEngine` → `BtServer` → `TrayApp`/`MainWindow`, holds an `fcntl.flock` single-instance guard on `~/.config/synqvia/synqvia.lock` (a second instance pokes the primary's Gtk application to raise its window), and runs a 2-second `GLib.timeout_add_seconds` spool drain. `python3 -m synqvia` and `python3 -m synqvia.cli` dispatch through here.
- **Threading**: GTK mainloop on the main thread; one daemon thread for `BtServer._serve_forever` (blocking `accept`), one `BtClient-<addr>` thread per connection, plus a clipboard polling thread only when headless. **All UI mutation from a background thread must go through `GLib.idle_add`** — clipboard writes, window refresh, tray status, log bridging. SQLite is serialized by `History._lock`.
- **`bt.py`** holds N `ClientConnection`s. Identity comes from the inbound `hello` (`client.peer_id`); a new connection from an already-known `peer_id` closes the old one with `bye` reason `duplicate`. The outbox is an in-memory FIFO pruned by `ack_received`.
- **`clipboard.py`** arms suppression for the **entire** 2s window rather than once, because GTK fires `owner-change` twice per programmatic `set_text`+`store`.
- **Multi-device relay**: `SyncEngine.on_remote_msg` acks the origin peer via `send_msg(ack, target_peer_id=...)` and then `broadcast_clip(msg, exclude_peer_id=rsrc)` so peers B..N get it and the sender's socket never sees its own clip echoed.
- **`cli.py`** talks to the same `history.db` and config without the daemon; `send` writes to `~/.config/synqvia/spool/<msg-id>.json` and the running daemon flushes it.

### State locations
Linux reads **JSON only** — `~/.config/synqvia/config.json` (`DEFAULTS = device_name=hostname, history_cap=500, channel=1`; channel clamped 1..30, cap 0 = unlimited), plus `history.db`, `synqvia.log`, `status.json`, and the `spool/` dir. `_migrate_legacy_config()` imports from `~/.config/myclipsync`. SQLite schema: `clips(id PK, text, ts, src, direction, conflict_loser)` with an index on `ts DESC`.

## Protocol invariants — change both sides together

Every change to `Protocol.kt` / `protocol.py` must land in both, and in `PROTOCOL.md`.

- **Framing**: 4-byte big-endian length prefix + UTF-8 JSON. Receivers must loop until they have all 4 bytes, then all LEN bytes; back-to-back messages on one socket are legal. LEN > 2 MiB → drop the connection and log.
- **Loop prevention** (the whole reason this design exists): suppress-on-apply (SHA-256 + 2s window), a 500-entry seen-id cache, and never rebroadcast a clip whose hash matches a just-received remote within the window.
- **LWW**: `Protocol.isLocalWinner` and `protocol.last_write_wins` must agree — larger `ts` wins, tie broken by lexicographically larger `src`. Applies only within `CONFLICT_WINDOW_MS` (1500ms). The loser is **kept in history** (`conflictLoser=true`) and never rebroadcast.
- **Outbox**: clips stay queued until `ack` or socket drop; on reconnect, unacked clips resend in `ts` order, deduped by `id`.
- Unknown `mime` values must be acked and ignored. Empty string is a valid clip (clipboard cleared) and **is** synced.

## Conventions

- `PROTOCOL.md` is normative — update it with any protocol change, and cross-reference it from code comments.
- Tests are Robolectric/JUnit on Android (`android/app/src/test/`) and pytest on Linux. `TEST_PLAN.md` maps the loop-prevention/LWW scenarios to specific test names — extend it when you add coverage. The IME privilege scenarios (Tests A–G there) describe the expected behavior of the `DefaultImeDetector` gate; preserve that gating when refactoring capture.
- Unused Gradle deps are commented out rather than deleted, deliberately — uncomment rather than re-adding a dependency entry.
- `lint` is set `abortOnError = false` and `checkReleaseBuilds = false`; it will not fail a build for you.