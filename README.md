<p align="center">
  <img src="synqvia_logo.png" alt="Synqvia Logo" width="360" />
</p>

<h1 align="center">Synqvia</h1>

<p align="center">
  <strong>Instant local clipboard handoff</strong>
</p>

<p align="center">
  <a href="https://github.com/premtechworks/Synqvia/releases"><img src="https://img.shields.io/github/v/release/premtechworks/Synqvia?color=blue&logo=github" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3" /></a>
  <img src="https://img.shields.io/badge/Build-Passing-brightgreen.svg" alt="Build Status" />
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Linux-teal.svg" alt="Platform: Android | Linux" />
  <a href="PROTOCOL.md"><img src="https://img.shields.io/badge/Bluetooth-RFCOMM%2FSPP-navy.svg" alt="Protocol: RFCOMM/SPP" /></a>
</p>

---

Synqvia is a privacy-first, offline-first clipboard synchronization tool between Android and Linux. It pairs devices directly over **Bluetooth Classic (RFCOMM/SPP)** with length-prefixed framing, an integrated full QWERTY keyboard with clipboard drawer for zero-friction capture and 1-tap pasting, multi-device relay coordination, and adaptive Light & Dark themes.

---

## Table of Contents

- [Why Synqvia](#why-synqvia)
- [Features](#features)
- [Screenshots](#screenshots)
- [Architecture](#architecture)
- [Installation](#installation)
  - [Android](#android)
  - [Linux (Debian / Ubuntu / Mint / Xfce)](#linux-debian--ubuntu--mint--xfce)
- [Pairing & Setup](#pairing--setup)
- [Input Pathways & Fallbacks](#input-pathways--fallbacks)
- [Contributing](#contributing)
- [License](#license)
- [Links](#links)

---

## Why Synqvia

Traditional clipboard sharing applications rely on cloud servers, external relays, or local Wi-Fi networks requiring user logins and telemetry:

- **100% Private & Local**: Your clipboard data—which often includes passwords, two-factor auth tokens, private messages, and API keys—never touches the internet, third-party relays, or proprietary servers. Everything travels directly over your local Bluetooth radio.
- **True Offline Operation**: Works without an internet connection, without Wi-Fi router association, and without user accounts or login credentials.
- **Sanctioned Android 10–15+ Clipboard Integration**: Modern Android restricts background clipboard inspection (`ClipboardManager.getPrimaryClip()`). Synqvia solves this cleanly via a native Input Method Editor (`SynqviaImeService`), granting fully compliant foreground access and direct 1-tap paste into any app without root permissions, adb permissions, or private OS bypasses.

---

## Features

- 🔒 **Bluetooth Classic RFCOMM/SPP Transport**: Persistent bidirectional stream over Bluetooth Classic using dedicated UUID `7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002`. Features length-prefixed JSON frames, keepalive heartbeats, and acknowledgements.
- ⌨️ **Full QWERTY Keyboard & IME Drawer**: Standalone custom input method featuring a 3-layer keyboard (Letters with number hints, Symbols 1, Symbols 2), popup key preview bubbles, hold-to-repeat backspace, tactile haptic feedback, dynamic enter action mapping, and an integrated 44dp toolbar with 1-tap clipboard panel toggle and pinned clips filter.
- 👥 **Multi-Device Relay**: The Linux daemon coordinates multiple concurrent Android devices, broadcasting clips and relaying cross-device copies seamlessly while suppressing origin loops.
- 🎨 **Adaptive Dual Theme (Light & Dark Mode)**: Complete semantic design token system (`SynqviaColors` and `KeyboardPalette`) with dynamic edge-to-edge system bar contrast handling and real-time theme synchronization between the app and the IME keyboard.
- 📌 **Pinning & Configurable Expiry**: Pin critical snippets indefinitely; unpinned clips safely expire from the keyboard drawer after a user-configured interval (default 1 hour), while remaining preserved in the permanent sync history.
- 🛡️ **Sensitive Data Masking**: Automatic classifier detects authorization tokens, passwords, and API keys, masking them in the keyboard UI (`•••••••• (Sensitive Content)`).
- ⚡ **Deterministic LWW Conflict Resolution**: Resolves concurrent edits within 1500ms using `(timestamp, src_id)` tiebreakers, preserving loser entries in history without rebroadcast loops.
- 🔄 **SHA-256 Echo Suppression**: 2000ms suppression hash window prevents remote clips applied to the system clipboard from echoing back.
- 📦 **Offline Outbox & Auto-Sync**: Copies made while disconnected queue in SQLite (Linux) and Room (Android), automatically flushing in chronological order upon reconnection.
- ⚡ **High-Performance History Pipeline**: Background-computed date grouping (`HistoryGroupUi`), pre-warmed queries (`HistoryUiState`), zero-lag tab switching, URL host extraction with 1-tap browser navigation, and undoable soft deletion.
- 📳 **Centralized Debounced Haptics**: Unified tactile feedback engine (`AppHaptics`) with an 80ms safety debounce window, respecting both user preferences and system settings.
- 💬 **Help, FAQs & Community Support**: Native interactive Help & Support screen with categorized FAQs, inline code badge rendering, instant debug info clipboard copying, and direct links to Telegram, GitHub, and Ko-fi.
- 🐧 **Native Linux Desktop UI & CLI**: Features an Ayatana AppIndicator / GTK3 status tray, a responsive management window, and a standalone headless CLI (`synqvia-cli`).

---

## Screenshots

### Android Mobile App

| Dashboard & Quick Sync | Bluetooth Setup & Pairing | History & Pinning |
| :---: | :---: | :---: |
| <img src="docs/screenshots/home_screen.jpg" width="260" alt="Dashboard Screen" /> | <img src="docs/screenshots/setup_screen.jpg" width="260" alt="Setup Screen" /> | <img src="docs/screenshots/histor_screen.jpg" width="260" alt="History Screen" /> |
| *Real-time sync status & quick actions* | *Bluetooth adapter selection & pairing* | *Chronological grouping, search & pin* |

<br/>

| Preferences & IME Settings | Interactive FAQs & Support |
| :---: | :---: |
| <img src="docs/screenshots/settings_screen.jpg" width="260" alt="Settings Screen" /> | <img src="docs/screenshots/settings_screen2.jpg" width="260" alt="Support & FAQs Screen" /> |
| *Background sync & keyboard configuration* | *Embedded FAQs, issue reporting & community* |

### Linux Desktop Client

<p align="center">
  <img src="docs/screenshots/linux_screen.png" width="760" alt="Linux Desktop Client" />
  <br />
  <em>Synqvia Linux GTK3 Desktop Client — Connection Status, Real-Time Clipboard History & Multi-Device Coordination</em>
</p>

---

## Architecture

Synqvia separates clipboard observation and injection from the underlying Bluetooth transport using a decoupled pipeline:

```mermaid
flowchart TD
    subgraph Android["Android System (Kotlin + Compose)"]
        direction TB
        KB["SynqviaKeyboardView\n• 3-Layer QWERTY + Symbols\n• Key Previews & Haptics\n• 44dp Action Toolbar"]
        KBS["KeyboardState State Machine\n(Pure Kotlin, Zero UI Imports)"]
        IME["SynqviaImeService\n• 1-Tap Paste: InputConnection.commitText()\n• Foreground Clipboard Monitor"]
        FALLBACKS["Fallbacks: Share / PROCESS_TEXT / Tile / a11y"]
        CCM["ClipboardCaptureManager\n• SHA-256 Echo Suppression (2s)\n• LWW Tiebreaker (1.5s)\n• Sensitive Content Masking\n• DefaultImeDetector Gate"]
        ROOM["Room Database (clips.db v2)\n• History Cap\n• Pinned & Sensitive Flags\n• 1-Hour Drawer Expiry"]
        CSS["ClipSyncService (Foreground Service)\n• RFCOMM Client & Outbox Queue\n• RetryPolicy with Exponential Backoff"]

        KB <--> KBS
        KB --> IME
        IME --> CCM
        FALLBACKS --> CCM
        CCM --> ROOM
        ROOM --> CSS
    end

    subgraph RFCOMM["Bluetooth Classic RFCOMM / SPP"]
        direction LR
        WIRE["UUID: 7be1e1f2-73a6-4d9c-8c7d-a6f3f93af002\n4-byte Length-Prefixed JSON Frames"]
    end

    subgraph Linux["Linux System (Python 3 + GTK3)"]
        direction TB
        BT["BtServer\n• RFCOMM Listener\n• Multi-Client Connection Pool\n• Cross-Device Relay (exclude origin)"]
        ENGINE["SyncEngine\n• Deduplication Cache (500 items)\n• LWW Conflict Resolution\n• Targeted ACKs"]
        CLIP["ClipboardMonitor (Gtk.Clipboard)"]
        SQLITE["SQLite Database (~/.config/synqvia/history.db)"]
        UI["UI Layer: TrayApp (AppIndicator) & MainWindow (GTK3) & synqvia-cli"]

        BT --> ENGINE
        ENGINE <--> CLIP
        ENGINE --> SQLITE
        ENGINE --> UI
    end

    CSS <==> WIRE <==> BT
```

For the complete packet structure and wire protocol specifications, see [PROTOCOL.md](PROTOCOL.md).

---

## Installation

### Android

1. Download the latest release APK (`synqvia-v1.2.0.apk`) or bundle (`synqvia-v1.2.0.aab`) from the [Releases page](https://github.com/premtechworks/Synqvia/releases).
2. Install the APK via ADB or by opening it on your device:
   ```bash
   adb install synqvia-v1.2.0.apk
   ```
3. Enable the **Synqvia Keyboard**:
   - Open **Settings → System → Languages & input → On-screen keyboard** (or **Manage keyboards**).
   - Toggle **Synqvia Keyboard** to **ON**.
   - (Optional) Set Synqvia as your active keyboard to enable automatic background clipboard capture on Android 10+.
4. Open the Synqvia app, grant required Bluetooth permissions, and exempt the app from battery optimization so background sync remains active.

---

### Linux (Debian / Ubuntu / Mint / Xfce)

#### Option 1: Prebuilt `.deb` Package (Recommended)

1. Download the `.deb` package from the [Releases page](https://github.com/premtechworks/Synqvia/releases):
   ```bash
   wget https://github.com/premtechworks/Synqvia/releases/download/v1.2.0/synqvia_1.2.0_all.deb
   sudo apt install ./synqvia_1.2.0_all.deb
   ```
2. The package automatically installs:
   - Daemon binary: `/usr/local/bin/synqvia`
   - CLI utility: `/usr/local/bin/synqvia-cli`
   - Python library: `/usr/local/lib/synqvia/synqvia/`
   - Autostart entry: `/etc/xdg/autostart/synqvia.desktop`
   - Desktop application launcher: `/usr/share/applications/synqvia.desktop`

#### Option 2: Build `.deb` Package Locally

You can package Synqvia into a Debian package directly using the provided build script:
```bash
./linux/build_deb.sh
sudo apt install ./dist/synqvia_1.2.0_all.deb
```

#### Option 3: From Source via `install.sh`

System dependencies required: `python3`, `python3-gi`, `gir1.2-gtk-3.0`, `gir1.2-ayatanaappindicator3-0.1`, `bluez`, `python3-pip`, `libbluetooth-dev`.

```bash
git clone https://github.com/premtechworks/Synqvia.git
cd Synqvia/linux
./install.sh
```

---

## Pairing & Setup

Before connecting inside the app, devices must be paired at the OS level:

1. **Pair over Bluetooth**:
   - Open a terminal on Linux and run `bluetoothctl`:
     ```bash
     bluetoothctl
     [bluetooth]# power on
     [bluetooth]# discoverable on
     [bluetooth]# pairable on
     ```
   - On Android: open **Settings → Connected devices → Pair new device**, select your PC, and confirm the pairing code on both screens.
   - On Linux: trust the device:
     ```bash
     [bluetooth]# trust <ANDROID_MAC>
     ```

2. **Retrieve PC Bluetooth Adapter Address**:
   ```bash
   bluetoothctl show | grep "Controller"
   # Example output: Controller AA:BB:CC:DD:EE:FF [default]
   ```

3. **Configure Synqvia**:
   - Open Synqvia on Android and navigate to the **✦ Setup** screen.
   - Enter your Linux Bluetooth MAC address (`AA:BB:CC:DD:EE:FF`).
   - Tap **Save & Connect**.
   - Start the Linux daemon:
     ```bash
     synqvia --show
     ```
   - The connection indicator turns green once connected.

---

## Input Pathways & Fallbacks

Synqvia normalizes all clipboard sources through `ClipboardCaptureManager`:

| Pathway | Trigger | Android Version | Redundancy Role |
| :--- | :--- | :--- | :--- |
| **IME Service** | Active keyboard / switch to Synqvia | API 26 – 34+ | **Primary path**: direct foreground access and 1-tap paste |
| **Selection Action** | Highlight text → tap "Send to PC via Synqvia" | API 23+ | Direct manual handoff via `PROCESS_TEXT` |
| **Share Sheet** | Share Sheet → "Send to PC" | All APIs | Standard Android `ACTION_SEND` handoff |
| **Quick Settings Tile** | Swipe down QS → "Sync to PC" | API 24+ | Quick sync for recent selection via `TileService` |
| **Notification Action** | Tap "Sync to PC" in persistent notif | API 26+ | Fast trigger without opening main UI |
| **Accessibility Service** | Copy gesture text observation | API 26 – 34+ | Fallback when non-default IME is used |
| **Trampoline Activity** | Brief transparent focus grab | API 29+ | Auxiliary clipboard read fallback |

---

## Contributing

We welcome issues and pull requests!

### Reporting Issues
Please report bugs or request features on the [GitHub Issues](https://github.com/premtechworks/Synqvia/issues) tracker. Include your Linux distribution, Android OS version, and relevant logs (`synqvia-cli log -n 50` or `adb logcat -s Synqvia`).

### Development Setup & Testing

#### Android
- Requires Android Studio / JDK 17 (Gradle wrapper 9.3.1, AGP 9.1.1, compileSdk 36 / target 34 / min 26).
- Run unit test suite (Robolectric & JUnit):
  ```bash
  cd android
  ./gradlew test
  ```
- Build debug APK:
  ```bash
  ./gradlew assembleDebug
  ```
- Build release APK / bundle:
  ```bash
  ./gradlew assembleRelease bundleRelease
  ```

#### Linux
- Requires Python 3.10+ and GTK3 development headers.
- Run unit tests:
  ```bash
  python3 -m pytest tests/ -v
  ```
- Run the Linux daemon:
  ```bash
  python3 -m synqvia --show
  ```
- Use the headless CLI:
  ```bash
  synqvia-cli status --json
  synqvia-cli send "Hello from terminal"
  ```

---

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

---

## Links

- **Repository**: [github.com/premtechworks/Synqvia](https://github.com/premtechworks/Synqvia)
- **Releases**: [github.com/premtechworks/Synqvia/releases](https://github.com/premtechworks/Synqvia/releases)
- **Issue Tracker**: [github.com/premtechworks/Synqvia/issues](https://github.com/premtechworks/Synqvia/issues)
- **Wire Protocol**: [PROTOCOL.md](PROTOCOL.md)
- **QA & Test Plan**: [TEST_PLAN.md](TEST_PLAN.md)
