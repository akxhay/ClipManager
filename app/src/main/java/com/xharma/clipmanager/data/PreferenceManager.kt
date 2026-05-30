package com.xharma.clipmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {

    companion object {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
    }

    val isServiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SERVICE_ENABLED] ?: false
    }

    val isAutoSaveEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SAVE_ENABLED] ?: false
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SAVE_ENABLED] = enabled
        }
    }
}
