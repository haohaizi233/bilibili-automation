package com.example.bilibiliautomation.browser

import android.graphics.Bitmap
import android.os.Build
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.bilibiliautomation.automation.PageState
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class WebViewManager(
    private val webView: WebView,
    private val config: BrowserConfig,
    private val logger: AutomationLogger = AutomationLogger,
) {
    @Volatile
    var pageState: PageState = PageState.LOADING
        private set

    @Volatile
    var lastErrorUrl: String? = null
        private set

    @Volatile
    var lastErrorCode: Int? = null
        private set

    @Volatile
    var lastErrorDescription: String? = null
        private set

    @Volatile
    var currentUrl: String = ""
        private set

    @Volatile
    var pageTitle: String = ""
        private set

    private var navigationWaiter: CompletableDeferred<Boolean>? = null
    private var attached = false

    fun attach() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                pageState = PageState.LOADING
                currentUrl = url.orEmpty()
                logger.i("onPageStarted url=$currentUrl")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                currentUrl = url.orEmpty()
                pageTitle = view?.title.orEmpty()
                if (pageState != PageState.FAILED) {
                    pageState = PageState.READY
                }
                logger.i("onPageFinished url=$currentUrl title=$pageTitle state=$pageState")
                completeNavigation(pageState != PageState.FAILED)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (request?.isForMainFrame != true) return
                pageState = PageState.FAILED
                lastErrorUrl = request.url?.toString()
                lastErrorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) error?.errorCode else null
                lastErrorDescription = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    error?.description?.toString()
                } else {
                    error?.toString()
                }
                logger.e("NAVIGATION_FAILED url=$lastErrorUrl code=$lastErrorCode desc=$lastErrorDescription")
                completeNavigation(false)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                val message = consoleMessage?.message().orEmpty()
                val level = when (consoleMessage?.messageLevel()) {
                    ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                    ConsoleMessage.MessageLevel.WARNING -> "WARN"
                    ConsoleMessage.MessageLevel.LOG,
                    ConsoleMessage.MessageLevel.TIP,
                    ConsoleMessage.MessageLevel.DEBUG,
                    null,
                    -> "INFO"
                }
                logger.web(level, message)
                return true
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                pageTitle = title.orEmpty()
            }
        }
        attached = true
        logger.i("WebViewClient and WebChromeClient attached")
    }

    fun detachClients() {
        if (!attached) return
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = null
        attached = false
        completeNavigation(false)
        logger.i("WebView clients detached")
    }

    suspend fun loadUrlAndWait(url: String, timeoutMs: Long = config.timeoutMs): Boolean {
        val waiter = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            navigationWaiter = waiter
            pageState = PageState.LOADING
            lastErrorUrl = null
            lastErrorCode = null
            lastErrorDescription = null
            webView.loadUrl(url)
        }
        val result = withTimeoutOrNull(timeoutMs) { waiter.await() }
        if (result == null) {
            navigationWaiter = null
            logger.e("goto timeout url=$url")
            return false
        }
        return result
    }

    suspend fun reloadAndWait(timeoutMs: Long = config.timeoutMs): Boolean {
        val waiter = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            navigationWaiter = waiter
            pageState = PageState.LOADING
            webView.reload()
        }
        val result = withTimeoutOrNull(timeoutMs) { waiter.await() }
        if (result == null) {
            navigationWaiter = null
            return false
        }
        return result
    }

    fun applySettings() {
        webView.settings.javaScriptEnabled = config.javaScriptEnabled
        webView.settings.domStorageEnabled = config.domStorageEnabled
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        val ua = config.userAgent
        if (!ua.isNullOrBlank()) {
            webView.settings.userAgentString = ua
        }
        logger.i(
            "WebView settings js=${config.javaScriptEnabled} domStorage=${config.domStorageEnabled} " +
                "debug=${config.debugEnabled}",
        )
    }

    private fun completeNavigation(success: Boolean) {
        navigationWaiter?.let { waiter ->
            if (!waiter.isCompleted) waiter.complete(success)
        }
        navigationWaiter = null
    }
}
