# Carpe Diem

A minimalist Android launcher that replaces your home screen with a life-in-months grid and a terminal for everything else.

No app drawer. No icons. No widgets. Just a grid showing how much of your life you've lived, and a terminal to launch apps and manage your phone.

## How it works

**Home screen** is a grid of 960 squares — one for each month of an 80-year life. Lived months are filled, one marks the current month, and the rest are what's left. When music is playing, the entire color palette shifts to match the album art.

**Swipe up** to open the terminal. Type to search apps or run commands. Tap a result to launch it.

**Swipe left** for the music page — album art, song info, playback controls. Blurred album cover as the background that fades smoothly as you swipe back to the home screen.

**Swipe down** to launch a favorite app you choose.

**Double tap** to lock the screen. **Long press** for settings.

## Screen time visualization

Red cells on the grid show how many of your remaining months you'll spend on your phone if you keep your current screen time. Uses actual usage data from the last 7 days.

## Dynamic colors

When music is playing, the launcher extracts 3 distinct colors from the album art and applies them everywhere — the grid, terminal, controls, page indicators. B&W albums get proper grey tones. Colors are picked by area and contrast, not just the brightest pixel.

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
shortcut down   set swipe down app
wallpaper       refresh wallpaper
clear           clear terminal
, <query>       google search
help            list all commands
```

Tab completion works for commands and file names.

## Setup

First launch walks you through:
1. Birth date (for the life grid)
2. Usage access (for screen time sorting and visualization)
3. Wallpaper settings (home screen, lock screen, or both)
4. Set as default launcher

## Wallpaper

The life grid is set as your device wallpaper with dynamic colors from whatever you're listening to. Lock screen has extra top padding for your phone's clock. Configurable in settings.

## Building

Open in Android Studio, sync Gradle, run on device. Requires Android 5.0+.

```
./gradlew assembleDebug
```

## Permissions

- **Notification listener** — media session access for music controls and album art
- **Usage access** — screen time data for app sorting and red cell visualization
- **Device admin** — double tap to lock screen
- **Storage** — terminal file commands
- **Set wallpaper** — life grid as home/lock screen wallpaper
- **Internet** — album art loading

## Stack

Kotlin, Jetpack Compose, single Activity, MVVM. No third-party dependencies beyond AndroidX and Palette.
