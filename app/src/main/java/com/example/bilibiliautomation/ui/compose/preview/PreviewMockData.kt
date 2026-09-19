package com.example.bilibiliautomation.ui.compose.preview

import com.example.bilibiliautomation.ui.compose.screens.TaskRunningPhase
import com.example.bilibiliautomation.ui.compose.screens.TaskRunningUiState

/**
 * Unified mock data for Compose @Preview.
 *
 * All values mirror the reference PNG / HTML mockups in /UI and /HTML so that
 * Previews render the same content as the design truth source.
 *
 * Previews must NEVER touch Room, Repository, WebView, ForegroundService,
 * TaskScheduler or the network — they only consume these in-memory constants.
 * UiShowcaseActivity runtime pages collect production ViewModels instead.
 */
object PreviewMockData {

    // ---- Page 01 · Home / 控制面板 ------------------------------------------
    const val HOME_ELAPSED = "00:12:36"
    const val HOME_ACCOUNT_LABEL = "当前账号"
    const val HOME_ACCOUNT_SUB = "账号1（LexBurner）"
    const val HOME_UP_LABEL = "当前UP"
    const val HOME_UP_SUB = "罗翔说刑法 (UID: 517327498)"
    const val HOME_PROGRESS_DONE = 12
    const val HOME_PROGRESS_TOTAL = 251
    const val HOME_PROGRESS_PERCENT = 4
    const val HOME_FOLLOWED = 38
    const val HOME_SKIPPED = 102
    const val HOME_FAILED = 3

    // ---- Page 02 · 账号管理 --------------------------------------------------
    val accountRows = listOf(
        AccountRow("账号1", "LexBurner", "927.4万", AccountStatus.OK, lastUsed = "2025-09-19 14:20"),
        AccountRow("账号2", "动漫小助手", "320.1万", AccountStatus.COOLING, cooldown = "剩余 18 分钟"),
        AccountRow("账号3", "游戏玩家", "88.7万", AccountStatus.FAILED, lastUsed = "2025-09-18 22:11"),
        AccountRow("账号4", "二次元少女", "115.6万", AccountStatus.OK),
        AccountRow("账号5", "追番达人", "64.2万", AccountStatus.IDLE),
    )

    // ---- Page 03 · 扫码登录 --------------------------------------------------
    // (QrLoginScreen has no externalized state — UI is self-contained.)

    // ---- Page 04 · 登录成功 --------------------------------------------------
    const val LOGIN_SUCCESS_USERNAME = "动漫小助手"
    const val LOGIN_SUCCESS_UID = "67283910"
    const val LOGIN_SUCCESS_FANS = "320.1 万"

    // ---- Page 05 · 目标UP管理 ------------------------------------------------
    val targetRows = listOf(
        TargetRow("LexBurner", "777536", "927.4万", hot = true),
        TargetRow("罗翔说刑法", "517327498", "3066.5万", hot = true),
        TargetRow("哔哩哔哩动画", "946974", "5842.1万", hot = false),
    )

    // ---- Page 06 · 添加目标UP ------------------------------------------------
    val searchResults = listOf(
        SearchResultRow("罗翔说刑法", "517327498", "3066.5万"),
        SearchResultRow("罗翔老师", "12345678", "102.3万"),
        SearchResultRow("法学罗翔", "87654321", "56.7万"),
        SearchResultRow("罗翔的日常", "11223344", "12.4万", verified = true),
    )

    // ---- Page 07 · 任务设置 --------------------------------------------------
    // (TaskSettingsScreen has no externalized state — UI is self-contained.)

    // ---- Page 08 · 任务运行中 ------------------------------------------------
    val taskRunning = TaskRunningUiState(
        phase = TaskRunningPhase.RUNNING,
        upName = "罗翔说刑法",
        upUid = "517327498",
        progressDone = 12,
        progressTotal = 251,
        videoTitle = "【罗翔】法律人的思维方式到底是什么?",
        videoBv = "BV1xx411c7mD",
        videoIndex = 3,
        videoTotal = 20,
        accountName = "LexBurner",
        accountStatus = "正常",
        actionLine = "正在遍历评论区...",
        actionLabel = "处理评论",
        actionDone = 38,
        actionTotal = 482,
        followed = 38,
        skipped = 102,
        failed = 3,
    )

    // ---- Page 09 · 任务日志 --------------------------------------------------
    val logRows = listOf(
        LogRow("14:32:15", LogKind.INFO, "开始遍历视频", "BV1xx411c7mD"),
        LogRow("14:32:18", LogKind.SUCCESS, "关注用户", "UID: 67283910"),
        LogRow("14:32:20", LogKind.INFO, "已跳过（已关注）", "UID: 12345678"),
        LogRow("14:32:25", LogKind.WARN, "操作过于频繁", "等待 30 秒后继续..."),
        LogRow("14:32:55", LogKind.INFO, "切换账号：账号2", null),
        LogRow("14:33:10", LogKind.ERROR, "账号登录失效", "请检查网络或Cookie", errorHighlight = true),
        LogRow("14:33:12", LogKind.INFO, "开始登录新账号", null),
        LogRow("14:34:01", LogKind.SUCCESS, "登录成功", "UID: 517327498"),
        LogRow("14:34:05", LogKind.INFO, "继续执行任务...", null),
    )

    // ---- Page 10 · 设置 ------------------------------------------------------
    // (SettingsScreen has no externalized state — UI is self-contained.)

    // ---- Lightweight value types so PreviewMockData has no dependency on the
    //      screen-level data classes (which currently bake their own mock data
    //      in and would create a circular reference if reused here). ----------
    enum class AccountStatus { OK, COOLING, FAILED, IDLE }
    data class AccountRow(
        val name: String,
        val nickname: String,
        val fans: String,
        val status: AccountStatus,
        val lastUsed: String? = null,
        val cooldown: String? = null,
    )
    data class TargetRow(val name: String, val uid: String, val fans: String, val hot: Boolean)
    data class SearchResultRow(val name: String, val uid: String, val fans: String, val verified: Boolean = false)
    enum class LogKind { INFO, SUCCESS, WARN, ERROR }
    data class LogRow(
        val time: String,
        val level: LogKind,
        val message: String,
        val sub: String? = null,
        val errorHighlight: Boolean = false,
    )
}
