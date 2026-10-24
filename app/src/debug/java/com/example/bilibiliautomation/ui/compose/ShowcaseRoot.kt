package com.example.bilibiliautomation.ui.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import android.Manifest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.example.bilibiliautomation.AutomationApplication
import com.example.bilibiliautomation.BuildConfig
import com.example.bilibiliautomation.R
import com.example.bilibiliautomation.data.api.qr.QrLoginPhase
import com.example.bilibiliautomation.data.api.sms.SmsLoginPhase
import com.example.bilibiliautomation.data.settings.FirstLaunchStore
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.domain.workflow.AccountPool
import com.example.bilibiliautomation.ui.AppViewModelFactory
import com.example.bilibiliautomation.ui.UiFormat
import com.example.bilibiliautomation.ui.account.AccountViewModel
import com.example.bilibiliautomation.ui.compose.components.BiliTab
import com.example.bilibiliautomation.ui.compose.components.WelcomeGuideDialog
import com.example.bilibiliautomation.ui.compose.screens.AccountScreen
import com.example.bilibiliautomation.ui.compose.screens.AddTargetScreen
import com.example.bilibiliautomation.ui.compose.screens.AddTargetUiState
import com.example.bilibiliautomation.ui.compose.screens.HomeScreen
import com.example.bilibiliautomation.ui.compose.screens.HomeTaskStatus
import com.example.bilibiliautomation.ui.compose.screens.LogScreen
import com.example.bilibiliautomation.ui.compose.screens.LoginChooserScreen
import com.example.bilibiliautomation.ui.compose.screens.LoginSuccessScreen
import com.example.bilibiliautomation.ui.compose.screens.NotificationSettingsScreen
import com.example.bilibiliautomation.ui.compose.screens.QrLoginScreen
import com.example.bilibiliautomation.ui.compose.screens.SettingsScreen
import com.example.bilibiliautomation.ui.compose.screens.SmsLoginScreen
import com.example.bilibiliautomation.ui.compose.screens.TargetScreen
import com.example.bilibiliautomation.ui.compose.screens.TaskRunningScreen
import com.example.bilibiliautomation.ui.compose.screens.TaskSettingsScreen
import com.example.bilibiliautomation.ui.compose.screens.TaskSettingsUiState
import com.example.bilibiliautomation.ui.home.HomeViewModel
import com.example.bilibiliautomation.ui.logs.LogViewModel
import com.example.bilibiliautomation.ui.settings.SettingsViewModel
import com.example.bilibiliautomation.ui.target.TargetViewModel
import com.example.bilibiliautomation.ui.compose.theme.BiliColors
import com.example.bilibiliautomation.ui.task.TaskViewModel
import com.example.bilibiliautomation.service.AutomationForegroundService
import com.example.bilibiliautomation.util.KeepAliveNeed
import com.example.bilibiliautomation.util.KeepAlivePermissions
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.delay

/**
 * Debug UI Runtime. Cross-page navigation lives here, not in @Preview.
 * Runtime pages collect production ViewModels; @Preview keep mock defaults.
 */
