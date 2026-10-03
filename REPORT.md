# Synqvia — Detailed Project Report
**Bluetooth Clipboard Sync: Linux Mint Xfce ↔ Android | Text + History + Full QWERTY IME + Multi-Device Relay v1.2.0**

> Sources inspected: `README.md`, `PROTOCOL.md`, `TEST_PLAN.md`, `CLAUDE.md`, `linux/synqvia/*.py` (~1520 LOC, 10 files), `linux/build_deb.sh`, `tests/*.py` (9 pytest tests), `android/app/src/main/java/com/github/premtechworks/synqvia/**/*.kt` (`clipboard/` 310 + `ime/` 1240 + `service/` 560 + `protocol/` 222 + `data/` 390 + `ui/` 5400+ LOC), `AndroidManifest.xml`, resources (`res/layout/`, `res/drawable*/`, `res/values*/`), `android/app/src/test/java/com/github/premtechworks/synqvia/**/*.kt` (20+ test classes).
>
> **Latest changes incorporated (v1.0.0 & IME Keyboard + Theming + Multi-Device enhancements):**
> 1. **Full QWERTY Android IME Keyboard (`SynqviaKeyboardView` + `KeyboardState`):** 3-layer programmatic layout (Letters with number row hints, Symbols 1, Symbols 2), popup key preview bubbles, hold-to-repeat backspace, grapheme-aware deletion, dynamic enter actions, 44dp toolbar with 1-tap clipboard panel toggle and pinned clips filter.
> 2. **Multi-Device Bluetooth Architecture & Cross-Device Relay:** Linux `BtServer` coordinates concurrent Android clients, broadcast relays clipboard updates across devices (excluding the origin peer), and handles client lifecycle and stale connection pruning.
> 3. **Dual Theme Engine (Dark & Light Mode) & Semantic Tokens:** Complete semantic design tokens (`SynqviaColors`, `DarkSynqviaColors`, `LightSynqviaColors`, `KeyboardPalette`) with dynamic edge-to-edge system bar contrast handling and real-time theme synchronization across the app and IME keyboard.
> 4. **High-Performance History & UX Pipeline:** Background-computed date grouping (`HistoryGroupUi`), pre-warmed queries (`HistoryUiState`), zero-lag tab transitions, `TabLatencyTracker`, URL host detection with 1-tap browser launch, and undoable soft deletion.
> 5. **Centralized Debounced Haptics (`AppHaptics`):** Unified haptic feedback engine with an 80ms safety debounce window, gating across user preferences and system settings.
> 6. **Production Packaging & Signing:** Debian `.deb` package generation via `linux/build_deb.sh` using `dpkg-deb`, autostart desktop integration, and Android release keystore signing configuration (`synqvia-release.jks`).

---

## 1. Executive Summary

**Synqvia** is an offline-first, Bluetooth Classic (RFCOMM/SPP) clipboard synchronizer between Linux (e.g. Linux Mint Xfce / Ubuntu / Debian) and one or more Android devices.

* **No cloud, no BLE, no LAN/Wi-Fi dependency.** Dedicated bidirectional RFCOMM stream using custom 128-bit SPP UUID `7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002`.
* **Linux = server/listener & relay coordinator**, **Android = client/initiator**.
* **Data scope v1.0.0:** `text/plain` only + persistent history on both sides. Empty string (clear) is a valid sync event.
* **Core differentiators vs KDE Connect / Cloud Clipboards:**
  - Explicit loop prevention (`suppress-on-apply` SHA-256 + 500-entry `seen-id` cache).
  - Deterministic Last-Write-Wins (LWW) conflict resolution within a 1500ms window.
  - Multi-device cross-device relay coordinating multiple Android devices through the Linux daemon without rebroadcast loops.
  - Outbox reliability with targeted acknowledgements (`ack`).
  - Sanctioned Android 10–15+ clipboard integration: full custom QWERTY Input Method Editor (`SynqviaImeService`) acting as both primary capture and 1-tap paste drawer, compliant with modern Android background clipboard read restrictions.
