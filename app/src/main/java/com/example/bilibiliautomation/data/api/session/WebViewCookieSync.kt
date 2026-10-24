package com.example.bilibiliautomation.data.api.session

import android.webkit.CookieManager
import com.example.bilibiliautomation.domain.model.SessionCookie

/**
 * Bridges OkHttp per-account jars and the process [CookieManager] used by HITL WebViews.
 * Login WebViews must not auto-solve captchas; they only host the official page / GeeTest widget.
 */
object WebViewCookieSync {
    private val snapshotUrls = listOf(
        "https://www.bilibili.com/",
        "https://passport.bilibili.com/",
        "https://api.bilibili.com/",
    )

    fun replaceWith(cookies: List<SessionCookie>, then: () -> Unit) {
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)
        manager.removeAllCookies {
            cookies.forEach { cookie -> inject(manager, cookie) }
            manager.flush()
            then()
        }
    }

    fun snapshotBili(): List<SessionCookie> {
        val manager = CookieManager.getInstance()
        manager.flush()
        return snapshotUrls
            .flatMap { url ->
                WebViewCookieParser.parseHeader(
                    raw = manager.getCookie(url),
                    domain = ".bilibili.com",
                )
            }
            .distinctBy { it.name.lowercase() }
    }

    private fun inject(manager: CookieManager, cookie: SessionCookie) {
        val host = cookie.domain.trimStart('.').ifBlank { "bilibili.com" }
        val path = cookie.path.ifBlank { "/" }
        val url = "https://$host$path"
        val parts = buildList {
            add("${cookie.name}=${cookie.value}")
            add("Path=$path")
            add("Domain=.bilibili.com")
            if (cookie.secure) add("Secure")
        }
        manager.setCookie(url, parts.joinToString("; "))
        manager.setCookie("https://www.bilibili.com$path", "${cookie.name}=${cookie.value}; Path=$path; Domain=.bilibili.com")
        manager.setCookie("https://passport.bilibili.com$path", "${cookie.name}=${cookie.value}; Path=$path; Domain=.bilibili.com")
    }
}
