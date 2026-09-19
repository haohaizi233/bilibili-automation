package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.repository.ActionRepository
import com.example.bilibiliautomation.data.repository.CommentRepository
import com.example.bilibiliautomation.data.repository.TaskRepository
import com.example.bilibiliautomation.data.repository.UserRepository
import com.example.bilibiliautomation.data.repository.VideoRepository
import com.example.bilibiliautomation.domain.account.AccountManager
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.RelationshipState
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.model.SessionStatus
import com.example.bilibiliautomation.domain.model.Task
import com.example.bilibiliautomation.domain.model.TaskCheckpoint
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.event.AutomationEvent
import com.example.bilibiliautomation.event.AutomationEventBus
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.logging.LogLevel
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

data class WorkflowContext(
    val task: Task,
    var currentAccountId: Long? = null,
    var videos: List<com.example.bilibiliautomation.domain.model.LoadedVideo> = emptyList(),
    var comments: List<com.example.bilibiliautomation.domain.model.LoadedComment> = emptyList(),
    var videoIndex: Int = 0,
    var commentIndex: Int = 0,
    var currentUserUid: String? = null,
    var currentUrl: String? = null,
    var followed: Int = 0,
    var skipped: Int = 0,
    var failed: Int = 0,
)

class WorkflowEngine(
    private val adapter: SiteAdapter,
    private val taskRepository: TaskRepository,
    private val accountRepository: AccountRepository,
    private val videoRepository: VideoRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val actionRepository: ActionRepository,
    private val accountManager: AccountManager,
    private val eventBus: AutomationEventBus,
    private val logger: AutomationLogger = AutomationLogger,
    private val cooldownMinutes: Int = 30,
    private val operationIntervalMs: Long = 50L,
) : RunnableWorkflow {
    private val _state = MutableStateFlow(WorkflowState.IDLE)
    override val state: StateFlow<WorkflowState> = _state.asStateFlow()

    @Volatile
    override var pauseRequested: Boolean = false

    @Volatile
    override var cancelRequested: Boolean = false

    override suspend fun run(
        task: Task,
        startState: WorkflowState,
        initial: WorkflowContext,
    ): AutomationResult<Unit> {
        var ctx = initial
        var current = startState
        setState(task.id, WorkflowState.IDLE, current)
        logger.currentTaskId = task.id
        logger.currentAccountId = task.accountId
        try {
            while (coroutineContext.isActive) {
                if (cancelRequested) {
                    persist(task, ctx, WorkflowState.FAILED, TaskStatus.CANCELLED, "cancelled")
                    eventBus.emit(AutomationEvent.TaskCancelled(task.id))
                    return AutomationResult.cancelled()
                }
                if (pauseRequested) {
                    persist(task, ctx, WorkflowState.PAUSED, TaskStatus.PAUSED, "paused")
                    eventBus.emit(AutomationEvent.TaskPaused(task.id, "pause requested"))
                    return AutomationResult.fail(ResultCode.CANCELLED, "paused")
                }
                logger.currentWorkflowState = current.name
                persistCheckpoint(task, ctx, current)
                current = when (current) {
                    WorkflowState.IDLE -> WorkflowState.VALIDATE_ACCOUNT
                    WorkflowState.VALIDATE_ACCOUNT -> stepValidate(task)
                    WorkflowState.OPEN_TARGET -> stepOpenTarget(task)
                    WorkflowState.LOAD_VIDEOS -> stepLoadVideos(task, ctx)
                    WorkflowState.OPEN_VIDEO -> stepOpenVideo(task, ctx)
                    WorkflowState.LOAD_COMMENTS -> stepLoadComments(task, ctx)
                    WorkflowState.PROCESS_COMMENT -> stepProcessComment(ctx)
                    WorkflowState.OPEN_USER -> stepOpenUser(ctx)
                    WorkflowState.CHECK_RELATIONSHIP -> stepCheckRelationship(task, ctx)
                    WorkflowState.PERFORM_ACTION -> stepPerformAction(task, ctx)
                    WorkflowState.RETURN_TO_COMMENTS -> {
                        adapter.returnToComments()
                        WorkflowState.NEXT_COMMENT
                    }
                    WorkflowState.NEXT_COMMENT -> stepNextComment(ctx)
                    WorkflowState.NEXT_VIDEO -> stepNextVideo(ctx)
                    WorkflowState.COOLDOWN,
                    WorkflowState.PAUSED,
                    WorkflowState.FINISHED,
                    WorkflowState.FAILED,
                    -> {
                        val status = when (current) {
                            WorkflowState.FINISHED -> TaskStatus.FINISHED
                            WorkflowState.COOLDOWN -> TaskStatus.COOLDOWN
                            WorkflowState.PAUSED -> TaskStatus.PAUSED
                            else -> TaskStatus.FAILED
                        }
                        persist(task, ctx, current, status, null)
                        if (current == WorkflowState.FINISHED) {
                            eventBus.emit(AutomationEvent.TaskFinished(task.id))
                        }
                        return when (current) {
                            WorkflowState.FINISHED -> AutomationResult.ok()
                            WorkflowState.COOLDOWN -> AutomationResult.fail(ResultCode.RATE_LIMITED, "cooldown")
                            WorkflowState.PAUSED -> AutomationResult.fail(ResultCode.CANCELLED, "paused")
                            else -> AutomationResult.fail(ResultCode.UNKNOWN, current.name)
                        }
                    }
                }
                if (operationIntervalMs > 0) delay(operationIntervalMs)
            }
            return AutomationResult.cancelled()
        } catch (cancelled: CancellationException) {
            persist(task, ctx, WorkflowState.PAUSED, TaskStatus.CANCELLED, "cancelled")
            throw cancelled
        } catch (error: Exception) {
            logger.e("workflow crashed", error)
            persist(task, ctx, WorkflowState.FAILED, TaskStatus.FAILED, error.message)
            eventBus.emit(AutomationEvent.TaskFailed(task.id, ResultCode.UNKNOWN, error.message ?: "crash"))
            return AutomationResult.fail(ResultCode.UNKNOWN, error.message ?: "crash")
        }
    }

    private suspend fun stepValidate(task: Task): WorkflowState {
        val session = adapter.validateSession()
        return when (session) {
            SessionStatus.VALID -> WorkflowState.OPEN_TARGET
            SessionStatus.LOGIN_EXPIRED, SessionStatus.LOGIN_REQUIRED -> {
                task.accountId?.let { accountManager.markLoginRequired(it, session.name, "session invalid") }
                eventBus.emit(AutomationEvent.TaskPaused(task.id, "login invalid"))
                WorkflowState.PAUSED
            }
            SessionStatus.UNKNOWN -> {
                logger.log(LogLevel.WARN, "session UNKNOWN — skip unsafe continue", action = "validateSession")
                WorkflowState.PAUSED
            }
        }
    }

    private suspend fun stepOpenTarget(task: Task): WorkflowState {
        val uid = task.targetUid ?: return WorkflowState.FAILED
        val target = com.example.bilibiliautomation.domain.model.Target(
            id = 0,
            uid = uid,
            nickname = uid,
            avatarUrl = "",
            enabled = true,
            createdAt = 0,
            updatedAt = 0,
            lastScanAt = null,
        )
                    val result = adapter.openTarget(target)
        return if (result.success) WorkflowState.LOAD_VIDEOS else failOrRestrict(task, WorkflowContext(task), result)
    }

    private suspend fun stepLoadVideos(task: Task, ctx: WorkflowContext): WorkflowState {
        val result = adapter.loadVideos()
        if (!result.success) return failOrRestrict(task, ctx, result)
        ctx.videos = result.data.orEmpty()
        task.targetUid?.let { videoRepository.upsertAll(it, ctx.videos) }
        ctx.videoIndex = ctx.videoIndex.coerceAtMost(ctx.videos.lastIndex.coerceAtLeast(0))
        return if (ctx.videos.isEmpty()) WorkflowState.FINISHED else WorkflowState.OPEN_VIDEO
    }

    private suspend fun stepOpenVideo(task: Task, ctx: WorkflowContext): WorkflowState {
        val video = ctx.videos.getOrNull(ctx.videoIndex) ?: return WorkflowState.FINISHED
                    val result = adapter.openVideo(video.bvid)
        return if (result.success) WorkflowState.LOAD_COMMENTS else failOrRestrict(task, ctx, result)
    }

    private suspend fun stepLoadComments(task: Task, ctx: WorkflowContext): WorkflowState {
        val result = adapter.loadComments()
        if (!result.success) return failOrRestrict(task, ctx, result)
        ctx.comments = result.data.orEmpty().filter { it.isRootComment }
        ctx.videos.getOrNull(ctx.videoIndex)?.let { commentRepository.upsertAll(it.bvid, ctx.comments) }
        ctx.commentIndex = ctx.commentIndex.coerceAtMost(ctx.comments.lastIndex.coerceAtLeast(0))
        return WorkflowState.PROCESS_COMMENT
    }

    private fun stepProcessComment(ctx: WorkflowContext): WorkflowState {
        val comment = ctx.comments.getOrNull(ctx.commentIndex) ?: return WorkflowState.NEXT_VIDEO
        ctx.currentUserUid = comment.userUid
        return WorkflowState.OPEN_USER
    }

    private suspend fun stepOpenUser(ctx: WorkflowContext): WorkflowState {
        val uid = ctx.currentUserUid ?: return WorkflowState.NEXT_COMMENT
        adapter.openUser(uid)
        return WorkflowState.CHECK_RELATIONSHIP
    }

    private suspend fun stepCheckRelationship(task: Task, ctx: WorkflowContext): WorkflowState {
        val uid = ctx.currentUserUid ?: return WorkflowState.NEXT_COMMENT
        val relationship = adapter.detectRelationship()
        userRepository.upsertSeen(uid, uid, "", relationship)
        logger.log(
            LogLevel.INFO,
            "relationship=$relationship uid=$uid",
            action = "detectRelationship",
            resultCode = ResultCode.OK,
        )
        return when (relationship) {
            RelationshipState.UNKNOWN -> {
                logger.log(LogLevel.WARN, "UNKNOWN relationship — SKIP, do not act", action = "checkRelationship")
                actionRepository.record(
                    accountUid = task.accountId?.toString() ?: "",
                    targetUid = task.targetUid,
                    sourceTargetUid = task.targetUid,
                    sourceVideoId = ctx.videos.getOrNull(ctx.videoIndex)?.bvid,
                    sourceCommentId = ctx.comments.getOrNull(ctx.commentIndex)?.commentId,
                    actionType = "SKIP",
                    beforeState = relationship.name,
                    afterState = relationship.name,
                    resultCode = ResultCode.SKIPPED,
                    resultMessage = "UNKNOWN must skip",
                )
                WorkflowState.RETURN_TO_COMMENTS
            }
            RelationshipState.NOT_FOLLOWING -> WorkflowState.PERFORM_ACTION
            RelationshipState.FOLLOWING,
            RelationshipState.MUTUAL,
            RelationshipState.SELF,
            -> WorkflowState.RETURN_TO_COMMENTS
        }
    }

    private suspend fun stepPerformAction(task: Task, ctx: WorkflowContext): WorkflowState {
        val before = adapter.detectRelationship()
        if (before == RelationshipState.UNKNOWN) {
            logger.log(LogLevel.WARN, "pre-check UNKNOWN — skip action", action = "precheck")
            return WorkflowState.RETURN_TO_COMMENTS
        }
        eventBus.emit(AutomationEvent.ActionStarted(task.id, "MOCK_ACTION"))
        val result = adapter.performMockAction()
        eventBus.emit(AutomationEvent.ActionCompleted(task.id, "MOCK_ACTION", result.code, result.message))
                    if (result.requiresCooldown) return failOrRestrict(task, ctx, result)
        val after = adapter.detectRelationship()
        actionRepository.record(
            accountUid = task.accountId?.toString() ?: "",
            targetUid = task.targetUid,
            sourceTargetUid = task.targetUid,
            sourceVideoId = ctx.videos.getOrNull(ctx.videoIndex)?.bvid,
            sourceCommentId = ctx.comments.getOrNull(ctx.commentIndex)?.commentId,
            actionType = "MOCK_ACTION",
            beforeState = before.name,
            afterState = after.name,
            resultCode = result.code,
            resultMessage = result.message,
        )
        return WorkflowState.RETURN_TO_COMMENTS
    }

    private fun stepNextComment(ctx: WorkflowContext): WorkflowState {
        ctx.commentIndex += 1
        return if (ctx.commentIndex < ctx.comments.size) WorkflowState.PROCESS_COMMENT
        else WorkflowState.NEXT_VIDEO
    }

    private suspend fun stepNextVideo(ctx: WorkflowContext): WorkflowState {
        ctx.videos.getOrNull(ctx.videoIndex)?.let { videoRepository.markProcessed(it.bvid, ctx.comments.size, null) }
        ctx.videoIndex += 1
        ctx.commentIndex = 0
        ctx.comments = emptyList()
        return if (ctx.videoIndex < ctx.videos.size) WorkflowState.OPEN_VIDEO else WorkflowState.FINISHED
    }

                    private suspend fun failOrRestrict(
        task: Task,
        ctx: WorkflowContext,
        result: AutomationResult<*>,
    ): WorkflowState {
        if (result.isRateLimited || result.isAccountRestricted) {
            logger.log(
                LogLevel.WARN,
                "restriction ${result.code} — cooldown / review, no account rotation",
                action = "restriction",
                resultCode = result.code,
            )
            task.accountId?.let { accountManager.markCooldown(it, result.code, result.message, cooldownMinutes) }
            persist(task, ctx, WorkflowState.COOLDOWN, TaskStatus.COOLDOWN, result.message)
            eventBus.emit(AutomationEvent.NeedsReview(task.id, task.accountId, result.code, result.message))
            return WorkflowState.COOLDOWN
        }
        persist(task, ctx, WorkflowState.FAILED, TaskStatus.FAILED, result.message)
        eventBus.emit(AutomationEvent.TaskFailed(task.id, result.code, result.message))
        return WorkflowState.FAILED
    }

    private suspend fun persist(
        task: Task,
        ctx: WorkflowContext,
        state: WorkflowState,
        status: TaskStatus,
        error: String?,
    ) {
        setState(task.id, _state.value, state)
        persistCheckpoint(task, ctx, state)
        val now = TimeMs.wallClock()
        taskRepository.updateStatus(
            id = task.id,
            status = status,
            lastError = error,
            startedAt = task.startedAt ?: now,
            pausedAt = if (status == TaskStatus.PAUSED || status == TaskStatus.COOLDOWN) now else task.pausedAt,
            finishedAt = if (status == TaskStatus.FINISHED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED) now else null,
        )
        if (task.accountId != null && state == WorkflowState.FINISHED) {
            accountRepository.markStatus(task.accountId, AccountStatus.ACTIVE)
        }
    }

    private suspend fun persistCheckpoint(task: Task, ctx: WorkflowContext, state: WorkflowState) {
        if (!state.isSafeCheckpoint) return
        taskRepository.saveCheckpoint(
            TaskCheckpoint(
                taskId = task.id,
                workflowState = state,
                targetUid = task.targetUid,
                videoId = ctx.videos.getOrNull(ctx.videoIndex)?.bvid,
                videoIndex = ctx.videoIndex,
                commentId = ctx.comments.getOrNull(ctx.commentIndex)?.commentId,
                commentCursor = null,
                commentIndex = ctx.commentIndex,
                currentUserUid = ctx.currentUserUid,
                currentUrl = ctx.currentUrl,
                updatedAt = TimeMs.wallClock(),
            ),
        )
    }

    private suspend fun setState(taskId: Long, from: WorkflowState, to: WorkflowState) {
        _state.value = to
        if (from != to) {
            eventBus.emit(AutomationEvent.StateChanged(taskId, from, to))
        }
    }
}
