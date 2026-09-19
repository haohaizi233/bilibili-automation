package com.example.bilibiliautomation.domain.task

import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.repository.TargetRepository
import com.example.bilibiliautomation.data.repository.TaskRepository
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.model.SessionStatus
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.domain.workflow.AccountPool
import com.example.bilibiliautomation.domain.workflow.RunnableWorkflow
import com.example.bilibiliautomation.domain.workflow.SiteAdapter
import com.example.bilibiliautomation.domain.workflow.WorkflowContext
import com.example.bilibiliautomation.domain.workflow.WorkflowState
import com.example.bilibiliautomation.event.AutomationEvent
import com.example.bilibiliautomation.event.AutomationEventBus
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.ui.task.TaskUiState
import com.example.bilibiliautomation.util.TimeMs
import com.example.bilibiliautomation.domain.model.Progress
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * First version allows one active workflow. A second start while RUNNING is queued
 * in Room but not executed until the active task finishes — never two WebViews.
 */
class TaskScheduler(
    private val taskRepository: TaskRepository,
    private val accountRepository: AccountRepository,
    private val targetRepository: TargetRepository,
    private val accountPool: AccountPool,
    private val eventBus: AutomationEventBus,
    private val engineFactory: () -> RunnableWorkflow,
    private val adapterFactory: () -> SiteAdapter,
    private val logger: AutomationLogger = AutomationLogger,
) {
    private val mutex = Mutex()
    private val exceptionHandler = CoroutineExceptionHandler { _, error ->
        // Backstop: any exception that escapes a child launch (e.g. a stray
        // non-cancellation throw out of the workflow) would otherwise reach the
        // default thread uncaught handler and crash the whole process. Log it
        // and surface it as a FAILED task so the UI can recover.
        if (error is kotlinx.coroutines.CancellationException) return@CoroutineExceptionHandler
        logger.e("TaskScheduler coroutine crashed", error)
        publish(
            _ui.value.copy(
                status = TaskStatus.FAILED,
                workflowState = WorkflowState.FAILED,
                currentAction = "crash",
                message = error.message ?: error.javaClass.simpleName,
            ),
        )
    }
    private val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.Default + exceptionHandler)

    private val _ui = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _ui.asStateFlow()

    @Volatile
    private var activeJob: Job? = null

    @Volatile
    private var engine: RunnableWorkflow? = null

    @Volatile
    private var startedAt: Long = 0L

    val hasActiveTask: Boolean get() = activeJob?.isActive == true

    init {
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is AutomationEvent.StateChanged -> publish(
                        _ui.value.copy(
                            workflowState = event.to,
                            currentAction = event.to.name,
                            message = event.to.name,
                        ),
                    )
                    is AutomationEvent.ActionStarted -> publish(
                        _ui.value.copy(
                            currentAction = event.action,
                            message = event.action,
                        ),
                    )
                    is AutomationEvent.ActionCompleted -> {
                        val current = _ui.value
                        publish(
                            current.copy(
                                followed = current.followed + if (event.resultCode == ResultCode.OK) 1 else 0,
                                skipped = current.skipped + if (event.resultCode == ResultCode.SKIPPED) 1 else 0,
                                failed = current.failed + if (
                                    event.resultCode != ResultCode.OK && event.resultCode != ResultCode.SKIPPED
                                ) 1 else 0,
                                currentAction = event.action,
                                message = event.message.ifBlank { event.action },
                            ),
                        )
                    }
                    is AutomationEvent.VideoProgress -> publish(
                        _ui.value.copy(
                            videoProgress = Progress(event.current, event.total),
                            currentVideoTitle = event.title,
                            currentBvid = event.bvid,
                            currentVideoCover = event.coverUrl,
                        ),
                    )
                    is AutomationEvent.CommentProgress -> publish(
                        _ui.value.copy(
                            commentProgress = Progress(event.current, event.total),
                        ),
                    )
                    is AutomationEvent.TaskPaused -> {
                        val refreshed = refreshAccountSnapshot()
                        publish(
                            _ui.value.copy(
                                status = TaskStatus.PAUSED,
                                workflowState = WorkflowState.PAUSED,
                                message = event.reason,
                                currentAction = "paused",
                                account = refreshed,
                                pausedAt = TimeMs.wallClock(),
                            ),
                        )
                    }
                    is AutomationEvent.TaskFailed -> {
                        val refreshed = refreshAccountSnapshot()
                        publish(
                            _ui.value.copy(
                                status = TaskStatus.FAILED,
                                workflowState = WorkflowState.FAILED,
                                message = event.message,
                                currentAction = event.code,
                                account = refreshed,
                            ),
                        )
                    }
                    is AutomationEvent.TaskFinished -> publish(
                        _ui.value.copy(
                            status = TaskStatus.FINISHED,
                            workflowState = WorkflowState.FINISHED,
                            currentAction = "finished",
                            message = "finished",
                        ),
                    )
                    is AutomationEvent.TaskCancelled -> publish(
                        _ui.value.copy(
                            status = TaskStatus.CANCELLED,
                            currentAction = "cancelled",
                            message = "cancelled",
                        ),
                    )
                    is AutomationEvent.NeedsReview -> {
                        val refreshed = refreshAccountSnapshot()
                        publish(
                            _ui.value.copy(
                                status = TaskStatus.PAUSED,
                                workflowState = WorkflowState.PAUSED,
                                message = event.message,
                                currentAction = event.code,
                                account = refreshed,
                                pausedAt = TimeMs.wallClock(),
                            ),
                        )
                    }
                    is AutomationEvent.AccountSwitched -> {
                        // Pool rotated the task onto a new account mid-run. Refresh
                        // the account snapshot so the "当前账号" card shows the
                        // new (active) account instead of the cooled-down old one.
                        val refreshed = event.newAccountId.let { id ->
                            accountRepository.getById(id)
                        } ?: refreshAccountSnapshot()
                        publish(
                            _ui.value.copy(
                                account = refreshed,
                                currentAction = "switchAccount",
                                message = "账号轮换：${event.oldAccountId ?: -1L} -> ${event.newAccountId}",
                            ),
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun start(targetUid: String, accountId: Long): AutomationResult<Long> {
        if (hasActiveTask) {
            logger.w("TaskScheduler busy — refusing second concurrent WebView task")
            return AutomationResult.fail(ResultCode.BUSY, "already running")
        }
        val launch = scope.launch {
            mutex.withLock {
                val resolved = resolveIdleAccount(accountId)
                val boundId = resolved?.id ?: accountId
                val task = taskRepository.create(targetUid, boundId)
                val now = TimeMs.wallClock()
                taskRepository.updateStatus(task.id, TaskStatus.RUNNING, startedAt = now)
                startedAt = TimeMs.now()
                val account = resolved ?: accountRepository.getById(boundId)
                val target = targetRepository.getByUid(targetUid)
                publish(
                    _ui.value.copy(
                        status = TaskStatus.RUNNING,
                        workflowState = WorkflowState.VALIDATE_ACCOUNT,
                        taskId = task.id,
                        account = account,
                        target = target,
                        currentAction = "start",
                        message = "running",
                        startedAt = now,
                        pausedAt = null,
                        followed = 0,
                        skipped = 0,
                        failed = 0,
                        interruptedTaskId = null,
                    ),
                )
                eventBus.emit(AutomationEvent.TaskStarted(task.id))
                val wf = engineFactory()
                engine = wf
                try {
                    wf.run(task.copy(status = TaskStatus.RUNNING, startedAt = now))
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    logger.e("TaskScheduler start workflow crashed", error)
                    publish(
                        _ui.value.copy(
                            status = TaskStatus.FAILED,
                            workflowState = WorkflowState.FAILED,
                            currentAction = "crash",
                            message = error.message ?: error.javaClass.simpleName,
                        ),
                    )
                }
                engine = null
                refreshFromDb(task.id)
            }
        }
        activeJob = launch
        return AutomationResult.ok(0L, "started")
    }

    fun pause() {
        engine?.pauseRequested = true
        logger.i("TaskScheduler pause requested")
    }

    fun resume(taskId: Long) {
        if (hasActiveTask) {
            logger.w("cannot resume while another task is running")
            return
        }
        activeJob = scope.launch {
            mutex.withLock {
                val task = taskRepository.getById(taskId) ?: return@withLock
                val checkpoint = taskRepository.getCheckpoint(taskId)
                val startState = checkpoint?.workflowState?.takeUnless { it.isTerminal }
                    ?: WorkflowState.VALIDATE_ACCOUNT
                val resolved = resolveIdleAccount(task.accountId)
                if (resolved != null && resolved.id != task.accountId) {
                    taskRepository.updateAccount(taskId, resolved.id)
                    publish(_ui.value.copy(account = resolved))
                }
                taskRepository.updateStatus(taskId, TaskStatus.RUNNING, startedAt = TimeMs.wallClock())
                val wf = engineFactory()
                engine = wf
                val boundTask = task.copy(accountId = resolved?.id ?: task.accountId)
                val ctx = WorkflowContext(
                    task = boundTask,
                    currentAccountId = boundTask.accountId,
                    videoIndex = checkpoint?.videoIndex ?: 0,
                    commentIndex = checkpoint?.commentIndex ?: 0,
                    currentUserUid = checkpoint?.currentUserUid,
                    currentUrl = checkpoint?.currentUrl,
                    followed = checkpoint?.followed ?: 0,
                    skipped = checkpoint?.skipped ?: 0,
                    failed = checkpoint?.failed ?: 0,
                )
                try {
                    wf.run(boundTask, startState, ctx)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    logger.e("TaskScheduler resume workflow crashed", error)
                    publish(
                        _ui.value.copy(
                            status = TaskStatus.FAILED,
                            workflowState = WorkflowState.FAILED,
                            currentAction = "crash",
                            message = error.message ?: error.javaClass.simpleName,
                        ),
                    )
                }
                engine = null
                refreshFromDb(taskId)
            }
        }
    }

    fun cancel() {
        engine?.cancelRequested = true
        activeJob?.cancel()
        _ui.value.taskId?.let { id ->
            scope.launch {
                taskRepository.updateStatus(id, TaskStatus.CANCELLED, lastError = "cancelled", finishedAt = TimeMs.wallClock())
            }
        }
        publish(_ui.value.copy(status = TaskStatus.CANCELLED, currentAction = "cancelled"))
    }

    /**
     * Crash recovery: validate session / target / page, then leave the task PAUSED.
     * Never continues PERFORM_ACTION automatically.
     */
    suspend fun restoreSafely(taskId: Long): AutomationResult<Unit> {
        val task = taskRepository.getById(taskId)
            ?: return AutomationResult.fail(ResultCode.INVALID_ARGUMENT, "task not found")
        val checkpoint = taskRepository.getCheckpoint(taskId)
        val adapter = adapterFactory()
        val session = adapter.validateSession()
        if (session != SessionStatus.VALID) {
            taskRepository.updateStatus(taskId, TaskStatus.PAUSED, lastError = "restore: session $session")
            publish(
                _ui.value.copy(
                    status = TaskStatus.PAUSED,
                    taskId = taskId,
                    workflowState = WorkflowState.PAUSED,
                    message = "restore paused: session $session",
                    interruptedTaskId = null,
                ),
            )
            return AutomationResult.fail(ResultCode.LOGIN_REQUIRED, "session $session")
        }
        val page = adapter.detectPage()
        logger.i("restore task=$taskId page=$page checkpoint=${checkpoint?.workflowState}")
        taskRepository.updateStatus(taskId, TaskStatus.PAUSED, lastError = "restored, waiting for resume")
        publish(
            _ui.value.copy(
                status = TaskStatus.PAUSED,
                taskId = taskId,
                workflowState = checkpoint?.workflowState ?: WorkflowState.PAUSED,
                message = "validated; resume when ready",
                interruptedTaskId = null,
            ),
        )
        return AutomationResult.ok(message = "ready to resume")
    }

    fun markInterrupted(taskId: Long) {
        publish(_ui.value.copy(interruptedTaskId = taskId, message = "previous run interrupted"))
    }

    /**
     * Rehydrate the in-memory TaskUiState from the most recent task + its
     * checkpoint so that closing and reopening the app does not wipe the
     * task progress / counters / current video. Called once on startup.
     */
    suspend fun restoreUiState() {
        val task = taskRepository.getLatest() ?: return
        val checkpoint = taskRepository.getCheckpoint(task.id)
        val account = resolveIdleAccount(task.accountId)
            ?: task.accountId?.let { accountRepository.getById(it) }
        if (account != null && account.id != task.accountId) {
            taskRepository.updateAccount(task.id, account.id)
        }
        val target = task.targetUid?.let { targetRepository.getByUid(it) }
        val videoTotal = checkpoint?.videoTotal ?: 0
        val commentTotal = checkpoint?.commentTotal ?: 0
        val videoCurrent = if (videoTotal > 0) (checkpoint?.videoIndex ?: 0).coerceIn(0, videoTotal - 1) + 1 else 0
        val commentCurrent = if (commentTotal > 0) (checkpoint?.commentIndex ?: 0).coerceIn(0, commentTotal - 1) + 1 else 0
        publish(
            TaskUiState(
                status = task.status,
                workflowState = checkpoint?.workflowState ?: WorkflowState.IDLE,
                taskId = task.id,
                target = target,
                account = account,
                videoProgress = Progress(videoCurrent, videoTotal),
                commentProgress = Progress(commentCurrent, commentTotal),
                currentAction = if (task.status == TaskStatus.PAUSED) "paused" else task.status.name,
                startedAt = task.startedAt ?: 0L,
                pausedAt = task.pausedAt,
                message = task.lastError.orEmpty(),
                interruptedTaskId = if (task.status == TaskStatus.INTERRUPTED) task.id else null,
                followed = checkpoint?.followed ?: 0,
                skipped = checkpoint?.skipped ?: 0,
                failed = checkpoint?.failed ?: 0,
                currentVideoTitle = checkpoint?.currentVideoTitle.orEmpty(),
                currentBvid = checkpoint?.videoId.orEmpty(),
                currentVideoCover = checkpoint?.currentVideoCover.orEmpty(),
            ),
        )
        logger.i("restored task ui state task=${task.id} status=${task.status} videos=$videoCurrent/$videoTotal followed=${checkpoint?.followed ?: 0}")
    }

    fun shutdown() {
        cancel()
        scope.cancel()
    }

    private suspend fun refreshFromDb(taskId: Long) {
        val task = taskRepository.getById(taskId) ?: return
        val checkpoint = taskRepository.getCheckpoint(taskId)
        // Refresh the account snapshot so the UI reflects status changes the
        // workflow made mid-run (e.g. LOGIN_EXPIRED after a session-expired
        // error). Without this the task card keeps showing the status captured
        // at task start.
        val refreshedAccount = task.accountId?.let { accountRepository.getById(it) } ?: _ui.value.account
        publish(
            _ui.value.copy(
                status = task.status,
                workflowState = checkpoint?.workflowState ?: _ui.value.workflowState,
                taskId = taskId,
                account = refreshedAccount,
                message = task.lastError.orEmpty(),
                elapsedTime = TimeMs.now() - startedAt,
            ),
        )
    }

    private fun publish(state: TaskUiState) {
        _ui.value = state
    }

    /**
     * Bind [accountId] onto the current task (UI + persisted row) so the
     * "当前账号" card and the next resume/start use the chosen idle account.
     */
    suspend fun bindAccount(accountId: Long): Account? {
        val account = accountRepository.getById(accountId) ?: return null
        _ui.value.taskId?.let { taskRepository.updateAccount(it, accountId) }
        publish(_ui.value.copy(account = account, currentAction = "switchAccount"))
        return account
    }

    /**
     * If the task has no account, or the bound one is cooling / expired,
     * occupy the first idle account from the pool. Returns the account that
     * should be shown as "当前账号" (may still be a non-idle bound account
     * when the pool is empty).
     */
    suspend fun ensureIdleAccount(): Account? {
        val resolved = resolveIdleAccount(
            preferredId = _ui.value.account?.id ?: _ui.value.taskId?.let { taskRepository.getById(it)?.accountId },
        )
        if (resolved != null) {
            val taskId = _ui.value.taskId
            if (taskId != null && resolved.id != _ui.value.account?.id) {
                taskRepository.updateAccount(taskId, resolved.id)
            }
            if (_ui.value.account?.id != resolved.id) {
                publish(_ui.value.copy(account = resolved))
            }
            return resolved
        }
        val leftover = _ui.value.account?.id?.let { accountRepository.getById(it) } ?: _ui.value.account
        if (leftover != null && leftover !== _ui.value.account) {
            publish(_ui.value.copy(account = leftover))
        }
        return leftover
    }

    private suspend fun refreshAccountSnapshot(): Account? {
        val id = _ui.value.account?.id ?: return _ui.value.account
        return accountRepository.getById(id) ?: _ui.value.account
    }

    /**
     * Prefer [preferredId] when it is still idle; otherwise pick another idle
     * account from the pool. Activates the API session for whichever is used.
     */
    private suspend fun resolveIdleAccount(preferredId: Long?): Account? {
        val preferred = preferredId?.let { accountRepository.getById(it) }
        if (preferred != null && AccountPool.isIdle(preferred)) {
            return accountPool.occupy(preferred.id) ?: preferred
        }
        return accountPool.pick(excluding = preferredId)
    }
}
