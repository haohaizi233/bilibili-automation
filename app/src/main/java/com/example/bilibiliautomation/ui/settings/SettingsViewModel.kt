package com.example.bilibiliautomation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.app.AppContainer
import com.example.bilibiliautomation.domain.model.AppSettings
import com.example.bilibiliautomation.ui.UiFormat
import com.example.bilibiliautomation.ui.compose.screens.TaskSettingsUiState
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

    fun setUnattended(enabled: Boolean) = update { it.copy(unattendedMode = enabled) }

    fun applyTaskSettings(state: TaskSettingsUiState) = update {
        it.copy(
            operationIntervalMs = UiFormat.parseIntervalMs(state.interval),
            retryCount = state.retries.toIntOrNull()?.coerceIn(0, 10) ?: 3,
            cooldownMinutes = state.cooldown.toIntOrNull()?.coerceAtLeast(1) ?: 30,
            dailyLimit = state.dailyLimit.toIntOrNull()?.coerceAtLeast(1) ?: 200,
            parallelTasks = state.parallelTasks,
            smartStatus = state.smartStatus,
            autoSwitchAccount = state.autoSwitchAccount,
            autoRetry = state.autoRetry,
            notificationEnabled = state.notifyOnComplete,
            debugEnabled = state.verboseLog,
        )
    }

    fun clearLogs() {
        com.example.bilibiliautomation.logging.AutomationLogger.clear()
    }

    fun clearHistory() {
        viewModelScope.launch {
            container.taskScheduler.cancel()
            container.taskRepository.deleteAll()
            container.videoRepository.deleteAll()
            container.commentRepository.deleteAll()
            container.userRepository.deleteAll()
            container.actionRepository.deleteAll()
            container.processedUserRepository.clear()
            com.example.bilibiliautomation.ui.FaceBitmapCache.clear()
            com.example.bilibiliautomation.logging.AutomationLogger.clear()
            container.taskScheduler.resetUi()
            com.example.bilibiliautomation.logging.AutomationLogger.i("history cache cleared")
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settingsRepository.update(transform) }
    }
}
