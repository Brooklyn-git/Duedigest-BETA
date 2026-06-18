# Moodle Calendar Bridge

Extracts assignments and deadlines from Moodle and outputs to ICS (for calendar apps), Logseq, and Obsidian.

| Platform | Interface | Stack |
|----------|-----------|-------|
| **Desktop** (Linux/Mac/Windows) | CLI + GUI | Python (Flet) |
| **Android** | Home screen widget + config app | Kotlin (Jetpack Glance) |

## Features

- Fetches upcoming deadlines via Moodle REST API (`core_calendar_get_action_events_by_timesort` + `mod_assign_get_assignments`)
- **ICS** — imports into Fossify Calendar, Google Calendar, Thunderbird, etc.
- **Logseq** — Markdown pages with `DEADLINE:` property
- **Obsidian** — Markdown notes with `due:` in YAML frontmatter
- **Event caching** — detects new/changed events, generates delta ICS (`calendar_new.ics`)
- **Auto-reauth** — token expired? Re-logs in automatically
- **Configurable fetch window** — how many days back, max events
- **Android home screen widget** — one-tap sync from your home screen
- **Notification on sync** — tap notification to open ICS in your calendar app
- **i18n** — English and Spanish
- **Dark/Light/System theme**

## Requirements

### Desktop (Python)

- Python 3.9+
- `flet` (for the GUI — `pip install flet`)
- Core logic uses **stdlib only**

### Android (Kotlin widget)

- [Android Studio](https://developer.android.com/studio) (or JDK 17+ + Android SDK)
- A device running Android 8.0+ (API 26)

## Usage

### Desktop GUI (Linux/Mac/Windows)

```bash
python3 moodle_cal_flet.py
```

### Desktop CLI

```bash
# First-time auth
python3 moodle_cal.py --login

# Fetch and generate outputs
python3 moodle_cal.py
```

### Android widget + app

Build the APK:

```bash
cd android
./gradlew assembleDebug
```

Install `android/app/build/outputs/apk/debug/app-debug.apk` on your phone.

**First launch**: Open the app → enter your Moodle URL and web service token → tap "Save & Continue".

**Daily use**: Long-press your home screen → add the "Moodle Bridge" widget → tap "Sync Now" → a notification appears → tap it to open the ICS in your calendar app.

To get a Moodle web service token: Profile → Security keys → Create token.

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
| `moodle_url` | `https://` | Your Moodle instance |
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
- **Kotlin Android app** stores credentials in app-private SharedPreferences; token is entered directly (no password)
- On Android, app data is sandboxed by the OS

## Project structure

```
moodle-calendar-bridge/
├── moodle_cal.py              # Core logic (stdlib-only)
├── moodle_cal_flet.py         # Flet GUI (desktop)
├── config.json                # Configuration (gitignored)
├── langs.json                 # Translations (en/es)
├── sync_to_android.sh         # Desktop: run + trigger Syncthing
├── import_fossify.sh          # Termux: open ICS in Fossify
├── android/                   # Kotlin Android app (widget + config)
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/moodlebridge/
│   │       ├── MainActivity.kt
│   │       ├── data/           # ConfigStore, MoodleApi, IcsGenerator, Event
│   │       ├── worker/         # SyncWorker (WorkManager)
│   │       ├── widget/         # SyncWidget (Jetpack Glance)
│   │       └── ui/             # SettingsScreen (Jetpack Compose)
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── README.md
└── output/                     # Generated files (gitignored)
    ├── calendar.ics
    ├── calendar_new.ics
    ├── .event_cache.json
    ├── logseq/
    └── obsidian/
```
