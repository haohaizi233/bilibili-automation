package com.example.bilibiliautomation.data.api.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewCookieParserTest {
    @Test
    fun parsesNameValuePairsAndDetectsLogin() {
        val cookies = WebViewCookieParser.parseHeader(
            raw = "SESSDATA=abc123; bili_jct=csrf; DedeUserID=88",
            domain = ".bilibili.com",
        )
        assertEquals(3, cookies.size)
        assertTrue(WebViewCookieParser.isLoginEstablished(cookies))
        assertTrue(cookies.first { it.name == "SESSDATA" }.httpOnly)
    }

    @Test
    fun missingCsrfIsNotLoggedIn() {
        val cookies = WebViewCookieParser.parseHeader(
            raw = "SESSDATA=abc123; buvid3=x",
            domain = ".bilibili.com",
        )
        assertFalse(WebViewCookieParser.isLoginEstablished(cookies))
    }

    @Test
    fun blankHeaderIsEmpty() {
        assertTrue(WebViewCookieParser.parseHeader(null, ".bilibili.com").isEmpty())
        assertTrue(WebViewCookieParser.parseHeader("   ", ".bilibili.com").isEmpty())
    }
}
