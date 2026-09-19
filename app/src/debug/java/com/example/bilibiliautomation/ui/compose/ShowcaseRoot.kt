package com.example.bilibiliautomation.ui.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelProvider
import com.example.bilibiliautomation.AutomationApplication
import com.example.bilibiliautomation.BuildConfig
import com.example.bilibiliautomation.R
import com.example.bilibiliautomation.data.api.qr.QrLoginPhase
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.domain.workflow.AccountPool
import com.example.bilibiliautomation.ui.AppViewModelFactory
import com.example.bilibiliautomation.ui.UiFormat
import com.example.bilibiliautomation.ui.account.AccountViewModel
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.screens.AccountScreen
import com.example.bilibiliautomation.ui.compose.screens.AddTargetScreen
import com.example.bilibiliautomation.ui.compose.screens.AddTargetUiState
import com.example.bilibiliautomation.ui.compose.screens.HomeScreen
import com.example.bilibiliautomation.ui.compose.screens.HomeTaskStatus
import com.example.bilibiliautomation.ui.compose.screens.LogScreen
import com.example.bilibiliautomation.ui.compose.screens.LoginSuccessScreen
import com.example.bilibiliautomation.ui.compose.screens.QrLoginScreen
import com.example.bilibiliautomation.ui.compose.screens.SettingsScreen
import com.example.bilibiliautomation.ui.compose.screens.TargetScreen
import com.example.bilibiliautomation.ui.compose.screens.TaskRunningScreen
import com.example.bilibiliautomation.ui.compose.screens.TaskSettingsScreen
import com.example.bilibiliautomation.ui.compose.screens.TaskSettingsUiState
import com.example.bilibiliautomation.ui.home.HomeViewModel
import com.example.bilibiliautomation.ui.logs.LogViewModel
import com.example.bilibiliautomation.ui.settings.SettingsViewModel
import com.example.bilibiliautomation.ui.target.TargetViewModel
import com.example.bilibiliautomation.ui.task.TaskViewModel
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.delay

/**
 * Debug UI Runtime. Cross-page navigation lives here, not in @Preview.
 * Runtime pages collect production ViewModels; @Preview keep mock defaults.
 */
private sealed interface ShowcasePage {
    data object Home : ShowcasePage
    data object Account : ShowcasePage
    data object QrLogin : ShowcasePage
    data object LoginSuccess : ShowcasePage
    data object Target : ShowcasePage
    data object AddTarget : ShowcasePage
    data object TaskSettings : ShowcasePage
    data object TaskRunning : ShowcasePage
    data object Logs : ShowcasePage
    data object Settings : ShowcasePage
}

@Composable
fun UiShowcaseApp() {
    ShowcaseRoot()
}

