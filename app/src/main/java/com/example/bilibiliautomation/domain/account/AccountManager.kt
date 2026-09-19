package com.example.bilibiliautomation.domain.account

import com.example.bilibiliautomation.browser.SessionManager
import com.example.bilibiliautomation.data.repository.AccountRepository
import com.example.bilibiliautomation.data.secure.SecureSessionStore
import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountStatus
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.model.SessionStatus
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.flow.Flow

class AccountManager(
    private val accounts: AccountRepository,
    private val sessionStore: SecureSessionStore,
    private val sessionManager: SessionManager,
    private val logger: AutomationLogger = AutomationLogger,
) {
    fun observeAccounts(): Flow<List<Account>> = accounts.observeAll()

    suspend fun addAccount(uid: String, nickname: String = ""): Account {
        val created = accounts.add(uid, nickname)
        logger.i("account added id=${created.id} uid=$uid")
        return created
    }

    suspend fun deleteAccount(id: Long) {
        val account = accounts.getById(id)
        account?.sessionProfileId?.let { sessionStore.delete(it) }
        if (sessionManager.currentAccountId == id) {
            sessionManager.clearCurrentSession()
        }
        accounts.delete(id)
        logger.i("account deleted id=$id")
    }

    suspend fun setCurrentAccount(id: Long): AutomationResult<Unit> {
        val account = accounts.getById(id)
            ?: return AutomationResult.fail(ResultCode.INVALID_ARGUMENT, "account not found")
        if (account.disabled || account.status == AccountStatus.DISABLED) {
            return AutomationResult.fail(ResultCode.ACCOUNT_RESTRICTED, "account disabled")
        }
        if (account.status == AccountStatus.COOLDOWN) {
            val until = account.cooldownUntil ?: 0L
            if (until > TimeMs.wallClock()) {
                return AutomationResult.fail(
                    ResultCode.RATE_LIMITED,
                    "account in cooldown until $until — waiting, not switching around the limit",
                )
            }
        }
        return sessionManager.switchAccount(id)
    }

    suspend fun validateCurrent(): SessionStatus = sessionManager.validateCurrentSession()

    suspend fun markCooldown(accountId: Long, code: String, message: String, minutes: Int) {
        val until = TimeMs.wallClock() + minutes * 60_000L
        val status = if (code == ResultCode.ACCOUNT_RESTRICTED) {
            AccountStatus.NEEDS_REVIEW
        } else {
            AccountStatus.COOLDOWN
        }
        accounts.markStatus(accountId, status, code, message, until)
        logger.w("account $accountId -> $status code=$code (no automatic account rotation)")
    }

    suspend fun markLoginRequired(accountId: Long, code: String, message: String) {
        val status = if (code == ResultCode.LOGIN_EXPIRED) AccountStatus.LOGIN_EXPIRED else AccountStatus.LOGIN_REQUIRED
        accounts.markStatus(accountId, status, code, message, null)
    }
}
