package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.data.api.dto.CommentPage
import com.example.bilibiliautomation.data.api.dto.NavUser
import com.example.bilibiliautomation.data.api.dto.QrGenerateData
import com.example.bilibiliautomation.data.api.dto.QrPollData
import com.example.bilibiliautomation.data.api.dto.RelationSnapshot
import com.example.bilibiliautomation.data.api.dto.RelationStat
import com.example.bilibiliautomation.data.api.dto.VideoItem
import com.example.bilibiliautomation.data.api.dto.SpaceUser
import com.example.bilibiliautomation.data.api.dto.VideoPage
import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Optional aggregator for public data only.
 * Must never receive Cookie / csrf / full login session.
 * UI should label any future Cookie-upload option as Sensitive credential delegation.
 */
class ThirdPartyBiliApiProvider(
    private val enabled: Boolean = false,
) : BiliApiProvider {
    private val disabled: ApiError = ApiError.UnverifiedOperation(
        "ThirdPartyBiliApiProvider",
        "third-party provider is not configured; cookies are never uploaded",
    )

    override suspend fun getLoginQr(accountId: Long) = err<QrGenerateData>()
    override suspend fun pollLogin(accountId: Long, qrcodeKey: String) = err<QrPollData>()
    override suspend fun checkSession(accountId: Long) = err<SessionState>()
    override suspend fun getUserVideos(accountId: Long, uid: Long, page: Int, pageSize: Int) = err<VideoPage>()
    override fun getAllUserVideos(accountId: Long, uid: Long, pageSize: Int, maxPages: Int): Flow<VideoItem> = flow {
        throw ApiFlowException(disabled)
    }
    override suspend fun getRootComments(accountId: Long, oid: Long, page: Int, pageSize: Int) = err<CommentPage>()
    override suspend fun getReplies(accountId: Long, oid: Long, rootId: Long, page: Int, pageSize: Int) = err<CommentPage>()
    override suspend fun getRelation(accountId: Long, uid: Long) = err<RelationSnapshot>()
    override suspend fun getRelationStat(accountId: Long, uid: Long) = err<RelationStat>()
    override suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean) = err<Unit>()
    override suspend fun getNav(accountId: Long) = err<NavUser>()
    override suspend fun getUserCard(accountId: Long, uid: Long) = err<SpaceUser>()
    override suspend fun searchUsers(accountId: Long, keyword: String) = err<List<SpaceUser>>()

    private fun <T> err(): ApiResult<T> = if (enabled) {
        ApiResult.err(ApiError.UnverifiedOperation("thirdParty", "not implemented"))
    } else {
        ApiResult.err(disabled)
    }
}