@Composable
fun ShowcaseRoot() {
    val activity = LocalContext.current as ComponentActivity
    val factory = remember(activity) {
        AppViewModelFactory(activity.application as AutomationApplication)
    }
    val homeVm = remember(activity) { ViewModelProvider(activity, factory)[HomeViewModel::class.java] }
    val accountVm = remember(activity) { ViewModelProvider(activity, factory)[AccountViewModel::class.java] }
    val targetVm = remember(activity) { ViewModelProvider(activity, factory)[TargetViewModel::class.java] }
    val taskVm = remember(activity) { ViewModelProvider(activity, factory)[TaskViewModel::class.java] }
    val logVm = remember(activity) { ViewModelProvider(activity, factory)[LogViewModel::class.java] }
    val settingsVm = remember(activity) { ViewModelProvider(activity, factory)[SettingsViewModel::class.java] }

    val dashboard by homeVm.dashboard.collectAsState()
    val accounts by accountVm.accounts.collectAsState()
    val qrLogin by accountVm.qrLogin.collectAsState()
    val qrBitmap by accountVm.qrBitmap.collectAsState()
    val targets by targetVm.targets.collectAsState()
    val search by targetVm.search.collectAsState()
    val task by taskVm.state.collectAsState()
    val logs by logVm.records.collectAsState()
    val appSettings by settingsVm.settings.collectAsState()

    var currentName by rememberSaveable { mutableStateOf("Home") }
    val backStack = remember { mutableStateListOf<String>() }
    var lastLoginName by rememberSaveable { mutableStateOf("") }
    var lastLoginUid by rememberSaveable { mutableStateOf("") }
    var taskSettings by remember { mutableStateOf(TaskSettingsUiState()) }
    var logFilter by rememberSaveable { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(TimeMs.wallClock()) }
    // When >= 0, the QrLogin page re-logs-in this existing account instead of
    // creating a new pending account. Set when the user taps "切换" on an
    // expired account with no usable alternative.
    var reloginAccountId by remember { mutableLongStateOf(-1L) }

    val current = currentName.toPage()

    fun navigate(page: ShowcasePage) {
        backStack.add(currentName)
        currentName = page.route()
    }

    fun switchTab(page: ShowcasePage) {
        backStack.clear()
        currentName = page.route()
    }

    fun goBack() {
        val popped = backStack.removeLastOrNull()
        currentName = popped ?: current.fallbackBack().route()
    }

    fun startTaskOrGuide() {
        val dash = dashboard
        when {
            dash.needLogin -> {
                accountVm.prepareQrLogin()
                navigate(ShowcasePage.QrLogin)
            }
            dash.needTarget -> navigate(ShowcasePage.AddTarget)
            else -> {
                homeVm.start(dash.target!!.uid, dash.account!!.id)
                navigate(ShowcasePage.TaskRunning)
            }
        }
    }

    BackHandler(enabled = current != ShowcasePage.Home || backStack.isNotEmpty()) {
        goBack()
    }

    LaunchedEffect(task.status) {
        while (true) {
            now = TimeMs.wallClock()
            if (task.status != TaskStatus.RUNNING) {
                break
            }
            delay(1_000)
        }
    }

    LaunchedEffect(current, task.taskId, appSettings) {
        if (current == ShowcasePage.TaskRunning) {
            taskVm.ensureIdleAccount()
        }
        if (current == ShowcasePage.TaskSettings) {
            taskSettings = UiFormat.taskSettings(appSettings)
        }
    }

    val onTab: (BiliTab) -> Unit = { tab ->
        switchTab(
            when (tab) {
                BiliTab.HOME -> ShowcasePage.Home
                BiliTab.ACCOUNT -> ShowcasePage.Account
                BiliTab.TARGET -> ShowcasePage.Target
                BiliTab.TASK -> ShowcasePage.TaskRunning
                BiliTab.SETTINGS -> ShowcasePage.Settings
            },
        )
    }

    val visibleAccounts = accounts.filterNot { it.uid.startsWith("pending-") }
    val idleAccounts = visibleAccounts.filter { AccountPool.isIdle(it) }
    val displayedTask = task.copy(
        account = task.account ?: idleAccounts.firstOrNull() ?: dashboard.account,
    )
    val homeStatus = UiFormat.homeStatus(dashboard.task.status)
    // dashboard.account is already resolved from the live accounts list +
    // the shared current-account pointer (see HomeViewModel), so it carries
    // the current DB status (e.g. LOGIN_EXPIRED). No extra override needed.
    val homeAccount = dashboard.account
    val homeAccountStatus = homeAccount?.status?.let(UiFormat::accountStatus)
        ?: com.example.bilibiliautomation.ui.compose.screens.AccountStatus.OK
    val accountSubtitle = homeAccount
        ?.let { "${UiFormat.accountLabel(it)}（UID: ${it.uid}）" }
        ?: "未登录，请先添加账号"
    val upSubtitle = dashboard.target
        ?.let { "${it.nickname.ifBlank { it.uid }} (UID: ${it.uid})" }
        ?: "未选择目标 UP"
    val upStatus = if (dashboard.target != null)
        com.example.bilibiliautomation.ui.compose.screens.AccountStatus.OK
    else
        com.example.bilibiliautomation.ui.compose.screens.AccountStatus.IDLE
    val currentAccountId = accountVm.currentAccountId.collectAsState().value

    when (current) {
        ShowcasePage.Home -> HomeScreen(
            selectedTab = BiliTab.HOME,
            onTabSelect = onTab,
            taskStatus = homeStatus,
            elapsed = if (homeStatus == HomeTaskStatus.RUNNING) {
                UiFormat.elapsed(dashboard.task.startedAt, now)
            } else {
                dashboard.task.pausedAt?.let { UiFormat.elapsed(dashboard.task.startedAt, it) }
                    ?: UiFormat.elapsed(dashboard.task.startedAt, now)
            },
            accountSubtitle = accountSubtitle,
            upSubtitle = upSubtitle,
            progressDone = dashboard.task.videoProgress.current,
            progressTotal = dashboard.task.videoProgress.total,
            followed = dashboard.task.followed,
            skipped = dashboard.task.skipped,
            failed = dashboard.task.failed,
            onPause = { homeVm.resume() },
            onStop = { homeVm.stop() },
            onStart = { startTaskOrGuide() },
            onTaskSettings = { navigate(ShowcasePage.TaskSettings) },
            onAccountMore = { switchTab(ShowcasePage.Account) },
            brandPainter = painterResource(R.drawable.ic_bili_logo),
            accountImageUrl = homeAccount?.avatarUrl,
            upImageUrl = dashboard.target?.avatarUrl,
            accountStatus = homeAccountStatus,
            upStatus = upStatus,
        )
        ShowcasePage.Account -> AccountScreen(
            selectedTab = BiliTab.ACCOUNT,
            onTabSelect = onTab,
            onAdd = {
                accountVm.prepareQrLogin()
                navigate(ShowcasePage.QrLogin)
            },
            onBack = ::goBack,
            onSetCurrent = { accountVm.setCurrent(it); accountVm.refreshProfiles() },
            onDelete = { accountVm.delete(it) },
            currentAccountId = currentAccountId,
            accounts = visibleAccounts.map { UiFormat.toAccountItem(it) },
        )
        ShowcasePage.QrLogin -> {
            var canComplete by remember(currentName) { mutableStateOf(false) }
            LaunchedEffect(currentName) {
                if (reloginAccountId >= 0) {
                    accountVm.startQrLogin(reloginAccountId)
                } else {
                    accountVm.startNewQrLogin()
                }
            }
            DisposableEffect(currentName) {
                onDispose {
                    accountVm.stopQrLogin()
                    reloginAccountId = -1L
                }
            }
            LaunchedEffect(qrLogin.phase, qrLogin.uid) {
                if (qrLogin.phase == QrLoginPhase.FETCHING_QR ||
                    qrLogin.phase == QrLoginPhase.WAITING_SCAN ||
                    qrLogin.phase == QrLoginPhase.SCANNED ||
                    qrLogin.phase == QrLoginPhase.WAITING_CONFIRM
                ) {
                    canComplete = true
                }
                if (canComplete && qrLogin.phase == QrLoginPhase.SUCCESS) {
                    lastLoginName = qrLogin.uname.orEmpty()
                    lastLoginUid = qrLogin.uid?.toString().orEmpty()
                    navigate(ShowcasePage.LoginSuccess)
                }
            }
            QrLoginScreen(
                selectedTab = BiliTab.ACCOUNT,
                onTabSelect = onTab,
                onBack = {
                    accountVm.stopQrLogin()
                    goBack()
                },
                onRefresh = { accountVm.refreshQr() },
                validityText = qrStatusText(qrLogin.phase, qrLogin.message),
                qrBitmap = qrBitmap?.asImageBitmap(),
                currentStep = when (qrLogin.phase) {
                    QrLoginPhase.WAITING_CONFIRM, QrLoginPhase.SCANNED -> 1
                    QrLoginPhase.SUCCESS -> 2
                    else -> 0
                },
                loading = qrLogin.phase == QrLoginPhase.FETCHING_QR || qrLogin.phase == QrLoginPhase.IDLE,
            )
        }
        ShowcasePage.LoginSuccess -> {
            val logged = visibleAccounts.firstOrNull { it.uid == lastLoginUid }
            LoginSuccessScreen(
                selectedTab = BiliTab.ACCOUNT,
                onTabSelect = onTab,
                onBack = ::goBack,
                onDone = { switchTab(ShowcasePage.Account) },
                onSetCurrent = {
                    logged?.id?.let {
                        accountVm.setCurrent(it)
                        accountVm.refreshProfiles()
                    }
                },
                userName = logged?.let { UiFormat.accountLabel(it) } ?: lastLoginName.ifBlank { "已登录账号" },
                uid = lastLoginUid.ifBlank { logged?.uid ?: "-" },
                fans = logged?.let { UiFormat.fansOrDash(it.fansCount) } ?: "—",
                avatarUrl = logged?.avatarUrl ?: qrLogin.face,
            )
        }
        ShowcasePage.Target -> TargetScreen(
            selectedTab = BiliTab.TARGET,
            onTabSelect = onTab,
            onAdd = { navigate(ShowcasePage.AddTarget) },
            onBack = ::goBack,
            onDelete = { targetVm.deleteByUid(it) },
            targets = targets.map(UiFormat::toTargetItem),
        )
        ShowcasePage.AddTarget -> AddTargetScreen(
            state = AddTargetUiState(
                keyword = search.keyword,
                uidInput = search.uidInput,
                selectedUid = search.selectedUid,
                results = search.results.map(UiFormat::toAddTargetResult),
                confirmMessage = when {
                    search.loading -> "正在查询…"
                    !search.error.isNullOrBlank() -> search.error
                    else -> null
                },
                confirmIsError = !search.loading && !search.error.isNullOrBlank(),
                selectedTab = BiliTab.TARGET,
            ),
            onTabSelect = onTab,
            onKeywordChange = targetVm::setKeyword,
            onUidInputChange = targetVm::setUidInput,
            onTargetSelect = targetVm::select,
            onSearch = { targetVm.lookupUid() },
            onConfirm = {
                targetVm.confirmSelected()
                switchTab(ShowcasePage.Target)
            },
            onBack = ::goBack,
        )
        ShowcasePage.TaskSettings -> TaskSettingsScreen(
            selectedTab = BiliTab.TASK,
            onTabSelect = onTab,
            onBack = ::goBack,
            state = taskSettings,
            onStateChange = { taskSettings = it },
            onSave = {
                settingsVm.applyTaskSettings(
                    intervalMs = UiFormat.parseIntervalMs(taskSettings.interval),
                    retryCount = taskSettings.retries.toIntOrNull() ?: 3,
                    cooldownMinutes = taskSettings.cooldown.toIntOrNull() ?: 30,
                    dailyLimit = taskSettings.dailyLimit.toIntOrNull() ?: 200,
                    notify = taskSettings.notifyOnComplete,
                    debug = taskSettings.verboseLog,
                )
                val dash = dashboard
                if (!dash.needLogin && !dash.needTarget) {
                    homeVm.start(dash.target!!.uid, dash.account!!.id)
                    switchTab(ShowcasePage.TaskRunning)
                } else {
                    goBack()
                }
            },
        )
        ShowcasePage.TaskRunning -> TaskRunningScreen(
            state = UiFormat.toTaskRunning(displayedTask),
            selectedTab = BiliTab.TASK,
            onTabSelect = onTab,
            onBack = ::goBack,
            onPause = { homeVm.resume() },
            onStop = {
                taskVm.cancel()
                switchTab(ShowcasePage.Home)
            },
            onLogs = { navigate(ShowcasePage.Logs) },
            onStart = { startTaskOrGuide() },
            switchableAccounts = idleAccounts.map(UiFormat::toAccountItem),
            onSelectAccount = { id -> taskVm.switchTo(id) },
            onNeedLogin = {
                val currentId = displayedTask.account?.id
                if (currentId != null) {
                    reloginAccountId = currentId
                    navigate(ShowcasePage.QrLogin)
                } else {
                    accountVm.prepareQrLogin()
                    navigate(ShowcasePage.QrLogin)
                }
            },
        )
        ShowcasePage.Logs -> LogScreen(
            selectedTab = BiliTab.TASK,
            onTabSelect = onTab,
            onBack = ::goBack,
            onClear = logVm::clear,
            logs = logs.map(UiFormat::toLogEntry),
            selectedFilter = logFilter,
            onFilterSelect = { index ->
                logFilter = index
                logVm.setFilter(
                    when (index) {
                        1 -> LogViewModel.Filter.INFO
                        2 -> LogViewModel.Filter.SUCCESS
                        3 -> LogViewModel.Filter.WARN
                        4 -> LogViewModel.Filter.ERROR
                        else -> LogViewModel.Filter.ALL
                    },
                )
            },
        )
        ShowcasePage.Settings -> SettingsScreen(
            selectedTab = BiliTab.SETTINGS,
            onTabSelect = onTab,
            onBack = ::goBack,
            onLogout = {
                dashboard.account?.id?.let(accountVm::delete)
                switchTab(ShowcasePage.Home)
            },
            onTaskSettings = { navigate(ShowcasePage.TaskSettings) },
            onClearHistory = settingsVm::clearLogs,
            versionName = "v${BuildConfig.VERSION_NAME}",
        )
    }
}

