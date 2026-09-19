package com.example.bilibiliautomation.browser

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the single active WebView, BrowserActor, and BrowserEngine.
 *
 * First version: one app, one WebView, one actor, one active workflow.
 * WebView is created with the attaching Activity context and destroyed with [destroy].
 */
class BrowserHost(
    private val appContext: Context,
    val config: BrowserConfig = BrowserConfig(),
    private val logger: AutomationLogger = AutomationLogger,
) {
    var webView: WebView? = null
        private set
    var actor: BrowserActor? = null
        private set
    var engine: WebViewBrowserEngine? = null
        private set
    var page: Page? = null
        private set
    var jsExecutor: JsExecutor? = null
        private set
    var webViewManager: WebViewManager? = null
        private set
    var debug: WebViewDebug? = null
        private set

    val isCreated: Boolean get() = webView != null

    fun create(hostContext: Context = appContext): WebView {
        val existing = webView
        if (existing != null) return existing
        val view = WebView(hostContext)
        val manager = WebViewManager(view, config, logger)
        val executor = JsExecutor(view)
        val waiter = Waiter(config, logger)
        lateinit var engineRef: WebViewBrowserEngine
        val actorRef = BrowserActor(
            handler = BrowserCommandHandler { command -> engineRef.handle(command) },
            logger = logger,
        )
        engineRef = WebViewBrowserEngine(
            webView = view,
            webViewManager = manager,
            jsExecutor = executor,
            waiter = waiter,
            config = config,
            actor = actorRef,
            logger = logger,
        )
        manager.applySettings()
        manager.attach()
        webView = view
        actor = actorRef
        engine = engineRef
        jsExecutor = executor
        webViewManager = manager
        page = Page(engineRef, manager)
        debug = WebViewDebug(hostContext, view, executor, manager, logger)
        logger.i("BrowserHost created")
        return view
    }

    fun attach(container: ViewGroup) {
        val view = webView ?: create(container.context)
        (view.parent as? ViewGroup)?.removeView(view)
        container.removeAllViews()
        container.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        logger.i("BrowserHost attached")
    }

    fun detach() {
        val view = webView ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        logger.i("BrowserHost detached")
    }

    fun pause() {
        val view = webView ?: return
        view.onPause()
        // pauseTimers() is process-wide; skip it so a foreground task can keep JS timers.
    }

    fun resume() {
        val view = webView ?: return
        view.resumeTimers()
        view.onResume()
    }

    fun destroy() {
        logger.i("BrowserHost destroying WebView")
        actor?.shutdown()
        actor = null
        val view = webView
        webView = null
        engine = null
        page = null
        jsExecutor = null
        debug = null
        if (view != null) {
            view.stopLoading()
            webViewManager?.detachClients()
            view.loadUrl("about:blank")
            (view.parent as? ViewGroup)?.removeView(view)
            view.removeAllViews()
            view.destroy()
        }
        webViewManager = null
    }

    suspend fun requireEngine(): WebViewBrowserEngine = withContext(Dispatchers.Main) {
        engine ?: error("BrowserHost not created")
    }
}
