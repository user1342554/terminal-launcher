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
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.terminallauncher.ui.screens.HomeScreen
import com.terminallauncher.ui.screens.SetupScreen
import com.terminallauncher.ui.screens.WidgetScreen
import com.terminallauncher.ui.theme.Background
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
        viewModel.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)


        @OptIn(ExperimentalFoundationApi::class)
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
                            onSetWallpaperConfig = { home, lock ->
                                viewModel.setWallpaperConfig(home, lock)
                            },
                            hasUsagePermission = viewModel.usageTracker.hasPermission()
                        )
                    }
                    Screen.HOME -> {
                        val pagerState = rememberPagerState(
                            initialPage = 1,
                            pageCount = { 2 }
                        )

                        // Reset to home page on resume (screen on, back from app, etc.)
                        val resumeTrigger by viewModel.resumeTrigger.collectAsState()
                        LaunchedEffect(resumeTrigger) {
                            if (pagerState.currentPage != 1) {
                                pagerState.scrollToPage(1)
                            }
                        }

                        val mediaState by com.terminallauncher.data.MediaState.nowPlaying.collectAsState()

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Background)
                        ) {
                            // Blurred album art behind everything
                            if (mediaState.albumArt != null) {
                                Image(
                                    bitmap = mediaState.albumArt!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                Modifier.graphicsLayer {
                                                    renderEffect = RenderEffect
                                                        .createBlurEffect(100f, 100f, Shader.TileMode.CLAMP)
                                                        .asComposeRenderEffect()
                                                }
                                            } else Modifier
                                        ),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.3f
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Background.copy(alpha = 0.5f))
                                )
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = !state.terminalVisible && !state.settingsVisible
                            ) { page ->
                                when (page) {
                                    0 -> WidgetScreen()
                                    1 -> HomeScreen(
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
                                        },
                                        onToggleWallpaperHome = {
                                            viewModel.setWallpaperConfig(!state.wallpaperHome, state.wallpaperLock)
                                        },
                                        onToggleWallpaperLock = {
                                            viewModel.setWallpaperConfig(state.wallpaperHome, !state.wallpaperLock)
                                        }
                                    )
                                }
                            }

                            // Animated page indicators — dynamic color
                            if (!state.terminalVisible && !state.settingsVisible) {
                                val mediaState by com.terminallauncher.data.MediaState.nowPlaying.collectAsState()
                                val defAccent = 0xFFC9956B.toInt()
                                val indicatorColor = if (mediaState.title.isNotEmpty() && mediaState.colors.accent != defAccent)
                                    androidx.compose.ui.graphics.Color(mediaState.colors.accent)
                                else com.terminallauncher.ui.theme.CopperLived

                                val page = pagerState.currentPage
                                val offset = pagerState.currentPageOffsetFraction

                                // Calculate how "active" each page is (1.0 = fully active, 0.0 = inactive)
                                val page0Active = if (page == 0) 1f - offset.coerceAtLeast(0f)
                                    else if (page == 1) (-offset).coerceAtLeast(0f)
                                    else 0f
                                val page1Active = 1f - page0Active

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(bottom = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Dot 0
                                    val w0 = androidx.compose.ui.unit.lerp(10.dp, 24.dp, page0Active.coerceIn(0f, 1f))
                                    val a0 = 0.25f + 0.75f * page0Active.coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .size(width = w0, height = 10.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor.copy(alpha = a0))
                                    )
                                    // Dot 1
                                    val w1 = androidx.compose.ui.unit.lerp(10.dp, 24.dp, page1Active.coerceIn(0f, 1f))
                                    val a1 = 0.25f + 0.75f * page1Active.coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .size(width = w1, height = 10.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor.copy(alpha = a1))
                                    )
                                }
                            }
                        }
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