* **Modern Android Decoupled Pipeline:**
  `SynqviaKeyboardView` (QWERTY + Toolbar) → `SynqviaImeService` (Foreground IME) → `ClipboardCaptureManager` (Funnel) → `ClipRepository` (Room) → `ClipSyncService` (Transport). All legacy explicit paths (`PROCESS_TEXT`, Share Sheet, Quick Settings Tile, Notification Action, Accessibility cache, Trampoline) are preserved as fallbacks and normalized through the same manager.

**Repository Layout:**

```
synqvia/
  PROTOCOL.md          # Wire spec v1 (normative)
  README.md            # User guide, architecture, installation, features
  REPORT.md            # Technical report (this document)
  TEST_PLAN.md         # Comprehensive test matrix and edge cases
  CLAUDE.md            # Developer & LLM guidance notes
  LICENSE              # GNU General Public License v3.0
  synqvia_logo.png     # Official high-resolution brand wordmark
  linux/               # Python daemon + GTK UI + CLI + Debian packaging
    build_deb.sh       # dpkg-deb packager creating standalone .deb
    install.sh         # Direct source installation script
    synqvia.desktop    # Application launcher & autostart entry
    requirements.txt   # PyGObject, PyBluez, pycairo
    synqvia/
      protocol.py  engine.py  bt.py  clipboard.py
      history.py  main.py  window.py  tray.py  cli.py
  android/             # Android application (com.github.premtechworks.synqvia)
    app/src/main/java/com/github/premtechworks/synqvia/
      clipboard/ClipboardCaptureManager.kt  SensitiveClassifier.kt  DefaultImeDetector.kt
      ime/SynqviaImeService.kt  SynqviaKeyboardView.kt  KeyboardState.kt
          KeyboardActionListener.kt  KeyboardPalette.kt  ClipImeAdapter.kt
      protocol/Protocol.kt
      data/ClipEntity.kt  ClipDao.kt  ClipRepository.kt  AppDatabase.kt  SyncPreferences.kt
      di/AppContainer.kt
      service/ClipSyncService.kt  ClipAccessService.kt  SelectionCache.kt
              SyncTileService.kt  RetryPolicy.kt
      receiver/BootReceiver.kt  NotificationActionReceiver.kt
      ui/MainActivity.kt  MainViewModel.kt  LiquidGlass.kt
      ui/theme/Theme.kt  SynqviaColors.kt  Color.kt  Type.kt
      ui/haptics/AppHaptics.kt
      ui/motion/Motion.kt  Haptics.kt
      ui/components/HeroSyncCard.kt  ClipDetailSheet.kt  ClipHistoryItem.kt
                    OneTapPathsInfoCard.kt  ScreenScaffold.kt  SynqviaComponents.kt  SynqviaLogo.kt
      ui/screens/DashboardScreen.kt  HistoryScreen.kt  HistoryUiState.kt
                 SetupScreen.kt  SettingsScreen.kt  OnboardingScreen.kt  PairScreen.kt  ImeSettingsScreen.kt
      ui/util/HistoryDateUtils.kt  UrlUtil.kt  TabLatencyTracker.kt
    app/src/main/res/
      layout/ime_keyboard_view.xml  ime_clipboard_view.xml  ime_key_preview.xml  item_ime_clip.xml
      xml/method.xml
      drawable/bg_ime_*.xml  ic_ime_*.xml  bg_app_gradient.xml  ic_synqvia_logo_mark.xml
      values/themes.xml  attrs.xml  ime_colors.xml  dimens.xml  strings.xml
      values-night/themes.xml  ime_colors.xml
    app/src/test/java/com/github/premtechworks/synqvia/   # 20+ unit test classes
  tests/               # Linux test suite
    test_protocol.py   # Protocol framing, LWW, echo suppression, dedup
    test_multidevice.py # Multi-client connection, cross-device relay, stale cleanup
```

---

## 2. PRD — Product Requirements Document

### 2.1 Problem Statement

