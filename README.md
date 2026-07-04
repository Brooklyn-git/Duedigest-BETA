# DueNest

DueNest fetches deadlines and activities from Moodle and pushes them to your calendar app. Android and Desktop share a single Kotlin codebase via Compose Multiplatform.

| Platform | Stack |
|----------|-------|
| **Android** | Kotlin (Compose, Jetpack Glance, WorkManager, OkHttp) |
| **Desktop** (Linux / Mac / Windows) | Compose Desktop (JVM, OkHttp) |

## Features

- Fetches upcoming deadlines via Moodle REST API (`core_calendar_get_action_events_by_timesort` + `mod_assign_get_assignments`)
- **ICS** — RFC 5545, importable into any calendar app
- **Logseq** — Markdown pages with `DEADLINE:` property
- **Obsidian** — Markdown daily notes with `due:` in YAML frontmatter
- **Tasks** — Grouped task list with checkboxes (intro file on first launch)
- **Manual events** — Add, edit, delete your own events alongside Moodle data
- **Auto-reauth** — re-logs in automatically when token expires
- **Android widgets** — Sync widget, task widget, combined fetch+tasks widget
- **Fetch / task reminders** — independent notifications with configurable schedules
- **i18n** — English and Spanish
- **4 theme modes** — Light / Dark / AMOLED Dark / System

## Requirements

### Android

- [Android Studio](https://developer.android.com/studio) or JDK 17+ + Android SDK
- Device running Android 8.0+ (API 26)
- Dependencies resolved automatically by Gradle

### Desktop

- JDK 17+
- Dependencies resolved automatically by Gradle

## Build & run

### Android

```bash
./gradlew :androidApp:assembleDebug
```

Install `android/app/build/outputs/apk/debug/app-debug.apk` on your phone.

### Desktop

```bash
./gradlew :desktopApp:run
```

## Configuration

### Android (in-app settings)

Credentials and preferences are stored in EncryptedSharedPreferences (AES-256 GCM). Configure via the app's settings screen.

| Setting | Description |
|---------|-------------|
| Theme | Light / Dark / AMOLED Dark / System |
| Language | English / Spanish |
| Fetch reminders | Independent schedule (daily / weekly / custom days & times) |
| Task reminders | Independent schedule — lists actual unchecked assignments with due dates |
| Output formats | ICS, Logseq, Obsidian, Tasks (each with configurable path) |
| Fetch options | Days back, max events |

## Project structure

```
moodle-calendar-bridge/
├── common/
│   ├── build.gradle.kts            # KMP — androidTarget + jvm("desktop")
│   └── src/commonMain/kotlin/com/moodlebridge/
│       ├── data/
│       │   ├── Event.kt            # Data classes for API + internal Event model
│       │   ├── IcsGenerator.kt     # ICS rendering (RFC 5545 VEVENT)
│       │   ├── MarkdownGenerator.kt# Markdown output (Logseq, Obsidian, Tasks)
│       │   ├── MoodleApi.kt        # REST client (OkHttp, login + 2 endpoints)
│       │   └── Strings.kt          # i18n strings (en/es)
│       └── ui/
│           └── Theme.kt            # Material 3 color schemes (Light / Dark / AMOLED Dark)
├── android/
│   └── app/
│       ├── build.gradle.kts        # App deps: :common, Compose, Glance, WorkManager
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── res/
│           │   ├── layout/
│           │   ├── values/
│           │   └── xml/
│           └── java/com/moodlebridge/
│               ├── MainActivity.kt              # App UI + settings (Compose)
│               ├── PasswordPromptActivity.kt     # Quick password prompt
│               ├── data/
│               │   ├── ConfigStore.kt            # EncryptedSharedPreferences wrapper
│               │   └── PathResolver.kt           # Content URI / file path handling
│               ├── worker/
│               │   ├── SyncWorker.kt             # Background sync (WorkManager)
│               │   ├── NotificationWorker.kt     # Fetch reminder notifications
│               │   └── TaskReminderWorker.kt     # Task deadline notifications
│               └── widget/
│                   ├── SyncWidget.kt
│                   ├── TaskWidget.kt
│                   ├── FetchTaskWidget.kt
│                   ├── OpacitySliderActivity.kt
│                   └── TaskOpacitySliderActivity.kt
├── desktopApp/
│   ├── build.gradle.kts            # Compose Desktop entry point
│   └── src/main/kotlin/com/moodlebridge/
│       └── Main.kt                 # Desktop app entry point (WIP)
├── build.gradle.kts                # Root — plugin declarations
├── settings.gradle.kts             # Module includes
├── gradlew / gradlew.bat           # Gradle wrapper
├── gradle/
│   └── wrapper/
├── AGENTS.md                       # Dev notes (architecture, API, design decisions)
├── LICENSE
└── README.md
```

## Security

### Android

- **AES-256 GCM encryption** — all credentials and preferences stored via `EncryptedSharedPreferences` (keys encrypted with AES-256 SIV, values with AES-256 GCM, master key in Android Keystore)
- **Cloud backup disabled** — `android:allowBackup="false"` prevents credential leakage via Google Drive
- **FileProvider** — ICS files shared via `content://` URI with `androidx.core.content.FileProvider` (no `file://` leaks)
- **Clear credentials** button in settings wipes all SharedPreferences
- **Password prompt** — when no password is stored, the widget launches `PasswordPromptActivity` (dialog-themed, no recents entry) to request one without exposing existing data

### Desktop

- Password never persisted by default — only the token stays on disk
- Token stored in local config (platform-specific preferences)
- Clear credentials button wipes all stored data
