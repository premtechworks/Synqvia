# Loop-prevention / Last-write-wins — test plan

## Automated (run: `python3 -m pytest tests/ -v`)
- `test_roundtrip`, `test_split_frames`: framing correct over stream splits.
- `test_lww`: higher ts wins; tie → larger `src` wins.
- `test_suppress_echo`: remote apply → same text seen locally is swallowed, new text broadcasts.
- `test_dedup_ids`: retransmitted msg id applied once.

## Manual two-device tests
1. **No ping-pong**: copy "A" on Linux → appears on Android. `adb logcat` / Linux log must show
   exactly 1 `clip` + 1 `ack`, no further messages for 10 s. Repeat in reverse direction.
2. **Near-simultaneous (conflict)**: disconnect BT, copy "LIN" on Linux and "AND" on Android
   within 1 s, reconnect. Both clipboards must converge to the copy with the larger
   timestamp (check history timestamps). Loser text must still exist in both histories
   (`conflict_loser` on Linux / present in Room on Android).
3. **Empty clear syncs**: clear clipboard (empty string) on one side → other side clears too.
4. **Resend storm**: kill app mid-sync, restart → outbox flushes unacked clips once each
   (dedup by id, no duplicates in history).

## Reconnect / offline tests
1. Kill Bluetooth on phone mid-sync → Linux tray shows "Offline (listening)", phone
   notification shows "Offline — retrying". Copy text on both sides while apart.
2. Re-enable BT → auto-reconnect within ~30 s (backoff cap), pending clips flush in ts
   order, no loss, no duplication.
3. Reboot PC → tray autostarts via `~/.config/autostart/synqvia.desktop`, resumes listening.
4. Reboot phone → `BOOT_COMPLETED` restarts foreground service + notification reappears.

---

## IME Privilege & Continuous Clipboard Capture Tests (Android Architecture)

Automated suite: `cd android && ./gradlew testDebugUnitTest --tests "com.github.premtechworks.synqvia.ImePrivilegeContinuousCaptureTest"` (all tests automated in Robolectric).

### Test A: Default IME + Keyboard Visible
- **Setup**: Synqvia set as Android default IME (`Settings.Secure.DEFAULT_INPUT_METHOD`). Active input view displayed (`onCreateInputView()`).
- **Action**: Copy text in Chrome or any app.
- **Expected**:
  - Immediate Room database entry (`ClipEntity` with direction `local`).
  - Immediate Bluetooth RFCOMM outbound dispatch to Linux.
  - Exactly one outbound clip transmission.
  - No loop/echo.

### Test B: Default IME + Keyboard Hidden (Primary Regression Test)
- **Setup**: Synqvia set as Android default IME. Keyboard UI is hidden; Synqvia main activity/UI is closed. `ClipSyncService` running in foreground.
- **Action**: Copy text in Chrome, Firefox, YouTube description, Notes, etc.
- **Expected**:
  - Automatic clipboard capture without opening the Synqvia application.
  - Immediate Room entry.
  - Immediate Linux transmission via `ClipSyncService`.

### Test C: Gboard Default IME (Synqvia merely enabled)
- **Setup**: Gboard (or another keyboard) set as default IME; Synqvia is enabled but NOT the default input method.
- **Action**: Copy text in any application.
- **Expected**:
  - Synqvia does not falsely claim continuous automatic clipboard monitoring.
  - Foreground `ClipSyncService` stops/does not start `ClipboardManager.OnPrimaryClipChangedListener`.
  - App UI Dashboard clearly reports: *"Automatic capture requires Synqvia to be the default keyboard."* (not labeled as sync or Bluetooth failure).
  - Explicit capture mechanisms remain fully operational: `PROCESS_TEXT` ("Send to PC"), Share menu, Quick Settings Tile, Notification action, manual Sync Now.

### Test D: Dynamic Transition (Gboard → Synqvia)
- **Setup**: Start with Gboard as default IME.
- **Action**: In Android Settings or input picker, switch default keyboard to Synqvia. Copy text.
- **Expected**:
  - `ContentObserver` on `DEFAULT_INPUT_METHOD` detects change immediately without tight polling.
  - `ClipSyncService` starts `ClipboardCaptureManager` monitoring.
  - Automatic clipboard capture resumes immediately on next copy.

### Test E: Dynamic Transition (Synqvia → Gboard)
- **Setup**: Start with Synqvia as default IME.
- **Action**: Switch default keyboard to Gboard. Copy text.
- **Expected**:
  - Continuous clipboard capture halts immediately; listener is unregistered.
  - App UI updates warning banner.
  - No background clipboard scraping hacks or unauthorized reads are attempted.

### Test F: UI Process Kill with Service Running
- **Setup**: Synqvia default IME, keyboard hidden, `ClipSyncService` running in foreground.
- **Action**: Force kill or swipe away the Synqvia app UI from Android Recents. Copy text in Chrome.
- **Expected**:
  - `ClipSyncService` and `ClipboardCaptureManager` survive UI destruction.
  - Automatic capture continues seamlessly to Room and Linux.

### Test G: Remote Linux Clip Echo Suppression
- **Setup**: Synqvia default IME with active Bluetooth connection.
- **Action**: Copy text on Linux terminal; remote clip arrives via RFCOMM and writes to Android clipboard via `applyRemoteClip()`.
- **Expected**:
  - SHA-256 echo suppression arms for 2.0s window.
  - Local `OnPrimaryClipChangedListener` callback is suppressed.
  - Exactly one Room entry recorded (`direction = "remote"`).
  - No ping-pong re-transmission from Android back to Linux.