Users working across Linux PC and Android continuously need to exchange URLs, OTPs, terminal outputs, and notes. Traditional cloud tools (Google Keep, Pushbullet, proprietary sync) demand internet connectivity, external user accounts, or open local Wi-Fi ports, leaking confidential credentials and private keys. Furthermore, Android 10+ strictly prohibits background apps from reading clipboard data (`ClipboardManager.getPrimaryClip()`). Synqvia solves this by providing direct, encrypted, offline Bluetooth synchronization paired with an integrated full QWERTY keyboard and drawer that operates fully within Android OS permissions.

### 2.2 Goals

| ID | Goal | Success Metric |
|----|------|----------------|
| G1 | Bidirectional text sync <2s when connected | `synqvia-cli send "hi"` appears on Android + clipboard in ~1s |
| G2 | No ping-pong / duplication | Exactly 1x `clip` + 1x `ack`, silent for 10s after one copy |
| G3 | Offline resilience | Copies made offline flush on reconnect in `ts` order, no loss/dup |
| G4 | History persistence + search + re-broadcast | 500-row default cap, SQLite/Room, tap-to-resend |
| G5 | Zero cloud / zero account | Works with Wi-Fi/mobile data off, Bluetooth on |
| G6 | Usable within Android OS clipboard limits | **Full QWERTY IME (primary) + 4 legacy one-tap paths + a11y/Trampoline fallbacks** |
| G7 | In-keyboard clipboard drawer with pin + expiry | Unpinned >1h hidden in IME drawer, pinned persist; sync history preserved |
| G8 | Multi-Device Bluetooth Coordination | Linux server connects multiple Android phones; phone A copy relays to phone B without loopback |
| G9 | Full QWERTY Keyboard with dynamic actions | Standalone input method with letters, numbers, symbols, repeat backspace, key preview, and enter mapping |
| G10 | Adaptive Dual Theme System | Seamless Light and Dark modes with semantic design tokens and dynamic status/navigation bar contrast |

### 2.3 Non-Goals (v1.0.0 explicitly out)

* Images, files, rich HTML. Reserved for future `clip_chunk{id,seq,total,bytes_b64}` (25MB max payload documented in `PROTOCOL.md`).
* In-app Bluetooth pairing. Devices must first be paired at the OS level (`bluetoothctl` / Android Settings).
* E2E application-layer encryption beyond Bluetooth bonding (relies on Classic Bluetooth link encryption).
* iOS and Windows platforms. Focus remains tightly on Linux and Android.
* Clock-skew correction. LWW is best-effort on wall-clock milliseconds.

### 2.4 Target Personas

1. **Linux Developer / Power User:** Frequently copies code, git hashes, and terminal logs to mobile devices.
2. **Privacy-Conscious User:** Refuses cloud relays for credentials, OTPs, and private messages.
3. **Field / Offline Professional:** Operates in air-gapped or restricted networking environments without local Wi-Fi.
4. **Mobile Power User:** Relies on the Synqvia keyboard as their daily driver or quick-access input method for instantaneous clipboard recall.

### 2.5 User Stories

* As a PC user, I copy text → it appears on all connected Android devices and their system clipboards automatically.
* As an Android user, I type on the Synqvia keyboard, tap the clipboard icon → my recent snippets appear; 1-tap pastes into the input field.
* As an Android user on Phone A, I copy text → it syncs to my Linux PC and automatically relays to Phone B.
* As an Android user, I toggle Dark/Light theme → the entire app and the active IME keyboard instantly adapt their palettes and system bar insets.
* As a user, I work offline on both devices → upon reconnection, all clips merge deterministically in timestamp order.

### 2.6 Functional Requirements

