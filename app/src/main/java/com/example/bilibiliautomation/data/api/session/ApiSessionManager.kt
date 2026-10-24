package com.example.bilibiliautomation.data.api.session

import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.secure.SecureSessionStore
import com.example.bilibiliautomation.domain.api.SessionState
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountSession
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.MediaUrls
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * HTTP multi-account session registry. Isolated CookieJars; never a process-wide jar.
 * WebView [com.example.bilibiliautomation.browser.SessionManager] stays for fallback only.
 */
class ApiSessionManager(
    private val accountRepository: AccountRepository,
    private val sessionStore: SecureSessionStore,
    private val logger: AutomationLogger = AutomationLogger,
) {
    private val mutex = Mutex()
    private val jars = ConcurrentHashMap<Long, AccountCookieJar>()
    private val sessions = ConcurrentHashMap<Long, AccountSession>()

    @Volatile
    var activeAccountId: Long? = null
        private set

    fun jar(accountId: Long): AccountCookieJar {
        return jars.getOrPut(accountId) { AccountCookieJar(accountId, logger = logger) }
    }

    fun get(accountId: Long): AccountSession? = sessions[accountId]

    fun current(): AccountSession? = activeAccountId?.let { sessions[it] }

    /**
     * Resolve an account whose CookieJar actually carries persisted cookies.
     * `jar(accountId)` alone returns an empty jar; only `activate()` / `loadJar()`
     * load cookies from the SecureSessionStore. Callers that need authenticated
     * requests (e.g. user-card / space-acc-info lookups) must go through here,
     * otherwise the request goes out anonymous and Bilibili returns blank `face`.
     *
     * Picks the most recently logged-in ACTIVE account (falls back to any non
     * LOGIN_REQUIRED non-disabled account) and activates it.
     */
    suspend fun ensureActiveAccountId(): Long? {
        activeAccountId?.let { return it }
        val now = TimeMs.wallClock()
        val all = accountRepository.getAll()
        val chosen = all
            .filter { !it.disabled && it.status == AccountStatus.ACTIVE && (it.cooldownUntil ?: 0L) <= now }
            .maxByOrNull { it.lastLoginAt ?: it.updatedAt }
            ?: all.firstOrNull { !it.disabled && it.status != AccountStatus.LOGIN_REQUIRED }
        if (chosen == null) {
            logger.w("apiSession ensureActiveAccountId: no usable account, requests will be anonymous")
            return null
        }
        activate(chosen.id)
        logger.i("apiSession ensureActiveAccountId auto-activated accountId=${chosen.id} cookieCount=${jar(chosen.id).snapshot().size}")
        return chosen.id
    }

    /**
     * Load persisted cookies into the jar for [accountId] WITHOUT changing
     * [activeAccountId]. Use this for background profile refresh of an account
     * that is not the active one, so its own cookies are sent on its behalf.
     * No-op if the jar is already loaded.
     */
    suspend fun loadJar(accountId: Long): AccountSession? = mutex.withLock {
        loadLocked(accountId)
    }

    suspend fun activate(accountId: Long): AccountSession? = mutex.withLock {
        val loaded = loadLocked(accountId)
        activeAccountId = accountId
        logger.i("apiSession activate accountId=$accountId cookieCount=${loaded?.cookies?.size ?: 0}")
        loaded
    }

    suspend fun invalidate(accountId: Long) = mutex.withLock {
        jars[accountId]?.clear()
        sessions.remove(accountId)
        val account = accountRepository.getById(accountId)
        account?.sessionProfileId?.let { sessionStore.delete(it) }
        if (activeAccountId == accountId) activeAccountId = null
        accountRepository.markStatus(accountId, AccountStatus.LOGIN_EXPIRED, "SESSION_EXPIRED", "invalidated")
        logger.i("apiSession invalidate accountId=$accountId")
    }

    suspend fun importCookies(accountId: Long, cookies: List<SessionCookie>) {
        mutex.withLock {
            jar(accountId).merge(cookies)
        }
        updateCookies(accountId)
    }

    suspend fun updateCookies(accountId: Long, uid: Long? = null, uname: String? = null, status: AccountStatus = AccountStatus.ACTIVE) {
        mutex.withLock {
            val jar = jar(accountId)
            val cookies = jar.snapshot()
            val now = TimeMs.wallClock()
            val previous = sessions[accountId]
            val session = AccountSession(
                accountUid = uid?.toString() ?: previous?.accountUid.orEmpty(),
                cookies = cookies,
                createdAt = previous?.createdAt ?: now,
                updatedAt = now,
                accountId = accountId,
                uid = uid ?: previous?.uid,
                uname = uname ?: previous?.uname,
                status = status,
            )
            sessions[accountId] = session
            persistLocked(accountId, session)
            logger.i(
                "apiSession updateCookies accountId=$accountId cookieCount=${cookies.size} status=$status",
            )
        }
    }

    /**
     * Decide the account status to apply after a successful re-login. A fresh
     * login normally clears everything to ACTIVE, but Bilibili风控 is
     * time-based and re-scanning does NOT bypass it: if the account is still
     * within its cooldown window we keep COOLDOWN/NEEDS_REVIEW so the UI does
     * not flash "正常" and then instantly flip back to "冷却中/异常" the moment
     * the next operation hits风控 again. Only LOGIN_EXPIRED/LOGIN_REQUIRED
     * (a real session death) is cleared by re-login.
     */
    private fun resolvePostLoginStatus(target: Account?): AccountStatus {
        val now = TimeMs.wallClock()
        val current = target ?: return AccountStatus.ACTIVE
        return when (current.status) {
            AccountStatus.COOLDOWN, AccountStatus.NEEDS_REVIEW ->
                if (current.cooldownUntil != null && current.cooldownUntil > now) current.status
                else AccountStatus.ACTIVE
            else -> AccountStatus.ACTIVE
        }
    }

    suspend fun validate(accountId: Long, state: SessionState) {
        when (state) {
            is SessionState.Valid -> {
                val realUid = state.uid.toString()
                val existing = accountRepository.getByUid(realUid)
                if (existing != null && existing.id != accountId) {
                    // Re-login of an already-stored Bilibili user through a fresh
                    // pending account. The pending row has a different id but the
                    // same uid would collide with accounts.uid UNIQUE. Merge:
                    // hand the freshly-saved session blob to the existing account,
                    // move the in-memory jar/session over, drop the pending
                    // duplicate, then validate the existing account.
                    val pending = accountRepository.getById(accountId)
                    if (pending != null && pending.sessionProfileId != null) {
                        accountRepository.updateSessionProfileId(existing.id, pending.sessionProfileId)
                    }
                    mergeSessionInto(accountId, existing.id)
                    accountRepository.delete(accountId)
                    val resolved = resolvePostLoginStatus(existing)
                    updateCookies(existing.id, uid = state.uid, uname = state.uname, status = resolved)
                    if (resolved == AccountStatus.ACTIVE) {
                        accountRepository.markValidated(existing.id, realUid, state.uname)
                    } else {
                        accountRepository.markRevalidated(existing.id, realUid, state.uname)
                    }
                    if (!state.face.isNullOrBlank()) {
                        accountRepository.updateProfile(
                            existing.id,
                            nickname = state.uname,
                            avatarUrl = MediaUrls.httpsFace(state.face) ?: state.face,
                        )
                    }
                    if (activeAccountId == accountId) activeAccountId = existing.id
                    logger.i("apiSession merged duplicate pending=$accountId into existing=${existing.id} uid=$realUid status=$resolved")
                } else {
                    val current = existing ?: accountRepository.getById(accountId)
                    val resolved = resolvePostLoginStatus(current)
                    updateCookies(accountId, uid = state.uid, uname = state.uname, status = resolved)
                    if (resolved == AccountStatus.ACTIVE) {
                        accountRepository.markValidated(accountId, realUid, state.uname)
                    } else {
                        accountRepository.markRevalidated(accountId, realUid, state.uname)
                    }
                    if (!state.face.isNullOrBlank()) {
                        accountRepository.updateProfile(
                            accountId,
                            nickname = state.uname,
                            avatarUrl = MediaUrls.httpsFace(state.face) ?: state.face,
                        )
                    }
                    logger.i("apiSession validated accountId=$accountId uid=$realUid status=$resolved")
                }
            }
            SessionState.Expired -> {
                accountRepository.markStatus(accountId, AccountStatus.LOGIN_EXPIRED, "LOGIN_EXPIRED", "expired")
            }
            SessionState.NotLoggedIn -> {
                accountRepository.markStatus(accountId, AccountStatus.LOGIN_REQUIRED, "LOGIN_REQUIRED", "not logged in")
            }
            SessionState.RiskControlled -> {
                accountRepository.markStatus(accountId, AccountStatus.NEEDS_REVIEW, "RISK", "risk control")
            }
            is SessionState.NetworkError, is SessionState.UnknownError -> Unit
        }
    }

    /**
     * Move the in-memory CookieJar + session snapshot from [fromId] onto [toId].
     * Called under the merge path; safe because the QR login coroutine is the
     * only writer for these ids at this point.
     */
    private fun mergeSessionInto(fromId: Long, toId: Long) {
        jars.remove(fromId)?.let { jars[toId] = it }
        sessions.remove(fromId)?.let { sessions[toId] = it.copy(accountId = toId) }
    }

    suspend fun rotate(): Account? {
        val all = accountRepository.getAll()
        val current = activeAccountId
        val next = all.firstOrNull { account ->
            account.id != current &&
                !account.disabled &&
                account.status == AccountStatus.ACTIVE &&
                (account.cooldownUntil ?: 0L) <= TimeMs.wallClock()
        } ?: return null
        activate(next.id)
        return next
    }

    private suspend fun loadLocked(accountId: Long): AccountSession? {
        sessions[accountId]?.let { return it }
        val account = accountRepository.getById(accountId) ?: return null
        val profileId = account.sessionProfileId ?: return null
        val stored = sessionStore.load(profileId) ?: return null
        val session = stored.copy(
            accountId = accountId,
            uid = stored.uid ?: stored.accountUid.toLongOrNull() ?: account.uid.toLongOrNull(),
            uname = stored.uname ?: account.nickname.ifBlank { null },
            status = account.status,
        )
        sessions[accountId] = session
        jar(accountId).replaceAll(session.cookies)
        return session
    }

    private suspend fun persistLocked(accountId: Long, session: AccountSession) {
        val account = accountRepository.getById(accountId) ?: return
        val profileId = account.sessionProfileId ?: return
        try {
            sessionStore.save(profileId, session)
        } catch (error: Exception) {
            // The in-memory session/jar is already updated, so the current
            // login still works. A persist failure (e.g. EncryptedSharedPreferences
            // throwing after a keystore reset) must not crash the process nor
            // fail an otherwise-successful login.
            logger.w("session persist failed accountId=$accountId ${error.javaClass.simpleName} ${error.message}")
        }
    }
}
