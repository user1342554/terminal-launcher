# Terminal Launcher — Design Spec

A minimalist Android launcher with a Memento-style life-in-months grid as the home screen and a terminal-style search overlay as the only way to launch apps.

## Overview

- **Platform:** Android (min SDK 21 / Android 5.0)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (100%)
- **Architecture:** Single Activity, MVVM with ViewModel + StateFlow

## Home Screen

The home screen displays a full-screen grid of 960 rounded squares representing 80 years of life in months. No clock, no date, no icons — just the grid.

### Grid Rendering

- **Total cells:** 960 (80 years × 12 months)
- **Column count:** Auto-optimized for screen width (tested from 16–40 columns, maximizing cell size)
- **Cell shape:** Rounded square with 25% corner radius relative to cell size
- **Gap ratio:** 18% of cell size between cells
- **Centering:** Grid centered horizontally and vertically with 5% horizontal padding, 6% vertical padding
- **Renderer:** Compose Canvas API

### Grid Colors

| Element | Color |
|---------|-------|
| Background | `#0A0A0F` (near-black) |
| Lived months | `#C9956B` (warm copper) with slight opacity variation (0.82–0.91) for organic feel |
| Current month | `#E8B87D` (brighter gold) with subtle glow (`box-shadow` equivalent) |
| Remaining months | `#16161D` (barely visible dark) |

### Interaction

- Swipe up from bottom → opens terminal overlay
- No other interactions on the home screen
- A subtle swipe-up hint shown at the bottom on first use, then hidden

## Terminal Overlay

Slides up from the bottom covering ~60% of the screen. The home grid remains visible behind it at ~8% opacity. Keyboard opens automatically on activation.

### Visual Style

- **Font:** Monospace (JetBrains Mono preferred, system monospace fallback)
- **Background:** `rgba(10, 10, 15, 0.95)` — near-opaque dark
- **Border:** Subtle 1px top border at `rgba(255, 255, 255, 0.06)`
- **Drag handle:** 32×4px rounded bar at top center, `rgba(255, 255, 255, 0.15)`
- **No app icons.** Results are pure text only — app names displayed lowercase for terminal feel.

### Prompt

- Prefix: `$` in `rgba(255, 255, 255, 0.3)`
- Input text: `rgba(255, 255, 255, 0.9)`
- Blinking block cursor
- Placeholder: "type to search..." in `rgba(255, 255, 255, 0.2)`

### Search Behavior

1. On each keystroke, filter installed apps by **lowercase prefix match**
2. If no prefix matches found, fall back to **fuzzy match** (subsequence matching with scoring)
3. Matching characters highlighted in brighter white
4. Results sorted by match quality
5. Results appear instantly as user types

### Selection & Navigation

- Top result auto-highlighted with `▸` indicator and subtle background (`rgba(255, 255, 255, 0.08)`)
- Arrow keys (↑↓) to change selection
- Enter to launch selected app
- Esc or swipe down to dismiss terminal
- Hint bar at bottom: `↵ open  ↑↓ navigate  esc close` in `rgba(255, 255, 255, 0.15)`

### After Launch

Terminal dismisses, app opens. When user returns to launcher (home button), terminal is closed and home screen grid is visible.

## First Launch

On first launch (no birth date stored), a terminal-styled setup screen appears:

- "SETUP" label in `rgba(255, 255, 255, 0.3)`, letter-spacing 2px
- "when were you born?" prompt
- Year and month input fields with underline style
- "continue →" button with subtle border
- All in monospace font, same dark background

Birth date is stored in Jetpack DataStore and used to calculate lived months.

## Architecture

### Components

```
LauncherActivity (single activity, edge-to-edge)
├── HomeScreen
│   ├── LifeGridCanvas (Compose Canvas)
│   └── SwipeDetector (gesture handling)
├── TerminalOverlay
│   ├── SearchInput (text field with $ prompt)
│   └── ResultsList (filtered app names)
└── SetupScreen (first launch only)
```

### Data Layer

- **LauncherViewModel:** Holds app list state, search query, selected index, terminal visibility, grid configuration. Survives configuration changes.
- **AppRepository:** Queries `PackageManager` for installed apps with launch intents, caches results in memory. Listens for `PACKAGE_ADDED` / `PACKAGE_REMOVED` broadcasts to update the list.
- **PreferencesStore:** Jetpack DataStore wrapper for birth date (year + month).

### Launcher Registration

```xml
<activity android:name=".LauncherActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

## Animation

- **Terminal slide-up:** Spring animation from bottom, ~300ms
- **Terminal dismiss:** Reverse spring animation + keyboard hide
- **Cursor blink:** 530ms interval block cursor
- **Swipe hint fade:** Fades out after first terminal use

## Testing Strategy

- **Unit tests:** Search algorithm (prefix match, fuzzy fallback, scoring, edge cases)
- **Unit tests:** Grid calculation (month counting, column optimization)
- **UI tests:** Terminal open/close via swipe gestures
- **UI tests:** App launch flow (type → select → launch intent fired)
- **Integration test:** Birth date setup → grid renders correctly