| ID | Requirement | Implementation |
|----|-------------|----------------|
| FR1 | RFCOMM server on Linux, client on Android | `linux/synqvia/bt.py`, `service/ClipSyncService.kt` |
| FR2 | 4-byte BE length-prefixed JSON framing, 2MiB max | `protocol.py`, `protocol/Protocol.kt` |
| FR3 | `hello/clip/ack/bye` schema, `v=1` | `protocol.py`, `PROTOCOL.md` |
| FR4 | Suppress-on-apply loop prevention (SHA-256, 2000ms) | `clipboard.py`, `clipboard/ClipboardCaptureManager.kt` |
| FR5 | Seen-ID dedup (500) + DB restart-proof dedup | `engine.py`, `ClipSyncService.kt` |
| FR6 | Deterministic LWW (1500ms window, `(ts, src)` ordering) | `protocol.py`, `Protocol.kt` |
| FR7 | Ack-tracked outbox, timestamp-ordered flush | `bt.py`, `ClipSyncService.kt` |
| FR8 | Storage history cap (default 500), search, clear | `history.py`, `ClipDao.kt` |
| FR9 | Android capture pathways (IME primary + fallbacks) | `SynqviaImeService.kt`, `ClipboardCaptureManager.kt` |
| FR10 | Status visibility across desktop & mobile UI | GTK header, tray tooltip, notification, CLI, Compose UI |
| FR11 | Shared capture pipeline normalizing all inputs | `ClipboardCaptureManager.kt` |
| FR12 | Pin / unpin, delete, configurable drawer expiry | `ClipEntity.kt`, `ClipDao.kt`, `SyncPreferences.kt` |
| FR13 | Sensitive classification + masking (`EXTRA_IS_SENSITIVE`) | `SensitiveClassifier.kt`, `ClipImeAdapter.kt` |
| FR14 | Full QWERTY keyboard with layers and previews | `SynqviaKeyboardView.kt`, `KeyboardState.kt` |
| FR15 | Multi-device Bluetooth coordination & relay | `BtServer.py`, `SyncEngine.py` (`broadcast_clip(exclude_peer_id)`) |
| FR16 | Semantic design tokens & dual Light/Dark themes | `SynqviaColors.kt`, `KeyboardPalette.kt`, `Theme.kt` |
| FR17 | Off-thread history grouping & pre-warmed UI state | `HistoryUiState.kt`, `HistoryDateUtils.kt`, `MainViewModel.kt` |
| FR18 | Centralized debounced tactile haptics | `AppHaptics.kt` (80ms debounce window) |

### 2.7 Non-Functional Requirements

* **Reliability:** `START_STICKY` service lifecycle, exponential reconnect backoff (2s → 30s with ±20% jitter via `RetryPolicy`), autostart on both OSes.
* **Performance:** 4096-byte receive buffers, incremental stream framers, SQLite WAL mode, background-threaded history grouping (`Dispatchers.Default`), zero jank on tab switches.
* **Privacy & Security:** Zero external network sockets, local storage only (`~/.config/synqvia/history.db` and Room `clips.db`). Sensitive clips masked in UI.
* **Robustness:** Frame length caps (2 MiB), malformed JSON drop, version mismatch rejection (`bye/version`), IME error handling against dead `InputConnection` handles.

---

## 3. Architecture & Subsystems

### 3.1 System Overview

```
┌─────────────────────────── Linux Mint Xfce (Python 3 + GTK3) ──────────────────────────┐
│                                                                                         │
│   ClipboardMonitor (GTK) ──► SyncEngine ◄──► History (SQLite WAL)                      │
│                                  │                                                      │
│                           BtServer (RFCOMM)                                             │
│                     ┌────────────┴────────────┐                                         │
│             ClientConnection 1         ClientConnection 2 (Multi-device)                 │
└─────────────────────┼─────────────────────────┼─────────────────────────────────────────┘
                      │  RFCOMM SPP             │  RFCOMM SPP
                      ▼  UUID: 7be1e1f2...002   ▼
┌────────────────── Android Client 1 ───────────────────┐  ┌──────── Android Client 2 ────────┐
│                                                       │  │                                  │
│   SynqviaKeyboardView (QWERTY + Preview + Toolbar)     │  │   SynqviaKeyboardView            │
│                 │                                     │  │                 │                │
│                 ▼                                     │  │                 ▼                │
│   SynqviaImeService (1-Tap Paste: commitText)         │  │   SynqviaImeService              │
│                 │                                     │  │                 │                │
│                 ▼                                     │  │                 ▼                │
│   ClipboardCaptureManager (Echo Suppress + LWW Funnel)│  │   ClipboardCaptureManager        │
│                 │                                     │  │                 │                │
│                 ▼                                     │  │                 ▼                │
│   ClipRepository (Room clips.db v2: Pinned + Expiry)  │  │   ClipRepository                 │
│                 │                                     │  │                 │                │
│                 ▼                                     │  │                 ▼                │
│   ClipSyncService (Client Transport + Outbox Queue)   │  │   ClipSyncService                │
└───────────────────────────────────────────────────────┘  └──────────────────────────────────┘
```

