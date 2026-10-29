# Changelog

All notable changes to PalateLog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.9] - 2026-10-29

### Fixed & Optimized
- **Static Analysis & Lint Cleanup (ADR 008)**:
  - Corrected WorkManager on-demand initialization in `AndroidManifest.xml` with `android:value="androidx.startup"` on the removal tag, resolving the `RemoveWorkManagerInitializer` lint error.
  - Eliminated Compose primitive boxing in `RestaurantDetailScreen.kt` by switching coordinate state to `mutableDoubleStateOf`.
  - Added explicit `Locale.getDefault()` to `GeoUtils.kt` distance formatting to prevent implicit locale conversion issues.
  - Modernized `Channel.values()` to Kotlin 2.0 `Channel.entries` in `VisitEntry.kt` for allocation-free enumeration.
  - Migrated URL parsing to KTX `.toUri()` in `ExternalMapNavigator.kt` and `SettingsScreen.kt`.
  - Migrated `SharedPreferences` write calls to KTX `prefs.edit { ... }` in `SyncRepository.kt`.
  - Quoted Mermaid diagram node strings containing emojis and quotes in `architecture_and_design.md` to resolve parser syntax errors.
  - Removed dead imports across `DashboardScreen.kt`, `DashboardViewModel.kt`, `MapScreen.kt`, `MapViewModel.kt`, `RestaurantDetailScreen.kt`, and `SettingsScreen.kt`.
  - Cleaned redundant properties in `.editorconfig`.

### Added & Documented
- **Lint Configuration & Intentional Suppressions (`app/lint.xml`)**:
  - Added `app/lint.xml` to globally ignore `UnusedResources` pending the planned full-app UI localization session.
  - Annotated intentional product design constraints: `UnnecessaryRequiredFeature` on touchscreen requirement in manifest, `@SuppressLint("HardwareIds")` on stable device ID in `SyncRepository.kt`, and `@Suppress("SameParameterValue")` in `SettingsViewModel.kt`.
- **Architecture Decision Record (ADR 008)**:
  - Added `ADR 008: Static Analysis & IDE Lint Observation Triage Strategy` to `doc/decisions_and_tradeoffs.md`.
- **Roadmap Tracked Task**:
  - Added comprehensive Compose UI localization and hardcoded string migration TODO to `doc/requirements_traceability_and_roadmap.md`.

## [1.2.8] - 2026-10-21

### Changed & Fixed
- **Touchscreen Hardware Filtering (`AndroidManifest.xml`)**:
  - Added `<uses-feature android:name="android.hardware.touchscreen" android:required="true" />` to manifest so Google Play filters out non-touchscreen Android TV set-top boxes and streaming devices.
- **Gradle Build Portability (`gradle.properties`)**:
  - Removed local machine hardcoded `org.gradle.java.home` path from `gradle.properties` so builds are portable across developer machines, operating systems, and CI/CD pipelines.
- **GitHub Documentation Mermaid Diagram Syntax (`architecture_and_design.md`)**:
  - Fixed a Mermaid flowchart rendering error in `doc/architecture_and_design.md` by escaping node label parentheses in double quotes (`"Tap 'Save & Finish (3s)'"`).
- **Public Repository Readiness & Git Author History**:
  - Unified repository Git commit history to use GitHub no-reply email (`5286594+karthikeyan-s@users.noreply.github.com`).
- **Repository Code Ownership (`.github/CODEOWNERS`)**:
  - Added `.github/CODEOWNERS` defining `@karthikeyan-s` as default code owner for repository governance.

## [1.2.7] - 2026-10-21

### Added & Refactored
- **Design System Dimensions & Spacing Tokens (`Dimens.kt`)**:
  - Created `core/designsystem/Dimens.kt` establishing central design system tokens for Spacing (`SpacingSmall = 8.dp`, `SpacingDefault = 16.dp`), Corner Radii (`RadiusMedium = 12.dp`, `RadiusLarge = 16.dp`, `RadiusDialog = 20.dp`), Icon Sizing (`IconSizeSmall = 16.dp`, `IconSizeDefault = 24.dp`, `IconSizeHero = 64.dp`), and Component Heights (`ButtonHeightStandard = 56.dp`).
  - Standardized component padding and dimensions across `Components`, `LocationPickerDialog`, `DashboardScreen`, `QuickCaptureScreen`, `EntryEditorScreen`, `RestaurantDetailScreen`, `MapScreen`, and `SettingsScreen`.
