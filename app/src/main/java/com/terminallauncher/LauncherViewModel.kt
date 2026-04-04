package com.terminallauncher

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terminallauncher.data.AppInfo
import com.terminallauncher.data.AppRepository
import com.terminallauncher.data.CommandProcessor
import com.terminallauncher.data.PreferencesStore
import com.terminallauncher.data.SearchEngine
import com.terminallauncher.data.SearchResult
import com.terminallauncher.data.TerminalOutput
import com.terminallauncher.data.UsageTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    SETUP, HOME
}

data class HistoryEntry(
    val command: String,
    val output: List<TerminalOutput>
)

data class LauncherState(
    val screen: Screen = Screen.SETUP,
    val terminalVisible: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val selectedIndex: Int = 0,
    val hasUsagePermission: Boolean = false,
    val isCommandMode: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val currentPath: String = "",
    val showAppPicker: String? = null,
    val musicPlayerVisible: Boolean = false
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesStore = PreferencesStore(application)
    val appRepository = AppRepository(application)
    private val searchEngine = SearchEngine()
    val usageTracker = UsageTracker(application)
    val commandProcessor = CommandProcessor(application)

    private val _terminalVisible = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedIndex = MutableStateFlow(0)
    private val _screenTime = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private val _isCommandMode = MutableStateFlow(false)
    private val _showAppPicker = MutableStateFlow<String?>(null)
    private val _musicPlayerVisible = MutableStateFlow(false)

    init {
        appRepository.start()
        refreshScreenTime()

        // Keep media state fresh for music player
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (true) {
                com.terminallauncher.data.MediaState.refresh(getApplication())
                kotlinx.coroutines.delay(2000)
            }
        }

        // Wire shortcut command to save the app
        commandProcessor.onShortcutChange = { direction, query ->
            viewModelScope.launch {
                val apps = appRepository.apps.value
                val match = SearchEngine().search(query, apps).firstOrNull()
                if (match != null) {
                    val app = match.app
                    if (direction == "right") {
                        preferencesStore.saveSwipeRightApp(app.packageName, app.activityName, app.label)
                    } else {
                        preferencesStore.saveSwipeLeftApp(app.packageName, app.activityName, app.label)
                    }
                    _history.value = _history.value + HistoryEntry(
                        "shortcut $direction $query",
                        listOf(TerminalOutput.TextLine("set $direction shortcut → ${app.label}", dim = true))
                    )
                } else {
                    _history.value = _history.value + HistoryEntry(
                        "shortcut $direction $query",
                        listOf(TerminalOutput.Error("no app found for '$query'"))
                    )
                }
            }
        }

        // Wire app info command
        commandProcessor.onAppInfo = { query ->
            viewModelScope.launch {
                val apps = appRepository.apps.value
                val match = SearchEngine().search(query, apps).firstOrNull()
                if (match != null) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${match.app.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        getApplication<Application>().startActivity(intent)
                    } catch (_: Exception) {
                        _history.value = _history.value + HistoryEntry(
                            "info $query",
                            listOf(TerminalOutput.Error("can't open settings for '${match.app.label}'"))
                        )
                    }
                } else {
                    _history.value = _history.value + HistoryEntry(
                        "info $query",
                        listOf(TerminalOutput.Error("no app found for '$query'"))
                    )
                }
            }
        }

        // Wire uninstall command
        commandProcessor.onMusicPlayer = { showMusicPlayer() }

        commandProcessor.onUninstall = { query ->
            viewModelScope.launch {
                val apps = appRepository.apps.value
                val match = SearchEngine().search(query, apps).firstOrNull()
                if (match != null) {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = android.net.Uri.parse("package:${match.app.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        getApplication<Application>().startActivity(intent)
                    } catch (_: Exception) {
                        _history.value = _history.value + HistoryEntry(
                            "uninstall $query",
                            listOf(TerminalOutput.Error("can't uninstall '${match.app.label}'"))
                        )
                    }
                } else {
                    _history.value = _history.value + HistoryEntry(
                        "uninstall $query",
                        listOf(TerminalOutput.Error("no app found for '$query'"))
                    )
                }
            }
        }

    }

    fun refreshScreenTime() {
        _screenTime.value = usageTracker.getScreenTime()
    }

    val state: StateFlow<LauncherState> = combine(
        preferencesStore.setupComplete,
        appRepository.apps,
        combine(_terminalVisible, _musicPlayerVisible) { tv, mp -> tv to mp },
        _searchQuery,
        combine(_selectedIndex, _screenTime, _history, _isCommandMode, _showAppPicker) { idx, st, hist, cmd, picker ->
            listOf(idx, st, hist, cmd, picker)
        }
    ) { setupComplete, apps, visibility, query, extras ->
        val (terminalVisible, musicPlayerVisible) = visibility
        @Suppress("UNCHECKED_CAST")
        val selectedIndex = extras[0] as Int
        @Suppress("UNCHECKED_CAST")
        val screenTime = extras[1] as Map<String, Long>
        @Suppress("UNCHECKED_CAST")
        val history = extras[2] as List<HistoryEntry>
        val isCmd = extras[3] as Boolean
        val showPicker = extras[4] as String?

        val commandMode = isCmd || commandProcessor.isCommand(query)

        val results = if (commandMode) emptyList()
        else searchEngine.search(query, apps, screenTime)

        val clampedIndex = if (results.isEmpty()) 0 else selectedIndex.coerceIn(0, results.size - 1)

        LauncherState(
            screen = if (setupComplete) Screen.HOME else Screen.SETUP,
            terminalVisible = terminalVisible,
            searchQuery = query,
            searchResults = results,
            selectedIndex = clampedIndex,
            hasUsagePermission = usageTracker.hasPermission(),
            isCommandMode = commandMode,
            history = history,
            currentPath = commandProcessor.currentPath,
            showAppPicker = showPicker,
            musicPlayerVisible = musicPlayerVisible
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LauncherState())

    override fun onCleared() {
        super.onCleared()
        appRepository.stop()
    }

    fun completeSetup() {
        viewModelScope.launch {
            preferencesStore.completeSetup()
            refreshScreenTime()
        }
    }

    fun updateQuery(query: String) {
        _searchQuery.value = query
        _selectedIndex.value = 0
    }

    fun tabComplete() {
        val completed = commandProcessor.tabComplete(_searchQuery.value)
        if (completed != null) {
            _searchQuery.value = completed
        }
    }

    fun executeCommand(): Boolean {
        val input = _searchQuery.value.trim()
        if (input.isEmpty()) return false

        // Clear command
        if (input.lowercase() == "clear") {
            _history.value = emptyList()
            _searchQuery.value = ""
            _isCommandMode.value = false
            return true
        }

        if (commandProcessor.isCommand(input)) {
            val result = commandProcessor.execute(input)
            _history.value = _history.value + HistoryEntry(input, result.output)
            _searchQuery.value = ""
            _isCommandMode.value = false
            return true
        }

        // Not a command — try launching top app result
        return launchSelected()
    }

    fun moveSelectionUp() {
        val current = _selectedIndex.value
        if (current > 0) _selectedIndex.value = current - 1
    }

    fun moveSelectionDown() {
        val results = state.value.searchResults
        val current = _selectedIndex.value
        if (current < results.size - 1) _selectedIndex.value = current + 1
    }

    fun launchSelected(): Boolean {
        val results = state.value.searchResults
        val index = state.value.selectedIndex
        if (index >= results.size) return false
        return launchApp(results[index].app)
    }

    fun launchAtIndex(index: Int): Boolean {
        val results = state.value.searchResults
        if (index >= results.size) return false
        return launchApp(results[index].app)
    }

    fun requestUsagePermission() {
        usageTracker.openPermissionSettings()
    }

    fun showMusicPlayer() {
        _musicPlayerVisible.value = true
    }

    fun hideMusicPlayer() {
        _musicPlayerVisible.value = false
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    // Returns the saved shortcut app or null (picker needed)
    suspend fun getSwipeApp(direction: String): PreferencesStore.SwipeApp? {
        return when (direction) {
            "right" -> preferencesStore.swipeRightAppCurrent()
            "left" -> preferencesStore.swipeLeftAppCurrent()
            "down" -> preferencesStore.swipeDownAppCurrent()
            else -> null
        }
    }

    fun showShortcutPicker(direction: String) {
        _showAppPicker.value = direction
        _terminalVisible.value = true
        _searchQuery.value = ""
    }

    fun setShortcutApp(app: AppInfo) {
        viewModelScope.launch {
            val direction = _showAppPicker.value ?: return@launch
            when (direction) {
                "right" -> preferencesStore.saveSwipeRightApp(app.packageName, app.activityName, app.label)
                "left" -> preferencesStore.saveSwipeLeftApp(app.packageName, app.activityName, app.label)
                "down" -> preferencesStore.saveSwipeDownApp(app.packageName, app.activityName, app.label)
            }
            _showAppPicker.value = null
            _searchQuery.value = ""
        }
    }

    private fun launchApp(app: AppInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            getApplication<Application>().startActivity(intent)
            _searchQuery.value = ""
            _selectedIndex.value = 0
            _isCommandMode.value = false
            true
        } catch (_: Exception) {
            false
        }
    }
}
