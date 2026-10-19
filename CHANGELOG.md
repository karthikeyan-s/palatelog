# Changelog

All notable changes to PalateLog will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Custom Order / Visit Channels**:
  - Expanded default channels to include **Ownly**, **EatClub**, and **Magicpin**.
  - Added an `+ Add Option` chip and custom option creation dialog in `EntryEditorScreen` so users can add any new channel (e.g., "Ownly") on the fly.
  - Custom channel choices persist across app launches in local preferences and are retrieved from Room DB visit history.
  - Updated `ChannelBadge` to support dynamic channel display names with contextual icons (e.g., `DeliveryDining`).
  - Updated `DashboardViewModel` delivery filter to recognize delivery apps like Ownly.
- **Project Guidelines**:
  - Added `GEMINI.md` defining strict development guidelines for Git confirmation, issue analysis/RCA, feature proposals, and changelog maintenance.

### Fixed
- **Keyboard Overlap on Log Visit / Order Screen**:
  - Added `.consumeWindowInsets(innerPadding).imePadding()` on main scrollable containers in `EntryEditorScreen`, `QuickCaptureScreen`, and `SettingsScreen`.
  - Input fields near the bottom (such as "General Visit Remarks", bill amounts, dish notes) now scroll smoothly above the software keyboard.

---

## [1.0.0] - 2025-02-23

### Added
- Initial Clean Architecture implementation using Jetpack Compose, Room Database, Hilt Dependency Injection, and Kotlin Coroutines.
- Core screens: Dashboard, Interactive OpenStreetMap, Settings & Google Shared Drive Sync, 3-Second Quick Capture Logger, and Full Visit/Dish Editor.
- Unit tests for distance calculation and location bounding box in `GeoUtilsTest`.
