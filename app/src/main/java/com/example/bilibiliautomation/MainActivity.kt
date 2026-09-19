package com.example.bilibiliautomation

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.webkit.WebViewFeature
import com.example.bilibiliautomation.browser.BrowserConfig
import com.example.bilibiliautomation.browser.BrowserController
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.service.AutomationForegroundService
import com.example.bilibiliautomation.task.LocalSelfTestTask
import com.example.bilibiliautomation.ui.AppViewModelFactory
import com.example.bilibiliautomation.ui.account.AccountViewModel
import com.example.bilibiliautomation.ui.home.HomeViewModel
import com.example.bilibiliautomation.ui.logs.LogViewModel
import com.example.bilibiliautomation.ui.settings.SettingsViewModel
import com.example.bilibiliautomation.ui.target.TargetViewModel
import com.example.bilibiliautomation.ui.task.TaskViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var urlInput: EditText
    private lateinit var debugInfo: TextView
    private lateinit var panelContent: TextView
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var browserContainer: FrameLayout
    private lateinit var controller: BrowserController

    private val factory by lazy { AppViewModelFactory(application as AutomationApplication) }
    private val homeVm: HomeViewModel by viewModels { factory }
    private val accountVm: AccountViewModel by viewModels { factory }
    private val targetVm: TargetViewModel by viewModels { factory }
    private val taskVm: TaskViewModel by viewModels { factory }
    private val logVm: LogViewModel by viewModels { factory }
    private val settingsVm: SettingsViewModel by viewModels { factory }

    private val container get() = (application as AutomationApplication).container
    private var busyJob: Job? = null
    private var currentTab = 0

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        debugInfo = findViewById(R.id.debugInfo)
        panelContent = findViewById(R.id.panelContent)
        logView = findViewById(R.id.logView)
        logScroll = findViewById(R.id.logScroll)
        browserContainer = findViewById(R.id.browserContainer)

        urlInput.setText(BrowserConfig.TEST_PAGE_URL)
        container.browserHost.attach(browserContainer)
        controller = BrowserController(container.browserHost)
        controller.start()
        AutomationLogger.i(
            "WebViewFeature.VISUAL_STATE_CALLBACK=" +
                WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK),
        )
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        refreshDebugPanel()

        findViewById<Button>(R.id.btnGo).setOnClickListener { goToUrl() }
        findViewById<Button>(R.id.btnTestJs).setOnClickListener { testJs() }
        findViewById<Button>(R.id.btnInspect).setOnClickListener { inspect() }
        findViewById<Button>(R.id.btnRunTest).setOnClickListener { runAutomationTest() }
        findViewById<Button>(R.id.btnStartWorkflow).setOnClickListener { startMockWorkflow() }
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            taskVm.pause()
            AutomationForegroundService.pause(this)
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            taskVm.cancel()
            AutomationForegroundService.stop(this)
        }
        findViewById<Button>(R.id.tabHome).setOnClickListener { showTab(0) }
        findViewById<Button>(R.id.tabAccounts).setOnClickListener { showTab(1) }
        findViewById<Button>(R.id.tabTargets).setOnClickListener { showTab(2) }
        findViewById<Button>(R.id.tabTask).setOnClickListener { showTab(3) }
        findViewById<Button>(R.id.tabLogs).setOnClickListener { showTab(4) }
        findViewById<Button>(R.id.tabSettings).setOnClickListener { showTab(5) }
        findViewById<Button>(R.id.btnAddEntity).setOnClickListener { addEntity() }
        findViewById<Button>(R.id.filterAll).setOnClickListener { logVm.setFilter(LogViewModel.Filter.ALL) }
        findViewById<Button>(R.id.filterInfo).setOnClickListener { logVm.setFilter(LogViewModel.Filter.INFO) }
        findViewById<Button>(R.id.filterSuccess).setOnClickListener { logVm.setFilter(LogViewModel.Filter.SUCCESS) }
        findViewById<Button>(R.id.filterWarn).setOnClickListener { logVm.setFilter(LogViewModel.Filter.WARN) }
        findViewById<Button>(R.id.filterError).setOnClickListener { logVm.setFilter(LogViewModel.Filter.ERROR) }
        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                goToUrl()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            controller.page.goto(BrowserConfig.TEST_PAGE_URL)
            homeVm.markBrowserReady(BrowserConfig.TEST_PAGE_URL)
            refreshDebugPanel()
        }
        collectUi()
        showTab(0)
    }

    private fun collectUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    logVm.records.collect { records ->
                        logView.text = records.takeLast(200).joinToString("\n") { it.formatLine() }
                        logScroll.post { logScroll.fullViewDown() }
                    }
                }
                launch { homeVm.state.collect { renderPanel() } }
                launch { accountVm.accounts.collect { renderPanel() } }
                launch { targetVm.targets.collect { renderPanel() } }
                launch { taskVm.state.collect { renderPanel() } }
                launch { settingsVm.settings.collect { renderPanel() } }
            }
        }
    }

    private fun ScrollView.fullViewDown() {
        fullScroll(ScrollView.FOCUS_DOWN)
    }

    private fun showTab(index: Int) {
        currentTab = index
        renderPanel()
    }

    private fun renderPanel() {
        panelContent.text = when (currentTab) {
            1 -> "Accounts\n" + accountVm.accounts.value.joinToString("\n") {
                "${it.id} uid=${it.uid} ${it.nickname} ${it.status}"
            }.ifBlank { "(none — add uid)" }
            2 -> "Targets\n" + targetVm.targets.value.joinToString("\n") {
                "${it.id} uid=${it.uid} ${it.nickname} enabled=${it.enabled}"
            }.ifBlank { "(none — add uid)" }
            3 -> {
                val t = taskVm.state.value
                "Task ${t.taskId ?: "-"} ${t.status}\nstate=${t.workflowState}\n" +
                    "target=${t.target?.uid ?: "-"}\naccount=${t.account?.uid ?: "-"}\n" +
                    "videos=${t.videoProgress.label} comments=${t.commentProgress.label}\n" +
                    "action=${t.currentAction ?: "-"}\n${t.message}"
            }
            4 -> "Logs filtered in the panel below."
            5 -> {
                val s = settingsVm.settings.value
                "timeout=${s.defaultTimeoutMs} poll=${s.pollIntervalMs}\n" +
                    "opInterval=${s.operationIntervalMs} retry=${s.retryCount}\n" +
                    "cooldownMin=${s.cooldownMinutes} notify=${s.notificationEnabled} debug=${s.debugEnabled}\n" +
                    "Cookie profile supported. Full WebStorage isolation pending."
            }
            else -> {
                val h = homeVm.state.value
                "Home\nbrowserReady=${h.browserReady}\nurl=${h.currentUrl}\n" +
                    "interrupted=${h.interruptedTaskId ?: "none"}\n${h.message}\n" +
                    "Tap Start Mock WF for local test.html state machine."
            }
        }
        if (this::controller.isInitialized) refreshDebugPanel()
    }

    private fun addEntity() {
        val uid = findViewById<EditText>(R.id.uidInput).text?.toString().orEmpty()
        val name = findViewById<EditText>(R.id.nameInput).text?.toString().orEmpty()
        when (currentTab) {
            1 -> accountVm.add(uid, name)
            2 -> targetVm.add(uid, name)
            0 -> homeVm.restoreInterrupted()
        }
    }

    private fun goToUrl() {
        runExclusive("goto") {
            val url = urlInput.text?.toString().orEmpty()
            val result = controller.page.goto(url)
            AutomationLogger.i(if (result.success) "PASS goto ${result.data}" else "FAIL goto ${result.message}")
            refreshDebugPanel()
        }
    }

    private fun testJs() {
        runExclusive("testJs") {
            val result = controller.page.evaluate("document.title")
            AutomationLogger.i(
                if (result.success) "PASS Test JS title=${result.data}"
                else "FAIL Test JS ${result.code} ${result.message}",
            )
            refreshDebugPanel()
        }
    }

    private fun inspect() {
        runExclusive("inspect") {
            val env = controller.debug.getEnvironmentInfo()
            if (env != null) AutomationLogger.i("ENV $env")
            AutomationLogger.i("Chrome Remote Debug: PC Chrome -> chrome://inspect/#devices")
            refreshDebugPanel()
        }
    }

    private fun runAutomationTest() {
        runExclusive("self-test") {
            val result = controller.taskRunner.run(LocalSelfTestTask.create())
            if (result.success) AutomationLogger.success("PASS local automation self-test")
            else AutomationLogger.e("FAIL local automation self-test ${result.code} ${result.message}")
            refreshDebugPanel()
        }
    }

    private fun startMockWorkflow() {
        lifecycleScope.launch {
            val account = accountVm.accounts.value.firstOrNull()
                ?: container.accountManager.addAccount("10001", "mock-account")
            val target = targetVm.targets.value.firstOrNull()
                ?: container.targetRepository.upsert("20002", "mock-target")
            AutomationForegroundService.start(this@MainActivity, target.uid, account.id)
        }
    }

    private fun runExclusive(name: String, block: suspend () -> Unit) {
        if (busyJob?.isActive == true) {
            AutomationLogger.w("BUSY ignore $name")
            return
        }
        busyJob = lifecycleScope.launch {
            try {
                block()
            } catch (error: Exception) {
                AutomationLogger.e("$name crashed", error)
            } finally {
                refreshDebugPanel()
            }
        }
    }

    private fun refreshDebugPanel() {
        if (this::controller.isInitialized) {
            runCatching { debugInfo.text = controller.debug.formatPanel() }
        }
    }

    override fun onResume() {
        super.onResume()
        container.browserHost.resume()
    }

    override fun onPause() {
        if (!container.taskScheduler.hasActiveTask) {
            container.browserHost.pause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        busyJob?.cancel()
        container.browserHost.detach()
        if (!container.taskScheduler.hasActiveTask) {
            container.browserHost.destroy()
        }
        super.onDestroy()
    }
}
