# PalateLog: Requirements Traceability Matrix & Roadmap

This document maps all original user requirements against the current codebase implementation and defines prioritized next steps.

---

## 1. Requirements Traceability Matrix

| # | User Requirement | Implementation Status | Implementing Components & Code Locations |
| :--- | :--- | :---: | :--- |
| **1** | Record feedback at Restaurant level or at food level in a restaurant | **100% Complete** ✅ | • `RestaurantEntity` (`overallVerdict` [GO, NO_GO, CAUTION])<br>• `DishFeedbackEntity` (`dishName`, `category`, `verdict` [GO, NO_GO, MEH], `tags`)<br>• `RestaurantDetailScreen` (Aggregated Dish Matrix, In-Place Editing of Restaurant & Visits)<br>• `EntryEditorScreen` (Multi-dish editor) |
| **2** | Geocoded, so that I can search about it when near that place | **100% Complete** ✅ | • `LocationClient.kt` (Fused GPS + reverse geocoding via `Geocoder`)<br>• `GeoUtils.kt` (Haversine distance calculation in meters/km)<br>• `DashboardViewModel.kt` (Near Me <15km filter & distance sorting)<br>• `RestaurantCard` (Displays distance badge e.g. "1.2 km away")<br>• `MapScreen.kt` (Interactive map with location centering)<br>• `RestaurantDetailScreen` (In-place edit of address, city, & GPS coordinates) |
| **3** | Record feedback with result (Go / No-Go) | **100% Complete** ✅ | • `Verdict` enum (`GO`, `NO_GO`, `CAUTION`)<br>• `DishVerdict` enum (`GO`, `NO_GO`, `MEH`)<br>• `VerdictBadge.kt` (Green thumbs-up, Red octagon warning, Amber caution)<br>• `DishVerdictBadge.kt` (🟢 Must Try, 🔴 Avoid, 🟡 Meh) |
| **4** | Query by various parameters (geo location, restaurant name, type, cuisine, food name) | **85% Complete** 🔄 | • `DashboardViewModel.kt` (Multi-parameter query across name, city, cuisine, highway route, tags)<br>• `RestaurantDao.searchRestaurants()`<br>• `DishFeedbackDao.searchDishes()`<br>*Next step: Join dish search directly into Dashboard search query.* |
| **5** | Record entry type (walk-in, online order, which app used, remarks like who liked food) | **100% Complete** ✅ | • `Channel` enum (`WALK_IN`, `HIGHWAY_STOP`, `ZOMATO`, `SWIGGY`, `TAKEAWAY`, `DRIVE_THRU`)<br>• `ChannelBadge.kt`<br>• `memberPreferences: Map<String, String>` on `DishFeedbackEntity`<br>• `FamilyReactionTag.kt` (e.g. "Wife: Loved it, Karthik: Avoid")<br>• Dashboard Filter Chip: "🛵 Zomato / Swiggy" |
| **6** | Sync data to Google Shared Drive (separate private vs shared data entries) | **100% Complete** ✅ | • `isShared: Boolean` on all entities<br>• `SyncRepository.kt` (Exports only `isShared == true` records; private records stay local)<br>• Two-way delta merge with Last-Write-Wins (LWW) conflict resolution<br>• `GoogleDriveSyncClient.kt` (Google Sign-In + OkHttp REST API client)<br>• `PalateLog_Family` folder in user's Google Drive<br>• WorkManager background sync (`SyncWorker.kt`) |
| **7** | Store pictures along with entries (optionally shown when viewing details) | **75% Complete** 🔄 | • `photoUris: List<String>` in `DishFeedbackEntity`<br>• `billReceiptPhotoUri: String?` in `VisitEntryEntity`<br>• Coil Compose library configured in `app/build.gradle.kts`<br>• FileProvider configured in `AndroidManifest.xml` & `file_paths.xml`<br>*Next step: Connect AndroidX PhotoPicker UI directly in editor & show thumbnail gallery.* |
| **8** | Optionally store bill, price details | **100% Complete** ✅ | • `billAmount: Double` in `VisitEntryEntity`<br>• `price: Double` in `DishFeedbackEntity`<br>• Bill amount input in `EntryEditorScreen`<br>• Bill amount display in `VisitHistoryCard` (`RestaurantDetailScreen`) |
| **9** | Store multiple visits to same restaurant for aggregated metrics | **100% Complete** ✅ | • `RestaurantEntity` 1-to-many `VisitEntryEntity`<br>• Total visits counter on `RestaurantCard`<br>• Visit history timeline on `RestaurantDetailScreen`<br>• Aggregated Dish Matrix showing cumulative verdicts across multiple visits |
| **10** | Work offline (without internet access), two-way sync periodically or by user trigger | **100% Complete** ✅ | • 100% offline-first architecture via local Room SQLite DB<br>• `SyncWorker` (WorkManager periodic background sync)<br>• "Sync Now" button in `SettingsScreen`<br>• OpenStreetMap offline tile caching |
| **UI 1** | Minimalist UI during data entry & pre-filling | **100% Complete** ✅ | • **3-Second Quick Regret Logger**: 1-tap GPS, 1-tap NO-GO verdict, quick tag chips ("Watery tea", "Soggy food", "Dirty restrooms"). Saves in 3s.<br>• **Pre-filling**: Selecting an existing restaurant in `EntryEditorScreen` auto-populates past dishes as instant selectable chips with past verdicts! |
| **UI 2** | Map View with custom markers & navigation | **100% Complete** ✅ | • Interactive OpenStreetMap (OSMDroid) with custom color-coded pins (🟢 Go, 🔴 No-Go, 🟡 Caution)<br>• Bottom sheet drawer on pin tap with "Navigate in Google Maps" handoff |

---

## 2. Prioritized Roadmap & Next Steps

Based on the gap analysis, here are the recommended next steps in order of priority:

### Phase 2A: UX Polish & Enhanced Querying (Immediate)
1. **Dish-Aware Unified Search**:
   - Update `DashboardViewModel` so that searching for a dish name (e.g. typing *"Biriyani"* or *"Chicken Fry"*) immediately matches and lists the restaurants serving that dish, showing the dish's specific verdict badge directly on the search result card!
2. **Camera & PhotoPicker Integration**:
   - Add a 1-tap photo button in `EntryEditorScreen` and `QuickCaptureScreen` using Android's native `ActivityResultContracts.PickVisualMedia` and Camera capture.
   - Display dish and receipt photo thumbnails using Coil in `RestaurantDetailScreen`.

### Phase 2B: Google Drive Direct Cloud Sync (Completed ✅)
3. **Google Sign-In & Drive REST Client**:
   - Integrated Google Sign-In and OAuth2 authorization (`https://www.googleapis.com/auth/drive.file` scope).
   - Created `GoogleDriveSyncClient` to search, create, upload, list, and download JSON delta payloads to/from the `PalateLog_Family` folder in Google Drive.
   - Connected WorkManager background worker (`SyncWorker`) and `SettingsScreen` UI.

### Phase 2C: Advanced Travel Features
4. **Highway Route Corridor Search**:
   - Allow entering an Origin and Destination (e.g. Bangalore to Chennai) to query all logged stops within 5-10 km along that highway corridor.
5. **Proximity Regret Warning Notification (Optional Geofence)**:
   - When driving within 200m of a restaurant previously marked **NO-GO 🔴**, pop up a friendly reminder: *"Heads up! You marked [Highway Tea Stall] as NO-GO (Terrible watery tea) on May 2024."*
