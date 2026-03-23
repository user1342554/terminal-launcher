package com.terminallauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "terminal_launcher")

class PreferencesStore(private val context: Context) {

    companion object {
        private val BIRTH_YEAR = intPreferencesKey("birth_year")
        private val BIRTH_MONTH = intPreferencesKey("birth_month")
        private val LAUNCH_COUNTS = stringPreferencesKey("launch_counts")
        private val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        private val SWIPE_RIGHT_PACKAGE = stringPreferencesKey("swipe_right_package")
        private val SWIPE_RIGHT_ACTIVITY = stringPreferencesKey("swipe_right_activity")
        private val SWIPE_RIGHT_LABEL = stringPreferencesKey("swipe_right_label")
        private val SWIPE_LEFT_PACKAGE = stringPreferencesKey("swipe_left_package")
        private val SWIPE_LEFT_ACTIVITY = stringPreferencesKey("swipe_left_activity")
        private val SWIPE_LEFT_LABEL = stringPreferencesKey("swipe_left_label")
    }

    data class BirthDate(val year: Int, val month: Int)

    val birthDate: Flow<BirthDate?> = context.dataStore.data.map { prefs ->
        val year = prefs[BIRTH_YEAR]
        val month = prefs[BIRTH_MONTH]
        if (year != null && month != null) BirthDate(year, month) else null
    }

    val launchCounts: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        val raw = prefs[LAUNCH_COUNTS] ?: ""
        if (raw.isEmpty()) emptyMap()
        else raw.split(";").mapNotNull { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap()
    }

    val setupComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SETUP_COMPLETE] ?: false
    }

    suspend fun completeSetup() {
        context.dataStore.edit { prefs ->
            prefs[SETUP_COMPLETE] = true
        }
    }

    suspend fun saveBirthDate(year: Int, month: Int) {
        context.dataStore.edit { prefs ->
            prefs[BIRTH_YEAR] = year
            prefs[BIRTH_MONTH] = month
        }
    }

    suspend fun resetSetup() {
        context.dataStore.edit { prefs ->
            prefs[SETUP_COMPLETE] = false
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    data class SwipeApp(val packageName: String, val activityName: String, val label: String)

    val swipeRightApp: Flow<SwipeApp?> = context.dataStore.data.map { prefs ->
        val pkg = prefs[SWIPE_RIGHT_PACKAGE]
        val activity = prefs[SWIPE_RIGHT_ACTIVITY]
        val label = prefs[SWIPE_RIGHT_LABEL]
        if (pkg != null && activity != null && label != null) SwipeApp(pkg, activity, label) else null
    }

    suspend fun swipeRightAppCurrent(): SwipeApp? {
        return swipeRightApp.first()
    }

    val swipeLeftApp: Flow<SwipeApp?> = context.dataStore.data.map { prefs ->
        val pkg = prefs[SWIPE_LEFT_PACKAGE]
        val activity = prefs[SWIPE_LEFT_ACTIVITY]
        val label = prefs[SWIPE_LEFT_LABEL]
        if (pkg != null && activity != null && label != null) SwipeApp(pkg, activity, label) else null
    }

    suspend fun swipeLeftAppCurrent(): SwipeApp? {
        return swipeLeftApp.first()
    }

    suspend fun saveSwipeLeftApp(packageName: String, activityName: String, label: String) {
        context.dataStore.edit { prefs ->
            prefs[SWIPE_LEFT_PACKAGE] = packageName
            prefs[SWIPE_LEFT_ACTIVITY] = activityName
            prefs[SWIPE_LEFT_LABEL] = label
        }
    }

    suspend fun saveSwipeRightApp(packageName: String, activityName: String, label: String) {
        context.dataStore.edit { prefs ->
            prefs[SWIPE_RIGHT_PACKAGE] = packageName
            prefs[SWIPE_RIGHT_ACTIVITY] = activityName
            prefs[SWIPE_RIGHT_LABEL] = label
        }
    }

    suspend fun recordLaunch(packageName: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[LAUNCH_COUNTS] ?: ""
            val counts = if (raw.isEmpty()) mutableMapOf()
            else raw.split(";").mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
            }.toMap().toMutableMap()

            counts[packageName] = (counts[packageName] ?: 0) + 1

            prefs[LAUNCH_COUNTS] = counts.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }
}
