package com.terminallauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.terminallauncher.LauncherState
import com.terminallauncher.ui.components.MusicPlayerOverlay
import com.terminallauncher.ui.components.TerminalOverlay
import com.terminallauncher.ui.theme.Background

@Composable
fun HomeScreen(
    state: LauncherState,
    onQueryChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSubmit: () -> Unit,
    onLaunchIndex: (Int) -> Unit,
    onTabComplete: () -> Unit,
    onCloseMusicPlayer: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (state.musicPlayerVisible) {
            MusicPlayerOverlay(onClose = onCloseMusicPlayer)
        } else {
            TerminalOverlay(
                visible = true,
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
                onDismiss = {},
                onTabComplete = onTabComplete,
                showAppPicker = state.showAppPicker != null
            )
        }
    }
}
