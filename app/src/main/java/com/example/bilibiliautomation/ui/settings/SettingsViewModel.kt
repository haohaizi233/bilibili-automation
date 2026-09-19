package com.example.bilibiliautomation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.app.AppContainer
import com.example.bilibiliautomation.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setTimeout(ms: Long) = update { it.copy(defaultTimeoutMs = ms) }

    fun setDebug(enabled: Boolean) = update { it.copy(debugEnabled = enabled) }

    fun setNotifications(enabled: Boolean) = update { it.copy(notificationEnabled = enabled) }

    fun applyTaskSettings(
        intervalMs: Long,
        retryCount: Int,
        cooldownMinutes: Int,
        dailyLimit: Int,
        notify: Boolean,
        debug: Boolean,
    ) = update {
        it.copy(
            operationIntervalMs = intervalMs.coerceAtLeast(200L),
            retryCount = retryCount.coerceIn(0, 10),
            cooldownMinutes = cooldownMinutes.coerceAtLeast(1),
            dailyLimit = dailyLimit.coerceAtLeast(1),
            notificationEnabled = notify,
            debugEnabled = debug,
        )
    }

    fun clearLogs() {
        com.example.bilibiliautomation.logging.AutomationLogger.clear()
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settingsRepository.update(transform) }
    }
}
