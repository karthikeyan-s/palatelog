# PalateLog 🍽️

> **Never repeat a dining regret; remember every family favorite.**

**PalateLog** (`com.prismpixelvertex.palatelog`) is an offline-first Android application designed to track, organize, and resurface personal and family dining learnings—from roadside tea stalls and outstation highway dhabas to online food delivery orders (Zomato/Swiggy).

---

## 🌟 The Problem PalateLog Solves

1. **The Roadside Regret**: You stop at a tea stall on a highway drive; the tea is terrible and you swear never to visit again. Months later, you stop there again because human memory is imperfect.
2. **The Delivery Dish Dichotomy**: You order from a restaurant (e.g. *Nagarjuna* on Zomato). The Biriyani is extraordinary, but the chicken fry arrives soggy and cold. Next time you order, you want an immediate reminder to skip the chicken fry.
3. **Outstation Family Drives**: You don't drive your family outstation frequently enough to remember which restaurants on NH44 or the highway corridor have great food and clean restrooms versus which ones are tourist traps.

---

## ✨ Key Features

- ⚡ **3-Second Speed Capture**: 1-tap floating action automatically grabs GPS coordinates, geocodes the place name, and records an immediate **NO-GO 🔴** or **GO 🟢** verdict with a quick note.
- 🍲 **Granular Food-Level Verdicts**: Rate restaurants as a whole, or isolate specific dishes (*Must Try / Avoid / Neutral*).
- 🔍 **Dish-Aware Unified Search**: Tokenized multi-field search matching across restaurant names, cuisines, cities, routes, tags, and specific dish names with relevance ranking.
- 📖 **Interactive Help & User Guide**: Contextual deep-linked Material 3 user guidebook with category filter chips, onboarding welcome dialog, and instant `?` shortcuts across screens.
- ⏬ **Floating Scroll Indicators**: Content-aware floating down-arrow indicators on Explore and Restaurant Detail screens signaling off-screen content with 1-tap scroll down.
- 👨‍👩‍👧 **Family Member Preferences**: Track who loved or hated what (e.g. *"Wife loved Mutton Pepper Fry; Karthik found Chicken Fry soggy"*).
- 🛣️ **Highway & Route Search**: Search logged spots by proximity radius ("Near Me") or along highway travel corridors.
- 🗺️ **Free Interactive Map**: Integrated with OpenStreetMap / MapLibre (zero API keys, zero cloud billing) with custom color-coded pins + 1-tap handoff to native Google Maps for turn-by-turn driving navigation.
- 🔄 **Google Shared Drive Two-Way Sync**: Offline-first dual-mode synchronization (Mode A: Single Shared Account / Mode B: Individual Accounts with automated REST API invites, static QR code, and WhatsApp recovery).
- 📲 **In-App Google Play Updates**: Automatic background flexible update detection, progress tracking, and explicit restart prompts.
- 🔒 **Private vs. Shared Isolation**: Keep personal entries strictly private or share them seamlessly with family.
- 🧾 **Bills, Prices & Photos**: Store receipts, total bill amounts, and dish photos with automated WebP compression and 2-way family photo sync.
- 🌐 **Full String Localization**: 100% extracted UI string resources (`res/values/strings.xml`) supporting internationalization (i18n).

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
│   ├── src/main/java/com/prismpixelvertex/palatelog/
│   │   ├── core/               # Database, Location, Sync, Update, Design System, Util
│   │   └── feature/            # Dashboard, Details, Editor, Help, Map, Navigation, Quick Capture, Settings
│   └── build.gradle.kts
├── doc/                        # Technical design & architecture documentation
│   ├── architecture_and_design.md
│   ├── decisions_and_tradeoffs.md
│   └── requirements_traceability_and_roadmap.md
├── gradle/
│   └── libs.versions.toml      # Dependency Version Catalog
├── .editorconfig               # Code formatting conventions
├── .gitattributes              # Line endings normalization
├── .gitignore                  # Git ignore rules for Android/Gradle
├── LICENSE                     # MIT License
└── README.md
```

---

## 📚 Technical Documentation

Comprehensive documentation is available in the [`doc/`](doc/) directory:
* [Architecture & Technical Design](doc/architecture_and_design.md): Deep dive into Clean Architecture, Room schemas, and UDF patterns.
* [Decisions & Trade-offs (ADRs)](doc/decisions_and_tradeoffs.md): Why we chose OpenStreetMap + Google Maps intent, 3-second quick logging, serverless Drive sync, OpenJDK 21 LTS, and deep-linked user guide architecture.
* [Requirements Traceability & Roadmap](doc/requirements_traceability_and_roadmap.md): Complete traceability matrix mapping original user requirements against the code, with next-step priorities.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
