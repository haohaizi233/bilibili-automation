package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.api.session.ApiSessionManager
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.TimeMs

class AccountPool(
    private val accounts: AccountRepository,
    private val sessions: ApiSessionManager,
    private val logger: AutomationLogger = AutomationLogger,
) {
    suspend fun listIdle(): List<Account> {
        val now = TimeMs.wallClock()
        return accounts.getAll().filter { isIdle(it, now) }
    }

    suspend fun occupy(accountId: Long): Account? {
        val account = accounts.getById(accountId) ?: return null
        if (!isIdle(account)) return null
        if (account.status != AccountStatus.ACTIVE) {
            accounts.markStatus(accountId, AccountStatus.ACTIVE)
        }
        sessions.activate(account.id)
        return accounts.getById(accountId) ?: account.copy(status = AccountStatus.ACTIVE)
    }

    suspend fun pick(excluding: Long? = null): Account? {
        val chosen = listIdle().firstOrNull { it.id != excluding }
        if (chosen == null) {
            logger.w("account pool empty — NeedLogin")
            return null
        }
        return occupy(chosen.id)
    }

    suspend fun rotate(currentId: Long?): Account? = pick(excluding = currentId)

    suspend fun millisUntilNextIdle(excluding: Long? = null, now: Long = TimeMs.wallClock()): Long? {
        return accounts.getAll()
            .filter { it.id != excluding }
            .mapNotNull { millisUntilIdle(it, now) }
            .minOrNull()
    }

    suspend fun hasRecoverable(excluding: Long? = null): Boolean {
        return millisUntilNextIdle(excluding) != null
    }

    companion object {
        fun isIdle(account: Account, now: Long = TimeMs.wallClock()): Boolean {
            return millisUntilIdle(account, now) == 0L
        }

        fun millisUntilIdle(account: Account, now: Long = TimeMs.wallClock()): Long? {
            if (account.disabled) return null
            if (account.uid.startsWith("pending-")) return null
            if (account.status == AccountStatus.DISABLED ||
                account.status == AccountStatus.LOGIN_EXPIRED ||
                account.status == AccountStatus.LOGIN_REQUIRED
            ) {
                return null
            }
            val until = account.cooldownUntil ?: 0L
            return (until - now).coerceAtLeast(0L)
        }
    }
}
