# Changelog

All notable changes to PalateLog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-10-21

### Added
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