### 3.2 Linux Subsystem (`linux/synqvia/`)

1. **`bt.py` (Multi-Device Bluetooth Server):**
   - Manages socket listener on RFCOMM channel (default 1) using PyBluez SDP advertisement with native fallback.
   - Holds an in-memory dictionary of active `ClientConnection` instances.
   - Dispatches incoming client messages to worker threads (`BtClient-<mac>`).
   - Cleans up stale connections when a reconnecting peer provides an existing `peer_id`.
   - Thread-safe frame delivery and per-client outbox management.

2. **`engine.py` (Sync Engine & Cross-Device Relay):**
   - Coordinates between local clipboard changes, storage, and connected Bluetooth peers.
   - **Cross-Device Relay:** When an incoming clip is received from client A, the engine verifies deduplication, commits the clip to SQLite, sends a targeted acknowledgement to client A (`send_msg(ack, target_peer_id=A)`), and broadcasts the clip to all other peers (`broadcast_clip(msg, exclude_peer_id=A)`).
   - Enforces 1500ms LWW conflict resolution and 2000ms echo suppression window.

3. **`clipboard.py` (Clipboard Monitor):**
   - Observes GTK clipboard changes using `owner-change` signals with 500ms polling fallback for headless environments.
   - Arms suppression hash for 2000ms across programmatic `set_text` + `store()` emissions.

4. **`history.py` (SQLite Storage):**
   - Thread-safe storage with WAL mode and `clips` table: `(id PK, text, ts, src, direction, conflict_loser)`.
   - Prunes history to user-configured capacity (`history_cap`, default 500).

5. **`main.py`, `window.py`, `tray.py`, `cli.py`:**
   - Single-instance lock via `fcntl.flock`.
   - GTK3 window displaying status, device pairing info, search, clip history, and real-time logs.
   - Ayatana AppIndicator system tray with quick-history repasting.
   - Headless CLI (`synqvia-cli`) operating directly over the SQLite database and spool outbox directory.

### 3.3 Android Subsystem (`com.github.premtechworks.synqvia`)

1. **IME Subsystem (`ime/`):**
   - **`SynqviaKeyboardView.kt`:** Hand-crafted, high-performance programmatic layout containing:
     - 3 Layers: Letters (QWERTY with numbers row hints), Symbols 1 (`?123`), Symbols 2 (`=/<`).
     - Touch handling: popup key preview bubble (`ime_key_preview.xml`), hold-to-repeat backspace, tactile feedback.
     - Dynamic Enter action: Search, Send, Next, Done, or standard Enter icons and actions.
     - 44dp Top Toolbar: toggle buttons for Clipboard Drawer, Pinned clips filter, and system Keyboard Picker.
   - **`KeyboardState.kt`:** Pure Kotlin state machine with zero Android UI dependencies. Manages shift/caps-lock toggling, active layer, grapheme-aware backspace, and double-space-to-period shortcuts. Fully tested via JUnit.
   - **`KeyboardPalette.kt`:** Resolves theme attributes dynamically (`attrs.xml`) to style the keyboard in Light or Dark mode.
   - **`SynqviaImeService.kt`:** Concrete `InputMethodService`. Manages `InputConnection`, performs 1-tap pasting via `commitText(clip.text, 1)`, and listens to `syncPreferences.themeModeFlow` to re-theme in real time.
   - **`ClipImeAdapter.kt`:** RecyclerView adapter backed by `DiffUtil`. Renders source badges (PC / Local), relative timestamps, pin buttons, delete actions, and sensitive content masking.

