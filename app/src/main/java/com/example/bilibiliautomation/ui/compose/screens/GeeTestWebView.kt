package com.example.bilibiliautomation.ui.compose.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.bilibiliautomation.data.api.BiliApiEndpoints
import com.example.bilibiliautomation.data.api.BiliHeaders
import com.example.bilibiliautomation.data.api.BiliJson
import com.example.bilibiliautomation.data.api.sms.PassportTakeoverJs
import com.example.bilibiliautomation.data.api.session.WebViewCookieParser
import com.example.bilibiliautomation.data.api.session.WebViewCookieSync
import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.delay
import org.json.JSONObject
import org.json.JSONTokener

class PassportTakeoverController {
    var webView: WebView? = null
        private set

    fun attach(view: WebView) {
        webView = view
    }

    fun detach(view: WebView) {
        if (webView === view) webView = null
    }

    fun submitSmsCode(code: String): Boolean {
        val view = webView ?: return false
        val trimmed = code.filter { it.isDigit() }
        if (trimmed.length < 4) return false
        view.post { view.evaluateJavascript(PassportTakeoverJs.injectCodeAndSubmit(trimmed), null) }
        return true
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GeeTestWebView(
    phone: String = "",
    cookies: List<SessionCookie> = emptyList(),
    controller: PassportTakeoverController? = null,
    onCaptchaVisible: () -> Unit = {},
    onOfficialSmsSent: (String) -> Unit = {},
    onLoginCookies: (List<SessionCookie>) -> Unit = {},
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentPhone by rememberUpdatedState(phone)
    val currentOnVisible by rememberUpdatedState(onCaptchaVisible)
    val currentOnSms by rememberUpdatedState(onOfficialSmsSent)
    val currentOnCookies by rememberUpdatedState(onLoginCookies)
    val currentOnError by rememberUpdatedState(onError)
    var polling by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    fun emitCookiesIfLoggedIn() {
        if (finished) return
        val snapshot = WebViewCookieSync.snapshotBili()
        if (WebViewCookieParser.isLoginEstablished(snapshot)) {
            finished = true
            currentOnCookies(snapshot)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
                settings.setSupportZoom(false)
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.userAgentString = BiliHeaders.MOBILE_USER_AGENT
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                val opener = PassportOpener(
                    view = this,
                    phone = { currentPhone },
                    onCaptchaVisible = { currentOnVisible() },
                )
                setTag(opener)
                addJavascriptInterface(
                    GeeBridge(
                        onCaptchaVisible = { currentOnVisible() },
                        onSmsSent = { currentOnSms(it) },
                        onError = { currentOnError(it) },
                    ),
                    "BiliGee",
                )
                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                    WebViewCompat.addDocumentStartJavaScript(
                        this,
                        PassportTakeoverJs.hook,
                        setOf(
                            "https://passport.bilibili.com",
                            "https://www.bilibili.com",
                        ),
                    )
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        AutomationLogger.d(
                            "passport webview console ${consoleMessage.messageLevel()} ${consoleMessage.message()}",
                        )
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        return false
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        AutomationLogger.w(
                            "passport resource ${request.url} ${error.errorCode} ${error.description}",
                        )
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        AutomationLogger.w("passport http ${request.url} ${errorResponse.statusCode}")
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                            view.evaluateJavascript(PassportTakeoverJs.hook, null)
                        }
                        val opener = view.tag as? PassportOpener ?: return
                        if (url.contains("geetest", ignoreCase = true)) {
                            opener.pollCaptchaOnly()
                            return
                        }
                        if (!url.contains("passport.bilibili.com")) return
                        opener.start()
                        emitCookiesIfLoggedIn()
                    }
                }
                controller?.attach(this)
                WebViewCookieSync.replaceWith(cookies) {
                    polling = true
                    loadUrl(BiliApiEndpoints.PASSPORT_H5_LOGIN)
                }
            }
        },
        onRelease = { view ->
            (view.tag as? PassportOpener)?.stop()
            controller?.detach(view)
            view.removeJavascriptInterface("BiliGee")
            view.stopLoading()
            view.destroy()
        },
    )

    LaunchedEffect(polling) {
        if (!polling) return@LaunchedEffect
        while (!finished) {
            delay(2_000)
            emitCookiesIfLoggedIn()
        }
    }
}