private sealed interface ShowcasePage {
    data object Home : ShowcasePage
    data object Account : ShowcasePage
    data object LoginChooser : ShowcasePage
    data object QrLogin : ShowcasePage
    data object SmsLogin : ShowcasePage
    data object LoginSuccess : ShowcasePage
    data object Target : ShowcasePage
    data object AddTarget : ShowcasePage
    data object TaskSettings : ShowcasePage
    data object TaskRunning : ShowcasePage
    data object Logs : ShowcasePage
    data object Settings : ShowcasePage
    data object Notifications : ShowcasePage
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
    val smsLogin by accountVm.smsLogin.collectAsState()
    val qrBitmap by accountVm.qrBitmap.collectAsState()
    val targets by targetVm.targets.collectAsState()
    val groups by targetVm.groups.collectAsState()
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
    var pendingUnattended by rememberSaveable { mutableStateOf(false) }
    var pendingNotifyEnable by rememberSaveable { mutableStateOf(false) }
    var grantUnattendedDialog by remember { mutableStateOf(false) }
    var revokedUnattendedDialog by remember { mutableStateOf(false) }
    var unattendedMissing by remember { mutableStateOf(emptyList<String>()) }
    var lastKeepAliveNeed by remember { mutableStateOf<KeepAliveNeed?>(null) }
    val firstLaunchStore = remember { FirstLaunchStore(activity) }
    var showWelcome by remember { mutableStateOf(firstLaunchStore.shouldShowWelcome()) }
    val keepAliveResult = remember { arrayOf({}) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { keepAliveResult[0].invoke() }
    val settingsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { keepAliveResult[0].invoke() }

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

    fun taskIsAlive(): Boolean {
        val status = dashboard.task.status
        return status == TaskStatus.RUNNING ||
            status == TaskStatus.PAUSED ||
            status == TaskStatus.INTERRUPTED
    }

    fun attachUnattendedService() {
        if (taskIsAlive()) AutomationForegroundService.attach(activity)
    }

    fun turnOnUnattended() {
        pendingUnattended = false
        grantUnattendedDialog = false
        settingsVm.setUnattended(true)
        KeepAlivePermissions.setKeepScreenOn(activity, true)
        attachUnattendedService()
    }

    fun turnOffUnattended() {
        pendingUnattended = false
        lastKeepAliveNeed = null
        settingsVm.setUnattended(false)
        KeepAlivePermissions.setKeepScreenOn(activity, false)
    }

    fun enforceUnattendedPermissions() {
        val check = KeepAlivePermissions.inspect(activity)
        if (pendingUnattended) {
            if (check.ready) {
                turnOnUnattended()
            } else {
                unattendedMissing = check.missingLabels
                grantUnattendedDialog = true
            }
            return
        }
        if (appSettings.unattendedMode && !check.ready) {
            turnOffUnattended()
            unattendedMissing = check.missingLabels
            revokedUnattendedDialog = true
            return
        }
        KeepAlivePermissions.setKeepScreenOn(activity, appSettings.unattendedMode && check.ready)
    }

    fun requestKeepAliveNeed(need: KeepAliveNeed) {
        lastKeepAliveNeed = need
        when (need) {
            KeepAliveNeed.READY -> turnOnUnattended()
            KeepAliveNeed.NOTIFICATION_RUNTIME ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            KeepAliveNeed.NOTIFICATION_SETTINGS ->
                settingsPermission.launch(KeepAlivePermissions.notificationSettingsIntent(activity))
            KeepAliveNeed.BATTERY_EXEMPTION -> {
                val battery = KeepAlivePermissions.batteryExemptionIntent(activity)
                runCatching { settingsPermission.launch(battery) }
                    .onFailure { settingsPermission.launch(KeepAlivePermissions.batterySettingsIntent()) }
            }
        }
    }

    fun startNextKeepAliveRequest() {
        requestKeepAliveNeed(KeepAlivePermissions.nextNeed(activity))
    }

    fun onKeepAliveResult() {
        if (pendingNotifyEnable) {
            pendingNotifyEnable = false
            settingsVm.setNotifications(KeepAlivePermissions.notificationsGranted(activity))
        }
        if (!pendingUnattended) {
            enforceUnattendedPermissions()
            return
        }
        val need = KeepAlivePermissions.nextNeed(activity)
        if (need == KeepAliveNeed.READY) {
            turnOnUnattended()
            return
        }
        if (lastKeepAliveNeed != null && need != lastKeepAliveNeed) {
            requestKeepAliveNeed(need)
            return
        }
        unattendedMissing = KeepAlivePermissions.inspect(activity).missingLabels
        grantUnattendedDialog = true
    }

    keepAliveResult[0] = { onKeepAliveResult() }

    fun startTaskOrGuide(targetUid: String? = null, groupId: Long? = null) {
        val dash = dashboard
        when {
            dash.needLogin -> {
                reloginAccountId = -1L
                navigate(ShowcasePage.LoginChooser)
            }
            dash.needTarget && targetUid == null && groupId == null -> navigate(ShowcasePage.AddTarget)
            else -> {
                val resolvedGroupId = groupId ?: if (targetUid == null) dash.task.groupId else null
                val uid = targetUid
                    ?: resolvedGroupId?.let { id -> groups.firstOrNull { it.group.id == id }?.members?.firstOrNull()?.uid }
                    ?: dash.target?.uid
                    ?: return
                val accountId = dash.account?.id ?: return
                val running = dash.task.status == TaskStatus.RUNNING ||
                    dash.task.status == TaskStatus.PAUSED ||
                    dash.task.status == TaskStatus.INTERRUPTED
                if (running && (targetUid != null || resolvedGroupId != dash.task.groupId || uid != dash.task.target?.uid)) {
                    homeVm.switchScope(uid, accountId, resolvedGroupId)
                } else if (running) {
                    homeVm.resume()
                } else {
                    homeVm.start(uid, accountId, resolvedGroupId)
                }
                if (appSettings.unattendedMode) {
                    AutomationForegroundService.attach(activity)
                }
                navigate(ShowcasePage.TaskRunning)
            }
        }
    }

    fun applyTargetScope(uid: String) {
        val dash = dashboard
        val running = dash.task.status == TaskStatus.RUNNING ||
            dash.task.status == TaskStatus.PAUSED ||
            dash.task.status == TaskStatus.INTERRUPTED
        if (running && dash.account != null) {
            homeVm.switchScope(uid, dash.account!!.id, null)
        } else {
            homeVm.selectTarget(uid)
        }
    }

    fun applyGroupScope(groupId: Long) {
        val dash = dashboard
        val group = groups.firstOrNull { it.group.id == groupId } ?: return
        val uid = group.members.firstOrNull()?.uid ?: return
        val running = dash.task.status == TaskStatus.RUNNING ||
            dash.task.status == TaskStatus.PAUSED ||
            dash.task.status == TaskStatus.INTERRUPTED
        if (running && dash.account != null) {
            homeVm.switchScope(uid, dash.account!!.id, groupId)
        } else {
            homeVm.selectGroup(groupId)
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
    val upSubtitle = when {
        dashboard.task.groupName != null && dashboard.target != null ->
            "任务组 ${dashboard.task.groupName} · ${dashboard.target!!.nickname.ifBlank { dashboard.target!!.uid }}"
        dashboard.target != null ->
            "${dashboard.target!!.nickname.ifBlank { dashboard.target!!.uid }} (UID: ${dashboard.target!!.uid})"
        else -> "未选择目标 UP"
    }
    val upStatus = if (dashboard.target != null)
        com.example.bilibiliautomation.ui.compose.screens.AccountStatus.OK
    else
        com.example.bilibiliautomation.ui.compose.screens.AccountStatus.IDLE
    val currentAccountId = accountVm.currentAccountId.collectAsState().value

    DisposableEffect(activity, appSettings.unattendedMode, pendingUnattended) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enforceUnattendedPermissions()
        }
        activity.lifecycle.addObserver(observer)
        enforceUnattendedPermissions()
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
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
            followed = dashboard.today.followed,
            skipped = dashboard.today.skipped,
            failed = dashboard.today.failed,
            onPause = { homeVm.resume() },
            onStop = { homeVm.stop() },
            onStart = { startTaskOrGuide() },
            onTaskSettings = { navigate(ShowcasePage.TaskSettings) },
            onAccountMore = { switchTab(ShowcasePage.Account) },
            switchableTargets = targets.map(UiFormat::toTargetItem),
            switchableGroups = groups.map(UiFormat::toTaskGroupItem),
            selectedGroupId = dashboard.task.groupId,
            selectedTargetUid = dashboard.target?.uid,
            onSelectTarget = ::applyTargetScope,
            onSelectGroup = ::applyGroupScope,
            brandPainter = painterResource(R.drawable.ic_bili_logo),
            accountImageUrl = homeAccount?.avatarUrl,
            upImageUrl = dashboard.target?.avatarUrl,
            accountStatus = homeAccountStatus,
            upStatus = upStatus,
            hasAccount = homeAccount != null,
            hasTarget = dashboard.target != null,
            onAddAccount = { switchTab(ShowcasePage.Account) },
            onAddTarget = { navigate(ShowcasePage.AddTarget) },
        )
        ShowcasePage.Account -> AccountScreen(
            selectedTab = BiliTab.ACCOUNT,
            onTabSelect = onTab,
            onAdd = {
                reloginAccountId = -1L
                navigate(ShowcasePage.LoginChooser)
            },
            onBack = ::goBack,
            onSetCurrent = { accountVm.setCurrent(it); accountVm.refreshProfiles() },
            onDelete = { accountVm.delete(it) },
            currentAccountId = currentAccountId,
            accounts = visibleAccounts.map { UiFormat.toAccountItem(it) },
        )
        ShowcasePage.LoginChooser -> LoginChooserScreen(
            selectedTab = BiliTab.ACCOUNT,
            onTabSelect = onTab,
            onBack = {
                reloginAccountId = -1L
                goBack()
            },
            onQrLogin = {
                accountVm.prepareQrLogin()
                navigate(ShowcasePage.QrLogin)
            },
            onSmsLogin = {
                accountVm.prepareSmsLogin(reloginAccountId.takeIf { it >= 0 })
                navigate(ShowcasePage.SmsLogin)
            },
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
        ShowcasePage.SmsLogin -> {
            LaunchedEffect(smsLogin.phase, smsLogin.uid) {
                if (smsLogin.phase == SmsLoginPhase.SUCCESS) {
                    lastLoginName = smsLogin.uname.orEmpty()
                    lastLoginUid = smsLogin.uid?.toString().orEmpty()
                    navigate(ShowcasePage.LoginSuccess)
                }
            }
            DisposableEffect(currentName) {
                onDispose {
                    accountVm.stopSmsLogin()
                    reloginAccountId = -1L
                }
            }
            SmsLoginScreen(
                state = smsLogin,
                selectedTab = BiliTab.ACCOUNT,
                onTabSelect = onTab,
                onBack = {
                    accountVm.stopSmsLogin()
                    goBack()
                },
                onSendCode = accountVm::requestSmsCode,
                onSubmit = accountVm::submitSmsCode,
                onGeeSuccess = { challenge, validate, seccode ->
                    accountVm.onGeeSuccess(challenge, validate, seccode)
                },
                onGeeError = accountVm::onGeeError,
                onGeeClosed = accountVm::onGeeClosed,
                onOpenOfficialPage = accountVm::openOfficialHitl,
                onOfficialSmsSent = accountVm::onOfficialSmsSent,
                onHitlSuccess = accountVm::onHitlCookies,
                onHitlClosed = accountVm::onHitlClosed,
                onWebViewLogin = accountVm::markWebViewLoggingIn,
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
                avatarUrl = logged?.avatarUrl ?: qrLogin.face ?: smsLogin.face,
            )
        }
        ShowcasePage.Target -> TargetScreen(
            selectedTab = BiliTab.TARGET,
            onTabSelect = onTab,
            onAdd = { navigate(ShowcasePage.AddTarget) },
            onBack = ::goBack,
            onDelete = { targetVm.deleteByUid(it) },
            targets = targets.map(UiFormat::toTargetItem),
            groups = groups.map(UiFormat::toTaskGroupItem),
            onCreateGroup = targetVm::createGroup,
            onRenameGroup = targetVm::renameGroup,
            onDeleteGroup = targetVm::deleteGroup,
            onAddToGroup = targetVm::addToGroup,
            onRemoveFromGroup = targetVm::removeFromGroup,
            onRunGroup = { id ->
                applyGroupScope(id)
                startTaskOrGuide(groupId = id)
            },
            onRunTarget = { uid ->
                applyTargetScope(uid)
                startTaskOrGuide(targetUid = uid, groupId = null)
            },
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
                settingsVm.applyTaskSettings(taskSettings)
                goBack()
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
            switchableTargets = targets.map(UiFormat::toTargetItem),
            switchableGroups = groups.map(UiFormat::toTaskGroupItem),
            onSelectAccount = { id -> taskVm.switchTo(id) },
            onSelectTarget = ::applyTargetScope,
            onSelectGroup = ::applyGroupScope,
            onNeedLogin = {
                val currentId = displayedTask.account?.id
                reloginAccountId = currentId ?: -1L
                navigate(ShowcasePage.LoginChooser)
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
            onNotifications = { navigate(ShowcasePage.Notifications) },
            onClearHistory = settingsVm::clearHistory,
            unattendedMode = appSettings.unattendedMode,
            onUnattendedChange = { want ->
                if (!want) {
                    turnOffUnattended()
                } else {
                    val check = KeepAlivePermissions.inspect(activity)
                    if (check.ready) {
                        turnOnUnattended()
                    } else {
                        pendingUnattended = true
                        unattendedMissing = check.missingLabels
                        grantUnattendedDialog = true
                    }
                }
            },
            versionName = "v${BuildConfig.VERSION_NAME}",
        )
        ShowcasePage.Notifications -> {
            NotificationSettingsScreen(
                selectedTab = BiliTab.SETTINGS,
                onTabSelect = onTab,
                onBack = ::goBack,
                notifyOnComplete = appSettings.notificationEnabled,
                onNotifyOnCompleteChange = { want ->
                    if (!want) {
                        settingsVm.setNotifications(false)
                    } else if (KeepAlivePermissions.notificationsGranted(activity)) {
                        settingsVm.setNotifications(true)
                    } else {
                        pendingNotifyEnable = true
                        if (KeepAlivePermissions.needsRuntimeNotificationPermission(activity)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            settingsPermission.launch(KeepAlivePermissions.notificationSettingsIntent(activity))
                        }
                    }
                },
                onOpenSystemSettings = {
                    if (KeepAlivePermissions.needsRuntimeNotificationPermission(activity)) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        settingsPermission.launch(KeepAlivePermissions.notificationSettingsIntent(activity))
                    }
                },
            )
        }
    }

    if (grantUnattendedDialog) {
        AlertDialog(
            onDismissRequest = {
                grantUnattendedDialog = false
                pendingUnattended = false
            },
            title = { Text("开启无人值守模式") },
            text = {
                Text(
                    buildString {
                        append("该模式自动化程度极高，需要长期挂在前台、保持亮屏、允许自启动。请先开启：\n")
                        unattendedMissing.forEach { append("· $it\n") }
                        append("并在系统设置中允许自启动 / 后台运行。缺少权限时无法开启。")
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        grantUnattendedDialog = false
                        startNextKeepAliveRequest()
                    },
                ) {
                    Text("去开启", color = Color(BiliColors.PINK), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        grantUnattendedDialog = false
                        pendingUnattended = false
                    },
                ) {
                    Text("取消", color = Color(0xFF5B6F8C))
                }
            },
        )
    }
    if (revokedUnattendedDialog) {
        AlertDialog(
            onDismissRequest = { revokedUnattendedDialog = false },
            title = { Text("无人值守已关闭") },
            text = {
                Text(
                    buildString {
                        append("检测到必要权限缺失，已自动关闭无人值守模式：\n")
                        unattendedMissing.forEach { append("· $it\n") }
                        append("重新开启前请先授予权限。")
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { revokedUnattendedDialog = false }) {
                    Text("知道了", color = Color(BiliColors.PINK), fontWeight = FontWeight.Bold)
                }
            },
        )
    }
    if (showWelcome) {
        WelcomeGuideDialog(
            onLater = {
                firstLaunchStore.markWelcomeShown()
                showWelcome = false
            },
            onStart = {
                firstLaunchStore.markWelcomeShown()
                showWelcome = false
                switchTab(ShowcasePage.Account)
            },
        )
    }
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
    ShowcasePage.LoginChooser -> ShowcasePage.Account
    ShowcasePage.QrLogin -> ShowcasePage.LoginChooser
    ShowcasePage.SmsLogin -> ShowcasePage.LoginChooser
    ShowcasePage.LoginSuccess -> ShowcasePage.Account
    ShowcasePage.Target -> ShowcasePage.Home
    ShowcasePage.AddTarget -> ShowcasePage.Target
    ShowcasePage.TaskSettings -> ShowcasePage.TaskRunning
    ShowcasePage.TaskRunning -> ShowcasePage.Home
    ShowcasePage.Logs -> ShowcasePage.TaskRunning
    ShowcasePage.Settings -> ShowcasePage.Home
    ShowcasePage.Notifications -> ShowcasePage.Settings
    ShowcasePage.Home -> ShowcasePage.Home
}

private fun String.toPage(): ShowcasePage = when (this) {
    "Account" -> ShowcasePage.Account
    "LoginChooser" -> ShowcasePage.LoginChooser
    "QrLogin" -> ShowcasePage.QrLogin
    "SmsLogin" -> ShowcasePage.SmsLogin
    "LoginSuccess" -> ShowcasePage.LoginSuccess
    "Target" -> ShowcasePage.Target
    "AddTarget" -> ShowcasePage.AddTarget
    "TaskSettings" -> ShowcasePage.TaskSettings
    "TaskRunning" -> ShowcasePage.TaskRunning
    "Logs" -> ShowcasePage.Logs
    "Settings" -> ShowcasePage.Settings
    "Notifications" -> ShowcasePage.Notifications
    else -> ShowcasePage.Home
}
