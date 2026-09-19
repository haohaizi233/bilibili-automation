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
        sessions.activate(account.id)
        return account
    }

    suspend fun pick(excluding: Long? = null): Account? {
        val chosen = listIdle().firstOrNull { it.id != excluding }
        if (chosen == null) {
            logger.w("account pool empty — NeedLogin")
        } else {
            sessions.activate(chosen.id)
        }
        return chosen
    }

    suspend fun rotate(currentId: Long?): Account? = pick(excluding = currentId)

    companion object {
        fun isIdle(account: Account, now: Long = TimeMs.wallClock()): Boolean {
            if (account.disabled) return false
            if (account.uid.startsWith("pending-")) return false
            if (account.status != AccountStatus.ACTIVE) return false
            return (account.cooldownUntil ?: 0L) <= now
        }
    }
}
