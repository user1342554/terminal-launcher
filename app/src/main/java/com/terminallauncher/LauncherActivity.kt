package com.terminallauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.terminallauncher.ui.screens.HomeScreen
import com.terminallauncher.ui.screens.SetupScreen
import com.terminallauncher.ui.theme.TerminalLauncherTheme

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private fun forceShowKeyboard() {
        window.decorView.post {
            window.insetsController?.show(WindowInsets.Type.ime())
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshScreenTime()
        forceShowKeyboard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TerminalLauncherTheme {
                val state by viewModel.state.collectAsState()

                when (state.screen) {
                    Screen.SETUP -> {
                        SetupScreen(
                            onRequestUsageAccess = {
                                viewModel.requestUsagePermission()
                            },
                            onRequestDefaultLauncher = {
                                viewModel.completeSetup()
                                showLauncherPicker()
                            },
                            hasUsagePermission = viewModel.usageTracker.hasPermission()
                        )
                    }
                    Screen.HOME -> {
                        forceShowKeyboard()
                        HomeScreen(
                            state = state,
                            onQueryChange = viewModel::updateQuery,
                            onMoveUp = viewModel::moveSelectionUp,
                            onMoveDown = viewModel::moveSelectionDown,
                            onSubmit = {
                                if (state.showAppPicker != null) {
                                    val results = state.searchResults
                                    if (results.isNotEmpty()) {
                                        viewModel.setShortcutApp(results[0].app)
                                    }
                                } else {
                                    viewModel.executeCommand()
                                }
                            },
                            onTabComplete = { viewModel.tabComplete() },
                            onLaunchIndex = { index ->
                                if (state.showAppPicker != null) {
                                    val results = state.searchResults
                                    if (index < results.size) {
                                        viewModel.setShortcutApp(results[index].app)
                                    }
                                } else {
                                    viewModel.launchAtIndex(index)
                                }
                            },
                            onCloseMusicPlayer = {
                                viewModel.hideMusicPlayer()
                                forceShowKeyboard()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showLauncherPicker() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            val cn = ComponentName(this, DummyHomeActivity::class.java)
            packageManager.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            startActivity(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME); flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            packageManager.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        if (viewModel.state.value.musicPlayerVisible) {
            viewModel.hideMusicPlayer()
            forceShowKeyboard()
        }
    }
}
