#!/bin/bash
# Installer: deps + launchers + Xfce autostart + app-menu entry.
# Layout (KDE-Connect style split: daemon-core + GUI + CLI in one package):
#   /usr/local/lib/synqvia/synqvia/   package
#   /usr/local/bin/synqvia               GUI daemon (tray; --show for window)
#   /usr/local/bin/synqvia-cli           headless CLI (status/history/send/clear/log)
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
sudo apt-get update
sudo apt-get install -y python3-gi gir1.2-gtk-3.0 gir1.2-ayatanaappindicator3-0.1 bluez python3-pip libbluetooth-dev
# PyBluez is required for SDP advertise (else Android UUID lookup fails;
# channel-1 fallback still works without it).
if command -v pip3 >/dev/null 2>&1; then
  pip3 install --user -r "$SCRIPT_DIR/requirements.txt" || sudo pip3 install --break-system-packages -r "$SCRIPT_DIR/requirements.txt"
else
  sudo apt-get install -y python3-bluez || echo "WARNING: install PyBluez manually for SDP advertise"
fi
# Single autostart entry (user-level only — a system copy would launch twice).
mkdir -p ~/.config/autostart
cp "$SCRIPT_DIR/synqvia.desktop" ~/.config/autostart/synqvia.desktop
# App-menu entry opens the window directly.
mkdir -p ~/.local/share/applications
sed 's|^Exec=.*|Exec=/usr/local/bin/synqvia --show|' "$SCRIPT_DIR/synqvia.desktop" \
  > ~/.local/share/applications/synqvia.desktop
sudo mkdir -p /usr/local/lib/synqvia
sudo cp -r "$SCRIPT_DIR/synqvia" /usr/local/lib/synqvia/
sudo tee /usr/local/bin/synqvia >/dev/null <<'EOF'
#!/bin/bash
export PYTHONPATH="/usr/local/lib/synqvia:${PYTHONPATH}"
exec python3 -m synqvia.main "$@"
EOF
sudo tee /usr/local/bin/synqvia-cli >/dev/null <<'EOF'
#!/bin/bash
export PYTHONPATH="/usr/local/lib/synqvia:${PYTHONPATH}"
exec python3 -m synqvia.cli "$@"
EOF
sudo chmod +x /usr/local/bin/synqvia /usr/local/bin/synqvia-cli
echo "Done. Pair via bluetoothctl first, then run: synqvia --show"
echo "Headless: synqvia-cli status | history | send \"text\" | log"
