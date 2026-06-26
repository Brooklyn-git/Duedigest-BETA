# DueNest

DueNest fetches deadlines and activities from Moodle and pushes them to your calendar app. Desktop (Python) and Android (Kotlin) are fully featured — both can log in, fetch, and generate ICS/Markdown. The Android app also includes a home screen widget for one-tap sync.

| Platform | Interface | Stack |
|----------|-----------|-------|
| **Desktop** (Linux/Mac/Windows) | CLI + GUI | Python (stdlib + Flet) |
| **Android** | App + home screen widget | Kotlin (Compose, Jetpack Glance, WorkManager, OkHttp) |

## Features

- Fetches upcoming deadlines via Moodle REST API (`core_calendar_get_action_events_by_timesort` + `mod_assign_get_assignments`)
- **ICS** — RFC 5545, importable into any calendar app (primary output)
- **Logseq** — Markdown pages with `DEADLINE:` property
- **Obsidian** — Markdown daily notes with `due:` in YAML frontmatter
- **Delta ICS** — detects new/changed events via cache, generates `calendar_new.ics` for incremental import (desktop only)
- **Auto-reauth** — re-logs in automatically when token expires on both platforms
- **Configurable fetch window** — days back and event limit
- **Android app** — login, fetch events, generate ICS, settings (theme, language), widget
- **Android home screen widget** — per-instance opacity slider on first placement, one-tap "Fetch && sync" button, tap notification to open ICS in calendar app
- **i18n** — English and Spanish on both platforms
- **Dark / Light / System theme**
- **API contract** — `desktop/moodle_api.yaml` (OpenAPI 3.0) documents both endpoints + auth as single source of truth across platforms

## Requirements

### Desktop (Python)

- Python 3.9+ (stdlib-only core — runs in Termux without pip)
- `flet` for the GUI (`pip install flet`)
- `keyring` for saving credentials in the OS keychain (optional — `pip install keyring`; falls back to config.json if absent)

### Android

- [Android Studio](https://developer.android.com/studio) or JDK 17+ + Android SDK
- Device running Android 8.0+ (API 26)
- Dependencies resolved automatically by Gradle (OkHttp, Compose, Glance, WorkManager, kotlinx-serialization, security-crypto)

## Usage

### Desktop GUI

```bash
python3 desktop/moodle_cal_flet.py
```

### Desktop CLI

```bash
# First-time auth
python3 desktop/moodle_cal.py --login

# Fetch and generate outputs
python3 desktop/moodle_cal.py
```

### Android app + widget

Build the APK:

```bash
cd android
./gradlew assembleDebug
```

Install `android/app/build/outputs/apk/debug/app-debug.apk` on your phone.

**First launch**: Open the app → enter Moodle URL, username, password → tap "Fetch". The app supports ICS generation, theme switching, and language selection.

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

The Android app stores credentials, preferences, and per-widget opacity in EncryptedSharedPreferences (AES-256 GCM). Configure via the app's settings screen.

## Project structure

```
moodle-calendar-bridge/
├── desktop/
│   ├── moodle_cal.py              # Core logic — fetch, parse, generate outputs (stdlib-only)
│   ├── moodle_cal_flet.py         # Desktop GUI (Flet)
│   ├── moodle_api.yaml            # OpenAPI 3.0 contract for all Moodle endpoints
│   ├── config.json                # Desktop configuration (gitignored)
│   ├── langs.json                 # Translations (en/es)
│   └── test_pipeline.py           # Test / validation (incl. contract conformance)
├── android/
│   ├── build.gradle.kts           # Root build — AGP 8.12.2, Kotlin 2.1.10
│   ├── settings.gradle.kts
│   ├── local.properties           # SDK path (machine-local)
│   ├── gradlew / gradlew.bat     # Gradle wrapper
│   ├── gradle/wrapper/
│   └── app/
│       ├── build.gradle.kts       # App deps: Compose, Glance, WorkManager, OkHttp, security-crypto
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── res/
│           │   ├── layout/
│           │   │   └── sync_widget_initial.xml
│           │   ├── values/
│           │   │   ├── strings.xml
│           │   │   └── themes.xml
│           │   └── xml/
│           │       ├── sync_widget_info.xml
│           │       └── file_paths.xml
│           └── java/com/moodlebridge/
│               ├── MainActivity.kt              # App UI + settings (Compose)
│               ├── PasswordPromptActivity.kt     # Quick password prompt (dialog-themed)
│               ├── data/
│               │   ├── ConfigStore.kt            # EncryptedSharedPreferences wrapper
│               │   ├── Event.kt                  # Data classes for API + internal Event model
│               │   ├── IcsGenerator.kt           # ICS rendering (RFC 5545 VEVENT)
│               │   ├── MoodleApi.kt              # REST client (OkHttp, login + 2 endpoints)
│               │   └── Strings.kt                # i18n strings (en/es)
│               ├── worker/
│               │   └── SyncWorker.kt             # Background sync (WorkManager)
│               └── widget/
│                   ├── SyncWidget.kt             # Glance widget (per-instance opacity)
│                   └── OpacitySliderActivity.kt  # Widget configure activity (slider + confirm)
├── AGENTS.md                  # Dev notes (architecture, API, design decisions)
├── LICENSE
└── README.md
```

## Security

### Desktop

- **Password never persisted by default** — discarded after login; only the token stays on disk
- **OS keychain** — password can be stored securely via `keyring` (optional, `pip install keyring`)
- **Token stored in `config.json`** — file permissions set to `600` (owner-only)
- **URL validation** — only HTTPS URLs accepted
- **Clear credentials** button in settings wipes token + keychain entry
- **Auto-reauth** — if token expires, re-authenticates using stored password (no manual re-entry)

### Android

- **AES-256 GCM encryption** — all credentials and preferences stored via `EncryptedSharedPreferences` (keys encrypted with AES-256 SIV, values with AES-256 GCM, master key in Android Keystore)
- **Cloud backup disabled** — `android:allowBackup="false"` prevents credential leakage via Google Drive
- **FileProvider** — ICS files shared via `content://` URI with `androidx.core.content.FileProvider` (no `file://` leaks)
- **Clear credentials** button in settings wipes all SharedPreferences
- **Password prompt** — when no password is stored, the widget launches `PasswordPromptActivity` (dialog-themed, no recents entry) to request one without exposing existing data
