# PalateLog 🍽️

> **Never repeat a dining regret; remember every family favorite.**

**PalateLog** (`com.karthik.palatelog`) is an offline-first Android application designed to track, organize, and resurface personal and family dining learnings—from roadside tea stalls and outstation highway dhabas to online food delivery orders (Zomato/Swiggy).

---

## 🌟 The Problem PalateLog Solves

1. **The Roadside Regret**: You stop at a tea stall on a highway drive; the tea is terrible and you swear never to visit again. Months later, you stop there again because human memory is imperfect.
2. **The Delivery Dish Dichotomy**: You order from a restaurant (e.g. *Nagarjuna* on Zomato). The Biriyani is extraordinary, but the chicken fry arrives soggy and cold. Next time you order, you want an immediate reminder to skip the chicken fry.
3. **Outstation Family Drives**: You don't drive your family outstation frequently enough to remember which restaurants on NH44 or the highway corridor have great food and clean restrooms versus which ones are tourist traps.

---

## ✨ Key Features

- ⚡ **3-Second Speed Capture**: 1-tap floating action automatically grabs GPS coordinates, geocodes the place name, and records an immediate **NO-GO 🔴** or **GO 🟢** verdict with a quick note.
- 🍲 **Granular Food-Level Verdicts**: Rate restaurants as a whole, or isolate specific dishes (*Must Try / Avoid / Neutral*).
- 👨‍👩‍👧 **Family Member Preferences**: Track who loved or hated what (e.g. *"Wife loved Mutton Pepper Fry; Karthik found Chicken Fry soggy"*).
- 🛣️ **Highway & Route Search**: Search logged spots by proximity radius ("Near Me") or along highway travel corridors.
- 🗺️ **Free Interactive Map**: Integrated with OpenStreetMap / MapLibre (zero API keys, zero cloud billing) + 1-tap handoff to native Google Maps for turn-by-turn driving navigation.
- 🔄 **Google Shared Drive Two-Way Sync**: Offline-first synchronization with family members via a shared Google Drive folder (`PalateLog_Sync/`). Zero monthly server costs.
- 🔒 **Private vs. Shared Isolation**: Keep personal entries strictly private or share them seamlessly with family.
- 🧾 **Bills, Prices & Photos**: Store receipts, total bill amounts, and dish photos with automated WebP compression.

---

## 🏗️ Architecture & Tech Stack

PalateLog follows Google's recommended **Clean Architecture** with MVVM / MVI Unidirectional Data Flow (UDF):

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose + Material 3 (Dynamic Color, Dark Mode)
- **Dependency Injection**: Hilt
- **Local Persistence**: Room SQLite DB (with FTS5 search and Haversine spatial queries)
- **Background Sync**: AndroidX WorkManager
- **Location & Geocoding**: Play Services Fused Location Provider (`FusedLocationProviderClient`) + Geocoder
- **Mapping**: OSMDroid / MapLibre Android (100% free, offline tile support)
- **Image Loading**: Coil 3 (Compose-first)
- **Build System**: Gradle Version Catalog (`gradle/libs.versions.toml`)

---

## 📁 Repository Structure

```
palatelog/
├── app/                        # Main Android application module
│   ├── src/main/java/com/karthik/palatelog/
│   │   ├── core/               # Database, Network, Sync, Design System
│   │   └── feature/            # Quick Capture, Map, Explore, Details, Settings
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml      # Dependency Version Catalog
├── .editorconfig               # Code formatting conventions
├── .gitattributes              # Line endings normalization
├── .gitignore                  # Git ignore rules for Android/Gradle
├── LICENSE                     # MIT License
└── README.md
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
