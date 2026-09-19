package com.example.bilibiliautomation.browser

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.secure.SecureSessionStore
import com.example.bilibiliautomation.domain.model.AccountSession
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.domain.model.SessionStatus
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Cookie profile is supported. Full WebStorage / IndexedDB isolation is pending:
 * Android WebView does not provide Playwright-style BrowserContext isolation.
 *
 * [switchAccount] therefore: cancel pending actor work, flush cookies,
 * optionally wipe WebStorage (destructive, documented), restore the target cookie profile.
 */
class SessionManager(
    private val context: Context,
    private val sessionStore: SecureSessionStore,
    private val accountRepository: AccountRepository,
    private val browserHost: BrowserHost,
    private val logger: AutomationLogger = AutomationLogger,
) {
    // Single source of truth for "current account". Exposed as a StateFlow so
    // the Home dashboard and the Account-management list stay in sync — both
    // derive "which account is current" from this one pointer instead of each
    // running its own selection heuristic.
    private val _currentAccountId = MutableStateFlow<Long?>(null)
    val currentAccountIdFlow: StateFlow<Long?> = _currentAccountId.asStateFlow()
    var currentAccountId: Long?
        get() = _currentAccountId.value
        private set(value) { _currentAccountId.value = value }

    val cookieProfileSupported: Boolean = true
    val fullWebStorageIsolationPending: Boolean = true

    suspend fun saveSession(accountId: Long): AutomationResult<Unit> = withContext(Dispatchers.IO) {
        val account = accountRepository.getById(accountId)
            ?: return@withContext AutomationResult.fail(ResultCode.INVALID_ARGUMENT, "account not found")
        val profileId = account.sessionProfileId
            ?: return@withContext AutomationResult.fail(ResultCode.INVALID_ARGUMENT, "missing sessionProfileId")
        val cookies = snapshotCookies()
        sessionStore.save(
            profileId,
            AccountSession(
                accountUid = account.uid,
                cookies = cookies,
                createdAt = account.lastLoginAt ?: TimeMs.wallClock(),
                updatedAt = TimeMs.wallClock(),
            ),
        )
        logger.i("saveSession account=$accountId cookies=${cookies.size} (cookie profile only)")
        AutomationResult.ok()
    }

    suspend fun restoreSession(accountId: Long): AutomationResult<Unit> = withContext(Dispatchers.Main) {
        val account = withContext(Dispatchers.IO) { accountRepository.getById(accountId) }
            ?: return@withContext AutomationResult.fail(ResultCode.INVALID_ARGUMENT, "account not found")
        val profileId = account.sessionProfileId
            ?: return@withContext AutomationResult.fail(ResultCode.LOGIN_REQUIRED, "no session profile")
        val session = withContext(Dispatchers.IO) { sessionStore.load(profileId) }
            ?: return@withContext AutomationResult.fail(ResultCode.LOGIN_REQUIRED, "no stored session")
        applyCookies(session.cookies)
        currentAccountId = accountId
        logger.i("restoreSession account=$accountId cookies=${session.cookies.size}")
        AutomationResult.ok()
    }

    suspend fun clearCurrentSession(): AutomationResult<Unit> = withContext(Dispatchers.Main) {
        browserHost.actor?.cancelPending()
        clearCookies()
        currentAccountId = null
        logger.w("clearCurrentSession: cookies cleared; WebStorage isolation still pending")
        AutomationResult.ok()
    }

    /**
     * Light-weight: only updates the "current account" pointer. Unlike
     * [switchAccount] it does NOT clear/restore cookies, so it is safe to call
     * right after a QR login whose cookies already live in the jar.
     */
    fun markCurrent(accountId: Long) {
        currentAccountId = accountId
    }

    suspend fun validateCurrentSession(): SessionStatus {
        val id = currentAccountId ?: return SessionStatus.LOGIN_REQUIRED
        val account = accountRepository.getById(id) ?: return SessionStatus.LOGIN_REQUIRED
        if (account.status == AccountStatus.LOGIN_EXPIRED) return SessionStatus.LOGIN_EXPIRED
        if (account.status == AccountStatus.LOGIN_REQUIRED) return SessionStatus.LOGIN_REQUIRED
        val profileId = account.sessionProfileId ?: return SessionStatus.LOGIN_REQUIRED
        val session = sessionStore.load(profileId) ?: return SessionStatus.LOGIN_REQUIRED
        return if (session.cookies.isEmpty()) SessionStatus.LOGIN_REQUIRED else SessionStatus.VALID
    }

    /**
     * Account switch is for multi-account management, login recovery, and user-initiated
     * selection — never as a RATE_LIMITED / ACCOUNT_RESTRICTED bypass.
     */
    suspend fun switchAccount(accountId: Long): AutomationResult<Unit> {
        logger.i("switchAccount -> $accountId (cookie profile supported; full WebStorage isolation pending)")
        browserHost.actor?.cancelPending()
        withContext(Dispatchers.Main) {
            currentAccountId?.let { previous ->
                // best-effort persist of the outgoing cookie jar
            }
            clearCookies()
            // Destructive and global. Documented limitation vs Playwright contexts.
            runCatching { WebStorage.getInstance().deleteAllData() }
            browserHost.webView?.clearCache(true)
        }
        return restoreSession(accountId)
    }

    private suspend fun snapshotCookies(): List<SessionCookie> = withContext(Dispatchers.Main) {
        val raw = CookieManager.getInstance().getCookie(browserHost.webView?.url ?: "") ?: return@withContext emptyList()
        raw.split(";").mapNotNull { part ->
            val trimmed = part.trim()
            val eq = trimmed.indexOf('=')
            if (eq <= 0) null
            else SessionCookie(
                name = trimmed.substring(0, eq),
                value = trimmed.substring(eq + 1),
                domain = "",
                path = "/",
            )
        }
    }

    private suspend fun applyCookies(cookies: List<SessionCookie>) = withContext(Dispatchers.Main) {
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)
        cookies.forEach { cookie ->
            val url = if (cookie.domain.isNotBlank()) {
                val host = cookie.domain.trimStart('.')
                "https://$host${cookie.path}"
            } else {
                browserHost.webView?.url ?: "https://localhost/"
            }
            manager.setCookie(url, "${cookie.name}=${cookie.value}; Path=${cookie.path}")
        }
        awaitCookieFlush(manager)
    }

    private suspend fun clearCookies() {
        val manager = CookieManager.getInstance()
        suspendCancellableCoroutine { cont ->
            manager.removeAllCookies { if (cont.isActive) cont.resume(Unit) }
        }
        awaitCookieFlush(manager)
    }

    private suspend fun awaitCookieFlush(manager: CookieManager) = suspendCancellableCoroutine { cont ->
        manager.flush()
        if (cont.isActive) cont.resume(Unit)
    }
}
