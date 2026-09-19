package com.example.bilibiliautomation.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestrictionPolicyTest {
    @Test
    fun rateLimitAndRestrictionRequireCooldownNotAccountHop() {
        val limited = AutomationResult.fail<Unit>(ResultCode.RATE_LIMITED, "slow down")
        val restricted = AutomationResult.fail<Unit>(ResultCode.ACCOUNT_RESTRICTED, "banned")
        assertTrue(limited.requiresCooldown)
        assertTrue(restricted.requiresCooldown)
        assertFalse(AutomationResult.ok<Unit>().requiresCooldown)
    }
}
