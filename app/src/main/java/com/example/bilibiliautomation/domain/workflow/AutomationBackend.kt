package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.data.api.dto.CommentPage
import com.example.bilibiliautomation.data.api.dto.RelationSnapshot
import com.example.bilibiliautomation.data.api.dto.VideoPage
import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.SessionState

interface AutomationBackend {
    val name: String
    suspend fun validateSession(accountId: Long): ApiResult<SessionState>
    suspend fun getUserVideos(accountId: Long, uid: Long, page: Int, pageSize: Int): ApiResult<VideoPage>
    suspend fun getRootComments(accountId: Long, oid: Long, page: Int, pageSize: Int): ApiResult<CommentPage>
    suspend fun getReplies(accountId: Long, oid: Long, rootId: Long, page: Int, pageSize: Int): ApiResult<CommentPage>
    suspend fun getRelation(accountId: Long, uid: Long): ApiResult<RelationSnapshot>
    suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean): ApiResult<Unit>
}

class ApiAutomationBackend(
    private val provider: com.example.bilibiliautomation.data.api.BiliApiProvider,
) : AutomationBackend {
    override val name: String = "API"

    override suspend fun validateSession(accountId: Long) = provider.checkSession(accountId)
    override suspend fun getUserVideos(accountId: Long, uid: Long, page: Int, pageSize: Int) =
        provider.getUserVideos(accountId, uid, page, pageSize)
    override suspend fun getRootComments(accountId: Long, oid: Long, page: Int, pageSize: Int) =
        provider.getRootComments(accountId, oid, page, pageSize)
    override suspend fun getReplies(accountId: Long, oid: Long, rootId: Long, page: Int, pageSize: Int) =
        provider.getReplies(accountId, oid, rootId, page, pageSize)
    override suspend fun getRelation(accountId: Long, uid: Long) = provider.getRelation(accountId, uid)
    override suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean) =
        provider.followUser(accountId, uid, dryRun)
}

class WebViewAutomationBackend : AutomationBackend {
    override val name: String = "WebViewFallback"
    private val unsupported = ApiError.UnverifiedOperation(
        "WebViewAutomationBackend",
        "WebView backend is fallback-only this round",
    )

    override suspend fun validateSession(accountId: Long): ApiResult<SessionState> =
        ApiResult.err(unsupported)
    override suspend fun getUserVideos(accountId: Long, uid: Long, page: Int, pageSize: Int): ApiResult<VideoPage> =
        ApiResult.err(unsupported)
    override suspend fun getRootComments(accountId: Long, oid: Long, page: Int, pageSize: Int): ApiResult<CommentPage> =
        ApiResult.err(unsupported)
    override suspend fun getReplies(accountId: Long, oid: Long, rootId: Long, page: Int, pageSize: Int): ApiResult<CommentPage> =
        ApiResult.err(unsupported)
    override suspend fun getRelation(accountId: Long, uid: Long): ApiResult<RelationSnapshot> =
        ApiResult.err(unsupported)
    override suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean): ApiResult<Unit> =
        ApiResult.err(unsupported)
}
