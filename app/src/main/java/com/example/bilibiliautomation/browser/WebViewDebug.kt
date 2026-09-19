package com.example.bilibiliautomation.browser

import android.content.Context
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import com.example.bilibiliautomation.logging.AutomationLogger
import org.json.JSONObject

data class DebugSnapshot(
    val currentUrl: String,
    val userAgent: String,
    val webViewVersion: String,
    val pageState: String,
    val lastAction: String,
    val lastError: String,
    val environmentJson: String? = null,
)

class WebViewDebug(
    private val context: Context,
    private val webView: WebView,
    private val jsExecutor: JsExecutor,
    private val webViewManager: WebViewManager,
    private val logger: AutomationLogger = AutomationLogger,
) {
    fun collectPanelInfo(): DebugSnapshot {
        return DebugSnapshot(
            currentUrl = webView.url ?: webViewManager.currentUrl,
            userAgent = webView.settings.userAgentString.orEmpty(),
            webViewVersion = resolveWebViewVersion(),
            pageState = webViewManager.pageState.name,
            lastAction = logger.lastAction,
            lastError = logger.lastError,
        )
    }

    fun formatPanel(snapshot: DebugSnapshot = collectPanelInfo()): String {
        return buildString {
            append("URL: ").append(snapshot.currentUrl).append('\n')
            append("State: ").append(snapshot.pageState)
            append("  WebView: ").append(snapshot.webViewVersion).append('\n')
            append("UA: ").append(snapshot.userAgent).append('\n')
            append("LastAction: ").append(snapshot.lastAction).append('\n')
            append("LastError: ").append(snapshot.lastError)
        }
    }

    suspend fun getEnvironmentInfo(): JSONObject? {
        val script = JsExecutor.jsonResultScript(
            """
            return ok({
              userAgent: navigator.userAgent,
              innerWidth: window.innerWidth,
              innerHeight: window.innerHeight,
              screenWidth: screen.width,
              screenHeight: screen.height,
              devicePixelRatio: window.devicePixelRatio,
              maxTouchPoints: navigator.maxTouchPoints || 0,
              href: location.href,
              title: document.title,
              readyState: document.readyState
            });
            """.trimIndent(),
        )
        val result = jsExecutor.evaluateObjectScript(script)
        if (!result.success) {
            logger.e("getEnvironmentInfo failed: ${result.message}")
            return null
        }
        return result.data
    }

    suspend fun captureDomSnapshot(): String? {
        logger.w("captureDomSnapshot invoked — large HTML, debug only")
        return jsExecutor.evaluate("document.documentElement ? document.documentElement.outerHTML : ''")
    }

    private fun resolveWebViewVersion(): String {
        val pkg = WebViewCompat.getCurrentWebViewPackage(context)
        return pkg?.versionName ?: pkg?.packageName ?: "unknown"
    }

    companion object {
        fun environmentToLog(json: JSONObject): String {
            val keys = listOf(
                "userAgent", "innerWidth", "innerHeight", "screenWidth", "screenHeight",
                "devicePixelRatio", "maxTouchPoints", "href", "title", "readyState",
            )
            return keys.joinToString(" ") { key ->
                "$key=${json.opt(key)}"
            }
        }
    }
}
