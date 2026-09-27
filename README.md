# Blockade 🏁

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![MinSDK](https://img.shields.io/badge/MinSDK-24-orange.svg)](https://developer.android.com/about/versions/nougat)

**Blockade** is a polished, local 2-player strategy race game for Android. Inspired by classic corridor-blocking board games, players compete on a 7×7 grid to race their token to the opposite side while strategically placing barriers to obstruct and redirect their opponent.

---

## 🎮 Game Rules & Objective

- **Objective**: Race your token from your starting row to any cell on the opposite row of the 7×7 board.
  - **Player 1 (◆)**: Starts at the bottom row (row 6) and races to the top row (row 0).
  - **Player 2 (●)**: Starts at the top row (row 0) and races to the bottom row (row 6).

### Turn Options
On each turn, a player must choose **one** of two actions:
1. **Move Token**: Advance one cell orthogonally (up, down, left, or right).
   - No diagonal movement or jumping over the rival token.
   - Tokens cannot occupy the same cell.
2. **Place Barrier**: Install a horizontal or vertical barrier on the edge between two cells to block passage.
   - Each player begins with **6 barriers**.
   - **Pathfinding Constraint**: A barrier cannot completely enclose either player. The game's pathfinding algorithm (Breadth-First Search) verifies that a valid path to the goal row remains open for both players before accepting any placement.

---

## ✨ Features

- **📱 Jetpack Compose & Custom Canvas Rendering**: High-performance UI built entirely declarative in Compose with dynamic Canvas rendering for board geometry, token animations, and barrier placement previews.
- **🧠 Pure Functional Rules Engine**: Completely decoupled game domain logic (`BlockadeRules.kt`) with deterministic side-effect-free move & barrier validation and BFS shortest-path calculations.
- **🎨 Custom Board Themes & Dark Mode**: Full support for System Light/Dark themes alongside distinct board palettes (*Classic*, *Dark Wood*, and *Modern Slate*).
- **🔊 Sound & Haptic Feedback**: Dynamic audio effects and tactile haptics for token selections, successful moves, barrier drops, invalid placements, and victory celebrations.
- **📊 Local Persistent Statistics**: Game counter, win records, and shortest-win metrics tracked persistently using **DataStore Preferences**.
- **🧪 Unit Test Coverage**: Thorough test suite verifying game rules, path validation edge cases, barrier bounds, and ViewModel state transitions.

---

## 🛠 Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Architecture Pattern**: MVVM with Unidirectional Data Flow (StateFlow / SharedFlow)
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Data Storage**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Asynchronous Execution**: Kotlin Coroutines & Flow
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)

---

## 📁 Project Structure

```
com.example.blockade
├── data/                  # Preferences DataStore & repository for settings/statistics
├── feedback/              # Audio playback (SoundPool) and haptics manager
├── game/                  # Pure game engine rules, models (GridPosition, Barrier, State), BFS pathfinding
└── ui/
    ├── components/        # Reusable Compose elements, buttons, and custom Canvas board drawing
    ├── game/              # GameScreen, GameBoard, and GameViewModel
    ├── screens/           # HomeScreen, SettingsScreen, HowToPlayScreen, ResultScreen, AboutScreen
    └── theme/             # Color palettes, typography, shapes, and board theme definitions
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug 2024.2+ or newer recommended)
- **JDK 11** or higher
- **Android SDK** API level 35+ (Minimum supported device API level 24 / Android 7.0)

### Building & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/Blockade.git
   cd Blockade
   ```

2. Open the project in Android Studio or build via command line:
   ```bash
   # Build debug APK
   ./gradlew assembleDebug

   # Run unit tests
   ./gradlew test
   ```

---

## 📄 License

```text
Copyright 2025 Abdullah Ateeq

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtainlinks or copies of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
