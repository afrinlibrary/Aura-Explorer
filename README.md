# Aura Explorer

A high-performance Android file manager built with Jetpack Compose and Material 3.

## Setup

### 1. Clone and open in Android Studio
Open Android Studio → **File → Open** → select the `AuraExplorer` folder.

### 2. Add the Gradle wrapper JAR
The `gradle-wrapper.jar` is not included in this repo (it's a binary). Android Studio will
prompt you to download the Gradle wrapper automatically on first sync. If it doesn't:

```bash
gradle wrapper --gradle-version 8.7
```
Or download manually from:
`https://services.gradle.org/distributions/gradle-8.7-bin.zip`
and place `gradle-wrapper.jar` at `gradle/wrapper/gradle-wrapper.jar`.

### 3. Sync and build
- **File → Sync Project with Gradle Files**
- Run on a device or emulator (API 26+)

## Permissions
On first launch the app will request **All Files Access** (`MANAGE_EXTERNAL_STORAGE`).
This is required to browse the full file system. You'll be redirected to the system
settings screen to grant it.

## Tech Stack
- Kotlin + Jetpack Compose
- Material 3 with Dynamic Color (Material You)
- Hilt for dependency injection
- MVVM + Clean Architecture
- Kotlin Coroutines & Flow

## Project Structure
```
app/src/main/kotlin/com/aura/explorer/
├── AuraApp.kt                   # Hilt application
├── MainActivity.kt
├── di/
│   └── AppModule.kt             # Hilt bindings
├── domain/
│   ├── model/FileModel.kt       # FileItem, FileType, SortOrder
│   └── repository/FileRepository.kt
├── data/
│   └── repository/FileRepositoryImpl.kt
└── ui/
    ├── theme/                   # AuraTheme, typography, shapes
    ├── viewmodel/FileViewModel.kt
    ├── screen/MainScreen.kt
    ├── component/FileDetailsBottomSheet.kt
    └── util/FileIconMapper.kt
```
