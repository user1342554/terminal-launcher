# Terminal Launcher

A minimalist Android launcher that replaces your home screen with a terminal. No app drawer. No icons. No widgets. Just a terminal to launch apps, run commands, and control your music.

## How it works

**Home screen** is a terminal with the keyboard always open. Type to search apps or run commands. Tap a result to launch it.

**Autocomplete** — as you type, matching apps appear as tappable results. Command suggestions show below with descriptions. Fuzzy matching for both.

**Music player** — type `mus` to open an ASCII-styled music player with animated equalizer, playback controls, and progress tracking. Screen stays on while playing.

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
shortcut <dir>  set swipe shortcut (left/right/down)
mus             music player
clear           clear terminal
, <query>       google search
help            list all commands
```

TAB completes commands, directions, and file paths.

## Setup

First launch walks you through:
1. Usage access (for screen time-based app sorting)
2. Set as default launcher

## Building

Open in Android Studio, sync Gradle, run on device. Requires Android 5.0+.

```
./gradlew assembleDebug
```

## Permissions

- **Notification listener** — media session access for music controls and album art
- **Usage access** — screen time data for app sorting
- **Storage** — terminal file commands
- **Internet** — album art loading

## Stack

Kotlin, Jetpack Compose, single Activity, MVVM. No third-party dependencies beyond AndroidX and Palette.
