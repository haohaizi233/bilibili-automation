package com.example.bilibiliautomation.task

import com.example.bilibiliautomation.browser.Page
import com.example.bilibiliautomation.domain.model.AutomationResult

fun interface AutomationStep {
    suspend fun execute(page: Page): AutomationResult<*>
}
