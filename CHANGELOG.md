# Changelog

All notable changes to PalateLog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
