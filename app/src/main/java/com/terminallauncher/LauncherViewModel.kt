package com.terminallauncher

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.terminallauncher.data.AppInfo
import com.terminallauncher.data.AppRepository
import com.terminallauncher.data.PreferencesStore
import com.terminallauncher.data.SearchEngine
import com.terminallauncher.data.SearchResult
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

data class LauncherState(
    val screen: Screen = Screen.SETUP,
    val birthYear: Int? = null,
    val birthMonth: Int? = null,
    val terminalVisible: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val selectedIndex: Int = 0,
    val hasUsagePermission: Boolean = false
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesStore = PreferencesStore(application)
    val appRepository = AppRepository(application)
    private val searchEngine = SearchEngine()
    val usageTracker = UsageTracker(application)

    private val _terminalVisible = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedIndex = MutableStateFlow(0)
    private val _screenTime = MutableStateFlow<Map<String, Long>>(emptyMap())

    init {
        appRepository.start()
        refreshScreenTime()
    }

    fun refreshScreenTime() {
        _screenTime.value = usageTracker.getScreenTime()
    }

    val state: StateFlow<LauncherState> = combine(
        combine(preferencesStore.birthDate, preferencesStore.setupComplete) { bd, sc -> bd to sc },
        appRepository.apps,
        _terminalVisible,
        _searchQuery,
        combine(_selectedIndex, _screenTime) { idx, st -> idx to st }
    ) { (birthDate, setupComplete), apps, terminalVisible, query, (selectedIndex, screenTime) ->
        val results = searchEngine.search(query, apps, screenTime)
        val clampedIndex = if (results.isEmpty()) 0 else selectedIndex.coerceIn(0, results.size - 1)

        LauncherState(
            screen = if (setupComplete) Screen.HOME else Screen.SETUP,
            birthYear = birthDate?.year,
            birthMonth = birthDate?.month,
            terminalVisible = terminalVisible,
            searchQuery = query,
            searchResults = results,
            selectedIndex = clampedIndex,
            hasUsagePermission = usageTracker.hasPermission()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LauncherState())

    override fun onCleared() {
        super.onCleared()
        appRepository.stop()
    }

    fun saveBirthDate(year: Int, month: Int) {
        viewModelScope.launch {
            preferencesStore.saveBirthDate(year, month)
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            preferencesStore.completeSetup()
            refreshScreenTime()
        }
    }

    fun showTerminal() {
        refreshScreenTime()
        _terminalVisible.value = true
        _searchQuery.value = ""
        _selectedIndex.value = 0
    }

    fun hideTerminal() {
        _terminalVisible.value = false
        _searchQuery.value = ""
        _selectedIndex.value = 0
    }

    fun updateQuery(query: String) {
        _searchQuery.value = query
        _selectedIndex.value = 0
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

    private fun launchApp(app: AppInfo): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            getApplication<Application>().startActivity(intent)
            hideTerminal()
            true
        } catch (_: Exception) {
            false
        }
    }
}
