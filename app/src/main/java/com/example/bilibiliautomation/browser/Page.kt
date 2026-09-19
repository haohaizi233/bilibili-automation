package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.automation.PageState
import com.example.bilibiliautomation.domain.model.AutomationResult
import org.json.JSONObject

/**
 * Playwright-style page facade. All operations go through [BrowserEngine] / [BrowserActor],
 * never through a raw WebView from business code.
 */
class Page(
    private val engine: BrowserEngine,
    internal val webViewManager: WebViewManager? = null,
) {
    val state: PageState get() = webViewManager?.pageState ?: PageState.READY

    suspend fun goto(url: String): AutomationResult<String> {
        val result = engine.goto(url)
        val current = engine.currentUrl().orEmpty()
        return if (result.success) AutomationResult.ok(current, result.message, result.code, result.durationMs)
        else AutomationResult.fail(result.code, result.message, current, result.durationMs)
    }

    suspend fun reload(): AutomationResult<String> {
        val result = engine.reload()
        val current = engine.currentUrl().orEmpty()
        return if (result.success) AutomationResult.ok(current, durationMs = result.durationMs)
        else AutomationResult.fail(result.code, result.message, current, result.durationMs)
    }

    suspend fun evaluate(js: String): AutomationResult<String> {
        val result = engine.evaluate(js)
        return if (result.success) AutomationResult.ok(result.data, result.message, result.code, result.durationMs)
        else AutomationResult.fail(result.code, result.message, result.data, result.durationMs)
    }

    suspend fun exists(selector: String): AutomationResult<Boolean> = exists(Locator.css(selector))

    suspend fun exists(locator: Locator, log: Boolean = true): AutomationResult<Boolean> {
        val found = engine.exists(locator)
        return AutomationResult.ok(found)
    }

    suspend fun getText(selector: String): AutomationResult<String> = getText(Locator.css(selector))

    suspend fun getText(locator: Locator): AutomationResult<String> = engine.getText(locator)

    suspend fun getAttribute(selector: String, attr: String): AutomationResult<String?> =
        getAttribute(Locator.css(selector), attr)

    suspend fun getAttribute(locator: Locator, attr: String): AutomationResult<String?> =
        engine.getAttribute(locator, attr)

    suspend fun click(selector: String): AutomationResult<JSONObject> = click(Locator.css(selector))

    suspend fun click(locator: Locator): AutomationResult<JSONObject> {
        val result = engine.click(locator)
        return if (result.success) AutomationResult.ok(JSONObject(), result.message, result.code, result.durationMs)
        else AutomationResult.fail(result.code, result.message, durationMs = result.durationMs)
    }

    suspend fun fill(selector: String, value: String): AutomationResult<JSONObject> =
        fill(Locator.css(selector), value)

    suspend fun fill(locator: Locator, value: String): AutomationResult<JSONObject> {
        val result = engine.fill(locator, value)
        return if (result.success) AutomationResult.ok(JSONObject(), result.message, result.code, result.durationMs)
        else AutomationResult.fail(result.code, result.message, durationMs = result.durationMs)
    }

    suspend fun waitForSelector(selector: String): AutomationResult<Boolean> =
        waitForSelector(Locator.css(selector))

    suspend fun waitForSelector(locator: Locator): AutomationResult<Boolean> {
        val result = engine.waitFor(locator)
        return if (result.success) AutomationResult.ok(true, "PASS waitForSelector", durationMs = result.durationMs)
        else AutomationResult.fail(result.code, result.message, durationMs = result.durationMs)
    }

    suspend fun waitForText(text: String): AutomationResult<Boolean> {
        val result = engine.waitForText(text)
        return if (result.success) AutomationResult.ok(true, "PASS waitForText", durationMs = result.durationMs)
        else AutomationResult.fail(result.code, result.message, durationMs = result.durationMs)
    }

    suspend fun currentUrl(): String = engine.currentUrl().orEmpty()
}
