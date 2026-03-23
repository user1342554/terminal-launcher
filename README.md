# Terminal Launcher

A minimalist Android launcher that replaces your home screen with a life-in-months grid and a terminal-style interface for everything else.

No app drawer. No icons. No widgets. Just a grid showing how much of your life you've lived, and a terminal to launch apps and manage your phone.

## How it works

**Home screen** is a grid of 960 squares — one for each month of an 80-year life. Copper squares are months you've lived. One gold square marks the current month. Dark squares are what's left. Red squares show how many of those remaining months you'll spend staring at your phone if you keep your current screen time.

**Swipe up** to open the terminal. Type to search apps or run commands. Tap a result to launch it. Swipe down or tap outside to close.

**Corner swipes** (bottom-left or bottom-right) launch shortcut apps you configure.

**Double tap** to lock the screen. **Long press** for settings.

## Terminal commands

```
ls              list files
cd <dir>        change directory
mkdir <name>    create folder
rm <name>       delete file/folder
touch <name>    create file
cat <file>      view file contents
pwd             current path
install <app>   search play store
uninstall <app> uninstall an app
info <app>      open app settings
shortcut left   set left swipe shortcut
shortcut right  set right swipe shortcut
wallpaper       refresh wallpaper
clear           clear terminal
, <query>       google search
help            list all commands
```

Tab completion works for commands and file names.

## Setup

First launch walks you through three steps:
1. Enter your birth date (for the life grid)
2. Grant usage access (for screen time sorting and visualization)
3. Set as default launcher

The grid is also set as your device wallpaper (home + lock screen). Lock screen has extra top padding so your phone's clock sits above the grid.

## Building

Open in Android Studio, sync Gradle, run on device. Requires Android 5.0+.

```
./gradlew assembleDebug
```

## Permissions

- **Usage access** — screen time data for app sorting and the red "phone time" visualization
- **Device admin** — double tap to lock screen
- **Storage** — terminal file commands (ls, cd, etc.)
- **Set wallpaper** — life grid as home/lock screen wallpaper

## Stack

Kotlin, Jetpack Compose, single Activity, MVVM. No third-party dependencies beyond AndroidX.