private class PassportOpener(
    private val view: WebView,
    private val phone: () -> String,
    private val onCaptchaVisible: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var sendTaps = 0
    private var agreeTapped = false
    private var captchaShown = false
    private var stopped = false
    private var started = false

    fun start() {
        if (stopped || captchaShown) return
        handler.removeCallbacksAndMessages(null)
        if (started) {
            handler.postDelayed({ fillThen("send") }, 400)
            return
        }
        started = true
        handler.postDelayed({ fillThen("agree") }, 700)
    }

    fun pollCaptchaOnly() {
        if (stopped || captchaShown) return
        snapshot("none") { if (!it) handler.postDelayed({ pollCaptchaOnly() }, 400) }
    }

    fun stop() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
    }

    private fun fillThen(focus: String) {
        if (stopped || captchaShown) return
        view.evaluateJavascript(PassportTakeoverJs.fillPhone(phone()), null)
        handler.postDelayed({ snapshot(focus) { handled ->
            if (handled || stopped || captchaShown) return@snapshot
            handler.postDelayed({ fillThen(nextFocus(focus)) }, 700)
        } }, 280)
    }

    private fun nextFocus(focus: String): String {
        return if (focus == "agree" && !agreeTapped) "agree" else "send"
    }

    private fun snapshot(focus: String, onIdle: (Boolean) -> Unit) {
        if (stopped) return
        view.evaluateJavascript(PassportTakeoverJs.queryTargets(focus)) { raw ->
            val json = jsResultObject(raw)
            AutomationLogger.i("passport targets $json")
            val captcha = json.optJSONObject("captcha")
            if (captcha != null) {
                markCaptcha()
                onIdle(true)
                return@evaluateJavascript
            }
            val agree = json.optJSONObject("agree")
            val send = json.optJSONObject("send")
            val agreed = json.optBoolean("agreed")
            when {
                agree != null && !agreed && !agreeTapped -> {
                    agreeTapped = true
                    tapNode(agree, "agree")
                    handler.postDelayed({ fillThen("send") }, 550)
                    onIdle(true)
                }
                send != null && sendTaps < 3 -> {
                    sendTaps += 1
                    tapNode(send, "send")
                    handler.postDelayed({ fillThen("send") }, 1_800)
                    onIdle(true)
                }
                else -> onIdle(false)
            }
        }
    }

    private fun markCaptcha() {
        if (captchaShown) return
        captchaShown = true
        handler.removeCallbacksAndMessages(null)
        onCaptchaVisible()
    }

    private fun tapNode(node: JSONObject, label: String) {
        val nx = node.optDouble("nx", Double.NaN)
        val ny = node.optDouble("ny", Double.NaN)
        if (nx.isNaN() || ny.isNaN()) return
        val cx = nx.coerceIn(0.04, 0.96)
        val cy = ny.coerceIn(0.04, 0.96)
        AutomationLogger.i("passport tap $label nx=$cx ny=$cy ${node.optString("t")}")
        view.post {
            if (view.width <= 0 || view.height <= 0) return@post
            view.requestFocus()
            tapNormalized(view, cx, cy)
        }
    }
}

private fun tapNormalized(view: WebView, nx: Double, ny: Double) {
    val x = (nx * view.width).toFloat()
    val y = (ny * view.height).toFloat()
    val downTime = SystemClock.uptimeMillis()
    val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
    val up = MotionEvent.obtain(downTime, downTime + 70, MotionEvent.ACTION_UP, x, y, 0)
    view.dispatchTouchEvent(down)
    view.dispatchTouchEvent(up)
    down.recycle()
    up.recycle()
}

private fun jsResultObject(raw: String?): JSONObject {
    if (raw.isNullOrBlank() || raw == "null") return JSONObject()
    val inner = runCatching {
        when (val value = JSONTokener(raw).nextValue()) {
            is String -> value
            is JSONObject -> return value
            else -> value.toString()
        }
    }.getOrDefault(raw)
    return runCatching { JSONObject(inner) }.getOrDefault(JSONObject())
}

private class GeeBridge(
    private val onCaptchaVisible: () -> Unit,
    private val onSmsSent: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private var smsEmitted = false
    private var visibleEmitted = false

    @JavascriptInterface
    fun onPageEvent(kind: String, payload: String) {
        main.post { handle(kind, payload) }
    }

    @JavascriptInterface
    fun onError(message: String) {
        main.post { onError(message) }
    }

    private fun handle(kind: String, payload: String) {
        when (kind) {
            "captcha_visible" -> {
                if (visibleEmitted) return
                visibleEmitted = true
                onCaptchaVisible()
            }
            "debug" -> {
                AutomationLogger.i("passport takeover $payload")
            }
            "captcha_net" -> {
                AutomationLogger.i("passport captcha_net $payload")
            }
            "sms_send" -> {
                if (smsEmitted) return
                val env = runCatching { BiliJson.envelope(payload) }.getOrNull()
                val key = env?.data?.let { BiliJson.parseSmsSend(it)?.captchaKey }.orEmpty()
                if (env != null && env.code != 0) {
                    onError(env.message.ifBlank { "发送验证码失败" })
                    return
                }
                smsEmitted = true
                onSmsSent(key)
            }
            else -> Unit
        }
    }
}
