package com.terminallauncher

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.terminallauncher.ui.screens.HomeScreen
import com.terminallauncher.ui.screens.SetupScreen
import com.terminallauncher.ui.theme.TerminalLauncherTheme

class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName


    fun forceShowKeyboard() {
        window.decorView.post {
            window.insetsController?.show(WindowInsets.Type.ime())
        }
    }

    fun forceHideKeyboard() {
        window.insetsController?.hide(WindowInsets.Type.ime())
    }

    fun lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow()
        } else {
            requestDeviceAdmin()
        }
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable to lock screen with double tap")
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshScreenTime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        setContent {
            TerminalLauncherTheme {
                val state by viewModel.state.collectAsState()
                val scope = rememberCoroutineScope()

                when (state.screen) {
                    Screen.SETUP -> {
                        SetupScreen(
                            onSaveBirthDate = { year, month ->
                                viewModel.saveBirthDate(year, month)
                            },
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
                        HomeScreen(
                            state = state,
                            onShowTerminal = viewModel::showTerminal,
                            onHideTerminal = {
                                forceHideKeyboard()
                                viewModel.hideTerminal()
                            },
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
                            onSwipeBottomLeftToRight = {
                                scope.launch {
                                    val app = viewModel.getSwipeApp("right")
                                    if (app != null) launchShortcut(app, fromLeft = true)
                                    else viewModel.showShortcutPicker("right")
                                }
                            },
                            onSwipeBottomRightToLeft = {
                                scope.launch {
                                    val app = viewModel.getSwipeApp("left")
                                    if (app != null) launchShortcut(app, fromLeft = false)
                                    else viewModel.showShortcutPicker("left")
                                }
                            },
                            onDoubleTap = { lockScreen() },
                            onLongPress = { viewModel.showSettings() },
                            onDismissSettings = { viewModel.hideSettings() },
                            onChangeBirthDate = {
                                viewModel.hideSettings()
                                viewModel.goToSetup()
                            },
                            onResetAll = {
                                scope.launch {
                                    viewModel.resetAll()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun launchShortcut(app: com.terminallauncher.data.PreferencesStore.SwipeApp, fromLeft: Boolean) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            if (fromLeft) {
                val options = android.app.ActivityOptions.makeCustomAnimation(
                    this, R.anim.slide_in_left, R.anim.slide_out_right
                )
                startActivity(intent, options.toBundle())
            } else {
                startActivity(intent)
            }
        } catch (_: Exception) {}
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
        if (viewModel.state.value.terminalVisible) {
            forceHideKeyboard()
            viewModel.hideTerminal()
        }
    }
}
