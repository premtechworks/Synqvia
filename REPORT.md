# Synqvia — Detailed Project Report
**Bluetooth Clipboard Sync: Linux Mint Xfce ↔ Android | Text + History Only v1 + IME Keyboard**

> Sources inspected: `README.md` (rewritten 2026-09-26 for IME), `PROTOCOL.md`, `TEST_PLAN.md`, `linux/synqvia/*.py` (~1324 LOC, 10 files), `android/app/src/main/java/com/example/**/*.kt` (`clipboard/ClipboardCaptureManager.kt` 237 + `SensitiveClassifier.kt` 27 + `ime/SynqviaImeService.kt` 254 + `ime/ClipImeAdapter.kt` 132 + `service/ClipSyncService.kt` 486 + `protocol/Protocol.kt` 222 + `data/` 279 + Compose UI), `AndroidManifest.xml`, `res/layout/ime_clipboard_view.xml`, `res/layout/item_ime_clip.xml`, `res/xml/method.xml`, `res/values/strings.xml`, `linux/install.sh`, `tests/test_protocol.py`, `android/app/src/test/java/com/example/*.kt` (605 LOC tests).
>
> **Recent changes incorporated (commit `af42300`, 2026-09-26 — "Add IME functionality and related resources" + root `README.md` rewrite):** Gboard-like IME keyboard as primary capture/paste path, shared `ClipboardCaptureManager` pipeline, `SensitiveClassifier` (`EXTRA_IS_SENSITIVE`), Room v1→v2 (`pinned`, `sensitive`), `SyncPreferences.imeExpiryHours`, `AppContainer.clipboardCaptureManager`, `ClipSyncService` refactor to delegate to manager, `SetupScreen` IME card, `MainViewModel` `PINNED` filter + pin/expiry APIs, `TrampolineActivity` dual-path via manager, `recyclerview` dep, `targetSdk 36→34`, Gradle wrapper 9.3.1, 3 new Robolectric test classes.

---

## 1. Executive Summary

**Synqvia** is an offline-first, Bluetooth Classic (RFCOMM/SPP) clipboard synchronizer between a Linux Mint Xfce PC and an Android phone.

* **No cloud, no BLE, no LAN/Wi-Fi dependency.** One persistent bidirectional RFCOMM stream.
* **Linux = server/listener**, **Android = client/initiator** using a custom 128-bit SPP UUID.
* **Data scope v1:** `text/plain` only + persistent history on both sides. Empty string (clear) is a valid sync event.
* **Core differentiator vs KDE Connect:** explicit loop-prevention (`suppress-on-apply` + `seen-id` cache) + deterministic Last-Write-Wins (LWW) conflict resolution + ack/outbox reliability + graceful degradation for Android 10/14 background clipboard restrictions.
* **New in this revision (IME overhaul):** Android now uses a **decoupled pipeline — `SynqviaImeService` (active IME) → `ClipboardCaptureManager` (shared) → `ClipRepository` (Room) → `ClipSyncService` (transport)**. The active keyboard is the *primary* automatic capture + one-tap paste path (sanctioned clipboard access on Android 10+ where background reads are blocked); all legacy paths (PROCESS_TEXT, Share, QS Tile, notif action, Accessibility cache, Trampoline) are preserved as fallbacks and normalized through the same manager.

**Repo layout (updated):**

```
synqvia/
  PROTOCOL.md          # wire spec v1 (normative, unchanged)
  README.md            # rewritten: IME architecture, enable-keyboard flow, pathways, tests
  REPORT.md            # this file
  TEST_PLAN.md         # auto + manual + reconnect tests (pre-IME text; see §10 for new tests)
  linux/               # Python daemon + GTK UI + CLI (unchanged in this revision)
    install.sh
    synqvia.desktop
    requirements.txt
    synqvia/
      protocol.py  engine.py  bt.py  clipboard.py
      history.py  main.py  window.py  tray.py  cli.py
  android/             # Kotlin app (namespace com.github.premtechworks.synqvia, applicationId com.github.premtechworks.synqvia)
    app/src/main/java/com/example/
      clipboard/ClipboardCaptureManager.kt  SensitiveClassifier.kt   # NEW shared pipeline
      ime/SynqviaImeService.kt  ClipImeAdapter.kt                # NEW Gboard-like keyboard
      protocol/Protocol.kt
      data/ClipEntity.kt  ClipDao.kt  ClipRepository.kt  AppDatabase.kt  SyncPreferences.kt
      di/AppContainer.kt
      service/ClipSyncService.kt  ClipAccessService.kt  SelectionCache.kt  SyncTileService.kt
      ui/MainViewModel.kt  ui/screens/SetupScreen.kt (+ Dashboard/History/Settings, Compose)
      MainActivity.kt  SynqviaApp.kt  ShareActivity.kt  ProcessTextActivity.kt  TrampolineActivity.kt
    app/src/main/res/
      layout/ime_clipboard_view.xml  layout/item_ime_clip.xml       # NEW
      xml/method.xml                                                 # NEW IME registration
      drawable/bg_ime_card.xml  bg_ime_header_button.xml  ic_ime_*.xml  # NEW (pin/pin_filled/close/delete/empty/keyboard)
      values/strings.xml  (15 new ime_* strings)
    app/src/test/java/com/example/
      ProtocolUnitTest.kt  ClipboardCaptureManagerTest.kt           # NEW
      ClipRepositoryImeTest.kt  ImeServiceIntegrationTest.kt        # NEW
  tests/test_protocol.py   # Linux pytest (unchanged)
```

> Correction to previous report: Android package is `com.github.premtechworks.synqvia` (not `com.github.premtechworks.synqvia`); Linux LOC ~1324 (not 1448). Android is Compose + Room + foreground-service, not the old `SetupActivity/ShareActivity`-only list.

---

## 2. PRD — Product Requirements Document

### 2.1 Problem Statement