- **Strict Non-Transitive R Classes (`android.nonTransitiveRClass`)**:
  - Added `android.nonTransitiveRClass=true` to `gradle.properties`, enforcing strict submodule resource scoping and reducing compiled R class bytecode overhead.
- **UI String Resource Extraction & Localization Preparedness (`strings.xml`)**:
  - Extracted hardcoded UI string literals across all Jetpack Compose screens, navigation components, dialogs, badges, and design system elements into `app/src/main/res/values/strings.xml`.
  - Refactored `PalateLogNavGraph`, `Components`, `LocationPickerDialog`, `DashboardScreen`, `QuickCaptureScreen`, `EntryEditorScreen`, `RestaurantDetailScreen`, `MapScreen`, and `SettingsScreen` to use `@StringRes` and `stringResource(...)`.
  - Prepared codebase for internationalization (i18n) and localized screen readers (TalkBack) while resolving Android Lint `HardcodedText` warnings.

## [1.2.6] - 2026-10-21

### Added
- **Dish-Aware Unified Search & Tokenized Relevance Ranking (Phase 2A)**:
  - Added `getAllDishes()` in `DishFeedbackDao` to stream active dish feedback entries across all restaurants.
  - Implemented tokenized query processing in `DashboardViewModel` matching queries across restaurant metadata and specific dishes.
  - Ranked search results by relevance (exact phrase match > all-token match > partial-token match) so searching e.g. `"Chicken Fry"` lists exact matches at top and partial matches (like `"Chicken 65"`) down the order.
  - Displayed matched dish chips with verdict badges directly on `RestaurantCard` on Dashboard.
- **Camera & System PhotoPicker Integration (Phase 2A)**:
  - Added 1-tap **Gallery 🖼️** (`PickVisualMedia`) and **Camera 📷** (`TakePicture` via `FileProvider`) attachments in `EntryEditorScreen` (bill receipt and per-dish photos) and `QuickCaptureScreen`.
  - Stored persistent copies of media in internal app storage via `MediaUtils.copyUriToInternalStorage()` for zero-permission offline rendering.
- **Restaurant Details Photo Gallery & Fullscreen Preview (Phase 2A)**:
  - Displayed dish photo thumbnails in the Dish Insights Matrix and bill receipt photo thumbnails in `VisitHistoryCard` via Coil `AsyncImage`.
  - Implemented interactive tap-to-expand `FullscreenPhotoDialog` for full-screen photo viewing.
- **Two-Way Family Photo Sync via Google Drive**:
  - Implemented `PalateLog_Family/Photos/` subfolder management in `GoogleDriveSyncClient`.
  - Added photo filename mapping in `SyncVisit` (`billReceiptPhotoName` / `photoNames`) and `SyncDish` (`photoNames`) using unique UUID filenames.
  - Automatically uploads local photo files and downloads remote family photo files during Google Drive 2-way sync for offline rendering on all family devices.
- **Multiple Visit Photo Attachments**:
  - Upgraded visit-level photo attachments in `EntryEditorScreen` and `QuickCaptureScreen` to support attaching multiple photos (receipts, menu cards, ambience, etc.).
  - Added `VisitEntry.photoUris` helper and rendered horizontal thumbnail gallery for all visit photos on `VisitHistoryCard` in `RestaurantDetailScreen`.

### Fixed & Cleaned
- **Camera Permission & Gallery Folder Access**:
  - Removed `android.permission.CAMERA` declaration from `AndroidManifest.xml` so `ActivityResultContracts.TakePicture()` delegates camera captures directly to system camera app without requiring runtime permissions.
  - Replaced `PickVisualMedia` with `ActivityResultContracts.GetContent("image/*")` allowing browsing all phone folders (WhatsApp Images, Pictures, Downloads, SD Card, etc.) in system picker.
- **In-App Update Activity Result API Migration**:
  - Migrated `InAppUpdateHelper` and `MainActivity` from deprecated `onActivityResult` callback to `ActivityResultContracts.StartIntentSenderForResult()`, eliminating deprecation build warnings and improving type safety.
- **Code Hygiene & Kotlin Modernization**:
  - Updated `DishVerdict.values()` to `DishVerdict.entries` in `EntryEditorScreen`.
  - Added explicit `Locale.getDefault()` to `String.format` bill total formatting in `RestaurantDetailScreen`.
  - Migrated `SharedPreferences` writes to Kotlin KTX `prefs.edit { ... }` blocks in `InAppUpdateHelper`, `SettingsViewModel`, and `SyncRepository`.
  - Removed unused imports and unneeded exception parameters across location, dashboard, and detail modules.

