#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
VERSION="1.0.0"
PKG_DIR="/tmp/synqvia_${VERSION}_all"

rm -rf "$PKG_DIR"
mkdir -p "$PKG_DIR/DEBIAN"
mkdir -p "$PKG_DIR/usr/local/lib/synqvia/synqvia"
mkdir -p "$PKG_DIR/usr/local/bin"
mkdir -p "$PKG_DIR/etc/xdg/autostart"
mkdir -p "$PKG_DIR/usr/share/applications"

# Control file
cat <<EOF > "$PKG_DIR/DEBIAN/control"
Package: synqvia
Version: ${VERSION}
Section: utils
Priority: optional
Architecture: all
Maintainer: premtechworks <premtechworks@gmail.com>
Depends: python3, python3-gi, gir1.2-gtk-3.0, gir1.2-ayatanaappindicator3-0.1, bluez
Description: Instant local clipboard handoff via Bluetooth
 Synqvia provides seamless, private, and offline clipboard synchronization
 between Linux and Android devices using Bluetooth Classic RFCOMM/SPP.
EOF

# Copy Python module files
cp -r "$SCRIPT_DIR/synqvia/"* "$PKG_DIR/usr/local/lib/synqvia/synqvia/"
# Remove any caches
find "$PKG_DIR" -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find "$PKG_DIR" -type f -name "*.pyc" -delete 2>/dev/null || true

# Launchers
cat <<'EOF' > "$PKG_DIR/usr/local/bin/synqvia"
#!/bin/bash
export PYTHONPATH="/usr/local/lib/synqvia:${PYTHONPATH}"
exec python3 -m synqvia.main "$@"
EOF

cat <<'EOF' > "$PKG_DIR/usr/local/bin/synqvia-cli"
#!/bin/bash
export PYTHONPATH="/usr/local/lib/synqvia:${PYTHONPATH}"
exec python3 -m synqvia.cli "$@"
EOF

chmod 755 "$PKG_DIR/usr/local/bin/synqvia" "$PKG_DIR/usr/local/bin/synqvia-cli"

# Desktop files
cp "$SCRIPT_DIR/synqvia.desktop" "$PKG_DIR/etc/xdg/autostart/synqvia.desktop"
sed 's|^Exec=.*|Exec=/usr/local/bin/synqvia --show|' "$SCRIPT_DIR/synqvia.desktop" \
  > "$PKG_DIR/usr/share/applications/synqvia.desktop"

OUTPUT_DIR="$ROOT_DIR/dist"
mkdir -p "$OUTPUT_DIR"
DEB_FILE="$OUTPUT_DIR/synqvia_${VERSION}_all.deb"

dpkg-deb --build --root-owner-group "$PKG_DIR" "$DEB_FILE"
rm -rf "$PKG_DIR"

echo "Successfully built: $DEB_FILE"
