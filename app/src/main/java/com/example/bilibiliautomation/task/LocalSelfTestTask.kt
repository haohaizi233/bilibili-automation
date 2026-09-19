package com.example.bilibiliautomation.task

import com.example.bilibiliautomation.browser.BrowserConfig
import com.example.bilibiliautomation.domain.model.AutomationResult

object LocalSelfTestTask {
    fun create(): AutomationTask = AutomationTask(
        name = "local-test-html",
        steps = listOf(
            step("goto") { page ->
                page.goto(BrowserConfig.TEST_PAGE_URL)
            },
            step("waitForSelector") { page ->
                page.waitForSelector("#username")
            },
            step("fill") { page ->
                page.fill("#username", "hello")
            },
            step("click") { page ->
                page.click("#submit")
            },
            step("getText") { page ->
                val text = page.getText("#result")
                if (!text.success) text
                else if (text.data?.contains("hello") == true) AutomationResult.ok(text.data, "PASS getText")
                else AutomationResult.fail("ASSERT", "expected result to contain hello, got=${text.data}")
            },
            step("async click") { page ->
                page.click("#asyncButton")
            },
            step("async wait") { page ->
                page.waitForSelector("#async-result")
            },
            step("async getText") { page ->
                val text = page.getText("#async-result")
                if (!text.success) text
                else if (text.data?.contains("Ready") == true) AutomationResult.ok(text.data, "PASS async wait")
                else AutomationResult.fail("ASSERT", "expected Ready, got=${text.data}")
            },
        ),
    )

    private fun step(name: String, block: AutomationStep): AutomationTask.NamedStep =
        AutomationTask.NamedStep(name, block)
}