## [1.2.5] - 2026-10-21

### Added
- **Dual-Mode Family Google Drive Sync**:
  - Implemented choice between **Mode A (Single Shared Google Account)** and **Mode B (Individual Google Accounts)** in Family Sync settings.
  - Added automated in-app family email invitations via Google Drive Permissions API (`POST /drive/v3/files/{folderId}/permissions`).
  - Added static, reusable QR Code & WhatsApp share link generation (`palatelog://sync?folderId=<folderId>`) encoding the immutable family `folderId`.
  - Implemented graceful permission pending error handling catching HTTP 404/403 when an uninvited family member scans the QR code, with a 1-tap **Ask Host via WhatsApp** pre-filled message action.

### Changed
- **Release Build R8 Minification & Resource Shrinking**:
  - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts` to optimize release AAB size, protect bytecode, and automatically supply deobfuscation mapping files to Google Play Console.
  - Added ProGuard rules in `proguard-rules.pro` for Room Database and Kotlinx Serialization data models.

## [1.2.4] - 2026-10-21

### Fixed
- **In-App Update Background Loop Fix**:
  - Prevented re-triggering update check loops in `onResume()`, resolving app backgrounding/closing when launching Google Play's system update dialog.
- **In-App Update "Restart App" Banner Layout**:
  - Added `.navigationBarsPadding()` to the bottom update banner in `MainActivity`, preventing overlap with Android system navigation gesture/button bar.
- **Layout & Double Inset Padding Fixes**:
  - Eliminated double Scaffold bottom padding in `PalateLogNavGraph`, `SettingsScreen`, and `DashboardScreen`.
  - Added bottom scroll spacer ensuring cards like "Offline Backup & Transfer" and "About PalateLog" are fully visible and never cut off.

## [1.2.3] - 2026-10-21

### Fixed & Enhanced
- **Google Shared Drive Single-File In-Place Sync**:
  - Replaced timestamped sync files with a single canonical file per device (`changes_<deviceId>.json`).
  - Implemented in-place `PATCH` updates on Google Drive, preventing folder clutter and eliminating missed entries caused by cutoff timestamps.
- **Visit & Dish Member Attribution**:
  - Automatically attaches the primary user's display name (`createdByMemberName`) to visit entries and dish feedback.
  - Displayed `👤 Logged by <MemberName>` on Visit History timeline cards in `RestaurantDetailScreen`.
- **Expandable Speed Dial FAB (Dashboard)**:
  - Replaced static action buttons with an expandable Material 3 Speed Dial FAB menu featuring smooth rotation and fade animations.
  - Added `140dp` bottom list padding so the last restaurant card can scroll completely above the action buttons.
- **Google Shared Drive Card UI Improvements**:
  - Re-laid out the Google Drive Connected status card with text truncation (`TextOverflow.Ellipsis`) and single-line wrapping to prevent button overlap on narrow screens.
- **Personalized Primary Profile Setup**:
  - Updated default primary member profile name to `"Me"`.
  - Added an **Edit Profile Name ✏️** action in `SettingsScreen` allowing each user to set their personalized name (e.g., `"Karthik"`, `"Priya"`).

## [1.2.2] - 2026-10-21

### Added
- **Google Play In-App Updates Integration**:
  - Integrated `com.google.android.play:app-update-ktx` for automatic update detection and seamless installation flows.
  - Implemented background **Flexible Updates** with an interactive Material 3 **Bottom "Restart App" Banner** upon download completion.
  - Implemented **Immediate / Forced Updates** for critical releases or releases matching configurable version staleness thresholds.

## [1.2.1] - 2026-10-21

### Added
- **Custom Color-Coded Map Pin Markers**:
  - Replaced plain map markers with vector-rendered pin badges on `MapScreen` and `LocationPickerDialog`.
  - Color-coded by overall verdict rating: 🟢 Emerald Green for **GO**, 🔴 Crimson Red for **NO-GO**, and 🟡 Amber Yellow for **CAUTION**.
  - Added category vector icons inside pins corresponding to restaurant type: Dine-in / Restaurant (🍽️), Highway Dhaba (🛖), Delivery / Cloud Kitchen (🛵), Cafe (☕), and Takeaway / Drive-Thru (🚗).
  - Designed distinct GPS user location pulse marker and custom location target pin.
  - Implemented `MapMarkerFactory` with memoized bitmap caching for zero-overhead rendering when panning or zooming the map.
- **Interactive Map Location Picker for Log Visit & Quick Capture**:
  - Added a **Pick on Map 🗺️** / **Change on Map** button to both **Log visit / Order** (`EntryEditorScreen`) and **3-Second Quick Log** (`QuickCaptureScreen`).
  - Implemented `LocationPickerDialog` featuring an interactive `osmdroid` map view with tap-to-pin placement and single-touch location locking.
  - Added automatic reverse-geocoding upon map location confirmation via `LocationClient.reverseGeocode(lat, lng)`, updating latitude, longitude, area/street, city, and nearby restaurant suggestion dynamically.

## [1.2.0] - 2026-10-21

### Added
- **Google Shared Drive Direct Cloud Sync (Phase 2B)**:
  - Integrated Google Account selection (`com.google.android.gms.common.AccountPicker`) and OAuth2 authorization (`https://www.googleapis.com/auth/drive.file` scope) for seamless cloud sync.
  - Implemented `GoogleDriveSyncClient` using OkHttp REST API calls to search, create, upload, list, and download JSON delta payloads to/from a dedicated `PalateLog_Family` folder in Google Drive.
  - Updated `SyncRepository` with 2-way delta merge capability (`performGoogleDriveSync()`) using Last-Write-Wins (LWW) conflict resolution and unique per-device delta files (`changes_<deviceId>_<timestamp>.json`).
  - Updated `SyncWorker` to run Google Drive cloud sync automatically in the background via WorkManager when connected.
  - Enhanced `SettingsScreen` with Google Drive Connection Card showing signed-in Google account email, connection status, Disconnect button, and manual "Sync Now" trigger.