Users working across Linux PC + Android constantly retype URLs, OTPs, notes, code snippets. Cloud clipboards (Google, Pushbullet, KDE Connect over LAN) require internet/accounts/LAN, leak sensitive clipboard data, or fail on restrictive networks. Need a **direct, private, offline** sync. On Android 10+ background clipboard reads are blocked entirely, so a sanctioned foreground capture path (active IME) is required — this revision delivers it.

### 2.2 Goals

| ID | Goal | Success Metric |
|----|------|----------------|
| G1 | Bidirectional text sync <2s when connected | `synqvia-cli send "hi"` appears on Android + clipboard in ~1s |
| G2 | No ping-pong / duplication | Exactly 1x `clip` + 1x `ack`, silent for 10s after one copy |
| G3 | Offline resilience | Copies made offline flush on reconnect in `ts` order, no loss/dup |
| G4 | History persistence + search + re-broadcast | 500-row default cap, SQLite/Room, tap-to-resend |
| G5 | Zero cloud / zero account | Works with Wi-Fi/data off, BT on |
| G6 | Usable within Android OS clipboard limits | **IME keyboard (primary) + 4 legacy one-tap paths + a11y/Trampoline fallbacks work without opening app** (was: 4 paths only) |
| G7 (new) | Gboard-like in-keyboard history with pin + 1h expiry | Unpinned >1h hidden in IME, pinned persist; sync history unaffected (covered by `ClipRepositoryImeTest`) |

### 2.3 Non-Goals (v1 explicitly out)

* Images, files, rich HTML. Reserved for `clip_chunk{id,seq,total,bytes_b64}` + MAX 25MB — documented in `PROTOCOL.md:147`.
* In-app Bluetooth pairing. Must pair via `bluetoothctl` / OS Settings first.
* E2E encryption beyond BT bonding (relies on OS-level bond + Classic BT encryption).
* iOS, Windows, multi-device mesh. Single paired pair only; second socket closed with `bye/duplicate`.
* Clock-skew correction. LWW is best-effort on wall-clock; future: hello-timestamp skew estimate.
* Full QWERTY replacement keyboard — IME panel is clipboard-history only with tap-to-paste via `InputConnection.commitText()` + switch-back button.

### 2.4 Target Users & Personas

1. **Linux-first dev (Mint Xfce)** — copies terminal output / code to phone.
2. **Privacy-conscious user** — refuses cloud clipboard.
3. **Field/offline user** — no Wi-Fi, only BT.
4. **(new) IME user** — keeps Synqvia Keyboard enabled, pastes from history without app-switching.

### 2.5 User Stories

* As a PC user, I copy text → it appears in Android app + Android system clipboard automatically.
* As an Android user, I select text → `Send to PC` → it appears in Linux window + Linux clipboard.
* **(new)** As an Android user with Synqvia Keyboard active, I copy anywhere → it is auto-captured; I open any text field → tap a card → it pastes instantly; I pin keepers, long-press for Pin/Send-to-PC/Copy/Delete.
* As a user, I go offline, copy on both sides, reconnect → both converge deterministically, loser text still in history.
* As a user, I click history → it re-copies + rebroadcasts.
* As a user, I reboot both devices → sync resumes without manual start.

### 2.6 Functional Requirements

| ID | Requirement | Implementation |
|----|-------------|----------------|
| FR1 | RFCOMM server on Linux, client on Android | `linux/synqvia/bt.py:20`, `service/ClipSyncService.kt:157` |
| FR2 | 4-byte BE length-prefixed JSON framing, 2MiB max | `protocol.py:48`, `protocol/Protocol.kt:143` |
| FR3 | `hello/clip/ack/bye` schema, `v=1` | `protocol.py:27-45`, `PROTOCOL.md:52-90` |
| FR4 | Suppress-on-apply loop prevention | `clipboard.py:41`, `engine.py:136`, **`clipboard/ClipboardCaptureManager.kt:59,187`** (moved out of service) |
| FR5 | Seen-ID dedup (500) + DB restart-proof dedup | `engine.py:24,85`, `ClipSyncService.kt:70,320` |
| FR6 | LWW with 1500ms window, `(ts, src)` ordering | `protocol.py:96`, `protocol/Protocol.kt:154` |
| FR7 | Ack-tracked outbox, ts-ordered flush | `bt.py:32,63`, `ClipSyncService.kt:68,130,256` |
| FR8 | History cap (0=∞), search, clear | `history.py:21`, `data/ClipDao.kt:36,44` |
| FR9 | Android capture paths: **IME (primary)** + PROCESS_TEXT + Share + QS Tile + Notif action + a11y cache + Trampoline | Manifest (incl. new `BIND_INPUT_METHOD` + `method.xml`) + `ime/` + `clipboard/` + 4 activities/services |
| FR10 | Status visibility everywhere | GTK header + tray tooltip + Android notif + `cli status` + Compose `connectionState` |
| FR11 (new) | Shared capture pipeline normalizing all inputs | `ClipboardCaptureManager.kt:131` `captureLocalClip` / `:90` `handlePrimaryClipChanged` / `:187` `applyRemoteClip` / `:215` `copyToClipboardWithoutBroadcast`, `OutboundClipListener` → service |
| FR12 (new) | Pin / unpin, delete, IME expiry (pinned-first ordering) | `ClipEntity.pinned/sensitive`, `ClipDao.getImeClips/setPinned/getClipById`, `ClipRepository.getImeClips/setPinned`, `AppDatabase v2 + MIGRATION_1_2`, `SyncPreferences.imeExpiryHours`, `MainViewModel.togglePin/updateImeExpiryHours`, `ClipFilter.PINNED` |
| FR13 (new) | Sensitive classification + masking | `clipboard/SensitiveClassifier.kt`, `EXTRA_IS_SENSITIVE` on API33+, `ClipImeAdapter.kt:69` masked `•••••••• (Sensitive Content)` |