2. **Capture Pipeline (`clipboard/` & `service/`):**
   - **`ClipboardCaptureManager.kt`:** Universal funnel for all incoming and outgoing clips. Arms SHA-256 suppression hash for 2000ms before writing remote clips, coalesces duplicate copies within 1500ms, and gates continuous background capture via `DefaultImeDetector`.
   - **`DefaultImeDetector.kt`:** Verifies if Synqvia is currently the active default input method in `Settings.Secure.DEFAULT_INPUT_METHOD`. Gating prevents OS-level SecurityExceptions while reporting privilege state cleanly.
   - **`SensitiveClassifier.kt`:** Detects passwords, OTPs, and authentication headers via `EXTRA_IS_SENSITIVE` on Android 13+ (API 33+).
   - **`ClipSyncService.kt`:** Foreground service (`connectedDevice|dataSync`). Maintains client RFCOMM connection loop, in-memory outbox (cap 1000), `Protocol.StreamFramer`, and `RetryPolicy` backoff.

3. **Data Layer (`data/`):**
   - **Room Database v2 (`AppDatabase.kt`, `ClipEntity.kt`, `ClipDao.kt`):** Stores clips with `pinned` and `sensitive` columns.
   - **`ClipRepository.kt`:** Handles storage operations, history cap enforcement, and queries for IME drawer items (`WHERE pinned = 1 OR ts > :expiryThreshold ORDER BY pinned DESC, ts DESC LIMIT :limit`).
   - **`SyncPreferences.kt`:** Wraps `SharedPreferences` with reactive `StateFlow` streams for every configuration key.

4. **UI & Theme Architecture (`ui/`):**
   - **`SynqviaColors.kt` & `Theme.kt`:** Complete semantic design tokens (`DarkSynqviaColors`, `LightSynqviaColors`). Adapts to System, Dark, or Light themes with dynamic window status/navigation bar insets.
   - **`HistoryUiState.kt` & `HistoryDateUtils.kt`:** History entries grouped by date (`Today`, `Yesterday`, or formatted header) on `Dispatchers.Default` with cached string formatters.
   - **`AppHaptics.kt`:** Central haptics provider with an 80ms de-bounce window and user setting checks.
   - **`MainViewModel.kt`:** Manages screen navigation, search queries, pre-warmed history state, soft deletes with undo snackbars, and diagnostic logs.

---

## 4. Key Specifications

### 4.1 Transport & Wire Framing

* **Transport:** Bluetooth Classic RFCOMM / SPP only.
* **Service UUID:** `7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002` (custom 128-bit).
* **Wire Framing:**
  ```
  [ Length: 4 Bytes Big-Endian u32 ] [ Payload: UTF-8 JSON Encoded Data ]
  ```
  Maximum frame payload: 2 MiB.

### 4.2 Protocol Messages (`{"v":1,"type":...}`)

| Message Type | Fields | Semantics |
|--------------|--------|-----------|
| `hello` | `id, src, name, ts` | Handshake exchange initiated immediately upon connection. Peer records `peer_id` and friendly name. |
| `clip` | `id, src, ts, text, mime` | Core clipboard payload (`mime="text/plain"`). Empty text signifies clipboard clear. |
| `ack` | `id, src, ts, for` | Targeted acknowledgment for a specific `clip.id`. Clears sender's outbox entry. |
| `bye` | `id, src, ts, reason` | Graceful disconnection (`shutdown`, `duplicate`, or `version`). |

### 4.3 Conflict Resolution & Loop Prevention