- **JSON Backup File Import Picker**:
  - Added a **Select Backup File 📁** button in the Settings Import Dialog launching Android's system file picker (`ActivityResultContracts.GetContent()`), allowing users to restore backup files directly from device storage or cloud drives.

### Changed
- **Target SDK Upgrade to API 36**:
  - Updated `compileSdk` and `targetSdk` to level 36 (Android 16) in `app/build.gradle.kts` to meet Google Play Console publishing requirements.
  - Bumped `versionCode` to `5`.

### Fixed
- **WorkManager Startup Initialization Lint Error**:
  - Removed default `androidx.work.WorkManagerInitializer` from `AndroidManifest.xml` via `tools:node="remove"` to resolve fatal lint error (`RemoveWorkManagerInitializer`) during release bundle assembly when using Hilt's custom `Configuration.Provider`.

## [1.1.0] - 2026-03-09

### Changed
- **Application ID & Namespace Rename**:
  - Refactored package name and app ID from `com.karthik.palatelog` to `com.prismpixelvertex.palatelog` across Gradle scripts, source code, and project documentation.

### Fixed
- **JSON Export & Share Crash**:
  - Added root cache path (`<cache-path name="cache_files" path="." />`) and files path (`<files-path name="internal_files" path="." />`) to `res/xml/file_paths.xml` so `FileProvider` can grant URI access to generated backup JSON files.
  - Wrapped `FileProvider.getUriForFile` and `startActivity` share intent chooser inside `SettingsScreen.kt` in a `try-catch` block to prevent app crashes if sharing fails or if no sharing application is available.

## [1.0.2] - 2025-02-23

### Added
- **Interactive Map Location Picker**:
  - Added a **Pick on Map 🗺️** button in `EditRestaurantDialog` launching `LocationPickerDialog`.
  - Embedded an interactive OSMDroid map view in a modal dialog with single-tap and long-press pin placement and real-time reverse geocoding via `LocationClient`.
- **Restaurant Details & Geolocation Editing**:
  - Added an **Edit Restaurant ✏️** action in the top app bar of `RestaurantDetailScreen`.
  - Added an `EditRestaurantDialog` allowing users to edit restaurant name, street address, city, highway/route, cuisine, restaurant type, and overall verdict.
  - Added precise geolocation editing with latitude and longitude numeric fields and a 1-tap **Use GPS** action to fetch live GPS coordinates and auto-fill address/city.
  - Updated `RestaurantDetailViewModel` to use `restaurantDao.update` (and `EntryEditorViewModel` for existing restaurant edits).
