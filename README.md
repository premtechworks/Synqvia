# Synqvia — Bluetooth clipboard sync (Linux Mint Xfce ↔ Android)

Offline-first Bluetooth Classic (RFCOMM/SPP) clipboard synchronization with Gboard-like Input Method Service (IME) clipboard capture. No cloud, no local network dependency. Text + history only (v1).

---

## 1. Architecture Overview

Synqvia features a decoupled, offline-first architecture separating Android clipboard integration from Bluetooth transport:

```text
Android System Clipboard
          │
          ▼
SynqviaImeService
(InputMethodService)
          │
          ├── observe/read accessible clipboard data
          ├── capture new clipboard entries
          ├── expose clipboard-history UI (pinned + 1h temporary)
          └── paste selected history item via InputConnection
          │
          ▼
ClipboardCaptureManager (Shared Pipeline)
          │
          ├── SHA-256 echo suppression (2s window)
          ├── 1.5s conflict window & duplicate coalescing
          ├── Sensitive data classification (EXTRA_IS_SENSITIVE)
          │
          ▼
Shared ClipRepository (Room Database: clips.db)
          │
          └── SyncEngine (Outbox Queue & Deduplication)
                    │
                    ▼
             ClipSyncService (Foreground Service)
                    │
                    ▼
             Bluetooth RFCOMM / SPP (UUID 7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002)
                    │
                    ▼
                Linux PC (Python Daemon + Tray + CLI)
```

### Why IME Clipboard Access Differs from Background Clipboard Access

* **Android 10+ (API 29)** introduced strict privacy restrictions preventing background applications from calling `ClipboardManager.getPrimaryClip()`. Only the app currently holding window focus or the **currently active default Input Method (IME)** can read the clipboard.
* **Android 12+ (API 31)** added system toast warnings whenever applications read clipboard data, except when the default IME reads/pastes data into the focused editor.
* **Android 13+ (API 33)** introduced `ClipDescription.EXTRA_IS_SENSITIVE` to prevent clipboard contents from being previewed unnecessarily.
* **Android 14+ (API 34)** strictly limits background apps from even receiving `onPrimaryClipChanged` listener callbacks.
* **The IME Advantage:** The active/default input method service is deeply integrated with Android's windowing and input system. When the user interacts with an input field, the IME has sanctioned access to inspect the clipboard and paste directly into the focused field via `InputConnection.commitText()`.
* **Important Note:** IME clipboard access is NOT unrestricted root-level access. It operates when Synqvia is selected as an input method. Capability detection and graceful fallbacks are built in for non-IME usage.

---

## 2. Enabling Synqvia Keyboard on Android

1. Open the Synqvia app on Android.
2. Navigate to the **✦ Setup** tab.
3. Locate the **Clipboard Keyboard (IME)** card and tap **Enable Keyboard**.
4. In Android's **Manage Keyboards** settings, toggle **Synqvia Keyboard** to ON.
5. In any text field:
   - Tap the keyboard switch icon on your navigation bar / current keyboard.
   - Select **Synqvia Keyboard**.
6. The Synqvia clipboard strip opens:
   - Recent clipboard items are displayed.
   - **Tap any card** to paste it directly into the active text field.
   - **Tap the Pin icon** to keep an item permanently.
   - **Long press a card** for options: *Pin / Unpin*, *Send to PC*, *Copy to Clipboard*, or *Delete*.
   - **Tap the Keyboard icon** in the top bar to seamlessly return to your primary keyboard (Gboard, Samsung Keyboard, etc.).

### Gboard-like Expiration Behavior
* **Unpinned items:** Automatically hidden from the IME panel after 1 hour (configurable in `SyncPreferences`).
* **Pinned items:** Persist indefinitely in the IME panel.
* **Persistent Sync History:** Expired IME clips remain fully preserved in Synqvia's main synchronization history up to the configured cap (default 500 records).

---

## 3. Pairing & Setup

### Step 1: Pair Bluetooth Devices (OS-level)
* **Linux:**
  ```bash
  bluetoothctl
  power on
  agent on
  scan on
  pair <PHONE_MAC>
  trust <PHONE_MAC>
  ```
* **Get Linux Bluetooth MAC:**
  ```bash
  bluetoothctl show | grep Controller
  ```
  Enter this MAC in the Android app under **Setup** or **Settings**.

