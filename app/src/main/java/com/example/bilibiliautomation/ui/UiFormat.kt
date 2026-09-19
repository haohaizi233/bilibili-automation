package com.example.bilibiliautomation.ui

import com.example.bilibiliautomation.data.api.dto.SpaceUser
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.AppSettings
import com.example.bilibiliautomation.domain.model.Target
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.logging.LogRecord
import com.example.bilibiliautomation.ui.compose.screens.AccountItem
import com.example.bilibiliautomation.ui.compose.screens.AddTargetResult
import com.example.bilibiliautomation.ui.compose.components.TagStyle
import com.example.bilibiliautomation.ui.compose.screens.HomeTaskStatus
import com.example.bilibiliautomation.ui.compose.screens.LogEntry
import com.example.bilibiliautomation.ui.compose.screens.LogLevel
import com.example.bilibiliautomation.ui.compose.screens.TargetItem
import com.example.bilibiliautomation.ui.compose.screens.TaskRunningUiState
import com.example.bilibiliautomation.ui.compose.screens.TaskRunningPhase
import com.example.bilibiliautomation.ui.compose.screens.TaskSettingsUiState
import com.example.bilibiliautomation.ui.task.TaskUiState
import com.example.bilibiliautomation.util.MediaUrls
import com.example.bilibiliautomation.util.TimeMs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UiFormat {
    fun fans(count: Long): String {
        if (count >= 10_000) return String.format("%.1f万", count / 10_000.0)
        return count.toString()
    }

    fun fansOrDash(count: Long): String = if (count < 0L) "—" else fans(count)

    fun elapsed(startedAt: Long, now: Long = System.currentTimeMillis()): String {
        if (startedAt <= 0L) return "00:00:00"
        val seconds = ((now - startedAt).coerceAtLeast(0L) / 1000L)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    fun accountStatus(status: AccountStatus): com.example.bilibiliautomation.ui.compose.screens.AccountStatus {
        return when (status) {
            AccountStatus.ACTIVE -> com.example.bilibiliautomation.ui.compose.screens.AccountStatus.OK
            AccountStatus.COOLDOWN, AccountStatus.NEEDS_REVIEW ->
                com.example.bilibiliautomation.ui.compose.screens.AccountStatus.COOLING
            AccountStatus.LOGIN_EXPIRED, AccountStatus.LOGIN_REQUIRED ->
                com.example.bilibiliautomation.ui.compose.screens.AccountStatus.FAILED
            AccountStatus.DISABLED -> com.example.bilibiliautomation.ui.compose.screens.AccountStatus.IDLE
        }
    }

    fun accountLabel(account: Account): String {
        val name = account.nickname.ifBlank { account.uid }
        return if (name.isBlank() || name.startsWith("pending-")) "未命名账号" else name
    }

    fun homeStatus(status: TaskStatus): HomeTaskStatus = when (status) {
        TaskStatus.RUNNING -> HomeTaskStatus.RUNNING
        TaskStatus.PAUSED, TaskStatus.COOLDOWN, TaskStatus.INTERRUPTED -> HomeTaskStatus.PAUSED
        else -> HomeTaskStatus.IDLE
    }

    fun actionLine(stateName: String, message: String): String {
        val mapped = when (stateName) {
            "VALIDATE_ACCOUNT" -> "正在校验登录态"
            "OPEN_TARGET", "LOAD_VIDEOS" -> "正在获取目标 UP 视频"
            "OPEN_VIDEO", "LOAD_COMMENTS" -> "正在读取一级评论"
            "PROCESS_COMMENT", "CHECK_RELATIONSHIP" -> "正在查询用户关系"
            "PERFORM_ACTION" -> "正在执行关系操作（Dry Run 默认开启）"
            "NEXT_COMMENT", "NEXT_VIDEO" -> "正在进入下一项"
            "COOLDOWN" -> "账号冷却中"
            "FINISHED" -> "任务已完成"
            "FAILED" -> "任务失败"
            "PAUSED" -> "任务已暂停"
            else -> message.ifBlank { stateName }
        }
        return mapped
    }

    fun lastUsed(epochMs: Long?): String? {
        if (epochMs == null || epochMs <= 0L) return null
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(epochMs))
    }

    fun cooldown(until: Long?, now: Long = TimeMs.wallClock()): String? {
        if (until == null || until <= now) return null
        val minutes = ((until - now) / 60_000L).coerceAtLeast(1L)
        return "剩余 $minutes 分钟"
    }

    fun parseIntervalMs(raw: String): Long {
        val nums = raw.split('-', '–', '—', '~').mapNotNull { it.trim().toLongOrNull() }
        if (nums.isEmpty()) return 5_000L
        return (nums.average() * 1000.0).toLong().coerceAtLeast(200L)
    }

    fun taskSettings(settings: AppSettings): TaskSettingsUiState {
        val seconds = (settings.operationIntervalMs / 1000L).coerceAtLeast(1L)
        return TaskSettingsUiState(
            interval = seconds.toString(),
            retries = settings.retryCount.toString(),
            cooldown = settings.cooldownMinutes.toString(),
            dailyLimit = settings.dailyLimit.toString(),
            notifyOnComplete = settings.notificationEnabled,
            verboseLog = settings.debugEnabled,
        )
    }

    fun toAccountItem(account: Account): AccountItem {
        val last = lastUsed(account.lastLoginAt)
        val cool = cooldown(account.cooldownUntil)
        return AccountItem(
            id = account.id,
            name = account.uid,
            nickname = accountLabel(account),
            fans = fansOrDash(account.fansCount),
            following = fansOrDash(account.followingCount),
            status = accountStatus(account.status),
            lastUsed = last,
            cooldown = cool,
            avatarUrl = account.avatarUrl,
            compact = last == null && cool == null,
        )
    }

    fun toTargetItem(target: Target): TargetItem = TargetItem(
        name = target.nickname.ifBlank { target.uid },
        uid = target.uid,
        fans = fansOrDash(target.fansCount),
        hot = target.enabled,
        avatarUrl = target.avatarUrl,
    )

    fun toAddTargetResult(user: SpaceUser): AddTargetResult = AddTargetResult(
        name = user.name.ifBlank { user.mid.toString() },
        uid = user.mid.toString(),
        fans = fansOrDash(user.fans),
        avatarUrl = MediaUrls.httpsFace(user.face),
    )

    fun toTaskRunning(state: TaskUiState): TaskRunningUiState {
        val target = state.target
        val account = state.account
        val phase = when (state.status) {
            TaskStatus.RUNNING -> TaskRunningPhase.RUNNING
            TaskStatus.PAUSED, TaskStatus.INTERRUPTED, TaskStatus.COOLDOWN -> TaskRunningPhase.PAUSED
            TaskStatus.FINISHED -> TaskRunningPhase.FINISHED
            TaskStatus.FAILED -> TaskRunningPhase.FAILED
            TaskStatus.CANCELLED -> TaskRunningPhase.CANCELLED
            else -> TaskRunningPhase.IDLE
        }
        // Don't coerce totals to 1 — that faked "0 / 1" progress in the IDLE
        // state. The screen already guards divide-by-zero, so pass the real
        // totals (0 until the workflow reports them).
        return TaskRunningUiState(
            phase = phase,
            upName = target?.nickname?.ifBlank { target.uid } ?: "未选择UP",
            upUid = target?.uid ?: "-",
            upAvatarUrl = target?.avatarUrl.orEmpty(),
            progressDone = state.videoProgress.current,
            progressTotal = state.videoProgress.total,
            videoTitle = state.currentVideoTitle.ifBlank { "等待视频" },
            videoBv = state.currentBvid.ifBlank { "-" },
            videoCoverUrl = state.currentVideoCover,
            videoIndex = state.videoProgress.current,
            videoTotal = state.videoProgress.total,
        accountName = account?.let { accountLabel(it) } ?: "未选择账号",
        accountStatus = account?.let { accountStatus(it.status).label } ?: "未登录",
        accountStatusStyle = account?.let { accountStatus(it.status).style } ?: TagStyle.OK,
        accountAvatarUrl = account?.avatarUrl.orEmpty(),
        accountId = account?.id,
            actionLine = actionLine(state.workflowState.name, state.message),
            actionLabel = state.currentAction ?: state.workflowState.name,
            actionDone = state.commentProgress.current,
            actionTotal = state.commentProgress.total,
            followed = state.followed,
            skipped = state.skipped,
            failed = state.failed,
            paused = state.status == TaskStatus.PAUSED ||
                state.status == TaskStatus.INTERRUPTED ||
                state.status == TaskStatus.COOLDOWN,
        )
    }

    fun toLogEntry(record: LogRecord): LogEntry {
        val time = record.timestampIso.substringAfter('T').take(8).ifBlank {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(record.timestamp))
        }
        val level = when (record.level) {
            com.example.bilibiliautomation.logging.LogLevel.SUCCESS -> LogLevel.SUCCESS
            com.example.bilibiliautomation.logging.LogLevel.WARN -> LogLevel.WARN
            com.example.bilibiliautomation.logging.LogLevel.ERROR -> LogLevel.ERROR
            else -> LogLevel.INFO
        }
        val message = record.message.ifBlank { record.action.orEmpty() }
            .ifBlank { record.workflowState.orEmpty() }
            .ifBlank { "—" }
        val sub = listOfNotNull(record.workflowState, record.resultCode).joinToString(" · ").ifBlank { null }
        return LogEntry(
            time = time,
            level = level,
            message = message,
            sub = sub,
            errorHighlight = record.level == com.example.bilibiliautomation.logging.LogLevel.ERROR,
        )
    }
}