### 2.7 Non-Functional Requirements

* **Reliability:** `START_STICKY` service, 2/5/10/30s backoff, autostart on both OSes.
* **Performance:** 4096B recv chunks, incremental framer, WAL SQLite, prune by `(ts,rowid)`; IME list capped 50 via `DiffUtil`, `pinned DESC, ts DESC`.
* **Privacy:** No network socket, no analytics, local DB only: `~/.config/synqvia/history.db`, Room `clips.db`. Sensitive clips masked in IME UI, flagged via `EXTRA_IS_SENSITIVE` on Tiramisu+.
* **Robustness:** Oversize frame → drop + log; invalid JSON → ignore; version mismatch → `bye/version` + close. Manager guards null/blank/non-`text/*` MIME, `SecurityException` (background gate), dead IPC.
* **Maintainability:** Protocol mirrors must stay identical — `protocol.py` ↔ `protocol/Protocol.kt`.

### 2.8 Constraints (Android OS — verified on-device + docs)

* Android 10+: only focused app (or active IME) reads clipboard data.
* Android 12+: system toast on clipboard read — exempt when default IME reads/pastes.
* Android 13+: `ClipDescription.EXTRA_IS_SENSITIVE` — now honored and propagated.
* Android 14+: background apps don't even get change callback.
* OEMs kill FG services → battery-optimization allowlist + autostart deep-links required (known limitation).
* Mitigations built (updated): **active IME (`SynqviaImeService`) is now the sanctioned primary** (reads on `onStartInputView` + `onPrimaryClipChanged`, pastes via `InputConnection`); Accessibility selection cache + freshness watermark, `TrampolineActivity` focus-grab, `ACTION_INJECT` explicit handoff, Seamless Setup screen (now with IME card) remain as fallbacks.

---

## 3. MVP Definition

**MVP = v1 as shipped + IME keyboard revision.** Text-only, single-pair, Bluetooth-direct.

**Must-have (done):**

* [x] Pair via OS, enter PC MAC once on Android (`pc_mac` in SharedPreferences, `AA:BB:CC:DD:EE:FF` validated by `MAC_RE`)
* [x] Bidirectional sync + clipboard apply both sides
* [x] Framing + validation + LWW + suppress + dedup + ack/outbox
* [x] Linux tray + window + CLI
* [x] Android FG service + notif + history + 4 share paths + Setup screen + Boot receiver
* [x] **NEW: IME keyboard (`BIND_INPUT_METHOD`, `method.xml`), clipboard strip (tap paste, pin, long-press menu, keyboard-switch), `ClipboardCaptureManager` shared pipeline, Room v2 pin/sensitive + 1h expiry, Setup IME card**
* [x] 5 pytest unit tests (no BT hardware needed) + **4 Robolectric suites (15 tests: Protocol, CaptureManager×7, RepositoryIme×3, ImeIntegration×4)**
* [x] Manual + reconnect test plan

**Explicitly deferred:**

* [ ] `clip_chunk` image/file transfer
* [ ] Keepalive / 24h idle recycle (not needed — RFCOMM reliable)
* [ ] Skew estimation, E2EE, multi-peer

**MVP acceptance (from `TEST_PLAN.md` + `README.md` §6):**

1. `synqvia-cli send "hi"` → Android in ~1s.
2. Share → Send to PC → Linux window/CLI + system clipboard.
3. No ping-pong for 10s, 1 clip + 1 ack in logs.
4. Simultaneous offline copies → converge to larger `ts`, loser preserved.
5. BT kill → `Offline (listening)` / `Offline — retrying` → auto-reconnect ≤30s.
6. **(new)** Enable Synqvia Keyboard → copy in Chrome → 1 Room row + Linux receipt, no echo; tap card → `commitText` paste; pin persists past 1h expiry; sensitive clip masked in IME.

---

## 4. Feature List

### 4.1 Linux (`linux/synqvia/` — unchanged in this revision)

| Feature | File | Notes |
|---------|------|-------|
| RFCOMM server, PyBluez SDP advertise + stdlib fallback | `bt.py:92` | Channel 1-30, `listen(1)`, duplicate → `bye/duplicate` |
| Outbox (cap 1000, drop-oldest) + `ack_received` + ts-ordered `_flush_outbox` | `bt.py:32-67` | Survives disconnect |
| GTK clipboard monitor (`owner-change` + 500ms poll fallback) | `clipboard.py:10` | Headless-safe, `feed_local()->bool` |
| Suppress hash + 2000ms window (stays armed for double-emit), coalesce identical within 1500ms | `clipboard.py:104-135` | Handles `set_text+store()` double event |
| SyncEngine: `on_local_change`, `on_remote_msg`, LWW branch, `mark_loser`, status fan-out | `engine.py:9` | UI-agnostic core |
| SQLite history (WAL, `clips(id PK,text,ts,src,direction,conflict_loser)`, `idx_clips_ts`, LIKE ESCAPE search) | `history.py:7` | `cap 0=∞` |
| Main entry: config sanitize, `fcntl` single-instance + `Gtk.Application` D-Bus activation, spool flush every 2s, baseline seed | `main.py:117` | KDE-Connect-inspired core+UI split |
| Main window 680x520: status dot, Sync now / Send test / Clear, adapter MAC row, search, 200-row list, device-name + cap settings, collapsible log | `window.py:50` | Close = hide to tray |
| Tray: Ayatana preferred, StatusIcon fallback, left-click/menu Show window, last-10 history repaste, Clear, Quit | `tray.py:21` | `edit-paste` icon |
| CLI: `status [--json] / history -n -q / send / clear / log -n` via DB + `spool/*.json` | `cli.py:126` | Works daemon-down |
| Installer: `apt` deps, pip/user fallback, `~/.config/autostart` + `~/.local/share/applications`, `/usr/local/lib` + `/usr/local/bin` shims | `install.sh` | |

