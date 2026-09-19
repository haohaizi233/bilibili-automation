package com.example.bilibiliautomation.browser

import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.task.TaskRunner

/**
 * Compatibility facade over [BrowserHost]. New code should depend on Host / Engine / Actor.
 */
class BrowserController(
    val host: BrowserHost,
    val logger: AutomationLogger = AutomationLogger,
) {
    val webView get() = host.webView
    val config get() = host.config
    val webViewManager get() = host.webViewManager
    val jsExecutor get() = host.jsExecutor
    val page get() = host.page ?: error("BrowserHost not created")
    val debug get() = host.debug ?: error("BrowserHost not created")
    val engine get() = host.engine ?: error("BrowserHost not created")
    val actor get() = host.actor ?: error("BrowserHost not created")
    val taskRunner: TaskRunner get() = TaskRunner(page, logger)

    fun start() {
        if (!host.isCreated) host.create()
        logger.i("BrowserController started")
    }

    fun pause() = host.pause()

    fun resume() = host.resume()

    fun release() = host.destroy()
}
