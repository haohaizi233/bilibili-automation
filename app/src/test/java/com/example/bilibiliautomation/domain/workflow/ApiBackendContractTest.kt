package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.data.api.dto.CommentPage
import com.example.bilibiliautomation.data.api.dto.RelationSnapshot
import com.example.bilibiliautomation.data.api.dto.RootComment
import com.example.bilibiliautomation.data.api.dto.VideoItem
import com.example.bilibiliautomation.data.api.dto.VideoPage
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.RelationState
import com.example.bilibiliautomation.domain.api.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class FakeApiBackend : AutomationBackend {
    override val name: String = "fake"
    var followCalls = AtomicInteger()
    override suspend fun validateSession(accountId: Long) = ApiResult.ok(SessionState.Valid(1L, "n"))
    override suspend fun getUserVideos(accountId: Long, uid: Long, page: Int, pageSize: Int) =
        ApiResult.ok(
            VideoPage(1, 30, 1, listOf(VideoItem(11L, "BV1", "t", 1L, 10)), false),
        )
    override suspend fun getRootComments(accountId: Long, oid: Long, page: Int, pageSize: Int) =
        ApiResult.ok(
            CommentPage(1, 20, 1, listOf(RootComment(1, oid, 99L, "u", "hi", 1L)), null, null),
        )
    override suspend fun getReplies(accountId: Long, oid: Long, rootId: Long, page: Int, pageSize: Int) =
        ApiResult.ok(CommentPage(page, pageSize, 0, emptyList(), null, null))
    override suspend fun getRelation(accountId: Long, uid: Long) =
        ApiResult.ok(RelationSnapshot(0, 0, RelationState.None))
    override suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean): ApiResult<Unit> {
        if (!dryRun) followCalls.incrementAndGet()
        return ApiResult.ok(Unit)
    }
}

class ApiBackendContractTest {
    @Test
    fun defaultBackendIsApi() {
        assertEquals("API", ApiAutomationBackend(com.example.bilibiliautomation.data.api.ThirdPartyBiliApiProvider()).name)
        assertEquals("WebViewFallback", WebViewAutomationBackend().name)
    }

    @Test
    fun dryRunDoesNotIncrementWriteCounter() {
        val backend = FakeApiBackend()
        kotlinx.coroutines.runBlocking {
            backend.followUser(1, 2, dryRun = true)
        }
        assertEquals(0, backend.followCalls.get())
        kotlinx.coroutines.runBlocking {
            backend.followUser(1, 2, dryRun = false)
        }
        assertEquals(1, backend.followCalls.get())
        assertTrue(RelationState.Following.isFollowing)
    }
}