private fun qrStatusText(phase: QrLoginPhase, message: String): String = when (phase) {
    QrLoginPhase.IDLE, QrLoginPhase.FETCHING_QR -> "正在向哔哩哔哩通行证申请二维码"
    QrLoginPhase.WAITING_SCAN -> "请使用哔哩哔哩 APP 扫码\n请勿向他人展示二维码"
    QrLoginPhase.SCANNED, QrLoginPhase.WAITING_CONFIRM -> "已扫描，请在手机上确认登录"
    QrLoginPhase.SUCCESS -> "登录成功，正在保存账号"
    QrLoginPhase.EXPIRED -> "二维码已过期，请点击刷新"
    QrLoginPhase.FAILED -> message.ifBlank { "获取二维码失败，请刷新重试" }
}

private fun ShowcasePage.route(): String = this::class.simpleName ?: "Home"

private fun ShowcasePage.fallbackBack(): ShowcasePage = when (this) {
    ShowcasePage.Account -> ShowcasePage.Home
    ShowcasePage.QrLogin -> ShowcasePage.Account
    ShowcasePage.LoginSuccess -> ShowcasePage.Account
    ShowcasePage.Target -> ShowcasePage.Home
    ShowcasePage.AddTarget -> ShowcasePage.Target
    ShowcasePage.TaskSettings -> ShowcasePage.TaskRunning
    ShowcasePage.TaskRunning -> ShowcasePage.Home
    ShowcasePage.Logs -> ShowcasePage.TaskRunning
    ShowcasePage.Settings -> ShowcasePage.Home
    ShowcasePage.Home -> ShowcasePage.Home
}

private fun String.toPage(): ShowcasePage = when (this) {
    "Account" -> ShowcasePage.Account
    "QrLogin" -> ShowcasePage.QrLogin
    "LoginSuccess" -> ShowcasePage.LoginSuccess
    "Target" -> ShowcasePage.Target
    "AddTarget" -> ShowcasePage.AddTarget
    "TaskSettings" -> ShowcasePage.TaskSettings
    "TaskRunning" -> ShowcasePage.TaskRunning
    "Logs" -> ShowcasePage.Logs
    "Settings" -> ShowcasePage.Settings
    else -> ShowcasePage.Home
}
