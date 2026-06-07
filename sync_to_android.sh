#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "=== Moodle Calendar Bridge ==="
python3 moodle_cal.py

echo ""
echo "Triggering Syncthing rescan..."
if command -v syncthing &> /dev/null; then
    syncthing cli rescan --all
    echo "Syncthing rescan triggered."
else
    echo "syncthing CLI not found. Rescan manually or use the GUI."
fi

echo "Done."
