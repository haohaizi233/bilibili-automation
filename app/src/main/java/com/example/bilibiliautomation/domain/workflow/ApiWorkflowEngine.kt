package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.BuildConfig
import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.repository.ActionRepository
import com.example.bilibiliautomation.data.repository.CommentRepository
import com.example.bilibiliautomation.data.repository.ProcessedUserRepository
import com.example.bilibiliautomation.data.repository.TaskRepository
import com.example.bilibiliautomation.data.repository.VideoRepository
import com.example.bilibiliautomation.domain.account.AccountManager
import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.RelationState
import com.example.bilibiliautomation.domain.api.SessionState
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.LoadedComment
import com.example.bilibiliautomation.domain.model.LoadedVideo
import com.example.bilibiliautomation.domain.model.ResultCode
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class ApiWorkflowEngine(
    private val backend: AutomationBackend,
    private val taskRepository: TaskRepository,
    private val accountRepository: AccountRepository,
    private val videoRepository: VideoRepository,
    private val commentRepository: CommentRepository,
    private val processedUsers: ProcessedUserRepository,
    private val actionRepository: ActionRepository,
    private val accountManager: AccountManager,
    private val accountPool: AccountPool,
    private val rateLimiter: RateLimiter,
    private val eventBus: AutomationEventBus,
    private val logger: AutomationLogger = AutomationLogger,
    val dryRun: Boolean = BuildConfig.DEBUG,
    private val maxVideoPages: Int = 20,
    private val commentPageSize: Int = 20,
    private val maxCommentPages: Int = 10,
    private val maxRetries: Int = 3,
    private val rateLimiterCooldownMs: Long = 2_000L,
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
        if (ctx.currentAccountId == null) ctx.currentAccountId = task.accountId
        var current = startState
        logger.currentTaskId = task.id
        logger.currentAccountId = ctx.currentAccountId
        logger.i("ApiWorkflowEngine backend=${backend.name} dryRun=$dryRun task=${task.id}")
        try {
            while (coroutineContext.isActive) {
                coroutineContext.ensureActive()
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
                // Emit a StateChanged whenever the engine advances to a new
                // state so the UI's workflowState (and thus the "当前操作" line)
                // tracks the live step instead of freezing on VALIDATE_ACCOUNT.
                // persist() only emits this on terminal/pause transitions, so
                // without this the action line never updates mid-run.
                if (_state.value != current) {
                    val from = _state.value
                    _state.value = current
                    eventBus.emit(AutomationEvent.StateChanged(task.id, from, current))
                }
                persistCheckpoint(task, ctx, current)
                current = when (current) {
                    WorkflowState.IDLE -> WorkflowState.VALIDATE_ACCOUNT
                    WorkflowState.VALIDATE_ACCOUNT -> stepValidate(task, ctx)
                    WorkflowState.OPEN_TARGET -> WorkflowState.LOAD_VIDEOS
                    WorkflowState.LOAD_VIDEOS -> stepLoadVideos(task, ctx)
                    WorkflowState.OPEN_VIDEO -> WorkflowState.LOAD_COMMENTS
                    WorkflowState.LOAD_COMMENTS -> stepLoadComments(task, ctx)
                    WorkflowState.PROCESS_COMMENT -> stepProcessComment(task, ctx)
                    WorkflowState.OPEN_USER -> WorkflowState.CHECK_RELATIONSHIP
                    WorkflowState.CHECK_RELATIONSHIP -> stepCheckRelationship(task, ctx)
                    WorkflowState.PERFORM_ACTION -> stepPerformAction(task, ctx)
                    WorkflowState.RETURN_TO_COMMENTS -> WorkflowState.NEXT_COMMENT
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
            }
            return AutomationResult.cancelled()
        } catch (cancelled: CancellationException) {
            // The coroutine is being cancelled (user tapped Stop / scope shut
            // down). Persist the terminal state inside NonCancellable so the
            // suspend DB writes + event emits actually complete; without this,
            // the already-cancelled context rethrows CancellationException out
            // of the first suspend call inside persist(), and any *other*
            // exception thrown there would replace the CancellationException
            // and escape the workflow as an uncaught error -> process crash.
            withContext(kotlinx.coroutines.NonCancellable) {
                persist(task, ctx, WorkflowState.PAUSED, TaskStatus.CANCELLED, "cancelled")
            }
            throw cancelled
        } catch (error: Exception) {
            logger.e("api workflow crashed", error)
            persist(task, ctx, WorkflowState.FAILED, TaskStatus.FAILED, error.message)
            eventBus.emit(AutomationEvent.TaskFailed(task.id, ResultCode.UNKNOWN, error.message ?: "crash"))
            return AutomationResult.fail(ResultCode.UNKNOWN, error.message ?: "crash")
        }
    }

    private suspend fun stepValidate(task: Task, ctx: WorkflowContext): WorkflowState {
        val accountId = ctx.currentAccountId ?: accountPool.pick()?.also { ctx.currentAccountId = it.id }?.id
        if (accountId == null) {
            logger.w("NeedLogin — no valid account in pool")
            return WorkflowState.PAUSED
        }
        return when (val result = backend.validateSession(accountId)) {
            is ApiResult.Ok -> when (result.value) {
                is SessionState.Valid -> WorkflowState.OPEN_TARGET
                SessionState.Expired, SessionState.NotLoggedIn -> {
                    accountManager.markLoginRequired(accountId, result.value.toString(), "session invalid")
                    eventBus.emit(AutomationEvent.TaskPaused(task.id, "login invalid"))
                    WorkflowState.PAUSED
                }
                SessionState.RiskControlled -> {
                    accountManager.markCooldown(accountId, ResultCode.ACCOUNT_RESTRICTED, "risk", 30)
                    eventBus.emit(AutomationEvent.TaskPaused(task.id, "risk control"))
                    WorkflowState.PAUSED
                }
                is SessionState.NetworkError, is SessionState.UnknownError -> WorkflowState.PAUSED
            }
            is ApiResult.Err -> mapError(task, ctx, result.error)
        }
    }

    private suspend fun stepLoadVideos(task: Task, ctx: WorkflowContext): WorkflowState {
        val uid = task.targetUid?.toLongOrNull() ?: return WorkflowState.FAILED
        val accountId = ctx.currentAccountId ?: return WorkflowState.PAUSED
        val pages = mutableListOf<LoadedVideo>()
        var page = 1
        while (page <= maxVideoPages) {
            coroutineContext.ensureActive()
            when (val result = backend.getUserVideos(accountId, uid, page, 30)) {
                is ApiResult.Err -> return mapError(task, ctx, result.error)
                is ApiResult.Ok -> {
                    pages += result.value.items.map {
                        LoadedVideo(it.bvid, it.title, it.aid, it.createdAt, it.durationSeconds, it.pic)
                    }
                    eventBus.emit(
                        AutomationEvent.VideoProgress(
                            taskId = task.id,
                            current = pages.size,
                            total = result.value.total,
                            title = "加载视频列表…",
                            bvid = "",
                        ),
                    )
                    if (!result.value.hasMore) break
                    page += 1
                }
            }
        }
        ctx.videos = pages
        videoRepository.upsertAll(uid.toString(), pages)
        ctx.videoIndex = ctx.videoIndex.coerceAtMost(ctx.videos.lastIndex.coerceAtLeast(0))
        return if (ctx.videos.isEmpty()) WorkflowState.FINISHED else WorkflowState.OPEN_VIDEO
    }

    private suspend fun stepLoadComments(task: Task, ctx: WorkflowContext): WorkflowState {
        val video = ctx.videos.getOrNull(ctx.videoIndex) ?: return WorkflowState.FINISHED
        val oid = video.aid ?: return WorkflowState.NEXT_VIDEO
        val accountId = ctx.currentAccountId ?: return WorkflowState.PAUSED
        eventBus.emit(
            AutomationEvent.VideoProgress(
                taskId = task.id,
                current = ctx.videoIndex + 1,
                total = ctx.videos.size,
                title = video.title,
                bvid = video.bvid,
                coverUrl = video.pic,
            ),
        )
        val collected = mutableListOf<LoadedComment>()
        var page = 1
        while (page <= maxCommentPages) {
            coroutineContext.ensureActive()
            when (val result = backend.getRootComments(accountId, oid, page, commentPageSize)) {
                is ApiResult.Err -> return mapError(task, ctx, result.error)
                is ApiResult.Ok -> {
                    for (root in result.value.comments) {
                        collected += LoadedComment(
                            commentId = root.rpid.toString(),
                            userUid = root.authorUid.toString(),
                            isRootComment = true,
                            preview = root.message,
                            authorName = root.authorName,
                            createdAt = root.createdAt,
                            oid = root.oid,
                        )
                        collected += fetchReplies(task, accountId, oid, root.rpid)
                    }
                    val next = result.value.nextPage
                    if (next == null) break
                    page = next
                }
            }
        }
        ctx.comments = collected
        commentRepository.upsertAll(video.bvid, collected)
        ctx.commentIndex = ctx.commentIndex.coerceAtMost(ctx.comments.lastIndex.coerceAtLeast(0))
        eventBus.emit(
            AutomationEvent.CommentProgress(
                taskId = task.id,
                current = 0,
                total = ctx.comments.size,
            ),
        )
        return WorkflowState.PROCESS_COMMENT
    }

    private suspend fun fetchReplies(
        task: Task,
        accountId: Long,
        oid: Long,
        rootId: Long,
    ): List<LoadedComment> {
        val out = mutableListOf<LoadedComment>()
        var page = 1
        while (page <= maxCommentPages) {
            coroutineContext.ensureActive()
            when (val result = backend.getReplies(accountId, oid, rootId, page, commentPageSize)) {
                is ApiResult.Err -> {
                    logger.w("fetchReplies failed root=$rootId page=$page ${result.error.message}")
                    return out
                }
                is ApiResult.Ok -> {
                    for (reply in result.value.comments) {
                        out += LoadedComment(
                            commentId = reply.rpid.toString(),
                            userUid = reply.authorUid.toString(),
                            isRootComment = false,
                            preview = reply.message,
                            authorName = reply.authorName,
                            createdAt = reply.createdAt,
                            oid = reply.oid,
                        )
                    }
                    val next = result.value.nextPage
                    if (next == null || result.value.comments.isEmpty()) break
                    page = next
                }
            }
        }
        return out
    }

    private suspend fun stepProcessComment(task: Task, ctx: WorkflowContext): WorkflowState {
        val comment = ctx.comments.getOrNull(ctx.commentIndex) ?: return WorkflowState.NEXT_VIDEO
        eventBus.emit(
            AutomationEvent.CommentProgress(
                taskId = task.id,
                current = ctx.commentIndex + 1,
                total = ctx.comments.size,
            ),
        )
        val target = task.targetUid.orEmpty()
        if (processedUsers.hasProcessed(target, comment.userUid)) {
            logger.i("skip duplicate uid=${comment.userUid}")
            eventBus.emit(AutomationEvent.ActionCompleted(task.id, "SKIP", ResultCode.SKIPPED, "already processed"))
            return WorkflowState.NEXT_COMMENT
        }
        ctx.currentUserUid = comment.userUid
        return WorkflowState.CHECK_RELATIONSHIP
    }

    private suspend fun stepCheckRelationship(task: Task, ctx: WorkflowContext): WorkflowState {
        val uid = ctx.currentUserUid?.toLongOrNull() ?: return WorkflowState.NEXT_COMMENT
        val accountId = ctx.currentAccountId ?: return WorkflowState.PAUSED
        return when (val result = backend.getRelation(accountId, uid)) {
            is ApiResult.Err -> mapError(task, ctx, result.error)
            is ApiResult.Ok -> {
                val state = result.value.state
                processedUsers.markProcessed(
                    targetUpUid = task.targetUid.orEmpty(),
                    commentAuthorUid = uid.toString(),
                    nickname = ctx.comments.getOrNull(ctx.commentIndex)?.authorName.orEmpty(),
                    relationshipState = state.toRelationshipState(),
                )
                if (state.isFollowing) {
                    logger.i("skip already following uid=$uid state=$state")
                    eventBus.emit(AutomationEvent.ActionCompleted(task.id, "SKIP", ResultCode.SKIPPED, "already following"))
                    WorkflowState.NEXT_COMMENT
                } else when (state) {
                    RelationState.Unknown -> {
                        logger.log(LogLevel.WARN, "UNKNOWN relation — SKIP uid=$uid", action = "checkRelationship")
                        eventBus.emit(AutomationEvent.ActionCompleted(task.id, "SKIP", ResultCode.SKIPPED, "unknown relation"))
                        WorkflowState.NEXT_COMMENT
                    }
                    RelationState.None, RelationState.Follower -> WorkflowState.PERFORM_ACTION
                    else -> WorkflowState.NEXT_COMMENT
                }
            }
        }
    }

    private suspend fun stepPerformAction(task: Task, ctx: WorkflowContext): WorkflowState {
        val uid = ctx.currentUserUid?.toLongOrNull() ?: return WorkflowState.NEXT_COMMENT
        val accountId = ctx.currentAccountId ?: return WorkflowState.PAUSED
        if (!rateLimiter.underDailyLimit()) {
            logger.w("daily limit reached accountId=$accountId")
            return WorkflowState.COOLDOWN
        }
        rateLimiter.awaitSlot()
        eventBus.emit(AutomationEvent.ActionStarted(task.id, "FOLLOW"))
        val result = performFollowWithRetry(accountId, uid)
        val code = result.fold(
            onOk = { ResultCode.OK },
            onErr = { error ->
                when (error) {
                    is ApiError.RateLimited -> ResultCode.RATE_LIMITED
                    is ApiError.AccountRestricted -> ResultCode.ACCOUNT_RESTRICTED
                    is ApiError.SessionExpired, is ApiError.SessionInvalid -> ResultCode.LOGIN_EXPIRED
                    else -> ResultCode.UNKNOWN
                }
            },
        )
        eventBus.emit(AutomationEvent.ActionCompleted(task.id, "FOLLOW", code, result.errorOrNull()?.message.orEmpty()))
        when (code) {
            ResultCode.OK -> ctx.followed += 1
            ResultCode.SKIPPED -> ctx.skipped += 1
            else -> ctx.failed += 1
        }
        actionRepository.record(
            accountUid = accountId.toString(),
            targetUid = uid.toString(),
            sourceTargetUid = task.targetUid,
            sourceVideoId = ctx.videos.getOrNull(ctx.videoIndex)?.bvid,
            sourceCommentId = ctx.comments.getOrNull(ctx.commentIndex)?.commentId,
            actionType = if (dryRun) "DRY_RUN_FOLLOW" else "FOLLOW",
            beforeState = RelationState.None.toString(),
            afterState = code,
            resultCode = code,
            resultMessage = result.errorOrNull()?.message,
        )
        rateLimiter.recordAction()
        return when (val error = result.errorOrNull()) {
            null -> WorkflowState.NEXT_COMMENT
            else -> mapError(task, ctx, error)
        }
    }

    private suspend fun performFollowWithRetry(accountId: Long, uid: Long): ApiResult<Unit> {
        var attempt = 0
        var last: ApiResult<Unit> = ApiResult.ok(Unit)
        while (attempt <= maxRetries) {
            coroutineContext.ensureActive()
            last = backend.followUser(accountId, uid, dryRun)
            if (last is ApiResult.Ok) return last
            val error = (last as ApiResult.Err).error
            if (error is ApiError.RateLimited || error is ApiError.SessionExpired ||
                error is ApiError.SessionInvalid || error is ApiError.AccountRestricted ||
                error is ApiError.RiskControl || error is ApiError.VerificationRequired
            ) {
                return last
            }
            attempt += 1
            if (attempt <= maxRetries) {
                logger.w("follow retry attempt=$attempt uid=$uid ${error.message}")
                delay(rateLimiterCooldownMs)
            }
        }
        return last
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

    private suspend fun mapError(task: Task, ctx: WorkflowContext, error: ApiError): WorkflowState {
        return when (error) {
            is ApiError.RateLimited -> {
                rateLimiter.triggerCooldown()
                task.accountId?.let { accountManager.markCooldown(it, ResultCode.RATE_LIMITED, error.message, 30) }
                eventBus.emit(AutomationEvent.NeedsReview(task.id, task.accountId, ResultCode.RATE_LIMITED, error.message))
                WorkflowState.COOLDOWN
            }
            is ApiError.SessionExpired, is ApiError.SessionInvalid -> {
                val old = ctx.currentAccountId
                handleSessionOrRisk(task, ctx, old, error)
            }
            is ApiError.VerificationRequired, is ApiError.RiskControl -> {
                val old = ctx.currentAccountId
                handleSessionOrRisk(task, ctx, old, error)
            }
            is ApiError.AccountRestricted -> {
                task.accountId?.let {
                    accountManager.markCooldown(it, ResultCode.ACCOUNT_RESTRICTED, error.message, 30)
                }
                WorkflowState.COOLDOWN
            }
            is ApiError.Cancelled -> WorkflowState.PAUSED
            else -> {
                logger.e("api workflow error ${error.message}")
                WorkflowState.FAILED
            }
        }
    }

    /**
     * An operation endpoint returned a session/risk error (often Bilibili
     * code -101 "账号未登录"). Bilibili风控 frequently masquerades as -101
     * even when the account is still logged in, so we must NOT trust a single
     * -101 from an action call. Re-validate via nav (which carries the
     * hadCookies heuristic + real isLogin flag): only when nav confirms the
     * session is actually dead do we mark LOGIN_EXPIRED; otherwise treat it
     * as风控 (cooldown) and rotate to another account.
     */
    private suspend fun handleSessionOrRisk(
        task: Task,
        ctx: WorkflowContext,
        oldAccountId: Long?,
        error: ApiError,
    ): WorkflowState {
        if (oldAccountId == null) {
            eventBus.emit(AutomationEvent.NeedsReview(task.id, null, "NO_ACCOUNT", error.message))
            return WorkflowState.PAUSED
        }
        val verdict = backend.validateSession(oldAccountId)
        val genuinelyExpired = when (verdict) {
            is ApiResult.Ok -> verdict.value is SessionState.Expired || verdict.value is SessionState.NotLoggedIn
            is ApiResult.Err -> false
        }
        return if (genuinelyExpired) {
            logger.i("account $oldAccountId genuinely expired (nav confirmed) → login required")
            accountManager.markLoginRequired(oldAccountId, ResultCode.LOGIN_EXPIRED, error.message)
            if (rotateToNextAccount(task, ctx, oldAccountId)) WorkflowState.VALIDATE_ACCOUNT
            else {
                eventBus.emit(AutomationEvent.NeedsReview(task.id, oldAccountId, ResultCode.LOGIN_EXPIRED, error.message))
                WorkflowState.PAUSED
            }
        } else {
            logger.i("account ${oldAccountId} risk/unconfirmed expiry (nav still valid or risk) → cooldown + rotate")
            accountManager.markCooldown(oldAccountId, ResultCode.ACCOUNT_RESTRICTED, error.message, 30)
            if (rotateToNextAccount(task, ctx, oldAccountId)) WorkflowState.VALIDATE_ACCOUNT
            else {
                eventBus.emit(AutomationEvent.NeedsReview(task.id, oldAccountId, "RISK", error.message))
                WorkflowState.PAUSED
            }
        }
    }

    /**
     * Move the task onto another eligible account from the pool. Returns true
     * when a replacement was activated (ctx.currentAccountId + the task row are
     * updated and AccountSwitched emitted); false when the pool is empty.
     */
    private suspend fun rotateToNextAccount(task: Task, ctx: WorkflowContext, oldAccountId: Long?): Boolean {
        val next = accountPool.rotate(oldAccountId) ?: return false
        ctx.currentAccountId = next.id
        logger.currentAccountId = next.id
        taskRepository.updateAccount(task.id, next.id)
        logger.i("account pool rotated task=${task.id} from=$oldAccountId to=${next.id}")
        eventBus.emit(AutomationEvent.AccountSwitched(task.id, oldAccountId, next.id))
        return true
    }

    private suspend fun persist(
        task: Task,
        ctx: WorkflowContext,
        state: WorkflowState,
        status: TaskStatus,
        error: String?,
    ) {
        val from = _state.value
        _state.value = state
        if (from != state) eventBus.emit(AutomationEvent.StateChanged(task.id, from, state))
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
        val video = ctx.videos.getOrNull(ctx.videoIndex)
        taskRepository.saveCheckpoint(
            TaskCheckpoint(
                taskId = task.id,
                workflowState = state,
                targetUid = task.targetUid,
                videoId = video?.bvid,
                videoIndex = ctx.videoIndex,
                commentId = ctx.comments.getOrNull(ctx.commentIndex)?.commentId,
                commentCursor = null,
                commentIndex = ctx.commentIndex,
                currentUserUid = ctx.currentUserUid,
                currentUrl = ctx.currentUrl,
                followed = ctx.followed,
                skipped = ctx.skipped,
                failed = ctx.failed,
                videoTotal = ctx.videos.size,
                commentTotal = ctx.comments.size,
                currentVideoTitle = video?.title,
                currentVideoCover = video?.pic,
                updatedAt = TimeMs.wallClock(),
            ),
        )
    }
}