### Step 2: Linux Install
```bash
cd linux && sudo bash install.sh
synqvia --show                                      # GTK window + status + tray
synqvia-cli status | history | send "text" | log   # Headless CLI
```
* Autostart entry installed to `~/.config/autostart`.
* SQLite history: `~/.config/synqvia/history.db`.

### Step 3: Android Install
```bash
cd android
./gradlew assembleDebug
```
* Sideload the APK or install via Android Studio.
* Launch app, grant Bluetooth permissions, and enter PC MAC.

---

## 4. Input Pathways & Fallbacks

Synqvia normalizes all clipboard sources through `ClipboardCaptureManager`:

| Pathway | Trigger | Android Version Support | Redundancy Status |
| :--- | :--- | :--- | :--- |
| **IME Service** | Active keyboard callback or start view | API 26 – 34+ | **Primary path** for automatic capture and fast paste. |
| **Selection Action** | Highlight text → `Send to PC` | API 23+ (`PROCESS_TEXT`) | **Preserved:** Direct selection share from any app. |
| **Share Sheet** | Share Sheet → `Send to PC` | All APIs (`ACTION_SEND`) | **Preserved:** Standard Android share target. |
| **Quick Settings Tile** | Swipe down QS → `Sync to PC` | API 24+ (`TileService`) | **Preserved:** Uses selection cache or Trampoline. |
| **Notification Action** | Tap `Sync to PC` in status bar | API 26+ (`ClipSyncService`) | **Preserved:** One-tap manual sync. |
| **Accessibility Cache** | Copy gesture text observation | API 26 – 34+ (`ClipAccessService`) | **Fallback:** For non-IME usage and unsaved selections. |
| **Trampoline Activity** | Focus-grab for QS tile | API 29+ (`TrampolineActivity`) | **Preserved:** For QS tile when IME is not active. |

---

## 5. Loop Prevention & Reliability Guarantees

* **Suppress-on-apply:** When a remote clip arrives from Linux, `ClipboardCaptureManager.applyRemoteClip()` writes to Android clipboard and arms a 2000ms SHA-256 suppression window. Subsequent callbacks from the IME or background listeners match the suppression hash and are swallowed.
* **Deterministic Last-Write-Wins (LWW):** Conflicts within 1500ms are resolved deterministically by `(timestamp, src_id)`. Loser clips are preserved in Room history marked with `conflictLoser = true`.
* **Deduplication:** Seen-ID LRU cache (500 items) + Room `INSERT OR IGNORE` ensure exactly one copy is persisted.
* **ACK & Outbox:** Local copies made offline queue in memory and Room; when RFCOMM reconnects, the outbox flushes in timestamp order.

---

## 6. Automated & Manual Testing

### Automated Unit & Integration Tests
Run the Android test suite:
```bash
cd android
./gradlew test
```
Test suite includes:
* `ProtocolUnitTest`: Wire framing roundtrip, partial stream framing, LWW conflict ordering, SHA-256 echo suppression.
* `ClipboardCaptureManagerTest`: Local clip capture, echo suppression on remote clip application, suppression window expiry, duplicate observer coalescing, sensitive data classification.
* `ClipRepositoryImeTest`: Gboard-like expiration (pinned persists, unpinned expires after 1h), Room sync history preservation, toggle pin, Room database v1 $\to$ v2 migration.
* `ImeServiceIntegrationTest`: IME service lifecycle, `ClipImeAdapter` formatting and sensitive content masking, `InputConnection.commitText` paste simulation.

### Manual Verification Checklist
1. **Local Copy via IME:**
   - Enable Synqvia Keyboard.
   - Copy text in Chrome or any app.
   - Verify 1 clip recorded in Synqvia History and received on Linux PC (`synqvia-cli history`).
   - Verify no echo is sent back to Android.
2. **Remote Clip Reception:**
   - Run `synqvia-cli send "Hello from Mint"`.
   - Android notification confirms reception.
   - Open Synqvia Keyboard in any text field: "Hello from Mint" appears in clipboard history.
   - Verify no loop-back packet is received by Linux.
3. **Pasting from IME:**
   - Tap the card in the Synqvia Keyboard panel.
   - Text is immediately committed into the editor.
4. **Pin & Expiration:**
   - Pin an item in the IME panel.
   - Verify pin icon changes to filled cyan.
   - Wait for unpinned items to expire; verify pinned item remains at the top.
5. **Keyboard Switching:**
   - Tap the keyboard switch button in the IME header.
   - System input method picker or previous keyboard opens immediately.
