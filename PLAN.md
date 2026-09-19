# Progress

## Done
- Multi-module build (root build.gradle.kts + settings.gradle.kts, :common, :androidApp, :desktopApp), Gradle wrapper moved to root, old Android root files deleted.
- common KMP module with androidTarget() + jvm("desktop"), Compose Multiplatform 1.7.3, OkHttp 4.12.0, kotlinx-serialization 1.6.2, kotlinx-coroutines 1.9.0.
- :androidApp updated to depend on :common; :desktopApp entry point with Compose Desktop.
- Shared sources (Event, MoodleApi, IcsGenerator, MarkdownGenerator, Strings, Theme) moved to common.
- MoodleApi: replaced Uri.encode with URLEncoder.encode for cross-platform.
- Theme: removed Android status bar code.
- Both platforms build and run.
- TaskItem rewritten to match Android TaskCard (animateColorAsState bg, course badge, colored icon + relative date, expand animation).
- FAB moved to BottomEnd (bottom-right).
- "Clear completed" only shown when completed tasks exist (derivedStateOf).
- Dialog theme fix (cs inside DuedigestTheme).
- Horizontal padding: 32dp throughout.
- 12h/24h order swapped (12h first).
- Date picker: 3 dropdowns (Year / Month / Day) in one row, widthIn(max=140.dp).
- Month names: localized via DateFormatSymbols (Jan/Feb… / ene/feb…).
- Time picker respects 12h/24h: 24h = Hour(0-23)+Min; 12h = Hour(1-12)+Min+AM/PM.
- File browser: Browse buttons open JFileChooser in Output Settings.
- Desktop sidebar layout replaces TabRow: collapsible left sidebar (180dp / 48dp) with NavItem composable.
- Settings as page on right side instead of dialog.
- Status text removed from Tasks tab.
- SyncManager: deletion tracking (deletedEventIds), merge with union of deleted IDs, skip completion for deleted events.
- Dual-format QR: `{"u":"http://<ip>:8765","d":"<base64>"}` for network+data, plain base64 for data-only fallback.
- Desktop HTTP server (com.sun.net.httpserver) for bidirectional sync.
- Android CameraX embedded scanner in sync dialog + OkHttp client for network sync.
- Android "Export QR" via FileProvider + Intent; Desktop "Import QR Image" via ZXing QRCodeReader.
- Desktop "Export QR" saves PNG file.
- i18n sync strings (en+es): sync_qr_title="Sync with nearby devices", sync_syncing, sync_connected, etc.
- Desktop JAR manifest: Add-Opens + Enable-Native-Access for JDK 26 compatibility.
- Server lifecycle: starts on app launch, stops on window close (not tied to dialog).
- Sync dialogs auto-close 1.5s after successful merge (both platforms).
- Both platforms build successfully.
- Android HTTP server on port 8766 for bidirectional sync (Android-to-Android and Android-to-Desktop).
- Desktop periodic sync loop: pushes changes to saved URL every N seconds (configurable).
- Android periodic sync loop: pushes changes to saved URL, now uses configurable interval.
- X-Sync-Server-URL header exchange: both platforms include their server URL in POST requests/responses, enabling bidirectional link.
- Desktop saves lastSyncUrl from QR import and from X-Sync-Server-URL response header.
- Android QR "Show QR" mode embeds server URL so scanning device can connect back.
- Auto-sync on app open: both platforms fire first sync within 1s of launch.
- WiFi change detection on Desktop: clears lastSyncUrl when network signature changes.
- Sync interval setting in Settings > Advanced: dropdown (Manual only, 15s, 30s, 1min, 5min, 10min, 30min).
- syncIntervalSeconds field in SyncPayload + MergeResult: synced between devices on merge.
- ConfigStore (Android) + DesktopConfigStore both persist syncIntervalSeconds + lastNetworkSignature.
- MoodleWebScraper: scrapeQuizzes() (via /mod/quiz/index.php + view page "closes/opens" labels) and scrapeForums() (via /mod/forum/index.php + due-date/cut-off notifications) wired into scrape(); quiz close in past is skipped; no-date forums/quizzes become "No due date" events.
- Task cards (desktop TaskItem + Android TaskCard): "Late" status (amber + warning icon) when due reached but cut-off not reached; expanded view shows Available from / Due / Cut-off lines (Format.formatDateTime) + "Description" subtitle above description; copy button hidden for quiz and forum events only.
- Quiz/forum course names resolved via core_enrol_get_users_courses (courseId→shortname map) instead of relying on the bounded calendar backfill; calendar backfill kept as fallback. core_enrol_get_users_courses requires the real userid (obtained via core_webservice_get_site_info) — userid=0/omitted makes it fail.
- Task badge shows event type (Task / Exam / Forum via Event.typeLabelKey + type_task/type_exam/type_forum) instead of course name, which stays as section grouping header.
- Course names use the WS full name (fullname/displayname/fullnamedisplay) with shortname fallback, in calendar events, assignments, and the enrolled-course map — matching the scraper's full-name headers (fixes course codes like 118C-12901).
- "Skip finished tasks" (Settings > Advanced, default on): uses mod_assign_get_submission_status per assignment to hide already-submitted/graded tasks at fetch time. Overdue tasks already filtered at fetch.
- Task type cards: badge color/icon per type (Task=secondary+EventNote, Exam=primary+School, Forum=teal+Forum) via Event.typeKey + typeBadgeStyle on both platforms. Android now depends on material-icons-extended.

