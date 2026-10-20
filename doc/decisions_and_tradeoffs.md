# PalateLog: Architecture Decision Records (ADRs) & Trade-offs

This document records the architectural and design decisions made for PalateLog, including context, alternatives considered, decision rationale, and trade-offs.

---

## ADR 001: App Identity & Naming

### Context
We needed an identity that conveys both positive discoveries and negative warnings (e.g. roadside tea regrets, delivery dish disappointments), suitable for personal and family use.

### Candidates Evaluated
1. **BiteMark**: Catchy, punchy ("never get bitten twice").
2. **TasteTrail**: Travel & highway-focused.
3. **NeverAgain!**: Humorous, warning-focused.
4. **PalateLog** *(Selected)*: Clean, gourmet, journal-like, balances recommendations with warnings.

### Decision
Adopted **PalateLog** with application ID `com.prismpixelvertex.palatelog` and tagline:  
> *"Never repeat a dining regret; remember every family favorite."*

---

## ADR 002: Free In-App Map with Zero-Cost Live Navigation Handoff

### Context
Requirement 2 calls for interactive map search and geocoded visual exploration. Google Maps SDK for Android requires creating a Google Cloud billing account and attaching a credit card to generate an API key.

### Options Considered
* **Option A: Google Maps Android SDK**: Native Google maps rendering, but requires mandatory Google Cloud project creation, billing account, credit card linking, and risks recurring costs or API key leakage.
* **Option B: OSMDroid / MapLibre (OpenStreetMap)**: 100% free, open-source, zero credit card requirements, zero API keys, and native offline tile caching.
* **Option C: Hybrid OSMDroid + Native Google Maps Intent (Selected)**:
  - In-app interactive map rendering powered by **OSMDroid / OpenStreetMap**.
  - When the user taps "Navigate" on any restaurant card, PalateLog fires an Android `geo:` / `google.navigation:` intent directly opening the official **Google Maps application** installed on the user's phone.

### Decision & Trade-offs
* **Benefits**: 
  - 100% free with zero monthly billing or credit card setup.
  - Complete turn-by-turn driving navigation with real-time traffic powered by the official Google Maps app.
  - Supports offline tile caching in remote highway stretches.
* **Trade-off**: In-app map styling is OpenStreetMap rather than proprietary Google Satellite view, which is acceptable for restaurant pin discovery.

---

## ADR 003: Dual-Mode Data Entry (The 3-Second Quick Logger vs. Full Editor)

### Context
When having a bad tea at a roadside stall or dhaba, a user does not have 3-5 minutes to fill out a 15-field form with dish prices, categories, and photos. If data entry is cumbersome, the user skips logging and the regret happens again.

### Options Considered
* **Option A: Comprehensive Form Only**: All fields (dishes, bills, tags, photos) on one screen. Too slow for field capture.
* **Option B: 3-Second Speed Dial + Deferred Full Editor (Selected)**:
  - **Quick Regret Logger (Screen A)**: 1 tap for location, 1 tap for verdict (defaults to NO-GO 🔴), 1-line reason with one-tap suggestions ("Watery tea", "Soggy food", "Dirty restrooms"). Saves in 3 seconds.
  - **Full Visit Editor (Screen B)**: Used for sitting down after a meal or online order; includes multi-dish matrix, remembered past dish chips, bill amount, and family preferences.

### Decision & Trade-offs
* Maximizes capture rate by reducing cognitive friction to under 3 seconds during live visits, while preserving rich details when the user has time.

---

## ADR 004: Serverless Google Shared Drive Sync vs. Dedicated Cloud Backend

### Context
Requirement 6 requires syncing data with family members while separating private and shared entries. Setting up a dedicated backend (e.g. Firebase, AWS Amplify, Supabase, Postgres) incurs recurring monthly costs, maintenance burden, and privacy concerns.

