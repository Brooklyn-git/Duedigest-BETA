#!/usr/bin/env bash
# Run this on Termux (Android) after syncing the ICS file
set -euo pipefail

ICS_FILE="$HOME/storage/shared/Syncthing/moodle-calendar-bridge/output/calendar.ics"

if [ ! -f "$ICS_FILE" ]; then
    echo "ICS file not found at $ICS_FILE"
    exit 1
fi

echo "Opening $ICS_FILE in Fossify Calendar..."
termux-open "$ICS_FILE"

echo "Done."