## In Progress
- (none)

## Blocked
- (none)

# Key Decisions
- Collapsible sidebar navigation (Connection / Tasks / Settings) instead of top TabRow.
- Settings is a full-page view on the right, not a modal dialog.
- Date/time input uses dropdown pickers (3-column date, 2- or 3-column time).
- showSettings state removed (replaced by selectedSection == 2).
- currentTab replaced by selectedSection (0=Connection, 1=Tasks, 2=Settings).
- NavItem composable with Build icon for Connection (not Link/Wifi, which aren't in default Material icons).
- Server runs at app level (not dialog level) so it persists across dialog open/close.
- Sync QR defaults to data+URL format; falls back to data-only if no IP available.

# Critical Context
- Kotlin 2.1.10, Gradle 9.5.1, AGP 8.12.2, Compose Multiplatform 1.7.3.
- animateColorAsState and AnimatedVisibility from androidx.compose.animation.
- Icons.Default.Build used for Connection (Link/Wifi not available in default icons core).
- Type badge icons (EventNote/School/Forum) need material-icons-extended on Android; desktop already includes compose.materialIconsExtended.
- mod_assign_get_submission_status: per-assignment student-usable WS call; submission.status=="submitted" or lastattempt.graded means finished. AssignResponse supplies the assignment instance id (not just cmid).
- widthIn(max = 140.dp) on SimpleDropdown to prevent stretching.
- DateFormatSymbols.getInstance(locale).shortMonths.take(12) for month names.
- JFileChooser from javax.swing used for directory browsing.
- SimpleDropdown helper wraps ExposedDropdownMenuBox + OutlinedTextField.
- NavItem composable in App.kt renders icon + label with selected background.
- Desktop runs on JDK 26 — JAR manifest must include Add-Opens and Enable-Native-Access for Skiko.
- Android CameraX PreviewView + ImageAnalysis + ZXing QRCodeReader for embedded scanner.
- Desktop HTTP server on port 8765; Android HTTP server on port 8766.
- X-Sync-Server-URL header exchange enables bidirectional link without QR re-scan.
- syncIntervalSeconds: 0=Manual, 15, 30, 60, 300, 600, 1800. Default 30.
- Desktop WiFi detection: network signature stored in DesktopConfigStore.lastNetworkSignature.

# Security Fix Plan (added 2026-09-19)

Security review outcome. Fixes are ordered by priority; each is an independent, atomically revertible commit. Follow TDD per AGENTS.md: write the failing test first, then implement, then build.

Build/verify after each fix:
- `./gradlew :common:desktopTest` (common lib + tests)
- `./gradlew :androidApp:assembleDebug`
- `./gradlew :desktopApp:jar && bash run.sh`
- Manually test the affected flow on both platforms.

## Threat model
- Active LAN attacker on the same Wi-Fi (can send/receive on the sync port).
- Passive LAN attacker (can sniff plaintext HTTP sync).
- Malicious/compromised data sources: QR codes, sync peers, Moodle event URLs.
- Trusted: the Moodle server (TOFU cert accept flow already exists), the user's own devices.

## Fix 1 — Authenticated LAN sync (High: open `/sync` endpoint + sync hijack)
Closes the unauthenticated POST and the `X-Sync-Server-URL` trust issue.

Design: two-token pairing.
- Each device owns a `deviceToken` (validates inbound POSTs to its own server) and a separate `peerToken` (sent on outbound POSTs to the paired device). Tokens are exchanged via the existing QR pairing and a request header — no new UI, no passwords to type.
- QR gains an `"s"` field carrying the generator's `deviceToken`: `{"u":"<server url>","d":"<data>","s":"<deviceToken>"}`. Old QRs without `s` are rejected with a "device must be updated to pair" message.

Changes:
- `common` `SyncManager.kt`:
  - `fun decodeQrContent(content): Triple<String?, SyncPayload?, String?>` (url, payload, token).
  - `fun encodeQrContent(serverUrl, payload, token?)` — includes `"s"` when token provided.
  - `expect fun randomToken(): String` + actuals in `androidMain`/`desktopMain` using `java.security.SecureRandom` (32 hex chars).
  - Size guards in `deserializePayload`: reject decoded base64 > MAX_SYNC_BODY (e.g. 2 MB) and decompressed output > MAX_SYNC_BODY (decompression-bomb DoS).
- `config` (both `ConfigStore.kt` and `DesktopConfigStore.kt`): add `deviceToken` (lazy-generated, persisted) and `peerToken` (string). `lastSyncUrl` stays as the peer URL.
- Sync servers (`MainActivity.kt` NanoHTTPD ~680-728, `App.kt` HttpServer ~793-836):
  - Read `Authorization: Bearer <token>`. If missing/!= `config.deviceToken` → 401, no merge, no `lastSyncUrl`/`peerToken` update.
  - On valid POST: merge, then adopt `X-Sync-Server-URL` (→ `lastSyncUrl`) and `X-Sync-Device-Token` (→ `peerToken`) so the peer can POST back.
- Sync clients (periodic loop `MainActivity.kt:612-675` / `App.kt:885-929`, scan POST ~1323-1365, import ~323-355):
  - Send `Authorization: Bearer <peerToken>` + `X-Sync-Device-Token: <deviceToken>` + existing `X-Sync-Server-URL`.
  - On scan/import: store `peerToken` from QR `s` + `lastSyncUrl` from `u`; skip the merge fallback if token is null (show "update other device").
- Test (`common/src/desktopTest/.../SyncSecurityTest.kt`): token encode/decode roundtrip; 401 path (server rejects wrong token) via unit-testing a small pure helper `SyncServerAuth.isAuthorized(header, deviceToken)`; old-QR (no `s`) yields null token.
- Note: pairs sync only if the other device is on this version. Forward-compatible QR generation.

## Fix 2 — Moodle URLs forced to HTTPS (High: cleartext credentials)
- `common` `MoodleApi.kt`: add `normalizeMoodleUrl(url)`: upgrade `http://` → `https://`; reject anything without http(s) scheme (throw config error). Call on `login()` entry.
- Android `MainActivity.kt` + desktop `App.kt`: normalize + validate on every URL save and at fetch start; set a clear error message via `Strings` (new `connection_url_invalid` EN/ES).
- Keep `android:usesCleartextTraffic="true"` — LAN sync needs plaintext HTTP to arbitrary LAN IPs (not expressible in network security config). Document: only the LAN sync channel is cleartext; Moodle creds are HTTPS-only.
- Behavior change: an `http://` Moodle URL is now auto-upgraded/blocked. Flag with the dev; route users with self-signed HTTPS through the existing TOFU accept flow.

## Fix 3 — Desktop no longer persists the password (Medium)
- `DesktopConfigStore.kt`: stop writing `password` to `Preferences`. Keep token persisted (needed for background fetch).
- `App.kt` (~170-178, 574-576): password stays in-memory for the session regardless of `savePassword`; `savePassword` only keeps the checkbox state + username. On close, password is gone.
- Android: already EncryptedSharedPreferences — no change.
- Test: unit test for the config-store behavior (desktopTest can't reach desktopApp module; do a manual check or move logic note). Manual verification: save with savePassword on → relaunch → password field empty.

## Fix 4 — Stop putting the password in WorkManager inputData (Medium)
- `PasswordPromptActivity.kt:58`: replace `putString("override_password", password)` with a sentinel flag only; write the password to `ConfigStore.pendingPassword` (EncryptedSharedPreferences, same store as `password`).
- `ConfigStore.kt`: add `pendingPassword: String` get/set + `clearPendingPassword()`.
- `SyncWorker.kt:38-39`: `val pw = config.pendingPassword.ifBlank { config.password }`; clear `pendingPassword` after reading. Keep `fromApp` detection via the inputData sentinel.

## Fix 5 — Validate server URLs from QR/import/headers before POSTing (Medium)
- `common` `SyncManager.kt`: `fun isValidServerUrl(url): Boolean` — scheme must be `http`/`https` and host must be `localhost`, a loopback, RFC1918, link-local, or ULA IP literal; refuse public hosts and other schemes. `fun isSafeHttpUrl(url): Boolean` — scheme ∈ {http, https} (used by Fix 6).
- Enforce in: scan POST (`MainActivity.kt:1323`, desktop import ~330), periodic loops (before each POST), and before adopting any `X-Sync-Server-URL`/response header value (server + client side).
- Test: acceptance/rejection matrix (public host rejected; localhost ok; `file://`, `intent://`, trailing-path weirdness rejected; IPv6 literal ok).

## Fix 6 — Event URL allowlist for "Open in browser" (Medium)
- `MainActivity.kt:2376-2388` and desktop open-browser path: before `ACTION_VIEW`, require `isSafeHttpUrl(event.url)`; otherwise ignore the tap.
- Test: `isSafeHttpUrl("intent://...")==false`, `("file:///etc/passwd")==false`, `("https://...")==true`.

## Fix 7 — Exported components + desktop logging (Low)
- `AndroidManifest.xml`: confirm only launcher (`MainActivity`) and `APPWIDGET_CONFIGURE` activities (Opacity/TaskOpacity sliders) are exported — required by Android; leave as-is. No other activity is exported. Widget receivers export is required. No change expected.
- Slider activities: validate the incoming widget-id extra (only used for opacity config) so malformed extras can't be written.
- Desktop `Main.kt:12`: `Log.init(false)` by default; enable only with `-Dduedigest.debug=true`.

## Fix 8 — Residual: plaintext LAN sync channel (accepted risk, High #3)
- Decide + document: sync stays plaintext HTTP on the LAN. Passive sniffers on shared Wi-Fi can read task data and the device token during sync. Mitigations without TLS: pair/sync only on trusted networks; add a "Forget synced device" action (Settings > Advanced) that wipes `lastSyncUrl`, `peerToken`, `deviceToken` (regenerate) — cheap and useful.
- Future work (not now): LAN TLS via ephemeral self-signed cert with the fingerprint carried in the QR `s` field + reuse of the TOFU accept flow.

## Out of scope for this pass
- `wstoken` in query string (Moodle REST design; covered by HTTPS enforcement in Fix 2).
- Rate-limiting the sync endpoints (auth token + small payload cap already raise the bar; revisit if needed).
- Full LAN TLS (Fix 8 future work).

# Relevant Files
- desktopApp/src/main/kotlin/com/moodlebridge/ui/App.kt: all desktop UI, sync server, QR display/import/export.
- desktopApp/src/main/kotlin/com/moodlebridge/Main.kt: desktop entry point.
- desktopApp/src/main/kotlin/com/moodlebridge/data/DesktopConfigStore.kt: preferences-based config store.
- desktopApp/build.gradle.kts: desktop deps (ZXing, OkHttp), JAR manifest (Add-Opens, Enable-Native-Access).
- common/src/commonMain/kotlin/com/moodlebridge/data/SyncManager.kt: core sync logic (SyncPayload, MergeResult, QR encoding).
- common/src/commonMain/kotlin/com/moodlebridge/data/Strings.kt: i18n strings (en+es).
- common/src/commonMain/kotlin/com/moodlebridge/data/Event.kt: Event data class.
- common/src/androidMain/kotlin/com/moodlebridge/data/PlatformEncoding.kt: expect/actual gzip+base64 for Android.
- common/src/desktopMain/kotlin/com/moodlebridge/data/PlatformEncoding.kt: expect/actual gzip+base64 for Desktop.
- android/app/src/main/java/com/moodlebridge/MainActivity.kt: Android UI, sync dialog with embedded scanner, OkHttp client.
- android/app/src/main/java/com/moodlebridge/data/ConfigStore.kt: Android config store.
- android/app/build.gradle.kts: Android deps (CameraX, ZXing, OkHttp, material-icons-extended).
- android/app/src/main/AndroidManifest.xml: permissions (INTERNET, CAMERA, etc.).
