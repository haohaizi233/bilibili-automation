package com.example.bilibiliautomation.data.api.session

import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.TimeMs
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Per-account CookieJar. Never share an instance across accounts.
 */
class AccountCookieJar(
    val accountId: Long,
    initial: List<SessionCookie> = emptyList(),
    private val logger: AutomationLogger = AutomationLogger,
) : CookieJar {
    private val stored = CopyOnWriteArrayList<SessionCookie>()

    init {
        stored.addAll(initial)
    }

    @Synchronized
    fun snapshot(): List<SessionCookie> = stored.toList()

    @Synchronized
    fun replaceAll(cookies: List<SessionCookie>) {
        stored.clear()
        stored.addAll(cookies)
        logMutation("replace", cookies.size)
    }

    @Synchronized
    fun clear() {
        stored.clear()
        logMutation("clear", 0)
    }

    @Synchronized
    fun merge(cookies: List<SessionCookie>) {
        if (cookies.isEmpty()) return
        cookies.forEach { incoming ->
            stored.removeAll { existing ->
                existing.name.equals(incoming.name, ignoreCase = true) &&
                    existing.domain.equals(incoming.domain, ignoreCase = true) &&
                    existing.path == incoming.path
            }
            stored.add(incoming)
        }
        logMutation("merge", stored.size)
    }

    val csrf: String?
        get() = snapshot().firstOrNull { it.name.equals("bili_jct", ignoreCase = true) }?.value

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(this) {
            cookies.forEach { incoming ->
                val mapped = incoming.toStored()
                stored.removeAll { existing ->
                    existing.name.equals(mapped.name, ignoreCase = true) &&
                        existing.domain.equals(mapped.domain, ignoreCase = true) &&
                        existing.path == mapped.path
                }
                stored.add(mapped)
            }
            logMutation("saveFromResponse", stored.size)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = TimeMs.wallClock()
        synchronized(this) {
            stored.removeAll { cookie ->
                val expires = cookie.expiresAt
                expires != null && expires > 0L && expires < now
            }
            return stored.mapNotNull { it.toOkHttp(url) }
        }
    }

    private fun logMutation(op: String, count: Int) {
        logger.d("cookieJar accountId=$accountId op=$op cookieCount=$count")
    }

    private fun Cookie.toStored(): SessionCookie = SessionCookie(
        name = name,
        value = value,
        domain = domain,
        path = path,
        secure = secure,
        httpOnly = httpOnly,
        expiresAt = if (persistent) expiresAt else null,
    )

    private fun SessionCookie.toOkHttp(url: HttpUrl): Cookie? {
        if (secure && !url.isHttps) return null
        val host = url.host
        if (!domainMatches(host, domain)) return null
        if (!url.encodedPath.startsWith(path.ifBlank { "/" })) return null
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .path(path.ifBlank { "/" })
        val cookieDomain = domain.removePrefix(".")
        if (cookieDomain.isNotBlank()) {
            builder.domain(cookieDomain)
        } else {
            builder.domain(host)
        }
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        expiresAt?.let { builder.expiresAt(it) }
        return runCatching { builder.build() }.getOrNull()
    }

    private fun domainMatches(host: String, cookieDomain: String): Boolean {
        val normalized = cookieDomain.removePrefix(".").lowercase()
        if (normalized.isBlank() || !normalized.contains('.')) return false
        val h = host.lowercase()
        return h == normalized || h.endsWith(".$normalized")
    }
}
