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
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "terminal_launcher")

class PreferencesStore(private val context: Context) {

    companion object {
        private val BIRTH_YEAR = intPreferencesKey("birth_year")
        private val BIRTH_MONTH = intPreferencesKey("birth_month")
        private val LAUNCH_COUNTS = stringPreferencesKey("launch_counts")
        private val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
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
