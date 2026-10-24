package com.example.bilibiliautomation.data.api.session

import com.example.bilibiliautomation.domain.model.SessionCookie

/**
 * Parses WebView [android.webkit.CookieManager] `name=value; …` headers into
 * [SessionCookie] rows. Pure so unit tests do not need a WebView.
 */
object WebViewCookieParser {
    fun parseHeader(raw: String?, domain: String, path: String = "/"): List<SessionCookie> {
        if (raw.isNullOrBlank()) return emptyList()
        val cookieDomain = domain.ifBlank { return emptyList() }
        return raw.split(';').mapNotNull { part ->
            val trimmed = part.trim()
            val eq = trimmed.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val name = trimmed.substring(0, eq).trim()
            val value = trimmed.substring(eq + 1).trim()
            if (name.isEmpty() || value.isEmpty()) return@mapNotNull null
            SessionCookie(
                name = name,
                value = value,
                domain = cookieDomain,
                path = path.ifBlank { "/" },
                secure = true,
                httpOnly = name.equals("SESSDATA", ignoreCase = true),
            )
        }
    }

    fun isLoginEstablished(cookies: List<SessionCookie>): Boolean {
        val names = cookies.associate { it.name.lowercase() to it.value }
        val sessdata = names["sessdata"].orEmpty()
        val csrf = names["bili_jct"].orEmpty()
        return sessdata.isNotBlank() && csrf.isNotBlank()
    }
}
