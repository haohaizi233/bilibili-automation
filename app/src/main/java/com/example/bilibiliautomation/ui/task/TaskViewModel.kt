package com.example.bilibiliautomation.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.app.AppContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val state: StateFlow<TaskUiState> = container.taskScheduler.uiState

    fun start(targetUid: String, accountId: Long) {
        container.taskScheduler.start(targetUid, accountId)
    }

    fun start(targetUid: String, accountId: Long, groupId: Long?) {
        container.taskScheduler.start(targetUid, accountId, groupId)
    }

    fun selectTarget(uid: String) = container.taskScheduler.selectTarget(uid)

    fun selectGroup(groupId: Long, startUid: String? = null) =
        container.taskScheduler.selectGroup(groupId, startUid)

    fun switchScope(targetUid: String, accountId: Long, groupId: Long?) {
        container.taskScheduler.switchScope(targetUid, accountId, groupId)
    }

    fun pause() = container.taskScheduler.pause()

    fun resume(taskId: Long) = container.taskScheduler.resume(taskId)

    fun cancel() = container.taskScheduler.cancel()

    fun ensureIdleAccount() {
        viewModelScope.launch {
            val account = container.taskScheduler.ensureIdleAccount() ?: return@launch
            container.sessionManager.markCurrent(account.id)
            container.apiSessionManager.activate(account.id)
        }
    }

    fun switchTo(accountId: Long) {
        viewModelScope.launch {
            val bound = container.taskScheduler.bindAccount(accountId) ?: return@launch
            container.sessionManager.markCurrent(bound.id)
            container.apiSessionManager.activate(bound.id)
        }
    }
}
