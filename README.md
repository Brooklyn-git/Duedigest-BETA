# Moodle Calendar Bridge

Extracts assignments and deadlines from iVirtual (Moodle) and outputs to ICS (for calendar apps), Logseq, and Obsidian.

Works as a CLI script, a desktop GUI (Flet), or an Android app (Flet APK).

## Features

- Fetches upcoming deadlines via Moodle REST API (`core_calendar_get_action_events_by_timesort` + `mod_assign_get_assignments`)
- **ICS** — imports into Fossify Calendar, Google Calendar, Thunderbird, etc.
- **Logseq** — Markdown pages with `DEADLINE:` property
- **Obsidian** — Markdown notes with `due:` in YAML frontmatter
- **Event caching** — detects new/changed events, generates delta ICS (`calendar_new.ics`)
- **Auto-reauth** — token expired? Re-logs in automatically
- **Configurable fetch window** — how many days back, max events
- **i18n** — English and Spanish
- **Dark/Light/System theme**

## Requirements

- Python 3.9+
- `flet` (for the GUI — `pip install flet`)
- Core logic uses **stdlib only** (no pip needed for CLI mode on Termux)

## Installation

```bash
git clone <repo>
cd moodle-calendar-bridge
python3 -m venv .venv
source .venv/bin/activate
pip install flet
```

## Usage

### GUI (desktop)

```bash
flet run moodle_cal_flet.py
```

Or directly:

```bash
python3 moodle_cal_flet.py
```

### GUI (Android APK)

```bash
flet build apk --org "com.yourorg" --product "Moodle Calendar"
```

Install the APK on your phone. The app stores data in its sandboxed directory and can share ICS files via the Android share sheet.

### CLI

```bash
# First-time auth
python3 moodle_cal.py --login

# Fetch and generate outputs
python3 moodle_cal.py
```

### Desktop sync with Syncthing (optional)

```bash
./sync_to_android.sh   # runs script + triggers Syncthing rescan
```

### Termux (optional)

```bash
./import_fossify.sh    # opens ICS in Fossify Calendar
```

## Configuration

All settings live in `config.json` (auto-created, gitignored):

| Key | Default | Description |
|---|---|---|
| `moodle_url` | `https://ivirtual.itson.edu.mx` | Your Moodle instance |
| `token` | `""` | Moodle web service token (set via `--login` or GUI auth) |
| `username` | `""` | Moodle username |
| `save_password` | `false` | Persist password to disk (not recommended) |
| `timezone` | auto-detect | IANA timezone (e.g. `America/Hermosillo`) |
| `language` | `en` | `en` or `es` |
| `theme_mode` | `system` | `system`, `light`, or `dark` |
| `output.ics` | `true` | Generate ICS file |
| `output.logseq` | `true` | Generate Logseq Markdown |
| `output.obsidian` | `true` | Generate Obsidian Markdown |
| `paths.ics` | `output/calendar.ics` | ICS output path |
| `paths.logseq` | `output/logseq/` | Logseq output directory |
| `paths.obsidian` | `output/obsidian/` | Obsidian output directory |
| `fetch_days_back` | `7` | How many days back to look for events |
| `fetch_limit` | `100` | Maximum events to fetch per request |

## Security

- **Password never stored by default** — discarded after login; only the token is kept
- **URL validation** — only HTTPS URLs accepted (rejected at input and before every API call)
- **config.json permissions** — set to `chmod 600` (owner-only read/write)
- **Clear credentials** button in GUI Settings wipes stored token
- On Android, app data is sandboxed by the OS

## Project structure

```
moodle-calendar-bridge/
├── moodle_cal.py          # Core logic (stdlib-only)
├── moodle_cal_flet.py     # Flet GUI (desktop + mobile)
├── config.json            # Configuration (gitignored)
├── langs.json             # Translations (en/es)
├── sync_to_android.sh     # Desktop: run + trigger Syncthing
├── import_fossify.sh      # Termux: open ICS in Fossify
├── README.md
└── output/                # Generated files (gitignored)
    ├── calendar.ics
    ├── calendar_new.ics
    ├── .event_cache.json
    ├── logseq/
    └── obsidian/
```