1. **SHA-256 Echo Suppression (2000ms):** When a remote clip is applied locally, its SHA-256 hash is recorded in a suppression register for 2000ms. Subsequent clipboard observer events matching this hash are ignored.
2. **Deterministic Last-Write-Wins (1500ms):** Concurrent edits within 1500ms are resolved deterministically: higher timestamp wins; identical timestamps broken by lexicographical source identifier comparison (`src_a > src_b`). The losing clip is stored in history with `conflict_loser = true` without rebroadcast.
3. **Cross-Device Relay Suppression:** The Linux relay server broadcasts incoming clips to all peers while explicitly excluding the originating peer (`exclude_peer_id`).

---

## 5. Design System & UX Breakdown

### 5.1 Semantic Design Tokens (`SynqviaColors`)

Synqvia employs a unified semantic design token system across both platforms:

* **Dark Theme:** High-contrast OLED/dark background (`#050B18` to `#0A1428`), deep surfaces (`#101B2F`), electric cyan primary (`#12D4FF`), vibrant blue accents (`#3D8BFF`), muted secondary text (`#8FA0BC`), and gold pin indicators (`#FFC21A`).
* **Light Theme:** Crisp slate background (`#E8EDF5`), clean card surfaces (`#FFFFFF`), high-contrast primary (`#1F6FEB`), rich deep text (`#0B1B33`), and soft borders (`#E1E9F5`).
* **Edge-to-Edge System Bars:** The Android theme actively controls status and navigation bar scrims via `WindowCompat.getInsetsController`, ensuring light bar icons in dark mode and dark icons in light mode.

### 5.2 Keyboard UI Components (`SynqviaKeyboardView`)

