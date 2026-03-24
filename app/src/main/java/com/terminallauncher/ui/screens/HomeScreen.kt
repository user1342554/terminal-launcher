package com.terminallauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.terminallauncher.data.MediaState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.terminallauncher.LauncherState
import com.terminallauncher.ui.components.LifeGrid
import com.terminallauncher.ui.components.SettingsOverlay
import com.terminallauncher.ui.components.TerminalOverlay
import com.terminallauncher.ui.theme.Background

@Composable
fun HomeScreen(
    state: LauncherState,
    onShowTerminal: () -> Unit,
    onHideTerminal: () -> Unit,
    onQueryChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSubmit: () -> Unit,
    onLaunchIndex: (Int) -> Unit,
    onTabComplete: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    onChangeBirthDate: () -> Unit = {},
    onResetAll: () -> Unit = {},
    onToggleWallpaperHome: () -> Unit = {},
    onToggleWallpaperLock: () -> Unit = {}
) {
    var dragY by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val nowPlaying by MediaState.nowPlaying.collectAsState()
    val defaultAccent = 0xFFC9956B.toInt()
    val hasColors = nowPlaying.title.isNotEmpty() && nowPlaying.colors.accent != defaultAccent
    val dynAccent = if (hasColors) Color(nowPlaying.colors.accent) else null
    val dynLight = if (hasColors) Color(nowPlaying.colors.accentLight) else null
    val dynMuted = if (hasColors) Color(nowPlaying.colors.muted) else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(state.terminalVisible, state.settingsVisible) {
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onDragEnd = {
                        if (!state.terminalVisible && dragY < -100f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShowTerminal()
                        } else if (state.terminalVisible && dragY > 100f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHideTerminal()
                        }
                        dragY = 0f
                    },
                    onVerticalDrag = { _, delta -> dragY += delta }
                )
            }
            .pointerInput(state.terminalVisible, state.settingsVisible) {
                if (!state.terminalVisible && !state.settingsVisible) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap() },
                        onLongPress = { onLongPress() }
                    )
                }
            }
    ) {
        if (state.birthYear != null && state.birthMonth != null) {
            LifeGrid(
                birthYear = state.birthYear,
                birthMonth = state.birthMonth,
                alpha = if (state.terminalVisible) 0.08f else 1f,
                screenTimeMonths = state.screenTimeMonths,
                dynamicAccent = dynAccent,
                dynamicLight = dynLight,
                dynamicMuted = dynMuted
            )
        }

        TerminalOverlay(
            visible = state.terminalVisible,
            query = state.searchQuery,
            results = state.searchResults,
            selectedIndex = state.selectedIndex,
            isCommandMode = state.isCommandMode,
            history = state.history,
            currentPath = state.currentPath,
            onQueryChange = onQueryChange,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onSubmit = onSubmit,
            onLaunchIndex = onLaunchIndex,
            onDismiss = onHideTerminal,
            onTabComplete = onTabComplete,
            showAppPicker = state.showAppPicker != null,
            dynamicAccent = dynAccent
        )

        if (state.settingsVisible) {
            SettingsOverlay(
                birthYear = state.birthYear,
                birthMonth = state.birthMonth,
                swipeLeftLabel = state.swipeLeftLabel,
                swipeRightLabel = state.swipeRightLabel,
                wallpaperHome = state.wallpaperHome,
                wallpaperLock = state.wallpaperLock,
                onChangeBirthDate = onChangeBirthDate,
                onToggleWallpaperHome = onToggleWallpaperHome,
                onToggleWallpaperLock = onToggleWallpaperLock,
                onResetAll = onResetAll,
                onDismiss = onDismissSettings
            )
        }
    }
}
