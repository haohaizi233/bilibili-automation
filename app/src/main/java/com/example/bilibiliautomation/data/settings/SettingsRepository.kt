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

    val settings: Flow<AppSettings> = dataStore.data.map { prefs -> prefs.toModel() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toModel())
            prefs[KEY_TIMEOUT] = next.defaultTimeoutMs
            prefs[KEY_POLL] = next.pollIntervalMs
            prefs[KEY_OP_INTERVAL] = next.operationIntervalMs
            prefs[KEY_RETRY] = next.retryCount
            prefs[KEY_COOLDOWN] = next.cooldownMinutes
            prefs[KEY_DAILY_LIMIT] = next.dailyLimit
            prefs[KEY_NOTIFICATION] = next.notificationEnabled
            prefs[KEY_DEBUG] = next.debugEnabled
            prefs[KEY_PARALLEL] = next.parallelTasks
            prefs[KEY_SMART_STATUS] = next.smartStatus
            prefs[KEY_AUTO_SWITCH] = next.autoSwitchAccount
            prefs[KEY_AUTO_RETRY] = next.autoRetry
            prefs[KEY_UNATTENDED] = next.unattendedMode
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.toModel(): AppSettings = AppSettings(
        defaultTimeoutMs = this[KEY_TIMEOUT] ?: 10_000L,
        pollIntervalMs = this[KEY_POLL] ?: 200L,
        operationIntervalMs = this[KEY_OP_INTERVAL] ?: 5_000L,
        retryCount = this[KEY_RETRY] ?: 3,
        cooldownMinutes = this[KEY_COOLDOWN] ?: 30,
        dailyLimit = this[KEY_DAILY_LIMIT] ?: 200,
        notificationEnabled = this[KEY_NOTIFICATION] ?: true,
        debugEnabled = this[KEY_DEBUG] ?: false,
        parallelTasks = this[KEY_PARALLEL] ?: false,
        smartStatus = this[KEY_SMART_STATUS] ?: true,
        autoSwitchAccount = this[KEY_AUTO_SWITCH] ?: true,
        autoRetry = this[KEY_AUTO_RETRY] ?: true,
        unattendedMode = this[KEY_UNATTENDED] ?: false,
    )

    private companion object {
        val KEY_TIMEOUT = longPreferencesKey("defaultTimeoutMs")
        val KEY_POLL = longPreferencesKey("pollIntervalMs")
        val KEY_OP_INTERVAL = longPreferencesKey("operationIntervalMs")
        val KEY_RETRY = intPreferencesKey("retryCount")
        val KEY_COOLDOWN = intPreferencesKey("cooldownMinutes")
        val KEY_DAILY_LIMIT = intPreferencesKey("dailyLimit")
        val KEY_NOTIFICATION = booleanPreferencesKey("notificationEnabled")
        val KEY_DEBUG = booleanPreferencesKey("debugEnabled")
        val KEY_PARALLEL = booleanPreferencesKey("parallelTasks")
        val KEY_SMART_STATUS = booleanPreferencesKey("smartStatus")
        val KEY_AUTO_SWITCH = booleanPreferencesKey("autoSwitchAccount")
        val KEY_AUTO_RETRY = booleanPreferencesKey("autoRetry")
        val KEY_UNATTENDED = booleanPreferencesKey("unattendedMode")
    }
}
