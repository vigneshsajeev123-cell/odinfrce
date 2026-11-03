# Memory Universe 🌌

> **A Fully Offline, Privacy-First, Open-Source 3D Android Memory Gallery**  
> Where photographs and text notes coexist in an immersive, pitch-black 3D universe.

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-brightgreen.svg)]()
[![Target](https://img.shields.io/badge/Target_SDK-35_(Android_15)-purple.svg)]()
[![Offline](https://img.shields.io/badge/Network-100%25_Offline-success.svg)]()

---

## 🛰️ Architecture & Principles

- **Pure Offline Architecture**: Zero internet permissions declared in `AndroidManifest.xml`. No backend, no telemetry, no cloud database.
- **On-Device Local Persistence**: Powered by SQLite & Room with strict normalization across memories, photo metadata, galaxies, and 3D coordinate space.
- **MediaStore Native Integration**: Directly indexes local device photographs without wasteful internal file duplication. Extracts unredacted EXIF timestamps, camera hardware info, and GPS coordinates.
- **Deep Space UI Identity**: Pitch-black canvas (`#030306`), subtle procedural starfield particles, violet/astral blue accents, and glassmorphic telemetry cards.

---

## 🛠️ Toolchain & Specifications (Phase 1)

| Parameter | Value |
|---|---|
| **Android Gradle Plugin (AGP)** | `8.7.3` |
| **Gradle** | `8.9+` |
| **Kotlin Version** | `2.0.21` |
| **Compose Compiler Plugin** | `2.0.21` (Kotlin 2.0 native compose compiler) |
| **Minimum SDK** | `API 26` (Android 8.0 Oreo - Required for 3D engine & modern media APIs) |
| **Target / Compile SDK** | `API 35` (Android 15) |
| **Database** | Room `2.6.1` with KSP `2.0.21-1.0.27` |
| **UI Framework** | Jetpack Compose Material 3 BOM `2024.10.01` |
| **Media Loader** | Coil Compose `2.7.0` |
| **EXIF Engine** | AndroidX ExifInterface `1.3.7` |

---

## 📂 Project Directory Structure

```
MemoryUniverse/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/memoryuniverse/
│       │   ├── MainActivity.kt
│       │   ├── MemoryUniverseApp.kt
│       │   ├── core/
│       │   │   ├── database/
│       │   │   │   ├── MemoryDatabase.kt
│       │   │   │   ├── converters/Converters.kt
│       │   │   │   ├── dao/MemoryDao.kt
│       │   │   │   ├── dao/ClusterDao.kt
│       │   │   │   └── entity/
│       │   │   │       ├── MemoryEntity.kt
│       │   │   │       ├── PhotoMetadataEntity.kt
│       │   │   │       ├── ClusterEntity.kt
│       │   │   │       └── MemoryPositionEntity.kt
│       │   │   ├── navigation/
│       │   │   │   ├── Screen.kt
│       │   │   │   └── AppNavGraph.kt
│       │   │   ├── permissions/
│       │   │   │   └── PermissionManager.kt
│       │   │   └── theme/
│       │   │       ├── Color.kt
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   ├── data/
│       │   │   ├── media/MediaStoreFetcher.kt
│       │   │   └── repository/PhotoRepository.kt
│       │   └── ui/
│       │       ├── components/CosmicComponents.kt
│       │       └── home/
│       │           ├── HomeScreen.kt
│       │           └── HomeViewModel.kt
│       └── res/
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── xml/
│               ├── backup_rules.xml
│               └── data_extraction_rules.xml
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 How to Build & Run

### In Android Studio
1. Open Android Studio Ladybug or newer.
2. Select **Open** and choose the repository folder.
3. Allow Gradle to sync.
4. Select a device running Android 8.0 (API 26) or higher.
5. Click **Run 'app'** or press `Shift + F10`.

### Command Line
```bash
./gradlew assembleDebug
# Output APK:
# app/build/outputs/apk/debug/app-debug.apk
```