- **Visit Entry Editing & Deletion**:
  - Added **Edit ✏️** and **Delete 🗑️** action icons to each `VisitHistoryCard` in `RestaurantDetailScreen`.
  - Added `EditVisitDialog` allowing users to modify order channel, total bill amount, visit verdict, and general notes.
  - Added soft-deletion for individual visit entries via `deleteVisit` in `RestaurantDetailViewModel`.
- **Reusable Verdict Choice Chips**:
  - Extracted `VerdictChoiceChip` into `com.karthik.palatelog.core.designsystem` for consistent design system usage across `EntryEditorScreen` and `RestaurantDetailScreen`.
- **Documentation & Architecture Decision Records (ADR)**:
  - Added **ADR 007: In-Place Metadata & Geolocation Correction vs. Immutable Logging** in `doc/decisions_and_tradeoffs.md`.
  - Updated requirements traceability matrix in `doc/requirements_traceability_and_roadmap.md`.

### Fixed
- **Foreign Key Cascade Deletion on Restaurant Updates**:
  - Fixed an issue where updating an existing restaurant replaced the parent row in SQLite via `OnConflictStrategy.REPLACE` (`DELETE` + `INSERT`), triggering foreign key `ON DELETE CASCADE` and wiping child visit history and dish feedback records. Replaced `restaurantDao.insert` with `restaurantDao.update` for existing entity updates, keeping all visit history completely intact.

## [1.0.1] - 2025-02-23

### Added
- **Interactive Map Spot Selection & Location Logging**:
  - Added single tap and long-press gesture handling on `MapScreen` using `MapEventsOverlay` to drop or move a custom **Selected Spot 📍** pin anywhere on the map.
  - Asynchronous reverse geocoding via `LocationClient` automatically resolves area/street and city details for tapped map points.
  - Added interactive bottom banner on `MapScreen` for custom map pins with one-tap actions for **Quick Log (3s)** and **Full Log** pre-filled with the selected coordinates and geocoded location.
  - Added a **Log Visit ➕** button to existing restaurant map cards, allowing users to quickly log a new visit to any existing restaurant directly from the map view.
  - Updated `QuickCaptureViewModel` and `EntryEditorViewModel` to accept custom latitude, longitude, area, and city navigation arguments passed from map selections.
  - Added editable Area, City, and Pinned Map Location indicators to `EntryEditorScreen`.
- **Custom Order / Visit Channels**:
  - Expanded default channels to include **Ownly**, **EatClub**, and **Magicpin**.
  - Added an `+ Add Option` chip and custom option creation dialog in `EntryEditorScreen` so users can add any new channel (e.g., "Ownly") on the fly.
  - Custom channel choices persist across app launches in local preferences and are retrieved from Room DB visit history.
  - Updated `ChannelBadge` to support dynamic channel display names with contextual icons (e.g., `DeliveryDining`).
  - Updated `DashboardViewModel` delivery filter to recognize delivery apps like Ownly.
- **Project Guidelines**:
  - Added `GEMINI.md` defining strict development guidelines for Git confirmation, issue analysis/RCA, feature proposals, and changelog maintenance.

### Fixed
- **External Map Navigation (`ExternalMapNavigator`)**:
  - Added `<queries>` declarations in `AndroidManifest.xml` for package visibility on Android 11+ (API Level 30+).
  - Implemented a robust 3-tier fallback chain (`google.navigation:` app -> `geo:` scheme -> `https://www.google.com/maps/...` browser) in `ExternalMapNavigator.kt`. Navigation now reliably opens driving directions across all devices, tablets, and emulators with or without Google Maps installed.
- **Keyboard Overlap on Log Visit / Order Screen**:
  - Added `.consumeWindowInsets(innerPadding).imePadding()` on main scrollable containers in `EntryEditorScreen`, `QuickCaptureScreen`, and `SettingsScreen`.
  - Input fields near the bottom (such as "General Visit Remarks", bill amounts, dish notes) now scroll smoothly above the software keyboard.

---

## [1.0.0] - 2025-02-23

### Added
- Initial Clean Architecture implementation using Jetpack Compose, Room Database, Hilt Dependency Injection, and Kotlin Coroutines.
- Core screens: Dashboard, Interactive OpenStreetMap, Settings & Google Shared Drive Sync, 3-Second Quick Capture Logger, and Full Visit/Dish Editor.
- Unit tests for distance calculation and location bounding box in `GeoUtilsTest`.
