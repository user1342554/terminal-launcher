package com.terminallauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.terminallauncher.LauncherState
import com.terminallauncher.ui.components.LifeGrid
import com.terminallauncher.ui.components.SettingsOverlay
import com.terminallauncher.ui.components.TerminalOverlay
import com.terminallauncher.ui.theme.Background
import kotlin.math.abs

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
    onSwipeBottomLeftToRight: () -> Unit,
    onSwipeBottomRightToLeft: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    onChangeBirthDate: () -> Unit = {},
    onResetAll: () -> Unit = {}
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var startX by remember { mutableFloatStateOf(0f) }
    var startY by remember { mutableFloatStateOf(0f) }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(state.terminalVisible, state.settingsVisible) {
                val screenW = size.width.toFloat()
                val screenH = size.height.toFloat()
                val bottomZone = screenH * 0.7f // bottom 30% of screen

                detectDragGestures(
                    onDragStart = { offset ->
                        dragX = 0f; dragY = 0f
                        startX = offset.x; startY = offset.y
                    },
                    onDragEnd = {
                        val startedBottom = startY > bottomZone
                        val startedLeft = startX < screenW * 0.3f
                        val startedRight = startX > screenW * 0.7f

                        if (!state.terminalVisible) {
                            when {
                                // Swipe up -> open terminal
                                dragY < -100f && abs(dragY) > abs(dragX) -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onShowTerminal()
                                }
                                // Bottom-left corner swipe right
                                startedBottom && startedLeft && dragX > 150f -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeBottomLeftToRight()
                                }
                                // Bottom-right corner swipe left
                                startedBottom && startedRight && dragX < -150f -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSwipeBottomRightToLeft()
                                }
                            }
                        } else if (dragY > 100f && abs(dragY) > abs(dragX)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHideTerminal()
                        }
                        dragX = 0f; dragY = 0f
                    },
                    onDrag = { _, delta ->
                        dragX += delta.x
                        dragY += delta.y
                    }
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
                screenTimeMonths = state.screenTimeMonths
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
            showAppPicker = state.showAppPicker != null
        )

        if (state.settingsVisible) {
            SettingsOverlay(
                birthYear = state.birthYear,
                birthMonth = state.birthMonth,
                swipeLeftLabel = state.swipeLeftLabel,
                swipeRightLabel = state.swipeRightLabel,
                onChangeBirthDate = onChangeBirthDate,
                onResetAll = onResetAll,
                onDismiss = onDismissSettings
            )
        }
    }
}
