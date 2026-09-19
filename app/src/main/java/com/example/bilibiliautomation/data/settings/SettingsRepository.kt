package com.example.bilibiliautomation.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bilibiliautomation.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            defaultTimeoutMs = prefs[KEY_TIMEOUT] ?: 10_000L,
            pollIntervalMs = prefs[KEY_POLL] ?: 200L,
            operationIntervalMs = prefs[KEY_OP_INTERVAL] ?: 5_000L,
            retryCount = prefs[KEY_RETRY] ?: 3,
            cooldownMinutes = prefs[KEY_COOLDOWN] ?: 30,
            dailyLimit = prefs[KEY_DAILY_LIMIT] ?: 200,
            notificationEnabled = prefs[KEY_NOTIFICATION] ?: true,
            debugEnabled = prefs[KEY_DEBUG] ?: true,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = AppSettings(
                defaultTimeoutMs = prefs[KEY_TIMEOUT] ?: 10_000L,
                pollIntervalMs = prefs[KEY_POLL] ?: 200L,
                operationIntervalMs = prefs[KEY_OP_INTERVAL] ?: 5_000L,
                retryCount = prefs[KEY_RETRY] ?: 3,
                cooldownMinutes = prefs[KEY_COOLDOWN] ?: 30,
                dailyLimit = prefs[KEY_DAILY_LIMIT] ?: 200,
                notificationEnabled = prefs[KEY_NOTIFICATION] ?: true,
                debugEnabled = prefs[KEY_DEBUG] ?: true,
            )
            val next = transform(current)
            prefs[KEY_TIMEOUT] = next.defaultTimeoutMs
            prefs[KEY_POLL] = next.pollIntervalMs
            prefs[KEY_OP_INTERVAL] = next.operationIntervalMs
            prefs[KEY_RETRY] = next.retryCount
            prefs[KEY_COOLDOWN] = next.cooldownMinutes
            prefs[KEY_DAILY_LIMIT] = next.dailyLimit
            prefs[KEY_NOTIFICATION] = next.notificationEnabled
            prefs[KEY_DEBUG] = next.debugEnabled
        }
    }

    private companion object {
        val KEY_TIMEOUT = longPreferencesKey("defaultTimeoutMs")
        val KEY_POLL = longPreferencesKey("pollIntervalMs")
        val KEY_OP_INTERVAL = longPreferencesKey("operationIntervalMs")
        val KEY_RETRY = intPreferencesKey("retryCount")
        val KEY_COOLDOWN = intPreferencesKey("cooldownMinutes")
        val KEY_DAILY_LIMIT = intPreferencesKey("dailyLimit")
        val KEY_NOTIFICATION = booleanPreferencesKey("notificationEnabled")
        val KEY_DEBUG = booleanPreferencesKey("debugEnabled")
    }
}
