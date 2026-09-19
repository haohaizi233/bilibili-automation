package com.example.bilibiliautomation

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestHtmlContractTest {
    @Test
    fun mockWorkflowDomExists() {
        val html = File("src/main/assets/test.html").readText()
        listOf(
            "id=\"username\"",
            "id=\"submit\"",
            "id=\"asyncButton\"",
            "id=\"session-status\"",
            "id=\"target-profile\"",
            "id=\"video-list\"",
            "class=\"video-item\"",
            "id=\"comment-list\"",
            "class=\"comment-item root\"",
            "id=\"user-profile\"",
            "id=\"relationship\"",
            "id=\"mock-action-btn\"",
            "id=\"rate-limit-banner\"",
            "id=\"account-restricted-banner\"",
            "id=\"back-to-comments\"",
        ).forEach { token ->
            assertTrue("missing $token", html.contains(token))
        }
    }
}
