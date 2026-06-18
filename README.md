# Moodle Calendar Bridge

Fetches deadlines and activities from Moodle and pushes them to your calendar app. Desktop (Python) and Android (Kotlin) are fully featured — both can log in, fetch, and generate ICS/Markdown. The Android app also includes a home screen widget for one-tap sync.

| Platform | Interface | Stack |
|----------|-----------|-------|
| **Desktop** (Linux/Mac/Windows) | CLI + GUI | Python (stdlib + Flet) |
| **Android** | App + home screen widget | Kotlin (Compose, Jetpack Glance, WorkManager, OkHttp) |

## Features

- Fetches upcoming deadlines via Moodle REST API (`core_calendar_get_action_events_by_timesort` + `mod_assign_get_assignments`)
- **ICS** — RFC 5545, importable into any calendar app
- **Logseq** — Markdown pages with `DEADLINE:` property
- **Obsidian** — Markdown daily notes with `due:` in YAML frontmatter
- **Event caching** — detects new/changed events, generates delta ICS (`calendar_new.ics`)
- **Auto-reauth** — re-logs in automatically when token expires
- **Configurable fetch window** — days back and event limit
- **Android app** — login, fetch events, generate ICS and MD, settings (theme, language), widget.
- **Android home screen widget** — per-instance opacity, one-tap sync from your home screen
- **Widget opacity** — adjustable per widget on first placement (slider + confirm), or globally from app settings
- **Sync notification** — tap to open ICS in your calendar app
- **i18n** — English and Spanish
- **Dark / Light / System theme**

## Requirements

### Desktop (Python)

- Python 3.9+
- `flet` for the GUI (`pip install flet`; core logic is stdlib-only)

### Android

- [Android Studio](https://developer.android.com/studio) or JDK 17+ + Android SDK
- Device running Android 8.0+ (API 26)

## Usage

### Desktop GUI

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

### Android app + widget

Build the APK:

```bash
cd android
./gradlew assembleDebug
```

Install `android/app/build/outputs/apk/debug/app-debug.apk` on your phone.

**First launch**: Open the app → enter Moodle URL, username, password → tap "Fetch". The app supports ICS generation, Logseq/Obsidian Markdown, event caching, theme switching, and language selection — same capabilities as the desktop version.

**Widget** (extra): Long-press home screen → add "Moodle Bridge" widget → configure opacity with the slider → tap "Confirm". The widget shows the last sync state and a "Fetch && sync" button.

**Sync from widget**: Tap "Fetch && sync" → notification appears with the result → tap it to open the ICS in your calendar app.


## Configuration

### Desktop (`config.json`)

| Key | Default | Description |
|---|---|---|
| `moodle_url` | — | Your Moodle instance URL |
| `token` | `""` | Web service token (set via `--login` or GUI login) |
| `username` | `""` | Moodle username |
| `save_password` | `false` | Persist password (not recommended) |
| `timezone` | auto-detect | IANA timezone, e.g. `America/Hermosillo` |
| `language` | `en` | `en` or `es` |
| `theme_mode` | `system` | `system`, `light`, or `dark` |
| `output.ics` | `true` | Generate ICS file |
| `output.logseq` | `true` | Generate Logseq Markdown |
| `output.obsidian` | `true` | Generate Obsidian Markdown |
| `paths.ics` | `output/calendar.ics` | ICS output path |
| `paths.logseq` | `output/logseq/` | Logseq output directory |
| `paths.obsidian` | `output/obsidian/` | Obsidian output directory |
| `fetch_days_back` | `7` | Days back to look for events |
| `fetch_limit` | `100` | Max events per request |

### Android (in-app settings)

The Android app stores credentials, preferences, and per-widget opacity in app-private SharedPreferences. Configure via the app's settings screen.

## Project structure

```
moodle-calendar-bridge/
├── moodle_cal.py              # Core logic — fetch, parse, generate outputs (stdlib-only)
├── moodle_cal_flet.py         # Desktop GUI (Flet)
├── config.json                # Desktop configuration (gitignored)
├── langs.json                 # Translations (en/es)
├── android/                   # Kotlin Android app
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── res/
│   │       │   ├── layout/sync_widget_initial.xml
│   │       │   ├── values/strings.xml, themes.xml
│   │       │   └── xml/sync_widget_info.xml, file_paths.xml
│   │       └── java/com/moodlebridge/
│   │           ├── MainActivity.kt           # App UI + settings (Compose)
│   │           ├── PasswordPromptActivity.kt  # Quick password prompt
│   │           ├── data/                      # ConfigStore, MoodleApi, IcsGenerator, Strings
│   │           ├── worker/SyncWorker.kt       # Background sync (WorkManager)
│   │           └── widget/                    # SyncWidget (Glance), OpacitySliderActivity
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── AGENTS.md                  # Dev notes (architecture, API, design decisions)
├── LICENSE
└── README.md
```

## Security

- **Password never stored by default** — discarded after login; only the token is kept
- **URL validation** — only HTTPS URLs accepted
- **config.json permissions** — set to `chmod 600` (owner-only read/write)
- **Clear credentials** button in settings wipes stored token
- **Android** stores data in app-private SharedPreferences, sandboxed by the OS
