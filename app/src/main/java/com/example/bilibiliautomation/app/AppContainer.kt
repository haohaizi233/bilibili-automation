package com.example.bilibiliautomation.app

import android.content.Context
import com.example.bilibiliautomation.browser.BrowserHost
import com.example.bilibiliautomation.browser.BrowserConfig
import com.example.bilibiliautomation.browser.SessionManager
import com.example.bilibiliautomation.data.api.BiliApiClient
import com.example.bilibiliautomation.data.api.DirectBiliApiProvider
import com.example.bilibiliautomation.data.api.RelationActionClient
import com.example.bilibiliautomation.data.api.UserProfileSync
import com.example.bilibiliautomation.data.api.session.ApiSessionManager
import com.example.bilibiliautomation.data.api.wbi.WbiKeyManager
import com.example.bilibiliautomation.data.db.AutomationDatabase
import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.repository.ActionRepository
import com.example.bilibiliautomation.data.repository.CommentRepository
import com.example.bilibiliautomation.data.repository.ProcessedUserRepository
import com.example.bilibiliautomation.data.repository.QrLoginRepository
import com.example.bilibiliautomation.data.repository.SmsLoginRepository
import com.example.bilibiliautomation.data.repository.TargetRepository
import com.example.bilibiliautomation.data.repository.TaskGroupRepository
import com.example.bilibiliautomation.data.repository.TaskRepository
import com.example.bilibiliautomation.data.repository.UserRepository
import com.example.bilibiliautomation.data.repository.VideoRepository
import com.example.bilibiliautomation.data.secure.EncryptedPrefsSessionBlobStore
import com.example.bilibiliautomation.data.secure.SecureSessionStore
import com.example.bilibiliautomation.data.settings.SettingsRepository
import com.example.bilibiliautomation.domain.account.AccountManager
import com.example.bilibiliautomation.domain.model.AppSettings
import com.example.bilibiliautomation.domain.recovery.CrashRecovery
import com.example.bilibiliautomation.domain.task.TaskScheduler
import com.example.bilibiliautomation.domain.workflow.AccountPool
import com.example.bilibiliautomation.domain.workflow.ApiAutomationBackend
import com.example.bilibiliautomation.domain.workflow.ApiWorkflowEngine
import com.example.bilibiliautomation.domain.workflow.MockSiteAdapter
import com.example.bilibiliautomation.domain.workflow.RateLimiter
import com.example.bilibiliautomation.domain.workflow.WebViewAutomationBackend
import com.example.bilibiliautomation.domain.workflow.WorkflowEngine
import com.example.bilibiliautomation.event.AutomationEventBus
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.service.TaskStatusNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(
    context: Context,
) {
    val appContext: Context = context.applicationContext
    val database: AutomationDatabase = AutomationDatabase.create(appContext)
    val eventBus = AutomationEventBus()
    val logger = AutomationLogger

    val accountRepository = AccountRepository(database.accountDao())
    val targetRepository = TargetRepository(database.targetDao(), database.taskGroupDao())
    val videoRepository = VideoRepository(database.videoDao())
    val commentRepository = CommentRepository(database.commentDao())
    val userRepository = UserRepository(database.userDao())
    val processedUserRepository = ProcessedUserRepository(userRepository)
    val actionRepository = ActionRepository(database.actionHistoryDao())
    val taskRepository = TaskRepository(database.taskDao())
    val taskGroupRepository = TaskGroupRepository(database.taskGroupDao(), database.targetDao())
    val settingsRepository = SettingsRepository(appContext)

    val sessionStore = SecureSessionStore(EncryptedPrefsSessionBlobStore(appContext), logger)
    val browserHost = BrowserHost(appContext, BrowserConfig(debugEnabled = com.example.bilibiliautomation.BuildConfig.DEBUG), logger)
    val sessionManager = SessionManager(
        context = appContext,
        sessionStore = sessionStore,
        accountRepository = accountRepository,
        browserHost = browserHost,
        logger = logger,
    )
    val apiSessionManager = ApiSessionManager(
        accountRepository = accountRepository,
        sessionStore = sessionStore,
        logger = logger,
    )
    val biliApiClient = BiliApiClient(wbi = WbiKeyManager(), logger = logger)
    val biliApiProvider = DirectBiliApiProvider(biliApiClient, apiSessionManager, logger)
    val userProfileSync = UserProfileSync(biliApiProvider, accountRepository, targetRepository, apiSessionManager, logger)
    val qrLoginRepository = QrLoginRepository(biliApiProvider, apiSessionManager, logger)
    val smsLoginRepository = SmsLoginRepository(biliApiProvider, apiSessionManager, logger)
    val relationActionClient = RelationActionClient(biliApiProvider, logger)
    val webViewFallbackBackend = WebViewAutomationBackend()
    val apiBackend = ApiAutomationBackend(biliApiProvider)
    val accountPool = AccountPool(accountRepository, apiSessionManager, logger)
    val accountManager = AccountManager(
        accounts = accountRepository,
        sessionStore = sessionStore,
        sessionManager = sessionManager,
        logger = logger,
    )
    val crashRecovery = CrashRecovery(taskRepository, logger)

    private val latestSettings = MutableStateFlow(AppSettings())
    private val taskNotifier = TaskStatusNotifier(appContext) { latestSettings.value }

    val taskScheduler: TaskScheduler = TaskScheduler(
        taskRepository = taskRepository,
        accountRepository = accountRepository,
        targetRepository = targetRepository,
        taskGroupRepository = taskGroupRepository,
        accountPool = accountPool,
        eventBus = eventBus,
        engineFactory = { createApiWorkflowEngine() },
        adapterFactory = { createAdapter() },
        logger = logger,
    )

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            settingsRepository.settings.collect { cfg ->
                latestSettings.value = cfg
                logger.verbose = cfg.debugEnabled
            }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            eventBus.events.collect { taskNotifier.onEvent(it) }
        }
        // Warm up the session jar: activate the most recent ACTIVE account so
        // every ViewModel that reads activeAccountId / calls jar(accountId) gets
        // a CookieJar that actually carries persisted cookies. Without this,
        // cold-start requests (e.g. target-UP search) go out anonymous and
        // Bilibili returns blank `face`.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // Purge zombie pending accounts left behind by abandoned QR logins
            // (a placeholder row is created the moment the user opens "+添加账号";
            // if they back out or the scan fails, it was never cleaned up).
            runCatching { accountRepository.deletePendingPlaceholders() }
            apiSessionManager.ensureActiveAccountId()
            runCatching {
                apiSessionManager.activeAccountId?.let { userProfileSync.refreshStats(it) }
            }
        }
    }

    fun createAdapter(): MockSiteAdapter {
        val engine = browserHost.engine ?: error("BrowserHost not attached")
        return MockSiteAdapter(engine)
    }

    fun createWorkflowEngine(): WorkflowEngine {
        return WorkflowEngine(
            adapter = createAdapter(),
            taskRepository = taskRepository,
            accountRepository = accountRepository,
            videoRepository = videoRepository,
            commentRepository = commentRepository,
            userRepository = userRepository,
            actionRepository = actionRepository,
            accountManager = accountManager,
            eventBus = eventBus,
            logger = logger,
        )
    }

    fun createApiWorkflowEngine(): ApiWorkflowEngine {
        return ApiWorkflowEngine(
            backend = apiBackend,
            taskRepository = taskRepository,
            accountRepository = accountRepository,
            videoRepository = videoRepository,
            commentRepository = commentRepository,
            processedUsers = processedUserRepository,
            actionRepository = actionRepository,
            accountManager = accountManager,
            accountPool = accountPool,
            rateLimiter = RateLimiter { latestSettings.value },
            settings = { latestSettings.value },
            eventBus = eventBus,
            logger = logger,
            dryRun = false,
        )
    }

    fun close() {
        taskScheduler.shutdown()
        browserHost.destroy()
        database.close()
    }
}
