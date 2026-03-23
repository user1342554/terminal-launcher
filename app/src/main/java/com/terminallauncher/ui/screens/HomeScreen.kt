package com.terminallauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.terminallauncher.LauncherState
import com.terminallauncher.ui.components.LifeGrid
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
    onLaunch: () -> Unit,
    onLaunchIndex: (Int) -> Unit
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(state.terminalVisible) {
                detectVerticalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onDragEnd = {
                        if (!state.terminalVisible && dragAmount < -100f) {
                            onShowTerminal()
                        } else if (state.terminalVisible && dragAmount > 100f) {
                            onHideTerminal()
                        }
                        dragAmount = 0f
                    },
                    onVerticalDrag = { _, delta ->
                        dragAmount += delta
                    }
                )
            }
    ) {
        if (state.birthYear != null && state.birthMonth != null) {
            LifeGrid(
                birthYear = state.birthYear,
                birthMonth = state.birthMonth,
                alpha = if (state.terminalVisible) 0.08f else 1f
            )
        }

        TerminalOverlay(
            visible = state.terminalVisible,
            query = state.searchQuery,
            results = state.searchResults,
            selectedIndex = state.selectedIndex,
            onQueryChange = onQueryChange,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onLaunch = onLaunch,
            onLaunchIndex = onLaunchIndex,
            onDismiss = onHideTerminal
        )
    }
}
