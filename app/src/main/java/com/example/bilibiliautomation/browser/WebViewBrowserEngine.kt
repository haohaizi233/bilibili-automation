package com.example.bilibiliautomation.browser

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import com.example.bilibiliautomation.automation.PageState
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.BrowserEnvironmentInfo
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.JsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WebViewBrowserEngine(
    private val webView: WebView,
    private val webViewManager: WebViewManager,
    private val jsExecutor: JsExecutor,
    private val waiter: Waiter,
    private val config: BrowserConfig,
    private val actor: BrowserActor,
    private val logger: AutomationLogger = AutomationLogger,
) : BrowserEngine {

    override suspend fun goto(url: String): AutomationResult<Unit> {
        return actor.submit(NavigateCommand(normalizeUrl(url), config.timeoutMs))
    }

    override suspend fun reload(): AutomationResult<Unit> {
        return actor.submit(ReloadCommand(config.timeoutMs))
    }

    override suspend fun click(locator: Locator): AutomationResult<Unit> {
        return actor.submit(ClickCommand(locator))
    }

    override suspend fun fill(locator: Locator, value: String): AutomationResult<Unit> {
        return actor.submit(FillCommand(locator, value))
    }

    override suspend fun exists(locator: Locator): Boolean {
        val result: AutomationResult<Boolean> = actor.submit(ExistsCommand(locator))
        return result.data == true
    }

    override suspend fun getText(locator: Locator): AutomationResult<String> {
        return actor.submit(GetTextCommand(locator))
    }

    override suspend fun getAttribute(locator: Locator, name: String): AutomationResult<String?> {
        return actor.submit(GetAttributeCommand(locator, name))
    }

    override suspend fun evaluate(script: String): AutomationResult<String?> {
        return actor.submit(EvaluateCommand(script))
    }

    override suspend fun waitFor(locator: Locator, timeoutMs: Long?): AutomationResult<Unit> {
        return actor.submit(WaitForSelectorCommand(locator, timeoutMs ?: config.timeoutMs))
    }

    override suspend fun waitForText(text: String, timeoutMs: Long?): AutomationResult<Unit> {
        return actor.submit(WaitForTextCommand(text, timeoutMs ?: config.timeoutMs))
    }

    override suspend fun currentUrl(): String? {
        val result: AutomationResult<String> = actor.submit(CurrentUrlCommand())
        return result.data
    }

    override suspend fun environmentInfo(): BrowserEnvironmentInfo? {
        val result: AutomationResult<BrowserEnvironmentInfo> = actor.submit(
            CustomCommand("environmentInfo") { collectEnvironment() },
        )
        return result.data
    }

    suspend fun handle(command: BrowserCommand): AutomationResult<*> {
        return when (command) {
            is NavigateCommand -> doGoto(command.url, command.timeoutMs ?: config.timeoutMs)
            is ReloadCommand -> doReload(command.timeoutMs ?: config.timeoutMs)
            is ClickCommand -> doClick(command.locator)
            is FillCommand -> doFill(command.locator, command.value)
            is EvaluateCommand -> doEvaluate(command.script)
            is WaitForSelectorCommand -> doWaitFor(command.locator, command.timeoutMs ?: config.timeoutMs)
            is WaitForTextCommand -> doWaitForText(command.text, command.timeoutMs ?: config.timeoutMs)
            is GetTextCommand -> doGetText(command.locator)
            is GetAttributeCommand -> doGetAttribute(command.locator, command.attr)
            is ExistsCommand -> doExists(command.locator)
            is CurrentUrlCommand -> AutomationResult.ok(readUrl())
            is CustomCommand -> command.block()
        }
    }

    private suspend fun doGoto(url: String, timeoutMs: Long): AutomationResult<Unit> {
        val started = logger.timed()
        val loaded = webViewManager.loadUrlAndWait(url, timeoutMs)
        val duration = logger.elapsed(started)
        val current = readUrl()
        return if (loaded && webViewManager.pageState != PageState.FAILED) {
            logger.action("GOTO", url = current, durationMs = duration, result = "SUCCESS", resultCode = ResultCode.OK)
            AutomationResult.ok(message = "PASS goto", durationMs = duration)
        } else {
            val error = "url=${webViewManager.lastErrorUrl ?: url} code=${webViewManager.lastErrorCode} desc=${webViewManager.lastErrorDescription}"
            logger.action("GOTO", url = url, durationMs = duration, result = "FAIL", error = error, resultCode = ResultCode.NAVIGATION_FAILED)
            AutomationResult.fail(ResultCode.NAVIGATION_FAILED, error, durationMs = duration)
        }
    }

    private suspend fun doReload(timeoutMs: Long): AutomationResult<Unit> {
        val started = logger.timed()
        val loaded = webViewManager.reloadAndWait(timeoutMs)
        val duration = logger.elapsed(started)
        val current = readUrl()
        return if (loaded) {
            logger.action("RELOAD", url = current, durationMs = duration, result = "SUCCESS", resultCode = ResultCode.OK)
            AutomationResult.ok(durationMs = duration)
        } else {
            logger.action("RELOAD", url = current, durationMs = duration, result = "FAIL", resultCode = ResultCode.NAVIGATION_FAILED)
            AutomationResult.fail(ResultCode.NAVIGATION_FAILED, "reload failed", durationMs = duration)
        }
    }

    private suspend fun doEvaluate(js: String): AutomationResult<String?> {
        val started = logger.timed()
        val result = jsExecutor.evaluateResult(js)
        val duration = logger.elapsed(started)
        val data = when (val payload = result.data?.opt("data")) {
            null -> result.data?.toString()
            JSONObject.NULL -> null
            else -> payload.toString()
        }
        logger.action(
            action = "EVALUATE",
            durationMs = duration,
            result = if (result.success) "SUCCESS" else result.code,
            resultCode = result.code,
            error = result.message.takeIf { !result.success },
            url = readUrl(),
        )
        return if (result.success) {
            AutomationResult.ok(data, result.message, result.code, duration)
        } else {
            AutomationResult.fail(result.code, result.message, data, duration)
        }
    }

    private suspend fun doExists(locator: Locator, log: Boolean = true): AutomationResult<Boolean> {
        val started = logger.timed()
        val script = JsExecutor.jsonResultScript(
            """
            var el = ${locator.toFinderJs()};
            if (!el) return fail("ELEMENT_NOT_FOUND", "selector not found", null);
            return ok({found:true, tag: el.tagName || "", id: el.id || ""});
            """.trimIndent(),
        )
        val result = jsExecutor.evaluateObjectScript(script)
        val found = result.success
        if (log) {
            logger.action(
                action = "EXISTS",
                selector = locator.description,
                durationMs = logger.elapsed(started),
                result = if (found) "SUCCESS" else result.code,
                resultCode = if (found) ResultCode.OK else result.code,
                url = readUrl(),
                error = result.message.takeIf { !found },
            )
        }
        return if (found) AutomationResult.ok(true) else AutomationResult.ok(false, result.message, result.code)
    }

    private suspend fun doGetText(locator: Locator): AutomationResult<String> {
        val started = logger.timed()
        val script = JsExecutor.jsonResultScript(
            """
            var el = ${locator.toFinderJs()};
            if (!el) return fail("ELEMENT_NOT_FOUND", "selector not found");
            var text = (el.innerText !== undefined ? el.innerText : (el.textContent || el.value || ""));
            return ok({text: String(text)});
            """.trimIndent(),
        )
        val result = jsExecutor.evaluateObjectScript(script)
        val text = result.data?.optString("text")
        val duration = logger.elapsed(started)
        logger.action(
            action = "GET_TEXT",
            selector = locator.description,
            durationMs = duration,
            result = if (result.success) "SUCCESS" else result.code,
            resultCode = result.code,
            url = readUrl(),
            error = result.message.takeIf { !result.success },
        )
        return if (result.success) AutomationResult.ok(text.orEmpty(), durationMs = duration)
        else AutomationResult.fail(result.code, result.message, durationMs = duration)
    }

    private suspend fun doGetAttribute(locator: Locator, attr: String): AutomationResult<String?> {
        val started = logger.timed()
        val script = JsExecutor.jsonResultScript(
            """
            var el = ${locator.toFinderJs()};
            if (!el) return fail("ELEMENT_NOT_FOUND", "selector not found");
            var value = el.getAttribute(${JsUtils.quote(attr)});
            return ok({value: value});
            """.trimIndent(),
        )
        val result = jsExecutor.evaluateObjectScript(script)
        val value = if (result.data?.isNull("value") == true) null else result.data?.optString("value")
        val duration = logger.elapsed(started)
        logger.action(
            action = "GET_ATTRIBUTE",
            selector = "${locator.description} @$attr",
            durationMs = duration,
            result = if (result.success) "SUCCESS" else result.code,
            resultCode = result.code,
            url = readUrl(),
            error = result.message.takeIf { !result.success },
        )
        return if (result.success) AutomationResult.ok(value, durationMs = duration)
        else AutomationResult.fail(result.code, result.message, durationMs = duration)
    }

    private suspend fun doClick(locator: Locator): AutomationResult<Unit> {
        val started = logger.timed()
        val script = JsExecutor.jsonResultScript(
            """
            var el = ${locator.toFinderJs()};
            if (!el) return fail("ELEMENT_NOT_FOUND", "selector not found");
            var style = window.getComputedStyle(el);
            var rect = el.getBoundingClientRect();
            var visible = style && style.display !== "none" && style.visibility !== "hidden" &&
              style.opacity !== "0" && rect.width > 0 && rect.height > 0;
            if (!visible) {
              return fail("ELEMENT_NOT_VISIBLE", "element is not visible", {
                display: style ? style.display : "",
                visibility: style ? style.visibility : ""
              });
            }
            if (el.disabled) return fail("ELEMENT_DISABLED", "element is disabled");
            if (typeof el.scrollIntoView === "function") {
              el.scrollIntoView({block:"center", inline:"center"});
            }
            if (typeof el.focus === "function") el.focus();
            el.click();
            return ok({tag: el.tagName || "", id: el.id || ""});
            """.trimIndent(),
        )
        val result = jsExecutor.evaluateObjectScript(script)
        val duration = logger.elapsed(started)
        logger.action(
            action = "CLICK",
            selector = locator.description,
            durationMs = duration,
            result = if (result.success) "SUCCESS" else result.code,
            resultCode = result.code,
            url = readUrl(),
            error = result.message.takeIf { !result.success },
        )
        return if (result.success) AutomationResult.ok(durationMs = duration)
        else AutomationResult.fail(result.code, result.message, durationMs = duration)
    }

    private suspend fun doFill(locator: Locator, value: String): AutomationResult<Unit> {
        val started = logger.timed()
        val script = JsExecutor.jsonResultScript(
            """
            var el = ${locator.toFinderJs()};
            if (!el) return fail("ELEMENT_NOT_FOUND", "selector not found");
            if (el.disabled || el.readOnly) return fail("ELEMENT_DISABLED", "element is disabled or readonly");
            var proto = (el.tagName === "TEXTAREA") ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
            var setter = Object.getOwnPropertyDescriptor(proto, "value");
            if (typeof el.focus === "function") el.focus();
            if (setter && setter.set) setter.set.call(el, ${JsUtils.quote(value)});
            else el.value = ${JsUtils.quote(value)};
            el.dispatchEvent(new Event("input", {bubbles:true}));
            el.dispatchEvent(new Event("change", {bubbles:true}));
            if (typeof el.blur === "function") el.blur();
            return ok({value: el.value || "", tag: el.tagName || "", id: el.id || ""});
            """.trimIndent(),
        )
        val result = jsExecutor.evaluateObjectScript(script)
        val duration = logger.elapsed(started)
        logger.action(
            action = "FILL",
            selector = locator.description,
            durationMs = duration,
            result = if (result.success) "SUCCESS" else result.code,
            resultCode = result.code,
            url = readUrl(),
            error = result.message.takeIf { !result.success },
        )
        return if (result.success) AutomationResult.ok(durationMs = duration)
        else AutomationResult.fail(result.code, result.message, durationMs = duration)
    }

    private suspend fun doWaitFor(locator: Locator, timeoutMs: Long): AutomationResult<Unit> {
        return waiter.retryUntilResult("waitForSelector ${locator.description}", timeoutMs) {
            val existing = doExists(locator, log = false)
            if (existing.data == true) AutomationResult.ok(true, "PASS waitForSelector")
            else AutomationResult.fail(ResultCode.ELEMENT_NOT_FOUND, existing.message)
        }.map { }
    }

    private suspend fun doWaitForText(text: String, timeoutMs: Long): AutomationResult<Unit> {
        return waiter.retryUntilResult("waitForText $text", timeoutMs) {
            val script = JsExecutor.jsonResultScript(
                """
                var body = document.body ? (document.body.innerText || document.body.textContent || "") : "";
                if (body.indexOf(${JsUtils.quote(text)}) >= 0) return ok({found:true});
                return fail("ELEMENT_NOT_FOUND", "text not found");
                """.trimIndent(),
            )
            val result = jsExecutor.evaluateObjectScript(script)
            if (result.success) AutomationResult.ok(true, "PASS waitForText")
            else AutomationResult.fail(result.code, result.message)
        }.map { }
    }

    private suspend fun collectEnvironment(): AutomationResult<BrowserEnvironmentInfo> {
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
        if (!result.success || result.data == null) {
            return AutomationResult.fail(result.code, result.message)
        }
        val json = result.data
        val pkg = withContext(Dispatchers.Main) {
            WebViewCompat.getCurrentWebViewPackage(webView.context)?.versionName ?: "unknown"
        }
        val info = BrowserEnvironmentInfo(
            userAgent = json.optString("userAgent"),
            innerWidth = json.optInt("innerWidth"),
            innerHeight = json.optInt("innerHeight"),
            screenWidth = json.optInt("screenWidth"),
            screenHeight = json.optInt("screenHeight"),
            devicePixelRatio = json.optDouble("devicePixelRatio"),
            maxTouchPoints = json.optInt("maxTouchPoints"),
            href = json.optString("href"),
            title = json.optString("title"),
            readyState = json.optString("readyState"),
            webViewVersion = pkg,
        )
        return AutomationResult.ok(info)
    }

    private suspend fun readUrl(): String = withContext(Dispatchers.Main) {
        webView.url ?: webViewManager.currentUrl
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return BrowserConfig.TEST_PAGE_URL
        if (trimmed.startsWith("http://") ||
            trimmed.startsWith("https://") ||
            trimmed.startsWith("file:") ||
            trimmed.startsWith("about:")
        ) {
            return trimmed
        }
        return "https://$trimmed"
    }
}