### 4.2 Android (`com.github.premtechworks.synqvia` — heavily changed)

| Feature | File | Notes (NEW = this revision) |
|---------|------|------------------------------|
| **NEW shared pipeline: `ClipboardCaptureManager`** — `captureLocalClip` (suppress→coalesce→persist→outbound callback), `handlePrimaryClipChanged` (MIME/text/* gate, blank ignore, `SecurityException` safe), `applyRemoteClip` + `copyToClipboardWithoutBroadcast` (arm-suppress-then-write, `EXTRA_IS_SENSITIVE` on API33+), `armSuppression/isSuppressed/isRecentDuplicate`, `OutboundClipListener`, `getLastLocalClip` | `clipboard/ClipboardCaptureManager.kt` | Single source of truth for echo/LWW inputs; injected with IO/Main dispatchers for testability |
| **NEW `SensitiveClassifier`** — `isSensitive(ClipData/ClipDescription)` via `description.extras` on Tiramisu+ | `clipboard/SensitiveClassifier.kt` | Persisted as `ClipEntity.sensitive`, masked in IME |
| **NEW IME service** — `onCreate` (DI + `OnPrimaryClipChangedListener`), `onCreateInputView` (inflate `ime_clipboard_view`, switch/close buttons, `ClipImeAdapter`), `onStartInputView` (sanctioned foreground read + `getImeClips` collect, limit 50), `pasteClip` (`commitText`, fallback copy+toast), `togglePin/deleteClip`, long-press `PopupMenu` (Paste/Pin-Copy-Send-Delete), `switchKeyboard` (prev method or picker) | `ime/SynqviaImeService.kt` | `BIND_INPUT_METHOD`, `method.xml` subtype `en_US/keyboard` |
| **NEW `ClipImeAdapter`** — `DiffUtil` RecyclerView, sensitive masking, PC/Local badge, relative time (Just now/Xm/Xh/MMM d), pin filled/outline + delete buttons, click/long-press dispatch | `ime/ClipImeAdapter.kt` | Backed by `item_ime_clip.xml` |
| FG service (refactored): **removed inline `suppressNextHash/lastLocalClip/processLocalClip`**; now `clipboardCaptureManager.setOutboundClipListener{queueAndSendClip}` on create (cleared on destroy), `clipboardListener/ACTION_INJECT/ACTION_SYNC_NOW` delegate to manager, LWW reads `manager.getLastLocalClip()`, remote apply via `manager.applyRemoteClip()` | `service/ClipSyncService.kt:59,83,99,107,337,360,464` | `connectedDevice\|dataSync` types, UUID+channel fallback, 2/5/10/30s backoff, `cancelDiscovery`, FG notif + `Sync to PC` action |
| Room v2: `ClipEntity.pinned/sensitive` (default 0), `ClipDao.getImeClips(pinned OR ts>threshold, pinned DESC, ts DESC, LIMIT)` + `getClipById/setPinned`, `AppDatabase v2 + MIGRATION_1_2`, `ClipRepository.getImeClips/getClipById/setPinned`, `SyncStats` combine | `data/ClipEntity.kt:32, `data/ClipDao.kt:62, `data/AppDatabase.kt:10, `data/ClipRepository.kt:79` | Sync history preserves expired IME clips (cap 500) |
| Prefs + DI: `SyncPreferences.imeExpiryHours` (default 1, 0=never), `AppContainer.clipboardCaptureManager` singleton | `data/SyncPreferences.kt:67`, `di/AppContainer.kt:35` | `MainViewModel.imeExpiryHours` StateFlow + `updateImeExpiryHours`, `ClipFilter.PINNED`, `togglePin` |
| Setup: **new card #5 "Clipboard Keyboard (IME)"** (checks `enabledInputMethodList`, deep-links `ACTION_INPUT_METHOD_SETTINGS`); one-tap card renumbered #6 | `ui/screens/SetupScreen.kt:204` | BT/notif/battery/a11y cards unchanged |
| `TrampolineActivity` dual-path: now also `captureManager.captureLocalClip(text)` on IO **plus** legacy `ACTION_INJECT` (redundant by design for reliability) | `TrampolineActivity.kt:23` | Translucent, `excludeFromRecents`, `noHistory` |
| Manifest: **new `<service .ime.SynqviaImeService BIND_INPUT_METHOD>`** with `android.view.InputMethod` filter + `method` meta-data | `AndroidManifest.xml:116` | Share/PROCESS_TEXT/Tile/a11y/Boot entries unchanged |
| Resources: `xml/method.xml`, `layout/ime_clipboard_view.xml` (260dp, header + RecyclerView + empty state), `layout/item_ime_clip.xml` (badge/time/pin/delete/preview), `drawable/bg_ime_card/bg_ime_header_button/ic_ime_{close,delete,empty,keyboard,pin,pin_filled}`, 15 `strings.xml` `ime_*` entries | `res/` | Dark-navy `#0A1120/#141E33`, cyan `#00E5FF` accents |
| Build: `compileSdk 36`, `targetSdk 34` (was 36), `recyclerview:1.3.2` added, Gradle wrapper 9.3.1 + `gradlew(.bat)` added, `.env`/secrets + Firebase/AI deps retained | `app/build.gradle.kts:13,17,100`, `gradle/wrapper/` | `namespace com.github.premtechworks.synqvia`, `applicationId com.github.premtechworks.synqvia`, `minSdk 26` |
| Compose UI (pre-existing, now IME-aware): Dashboard/History/Settings/Setup, `LiquidGlass` theme, `MainViewModel` search+filter+stats+logs+sync/test/resend/pin/delete/clear/save/reconnect | `ui/` | History filter now includes PINNED |

---

## 5. Production / Deployment & Operations

### 5.1 Linux Install

```bash
cd linux && sudo bash install.sh
synqvia --show
synqvia-cli status | history | send "text" | log
```

* Deps: `python3-gi gir1.2-gtk-3.0 gir1.2-ayatanaappindicator3-0.1 bluez python3-pip libbluetooth-dev` + `pybluez PyGObject pycairo` (`requirements.txt`).
* PyBluez **required** for SDP advertise — without it Android UUID lookup fails (channel-1 fallback socket-to-socket only).
* Pair first: `bluetoothctl power on / agent on / scan on / pair <PHONE_MAC> / trust <PHONE_MAC>`; PC MAC via `bluetoothctl show | grep Controller`.

### 5.2 Runtime Artifacts (Linux — unchanged)

| Path | Purpose |
|------|---------|
| `~/.config/synqvia/config.json` | `{device_name, history_cap, channel}` sanitized (channel 1-30, cap ≥0, name ≤48) |
| `~/.config/synqvia/history.db` | SQLite WAL |
| `~/.config/synqvia/synqvia.log` | DEBUG file + INFO stderr + in-window LogHandler |
| `~/.config/synqvia/spool/*.json` | CLI `send` outbox flushed every 2s (`_flush_spool`) |
| `~/.config/synqvia/synqvia.lock` | `fcntl` single-instance |
| `~/.config/autostart/synqvia.desktop` | Xfce autostart (tray-only) |
| `~/.local/share/applications/synqvia.desktop` | App-menu (`--show`) |

### 5.3 Android Release

* Open `android/` in Android Studio → `assembleDebug` → sideload, or `./gradlew :app:assembleDebug` (wrapper now 9.3.1, `gradlew`/`gradlew.bat` committed in this revision).
* `namespace com.github.premtechworks.synqvia`, `applicationId com.github.premtechworks.synqvia`, `compileSdk 36`, `minSdk 26`, `targetSdk 34`, `versionCode 1`, `versionName 1.0`. Debug signing via `debug.keystore`.
* Runtime: grant BT perms, enter PC MAC → Save+connect, accept battery-optimization prompt, complete Seamless Setup **including new IME step**: enable Synqvia Keyboard in system Manage Keyboards → switch to it in any text field.
* Room auto-migrates v1→v2 (`pinned`/`sensitive` columns, default 0); no manual DB action.

### 5.4 Operations

* Status everywhere: window header dot (green/red), tray tooltip/menu header, Android notif (`Synqvia: Connected/Offline — retrying`), `cli status` (`log recent <120s` heuristic), Compose `SyncConnectionState` + `diagnosticLogs`.
* Logs: `synqvia-cli log -n 50`, window Log view, `adb logcat -s Synqvia`.
* Backup: copy `history.db` / Room `clips.db`; no server to migrate.

---

## 6. Architecture

### 6.1 System Overview (updated)

```
┌─ Linux Mint Xfce (Python 3 + GTK3) ───────┐    BT Classic RFCOMM    ┌─ Android (Kotlin, com.github.premtechworks.synqvia) ─────────────┐
│ ClipboardMonitor → SyncEngine → BtServer  │◄═════════════════════►│ ClipSyncService (client, transport only)    │
│        ↕ History (SQLite)    ↕ outbox     │  7be1e1f2-...af002     │  ↕ ClipRepository (Room clips.db v2) ↕ outbox│
│ Window + Tray (listeners)    CLI (DB)     │  4B BE len + JSON      │  ↕ ClipboardCaptureManager (shared pipeline)│
└───────────────────────────────────────────┘                       │ IME / Share / PROCESS_TEXT / Tile / a11y ↕  │
                                                                     │ SynqviaImeService → InputConnection      │
                                                                     └─────────────────────────────────────────────┘
```

* Single-connection model: Android-client → Linux-server wins.
* KDE-Connect-style split on Linux: UI-agnostic core (`engine+bt+history+clipboard`) with thin consumers (`window`, `tray`, `cli`).
* **NEW decoupled split on Android (see `README.md` §1):** clipboard integration (`SynqviaImeService` + all fallback entry points) is separated from transport (`ClipSyncService`) by `ClipboardCaptureManager` + `ClipRepository`. Capture paths never touch sockets; service never touches `ClipboardManager` directly except via manager.

### 6.2 Data Flow (updated)

**Local copy (any path → shared pipeline):**
`IME onPrimaryClipChanged / onStartInputView read / Share / PROCESS_TEXT / Tile / notif / a11y / Trampoline / ACTION_INJECT` → `ClipboardCaptureManager.captureLocalClip/handlePrimaryClipChanged` → suppress/coalesce check (SHA-256 2s, dup 1.5s) → `make_clip(uuid4,ts)` → `ClipRepository.insertClip(local, pinned, sensitive)` → `OutboundClipListener.onClipCaptured` → `ClipSyncService.queueAndSendClip` → `sendall(encode)` → await `ack` → outbox removal.

**Remote clip:**
`recv → StreamFramer/decode → validate → ack immediately → seen? return : DB exists? return : mime!=text/plain? return : insert(remote, loser-flag TBD) → LWW check (`manager.getLastLocalClip()`, 1500ms) → loser? mark_loser (no apply) : manager.applyRemoteClip(suppress arm + ClipboardManager write + EXTRA_IS_SENSITIVE)`.

**IME paste (no BT involved):**
Tap card → `currentInputConnection.commitText(text)`; long-press menu → Paste / Pin-Unpin (`setPinned`) / Copy-without-broadcast (suppression-armed) / Send-to-PC (`captureLocalClip`) / Delete (`deleteById`).

**Offline:** `conn=None` → `queue_send` only appends outbox/spool; on `accept/connect` → `_flush_outbox` ts-sorted; dedup by `id` both sides.

### 6.3 Module Responsibilities

* `protocol.py / protocol/Protocol.kt` — pure framing + factories + validation + `lww()`. No I/O. Unit-testable.
* `clipboard.py / clipboard/ClipboardCaptureManager.kt` — edge-triggered capture + echo suppression. **Manager is now the single choke point (all 7 Android entry points funnel through it).**
* `clipboard/SensitiveClassifier.kt` — `EXTRA_IS_SENSITIVE` detection (new).
* `engine.py / ClipSyncService.onRemote()` — ordering, conflict, persistence, ack policy.
* `bt.py / ClipSyncService.connectLoop()` — transport, reconnect, lifecycle.
* `history.py / data/ClipRepository+ClipDao` — storage, cap, search, **IME expiry query + pin ops (new)**.
* `main.py / SynqviaApp+AppContainer` — wiring, lifecycle, **DI singleton for manager (new)**.
* `ime/SynqviaImeService + ClipImeAdapter` — sanctioned foreground capture + Gboard-like history UI (new).

---

## 7. Key Specifications

### 7.1 Transport

* **Bluetooth Classic RFCOMM/SPP only.** No BLE/GATT, no TCP/cloud.
* Linux binds channel (default 1, 1-30) + advertises SPP via PyBluez SDP; Android `createRfcommSocketToServiceRecord(MAC,UUID)` with `createRfcommSocket(channel)` fallback.
* Backoff Android: 2s→5s→10s→30s cap, reset on success. Linux `accept()` loop with 2s timeout for clean stop.
* Keepalive: none v1. Either side may recycle idle socket after 24h (optional).

### 7.2 Service UUID

```
7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002
```

Custom, not generic `00001101-...`. Hardcoded both sides (`protocol.py:9`, `protocol/Protocol.kt:11`).

### 7.3 Framing

```
LEN (4B BE u32) + PAYLOAD (LEN bytes UTF-8 JSON)
MAX_PAYLOAD = 2 MiB (v1 text-only; future 25 MB)
```

Sender `sendall`; receiver loops `recv` + buffers; back-to-back allowed; `LEN>MAX` → drop connection + log + `bye`.

### 7.4 Message Schema (`{"v":1,"type":...}`)

| Type | Fields | Policy |
|------|--------|--------|
| `hello` | `id,src,name,ts` | First both dirs, no ack. `v` mismatch → `bye/version`+close. Peer records `peer_id/name`. |
| `clip` | `id(uuid4),src,ts(wall ms),text(empty valid),mime=text/plain` | Only data msg. Unknown mime → ack but ignore. |
| `ack` | `id,src,ts,for(clip id)` | Sender holds in outbox until ack/drop; resend ts-ordered on reconnect. |
| `bye` | `id,src,ts,reason: shutdown\|duplicate\|version` | Graceful close. |

`src` ≤64 chars: `linux-<hostname>` / `android-<ANDROID_ID>`.

### 7.5 Conflict & Loop Prevention (now centralized in `ClipboardCaptureManager` on Android)

* `SUPPRESS_WINDOW_MS=2000`: `suppress_next=sha256(text)` set on `apply_remote`; echo within window swallowed, suppress stays armed. Exposed as `armSuppression/isSuppressed` (unit-tested incl. expiry).
* `CONFLICT_WINDOW_MS=1500`: near-simultaneous copies → `lww = higher ts wins; tie → larger src wins`. Loser kept in history with `conflict_loser=1`, not rebroadcast. Duplicate observer callbacks coalesced via `isRecentDuplicate` (multi-path: IME+listener+a11y+share).
* `seen` 500 IDs + DB `exists` (restart-proof) + `INSERT OR IGNORE` / `OnConflict IGNORE`.
* Self-echo: never send `hash==last received` within 2s from remote source; blank/whitespace now explicitly ignored by manager (`captureLocalClip` returns null).
* Sensitive: `isSensitive` persisted per-row; masked in IME; `EXTRA_IS_SENSITIVE` set on write (API33+); does not affect sync routing.

### 7.6 Storage Schema (Room v2 — changed)

```sql
-- Linux SQLite (unchanged, v1 shape)
CREATE TABLE clips(
  id TEXT PRIMARY KEY, text TEXT NOT NULL, ts INTEGER NOT NULL,
  src TEXT NOT NULL, direction TEXT NOT NULL, -- local|remote
  conflict_loser INTEGER DEFAULT 0
);
CREATE INDEX idx_clips_ts ON clips(ts DESC);
-- prune: DELETE WHERE id NOT IN (SELECT id ORDER BY ts DESC,rowid DESC LIMIT cap)
-- search: WHERE text LIKE ? ESCAPE '\' (caller escapes \ % _)

-- Android Room (v2, MIGRATION_1_2 adds pinned/sensitive DEFAULT 0)
-- entities = [ClipEntity], version = 2
-- ALTER TABLE clips ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0;
-- ALTER TABLE clips ADD COLUMN sensitive INTEGER NOT NULL DEFAULT 0;
-- IME panel query (unpinned expire, pinned persist, pinned-first):
-- SELECT * FROM clips WHERE pinned = 1 OR ts > :expiryThreshold
-- ORDER BY pinned DESC, ts DESC LIMIT :limit;
-- + getClipById / setPinned / deleteById; sync history (allClips) unaffected by expiry
```

### 7.7 Config & Prefs

* Linux `config.json`: `device_name (≤48), history_cap (0=∞, default 500), channel (1-30, default 1)`.
* Android `PREFS sync`: `pc_mac (uppercase MAC_RE), channel, cap (default 500)`, **`ime_expiry_hours` (default 1, 0=never)** via `SyncPreferences.imeExpiryHours` + `MainViewModel.updateImeExpiryHours`.
* Device IDs: `("linux-"+name)[:64]`, `"android-"+ANDROID_ID`.

---

## 8. Tools, Languages, Frameworks

### 8.1 Languages

* **Python 3** — Linux daemon, protocol, engine, GTK UI, CLI (~1324 LOC over 10 files).
* **Kotlin** — Android app, service, Room, IME, Compose UI (`clipboard/` 264 + `ime/` 386 + `service/ClipSyncService` 486 + `protocol/` 222 + `data/` 279 + UI 1000+ LOC).
* **SQL** — SQLite/Room queries (incl. new `getImeClips` pinned/expiry query).
* **Bash** — `install.sh`.
* **XML** — Manifest, themes, `method.xml`, `ime_clipboard_view.xml`, `item_ime_clip.xml`, accessibility/device-admin configs.

### 8.2 Linux Stack (unchanged)

| Layer | Tech |
|-------|------|
| UI | GTK 3 via PyGObject (`gi.repository Gtk/Gdk/GLib`), AyatanaAppIndicator3 (fallback `Gtk.StatusIcon`) |
| BT | PyBluez `bluetooth.BluetoothSocket(RFCOMM)` + SDP `advertise_service(SERIAL_PORT_PROFILE)`; stdlib `AF_BLUETOOTH/BTPROTO_RFCOMM` fallback |
| DB | `sqlite3` + WAL |
| IPC/single-instance | `fcntl` lock + `Gtk.Application(application_id=com.github.premtechworks.synqvia)` D-Bus activation |
| Deps | `pybluez, PyGObject, pycairo`; sys pkgs `python3-gi gir1.2-gtk-3.0 gir1.2-ayatanaappindicator3-0.1 bluez libbluetooth-dev` |
| Tests | `pytest` (`tests/test_protocol.py`: roundtrip, split-frames, lww, suppress-echo, dedup-ids) |

### 8.3 Android Stack (updated)

| Layer | Tech |
|-------|------|
| SDK | **`namespace com.github.premtechworks.synqvia`, `applicationId com.github.premtechworks.synqvia`, `compileSdk 36`, `minSdk 26`, `targetSdk 34`** (was 36), Java 11, KSP + Moshi codegen, Secrets plugin |
| UI | Compose (`material3`, BOM) + `recyclerview:1.3.2` **(new, for IME panel)** + legacy Views for IME layouts; `LiquidGlass` theme; screens Dashboard/History/Settings/Setup |
| IME | `InputMethodService`, `method.xml` (`en_US/keyboard`, `supportsSwitchingToNextInputMethod`), `InputConnection.commitText`, `InputMethodManager` switch/picker |
| Persistence | `room-runtime/ktx` **v2** (`MIGRATION_1_2`), `clips.db` |
| BT | `BluetoothManager/Adapter/Device/Socket`, `BLUETOOTH/CONNECT/SCAN` (API31 split, `maxSdkVersion 30` legacy) |
| FG service | `connectedDevice\|dataSync` types, `NotificationCompat`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `BIND_INPUT_METHOD` (new) |
| System integration | **IME (primary)**, AccessibilityService, DeviceAdminReceiver, Quick Settings Tile (`BIND_QUICK_SETTINGS_TILE`), `PROCESS_TEXT` + `SEND` intents |
| Build | Gradle **wrapper 9.3.1** (new `gradlew`/`gradlew.bat` + jar), Android Studio |

### 8.4 DevOps / Tooling

* `bluetoothctl/hciconfig` for pairing/MAC, `adb logcat` for device logs, `pytest` for Linux unit, **`./gradlew test` (Robolectric, `isIncludeAndroidResources=true`) for 4 Android suites**.
* No CI config in repo; manual `./gradlew :app:assembleDebug` + `python3 -m pytest tests/ -v`.

---

## 9. Design System & UX

### 9.1 Principles

KDE-Connect / ClipCascade / **Gboard** inspired: **status-first device card, searchable history, one-tap resend, in-keyboard clipboard strip, headless parity.**

### 9.2 Linux GTK Window (`window.py` — unchanged)

* 680×520, `CENTER`, border 10, vertical box spacing 6.
* Header: `○/● 24pt` dot (red/green) + status label (`Connected (peer)` / `Offline — listening`) + `Sync now` (foreground clipboard read+broadcast) + `Send test` (timestamped) + `Clear`.
* Pairing row: `This PC adapter: <MAC> • channel N` (selectable, via `bluetoothctl show` regex `Controller ([0-9A-F:]{17})`) + hint `Pair first with bluetoothctl...`.
* `Search history…` (`Gtk.SearchEntry`, live `refresh()`).
* History `Gtk.ListBox` (SINGLE, `row-activated` → `repaste`): `▶ local / ◀ remote`, `MM-DD HH:MM`, `src`, 120-char preview (`\n→⏎`, `(empty)`, `[conflict loser]`), `markup_escape_text`.
* Settings: `Device name Entry + Cap SpinButton 0-100000 step 50 (0=∞) + Save`.
* Collapsible `Log Expander` → monospace non-editable `TextView`, autoscroll, fed by `LogHandler`.
* Close → hide to tray (quit only via tray).

### 9.3 Linux Tray (`tray.py` — unchanged)

* Ayatana `APPLICATION_STATUS` (`edit-paste` icon) else legacy `StatusIcon` + `activate→show_window`.
* Menu: disabled `Synqvia: <state>` header, `Show window`, last-10 clips (`▶/◀` + 60 chars), `Clear history`, `Quit`. Rebuilt on every status change.

### 9.4 Android Material UI + IME (changed)

* Compose `Theme` + `LiquidGlass` cards; `MainViewModel` tabs Sync/History/Setup/Settings, search + `ClipFilter` (**now incl. `PINNED`**), stats, diagnostic logs, sync/test/resend/pin/delete/clear.
* **NEW IME panel (`ime_clipboard_view.xml`, 260dp `#0A1120`):** header (`Synqvia` cyan + `Clipboard` subtitle + switch-keyboard + close buttons on `#141E33`), `RecyclerView` (6dp/8dp padding) + empty state (`ic_ime_empty`, `ime_no_clips`, hint text). Cards (`item_ime_clip.xml`, `bg_ime_card`, 12dp/4dp margins, 12dp padding): source badge (PC indigo / Local cyan), relative time, pin (filled cyan vs outline gray) + delete buttons, 3-line preview (white, or gray masked `•••••••• (Sensitive Content)`).
* **NEW interactions:** tap → `commitText` paste (or copy+toast fallback); pin icon → `setPinned`; trash → `deleteById`; long-press → themed `PopupMenu` (Paste / Pin-Unpin / Copy to Clipboard / Send to PC / Delete); keyboard icon → previous IME or system picker.
* Persistent notif: `Synqvia: <status>`, ongoing, open-app + `Sync to PC` actions.
* QS Tile `Sync to PC`, Share/PROCESS_TEXT `Send to PC`, Setup wizard (`✦ Seamless setup`) now with **IME card (#5)** + OEM deep-links + device-admin enrollment.

### 9.5 Copy / Tone

Status strings consistent: `Starting… / Connected (peer) / Offline — listening / Offline — retrying / Offline — set PC MAC / Offline — Bluetooth off / Offline — bad PC MAC`. History arrows `▶/◀`, empty `(empty)`, loser `[conflict loser]/[loser]`. **New IME strings:** `Synqvia Keyboard / Clipboard History / Tap to paste, long press for options / Pinned-Unpinned / Send to PC / Copy to Clipboard / •••••••• (Sensitive Content) / Pasted / Copied to clipboard.**

---

## 10. Testing & Quality (expanded)

**Automated Linux (`tests/test_protocol.py`, `pytest` — unchanged):**

* `test_roundtrip` (unicode `hello ✓`), `test_split_frames` (partial second frame buffering), `test_lww` (higher-ts + tie-larger-src), `test_suppress_echo` (echo swallowed, fresh broadcasts), `test_dedup_ids` (retransmit once).

**Automated Android (`./gradlew test`, Robolectric SDK 34, `isIncludeAndroidResources=true` — NEW except `ProtocolUnitTest`):**

* `ProtocolUnitTest` (extended + sha256 test): roundtrip (unicode ✓🚀), 10-byte-chunk split frames (clip+ack), LWW + tie-break, SHA-256 determinism/64-hex.
* `ClipboardCaptureManagerTest` (7): persist+outbound dispatch (id match), echo suppression on `applyRemoteClip` (clipboard assert + null capture + 0 outbound), suppression expiry (100ms arm / +200ms clear), duplicate-observer coalescing (1 row + 1 outbound), distinct clips pass, blank/whitespace ignored, sensitive flag persisted.
* `ClipRepositoryImeTest` (3): 1h expiry — old-unpinned hidden, old-pinned + fresh shown pinned-first, sync history still 3/3; `setPinned` toggle; `deleteById`.
* `ImeServiceIntegrationTest` (4): service lifecycle (create + `onCreateInputView` + destroy), adapter binding/formatting (text, `2m ago`, `Local`, click/pin/delete dispatch), sensitive masking (no plaintext leak), `commitText` paste simulation (`Hello ` → `Hello World from Synqvia!`).

**Manual (from `TEST_PLAN.md` + `README.md` §6 — IME rows new):** no ping-pong 10s, near-simultaneous converge + loser in both histories, empty-clear syncs, kill-mid-sync outbox flush once each, BT-kill offline UI → ≤30s reconnect ts-ordered, PC reboot autostart, phone reboot `BOOT_COMPLETED` + notif, **plus IME: enable keyboard → copy in Chrome → Linux receipt + no echo; remote `send` → IME strip shows it + no loopback; tap-to-paste commits; pin persists past expiry; keyboard-switch returns to Gboard.**

**Gaps:** no BT-hardware CI, no E2E harness, no fuzz for corrupt frames, no skew test. `linux/tests/` currently empty placeholders (`test_protocol.py`/`test_history.py` 0B). `TEST_PLAN.md` not yet updated for IME steps (covered in `README.md` instead).

---

## 11. Security, Privacy, Limitations

* **Privacy-positive:** BT-direct, no accounts/servers/analytics; bond-level Classic encryption only.
* **New:** sensitive clipboard sources flagged (`EXTRA_IS_SENSITIVE` Tiramisu+) and **masked in IME UI** (`ClipImeAdapter` never renders plaintext for `sensitive=true`); still synced as normal text (no redaction on wire — documented behavior).
* **Risks:** No app-layer E2EE (BT sniffing theoretically possible); wall-clock LWW vulnerable to skew; `allowMainThreadQueries` in tests only (prod uses IO dispatcher); OEM battery killers require manual allowlist; Android 14+ background copy fundamentally impossible without IME — degraded to one-tap paths by OS design (IME now primary mitigation).
* **Hardening done:** MAX cap, version gate, LIKE-escape (SQL LIKE injection safe), src-length cap, outbox caps (1000), `bye` on abuse, manager null/MIME/`SecurityException`/IPC guards, blank-text ignore.

---

## 12. Roadmap (Suggested — updated)

1. **v1.1 (next):** update `TEST_PLAN.md` for IME, IME expiry setting UI (currently pref-only), `clip_chunk` image support (25MB), skew estimate via hello exchange, encrypted `spool`/`outbox` at rest.
2. **v1.2:** LAN fallback (same protocol over TCP), multi-device (per-peer outbox), E2EE (X25519 + bonded pairing), sensitive-clip redaction option (don't sync flagged clips).
3. **v2:** Background `WorkManager` retry, Fuzz + E2E BT harness in CI, Flatpak + F-Droid packaging, full custom keyboard (typing, not just clipboard strip).

---

*Regenerated from repo inspection 2026-09-26 incorporating commit `af42300` (IME) + root `README.md` rewrite. Normative docs remain `PROTOCOL.md` (wire) + `README.md` (install/IME) + `TEST_PLAN.md` (QA). Prior REPORT inaccuracies fixed: Android package `com.github.premtechworks.synqvia`, `applicationId com.github.premtechworks.synqvia`, `compileSdk 36/targetSdk 34`, Compose UI, Room v2, shared-pipeline architecture.*
