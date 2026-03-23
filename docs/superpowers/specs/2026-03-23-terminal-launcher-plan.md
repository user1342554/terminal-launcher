# Terminal Launcher — Implementation Plan

## Step 1: Project Scaffolding
- Create Android project structure with Gradle (Kotlin DSL)
- Configure build.gradle.kts with Compose dependencies, DataStore, min SDK 21, target SDK 34
- Set up package structure: `com.terminallauncher`
- Create AndroidManifest.xml with launcher intent filter

## Step 2: Theme & Foundation
- Create Color.kt with the Memento color palette
- Create Theme.kt with dark-only Material3 theme
- Create Type.kt with monospace typography
- Set up LauncherActivity as single edge-to-edge Compose activity

## Step 3: Data Layer
- PreferencesStore: DataStore wrapper for birth date (year + month)
- AppRepository: Query PackageManager for launchable apps, cache in memory, BroadcastReceiver for install/uninstall
- LauncherViewModel: State management for all screens

## Step 4: Home Screen — Life Grid
- LifeGrid composable using Canvas API
- Calculate lived months from birth date
- Auto-optimize column count for screen dimensions
- Draw rounded squares with correct colors (copper lived, gold current, dim remaining)

## Step 5: Setup Screen
- Terminal-styled birth date input (first launch only)
- Year and month fields, monospace font, dark background
- Save to DataStore on continue

## Step 6: Terminal Overlay
- Slide-up overlay with spring animation triggered by swipe-up gesture
- Search input with $ prompt, blinking cursor, monospace font
- Auto-open keyboard on activation

## Step 7: App Search
- Prefix match algorithm (lowercase)
- Fuzzy fallback (subsequence matching with scoring)
- Results list with ▸ selection indicator
- Arrow key navigation, Enter to launch, Esc to dismiss

## Step 8: Integration & Polish
- Wire all screens together in LauncherActivity
- Handle back button (dismiss terminal)
- Handle home button (return to grid)
- Swipe hint on first use
- Edge cases: no apps matching, empty query
