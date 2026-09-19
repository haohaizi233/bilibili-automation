package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.BrowserEnvironmentInfo

interface BrowserEngine {
    suspend fun goto(url: String): AutomationResult<Unit>

    suspend fun reload(): AutomationResult<Unit>

    suspend fun click(locator: Locator): AutomationResult<Unit>

    suspend fun fill(locator: Locator, value: String): AutomationResult<Unit>

    suspend fun exists(locator: Locator): Boolean

    suspend fun getText(locator: Locator): AutomationResult<String>

    suspend fun getAttribute(locator: Locator, name: String): AutomationResult<String?>

    suspend fun evaluate(script: String): AutomationResult<String?>

    suspend fun waitFor(locator: Locator, timeoutMs: Long? = null): AutomationResult<Unit>

    suspend fun waitForText(text: String, timeoutMs: Long? = null): AutomationResult<Unit>

    suspend fun currentUrl(): String?

    suspend fun environmentInfo(): BrowserEnvironmentInfo?
}
