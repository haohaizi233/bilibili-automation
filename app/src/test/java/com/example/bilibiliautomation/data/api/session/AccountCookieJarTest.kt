package com.example.bilibiliautomation.data.api.session

import com.example.bilibiliautomation.domain.model.SessionCookie
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCookieJarTest {
    @Test
    fun accountsDoNotShareCookies() {
        val one = AccountCookieJar(1L)
        val two = AccountCookieJar(2L)
        val url = "https://api.bilibili.com/x/web-interface/nav".toHttpUrl()
        val cookie = Cookie.Builder().domain("bilibili.com").path("/").name("SESSDATA").value("secret-one").build()
        one.saveFromResponse(url, listOf(cookie))
        assertEquals(1, one.snapshot().size)
        assertTrue(two.snapshot().isEmpty())
        assertEquals("SESSDATA", one.loadForRequest(url).single().name)
        assertTrue(two.loadForRequest(url).isEmpty())
    }

    @Test
    fun extractsCsrfWithoutLoggingValue() {
        val jar = AccountCookieJar(
            3L,
            listOf(SessionCookie("bili_jct", "csrf-token", ".bilibili.com")),
        )
        assertEquals("csrf-token", jar.csrf)
        assertEquals(listOf("bili_jct"), jar.snapshot().map { it.name })
    }
}
