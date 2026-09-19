package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.domain.model.AutomationResult
import kotlinx.coroutines.CompletableDeferred

sealed interface BrowserCommand {
    val deferred: CompletableDeferred<AutomationResult<*>>
    val name: String
}

data class NavigateCommand(
    val url: String,
    val timeoutMs: Long?,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "goto"
}

data class ReloadCommand(
    val timeoutMs: Long?,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "reload"
}

data class ClickCommand(
    val locator: Locator,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "click"
}

data class FillCommand(
    val locator: Locator,
    val value: String,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "fill"
}

data class EvaluateCommand(
    val script: String,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "evaluate"
}

data class WaitForSelectorCommand(
    val locator: Locator,
    val timeoutMs: Long?,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "waitForSelector"
}

data class WaitForTextCommand(
    val text: String,
    val timeoutMs: Long?,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "waitForText"
}

data class GetTextCommand(
    val locator: Locator,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "getText"
}

data class GetAttributeCommand(
    val locator: Locator,
    val attr: String,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "getAttribute"
}

data class ExistsCommand(
    val locator: Locator,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "exists"
}

data class CurrentUrlCommand(
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
) : BrowserCommand {
    override val name: String = "currentUrl"
}

data class CustomCommand(
    override val name: String,
    override val deferred: CompletableDeferred<AutomationResult<*>> = CompletableDeferred(),
    val block: suspend () -> AutomationResult<*>,
) : BrowserCommand