### Options Considered
* **Option A: Firebase / Supabase Backend**: Fast real-time sync, but incurs monthly subscription fees, vendor lock-in, and requires hosting servers.
* **Option B: Google Shared Drive Folder-Based Distributed Sync (Selected)**:
  - Family members share a dedicated Google Drive folder (e.g. `PalateLog_Family/`).
  - The app writes lightweight append-only JSON deltas (`changes_<deviceId>_<timestamp>.json`) and WebP compressed photos.
  - Two-way sync is orchestrated by AndroidX WorkManager or triggered on-demand by the user.
  - Conflict resolution uses Last-Write-Wins (LWW) with millisecond timestamps and UUID primary keys.
  - `isShared: Boolean` controls whether an entry is exported to the cloud or kept strictly local.

### Decision & Trade-offs
* **Benefits**: Zero monthly cloud costs, full user data sovereignty, private entries never leave the device.
* **Trade-off**: Sync is eventual rather than real-time sub-second WebSocket sync (ideal for restaurant visits where multi-second latency is completely harmless).

---

## ADR 005: Spatial Search & Proximity Filtering in Room SQLite

### Context
Requirement 2 & 4 require querying restaurants near the user's current GPS location or along highway corridors.

### Options Considered
* **Option A: SpatiaLite Extension**: Adds geospatial SQL functions to SQLite, but requires custom NDK binaries (.so files) and increases APK size significantly.
* **Option B: Bounding Box Pre-filter + Haversine Formula (Selected)**:
  - SQLite query filters by latitude/longitude bounding box (`latitude BETWEEN :minLat AND :maxLat`).
  - Precise distance is computed in Kotlin using the Haversine formula in `GeoUtils.kt`.
  - Results are sorted by distance and filtered by radius chips (<15km).

### Decision & Trade-offs
* Extremely fast, lightweight, zero native C++ dependencies, and 100% compatible with standard Android Room SQLite.

---

## ADR 006: JDK Runtime Strategy (OpenJDK 21 LTS)

### Context
Android Studio's bundled internal JBR is version 25 (`25.0.3` preview). Android Gradle Plugin (AGP) 8.7+ and Kotlin 2.0 compiler fail when encountering Java 25 (`java.lang.IllegalArgumentException: 25.0.3`).

### Decision
We installed **Microsoft OpenJDK 21 LTS** (`C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`) via winget, set `org.gradle.java.home` in `gradle.properties`, and updated `gradlew.bat`.  
This guarantees clean, repeatable, LTS-backed Gradle builds across command line and Android Studio.

---

## ADR 007: In-Place Metadata & Geolocation Correction vs. Immutable Logging

### Context
When logging entries or quick regrets in the field, GPS precision might vary or address geocoding might yield inaccurate street names/coordinates (e.g., misidentified "California Burrito" address). Users need a simple way to edit restaurant metadata (Name, Address, City, Highway/Route, Map Latitude/Longitude, Cuisine, Type, Verdict) and visit records without deleting and recreating entities or creating duplicates.

### Options Considered
* **Option A: Immutable Log Entries**: Disallow editing past entities. Require deleting and recreating a restaurant or visit entry. High user friction and risk of orphaned records.
* **Option B: In-Place Contextual Editing in Restaurant Detail View (Selected)**:
  - Provide an "Edit Restaurant Details" dialog in `RestaurantDetailScreen` with editable fields for address, city, route, cuisine, type, overall verdict, and precise map coordinates (lat/lng) supported by a 1-tap "Use Current GPS Location" button.
  - Provide inline edit and delete actions on `VisitHistoryCard` components for past visits.
  - Retain the entity's UUID primary key (`id`) and update the `updatedAt` timestamp to ensure Last-Write-Wins (LWW) conflict resolution compatibility during offline-first JSON delta syncs.

### Decision & Trade-offs
* **Benefits**: High flexibility, instantaneous correction of address/coordinate errors, zero orphaned child entities (dishes/visits remain correctly linked), and full sync compatibility.