* **Layer 1 (Letters):** Full QWERTY layout with secondary number hint glyphs on the top row (Q→1, W→2, etc.).
* **Layer 2 (Symbols 1):** Numeric keypad layout (1–0) with primary punctuation and symbols (`@`, `#`, `$`, `%`, `&`, `*`, `(`, `)`).
* **Layer 3 (Symbols 2):** Extended symbols (`~`, `\`, `|`, `^`, `<`, `>`, `€`, `£`, `¥`).
* **Toolbar:** 44dp height. Contains quick-toggle buttons for the clipboard drawer, pinned filter, and keyboard picker.
* **Preview Bubble:** Floating popup bubble displayed on touch down over letter keys for typing confidence.

### 5.3 History Screen & Modal Detail Sheet

* **History Grouping:** Clips grouped into immutable `HistoryGroupUi` structures by date (`Today`, `Yesterday`, date string) computed off the UI thread.
* **Filter Pills:** Dynamic filter pills (`All`, `Sent`, `Received`, `Conflicts`, `Pinned`) with badge counters.
* **Clip Detail Sheet (`ClipDetailSheet.kt`):** Full-screen bottom sheet offering:
  - Complete snippet text inspection with word and character counters.
  - Action buttons: Copy, Share, Pin/Unpin (with soft-spring animation), and Delete.
  - Automatic URL link extraction via `UrlUtil` with a direct "Open Link" button.
* **Undoable Deletion:** Soft deletion removes clips from view immediately with a floating snackbar enabling one-tap restoration.

---

## 6. Testing & Quality Assurance

### 6.1 Linux Test Suite (Pytest)

Run via `python3 -m pytest tests/ -v`:

| Test Name | File | Description |
|-----------|------|-------------|
| `test_roundtrip` | `tests/test_protocol.py` | Validates JSON encoding, length-prefix framing, and unicode decoding. |
| `test_split_frames` | `tests/test_protocol.py` | Verifies stream framer buffering across partial network chunks. |
| `test_lww` | `tests/test_protocol.py` | Tests timestamp ordering and source tie-breaking logic. |
| `test_suppress_echo` | `tests/test_protocol.py` | Asserts echo suppression window swallows reflected copies. |
| `test_dedup_ids` | `tests/test_protocol.py` | Asserts duplicate message IDs are ignored. |
| `test_client_connection_basic` | `tests/test_multidevice.py` | Tests client socket abstraction and hello handshake. |
| `test_multi_device_broadcast_and_status` | `tests/test_multidevice.py` | Verifies broadcasting to multiple connected clients and status reporting. |
| `test_cross_device_relay` | `tests/test_multidevice.py` | Asserts client A clip is relayed to client B while excluding client A. |
| `test_reconnection_cleans_stale_connection` | `tests/test_multidevice.py` | Asserts reconnecting peer closes previous stale socket cleanly. |

*Result:* **9 passed in 0.17s.**

### 6.2 Android Test Suite (Robolectric & JUnit)

Run via `./gradlew test` (or `./gradlew testDebugUnitTest`):

* **Protocol & Logic:**
  - `ProtocolUnitTest.kt`: Roundtrip encoding, chunked framing, LWW winner evaluation, SHA-256 determinism.
  - `KeyboardStateTest.kt`: Pure unit tests of shift states, caps lock, layer navigation, grapheme backspace, enter action resolution, double-space period.
  - `RetryPolicyTest.kt`: Exponential backoff calculation and jitter boundaries.
* **Capture & Privilege Gating:**
  - `ClipboardCaptureManagerTest.kt`: Echo suppression, coalescing, sensitive flag persistence, outbound dispatch.
  - `DefaultImeDetectorTest.kt`: Default IME detection against secure settings.
  - `ImePrivilegeContinuousCaptureTest.kt`: Continuous monitoring gating based on default IME privilege.
  - `SettingsPreferencesGatingTest.kt`: Setting toggles and preference state flows.
* **Storage & UI State:**
  - `ClipRepositoryImeTest.kt`: Expiry query filtering, pin persistence, soft deletions.
  - `HistoryDateUtilsTest.kt`: Date grouping and header formatting.
  - `UrlUtilTest.kt`: URL pattern matching and normalization.
  - `MainViewModelTest.kt`: Search filtering, statistics calculations, undo buffer.
  - `NavigationStartDestinationTest.kt`: Start destination resolution across setup states.
  - `OnboardingAndPairScreenTest.kt` & `SetupStateTest.kt`: Wizard flow assertions.
* **Integration:**
  - `ImeServiceIntegrationTest.kt`: Service lifecycle, adapter binding, sensitive masking, paste simulation.
  - `BootReceiverTest.kt` & `NotificationActionReceiverTest.kt`: Broadcast handling and service intents.

*Result:* **All suites passing cleanly.**

---

## 7. Packaging & Deployment Operations

### 7.1 Linux Debian Packaging (`linux/build_deb.sh`)

Synqvia includes an automated Debian packager:
```bash
./linux/build_deb.sh
sudo apt install ./dist/synqvia_1.0.0_all.deb
```
The resulting `.deb` package installs:
* Python module files to `/usr/local/lib/synqvia/synqvia/`
* Launchers to `/usr/local/bin/synqvia` and `/usr/local/bin/synqvia-cli`
* Autostart service entry to `/etc/xdg/autostart/synqvia.desktop`
* Application menu launcher to `/usr/share/applications/synqvia.desktop`

### 7.2 Android Release Signing

Configured in `android/app/build.gradle.kts`:
* Sideload debug build: `./gradlew assembleDebug`
* Signed production release: `./gradlew assembleRelease bundleRelease`
* Resolves keystore credentials from environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `android/keystore.properties`, or `android/synqvia-release.jks`.

---

## 8. Summary of Completed Improvements

1. **Rebranding:** Full migration from legacy naming to **Synqvia** across Android, Linux, protocols, and documentation.
2. **QWERTY IME:** Evolved from a simple paste-only drawer into a complete, standalone 3-layer QWERTY input method with key previews and haptics.
3. **Multi-Device Relay:** Evolved Linux daemon from 1-to-1 pairing into a multi-device coordinator supporting cross-device synchronization.
4. **Adaptive Themes:** Complete dual-theme support (Dark & Light) across both the app and the IME keyboard with semantic tokens.
5. **History Optimization:** Off-thread background grouping and pre-warmed UI queries for zero-lag interactions.
6. **Robust Packaging:** Official Debian packaging script and production Android release signing configuration.

*Document updated and verified on 2026-10-03 against repository state v1.0.0.*
