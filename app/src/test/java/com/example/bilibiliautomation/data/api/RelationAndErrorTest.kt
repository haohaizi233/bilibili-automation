package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.domain.api.ApiError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationAndErrorTest {
    @Test
    fun mapsRiskAndRateLimit() {
        assertTrue(BiliErrorMapper.fromApiCode(-509, "频繁") is ApiError.RateLimited)
        assertTrue(BiliErrorMapper.fromApiCode(-101, "账号未登录") is ApiError.SessionExpired)
        assertTrue(BiliErrorMapper.fromApiCode(-352, "风控") is ApiError.WbiSignFailure)
        assertTrue(BiliErrorMapper.fromApiCode(0, "需要验证码") is ApiError.ApiCode)
        assertTrue(BiliErrorMapper.fromApiCode(1, "需要验证") is ApiError.VerificationRequired)
        assertTrue(BiliErrorMapper.fromHttp(412) is ApiError.RiskControl)
    }

    @Test
    fun dryRunFollowDoesNotRequireVerifiedEndpoint() = runBlocking {
        val client = RelationActionClient(ThirdPartyBiliApiProvider(enabled = false))
        val result = client.followUser(1L, 2L, dryRun = true)
        assertTrue(result.isOk)
        val live = client.followUser(1L, 2L, dryRun = false)
        assertTrue(live.errorOrNull() is ApiError.UnverifiedOperation)
    }

    @Test
    fun csrfMissingWhenNoBiliJct() {
        val jar = com.example.bilibiliautomation.data.api.session.AccountCookieJar(1L)
        val error = AuthenticatedRequestBuilder(jar).requireCsrf()
        assertTrue(error is ApiError.CsrfMissing)
    }
}
