package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.AccountStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountPoolIdleTest {
    @Test
    fun cooledAccountBecomesIdleAfterCooldownExpires() {
        val account = sample(status = AccountStatus.COOLDOWN, cooldownUntil = 1_000L)
        assertEquals(500L, AccountPool.millisUntilIdle(account, now = 500L))
        assertFalse(AccountPool.isIdle(account, now = 500L))
        assertEquals(0L, AccountPool.millisUntilIdle(account, now = 1_000L))
        assertTrue(AccountPool.isIdle(account, now = 1_000L))
    }

    @Test
    fun loginExpiredAndDisabledNeverBecomeIdle() {
        assertNull(AccountPool.millisUntilIdle(sample(status = AccountStatus.LOGIN_EXPIRED), now = 0L))
        assertNull(AccountPool.millisUntilIdle(sample(status = AccountStatus.DISABLED), now = 0L))
        assertNull(AccountPool.millisUntilIdle(sample(status = AccountStatus.ACTIVE, disabled = true), now = 0L))
        assertNull(AccountPool.millisUntilIdle(sample(uid = "pending-1"), now = 0L))
    }

    @Test
    fun needsReviewIsIdleOnceCooldownClears() {
        val cooling = sample(status = AccountStatus.NEEDS_REVIEW, cooldownUntil = 2_000L)
        assertEquals(1_000L, AccountPool.millisUntilIdle(cooling, now = 1_000L))
        assertTrue(AccountPool.isIdle(cooling, now = 2_000L))
    }

    private fun sample(
        uid: String = "123",
        status: AccountStatus = AccountStatus.ACTIVE,
        cooldownUntil: Long? = null,
        disabled: Boolean = false,
    ): Account = Account(
        id = 1L,
        uid = uid,
        nickname = "up",
        avatarUrl = "",
        status = status,
        createdAt = 0L,
        updatedAt = 0L,
        lastLoginAt = null,
        lastValidatedAt = null,
        cooldownUntil = cooldownUntil,
        disabled = disabled,
        lastErrorCode = null,
        lastErrorMessage = null,
        sessionProfileId = null,
    )
}
