package com.example.bilibiliautomation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibiliautomation.app.AppContainer
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.Target
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.ui.task.TaskUiState
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val browserReady = MutableStateFlow(false)
    private val currentUrl = MutableStateFlow("")

    val accounts: StateFlow<List<Account>> = container.accountManager.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val targets: StateFlow<List<Target>> = container.targetRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val task: StateFlow<TaskUiState> = container.taskScheduler.uiState

    val dashboard: StateFlow<HomeDashboard> = combine(
        accounts,
        targets,
        task,
        container.sessionManager.currentAccountIdFlow,
    ) { accountList, targetList, taskState, currentAccountId ->
        // Single source of truth: the user-chosen current account wins.
        // Always resolve the live Account from accountList so the badge
        // reflects the current DB status (e.g. LOGIN_EXPIRED) rather than a
        // snapshot captured at task start.
        val selectedAccount = currentAccountId
            ?.let { id -> accountList.firstOrNull { it.id == id } }
            ?: taskState.account?.let { shown -> accountList.firstOrNull { it.id == shown.id } ?: shown }
            ?: accountList.firstOrNull { it.status == AccountStatus.ACTIVE }
            ?: accountList.firstOrNull { !it.uid.startsWith("pending-") }
            ?: accountList.firstOrNull()
        val selectedTarget = taskState.target ?: targetList.firstOrNull { it.enabled } ?: targetList.firstOrNull()
        HomeDashboard(
            account = selectedAccount,
            target = selectedTarget,
            task = taskState,
            needLogin = selectedAccount == null ||
                selectedAccount.status == AccountStatus.LOGIN_REQUIRED ||
                selectedAccount.status == AccountStatus.LOGIN_EXPIRED,
            needTarget = selectedTarget == null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeDashboard())

    val state: StateFlow<HomeUiState> = combine(browserReady, currentUrl, task) { ready, url, taskState ->
        HomeUiState(
            browserReady = ready,
            currentUrl = url,
            interruptedTaskId = taskState.interruptedTaskId,
            message = taskState.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun markBrowserReady(url: String) {
        browserReady.value = true
        currentUrl.value = url
    }

    fun start(targetUid: String, accountId: Long) {
        container.taskScheduler.start(targetUid, accountId)
    }

    fun pause() = container.taskScheduler.pause()

    fun resume() {
        val id = task.value.taskId ?: return
        if (task.value.status == TaskStatus.PAUSED || task.value.status == TaskStatus.INTERRUPTED) {
            container.taskScheduler.resume(id)
        } else {
            pause()
        }
    }

    fun stop() = container.taskScheduler.cancel()

    fun restoreInterrupted() {
        val id = task.value.interruptedTaskId ?: return
        viewModelScope.launch { container.taskScheduler.restoreSafely(id) }
    }
}

data class HomeUiState(
    val browserReady: Boolean = false,
    val currentUrl: String = "",
    val interruptedTaskId: Long? = null,
    val message: String = "",
)

data class HomeDashboard(
    val account: Account? = null,
    val target: Target? = null,
    val task: TaskUiState = TaskUiState(),
    val needLogin: Boolean = true,
    val needTarget: Boolean = true,
) {
    val nowElapsed: String
        get() = if (task.startedAt <= 0L) "00:00:00" else
            com.example.bilibiliautomation.ui.UiFormat.elapsed(task.startedAt, TimeMs.wallClock())
}
