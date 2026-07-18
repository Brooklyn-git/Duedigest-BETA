# DueNest

**Sync your Moodle deadlines with your calendar, effortlessly.**

DueNest is a cross-platform app (Android + Desktop) that automatically fetches your Moodle assignment deadlines and exports them to your favorite calendar, note-taking, or task manager app. Never miss a deadline again.

## Why DueNest?

Manually tracking Moodle deadlines is tedious and error-prone. DueNest connects directly to your Moodle instance, pulls your upcoming assignments, and pushes them wherever you already organize your life — whether that's Google Calendar, Obsidian, Logseq, or a simple task list.

## What it does

1. **Logs into Moodle** using your credentials (securely stored)
2. **Fetches your deadlines** — assignments, quizzes, and other time-sensitive activities
3. **Exports them** in the format you prefer:
   - **ICS files** — import into any calendar app (Google Calendar, Outlook, Apple Calendar, etc.)
   - **Logseq / Obsidian** — markdown files with proper metadata for your knowledge base
   - **Tasks** — a simple checkbox-based task list
4. **Stays up to date** — set up automatic reminders and background sync (Android)

You can also add **manual events** alongside your Moodle data, so everything lives in one place.

## Platforms

| Platform | Requirements |
|----------|--------------|
| **Android** (8.0+) | Android phone or tablet |
| **Desktop** (Linux / Mac / Windows) | JDK 17+ |

## Get started

### Android

1. Download the APK from the [Releases](https://github.com/brooklyn47/DueNest/releases) page (or build it yourself — see [DEV-README.md](DEV-README.md))
2. Install the APK on your device
3. Open DueNest and enter your Moodle URL, username, and password
4. Choose your output format and let DueNest fetch your deadlines

### Desktop

1. Clone the repository
2. Run `./gradlew :desktopApp:run`
3. Enter your Moodle credentials and configure your output

For detailed build instructions and development setup, see [DEV-README.md](DEV-README.md).

## Features at a glance

- **Multi-format export** — ICS, Logseq, Obsidian, Tasks
- **Auto-reauthentication** — seamless session handling
- **Customizable reminders** — daily, weekly, or custom schedules
- **Manual events** — add your own deadlines alongside Moodle data
- **Themes** — Light, Dark, AMOLED Dark, or follow your system setting
- **English & Spanish** — full localization support

## License

See [LICENSE](LICENSE) for details.