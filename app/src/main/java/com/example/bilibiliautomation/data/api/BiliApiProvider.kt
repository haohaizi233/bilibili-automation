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
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.SessionState
import kotlinx.coroutines.flow.Flow

interface BiliApiProvider {
    suspend fun getLoginQr(accountId: Long): ApiResult<QrGenerateData>
    suspend fun pollLogin(accountId: Long, qrcodeKey: String): ApiResult<QrPollData>
    suspend fun checkSession(accountId: Long): ApiResult<SessionState>
    suspend fun getUserVideos(accountId: Long, uid: Long, page: Int, pageSize: Int): ApiResult<VideoPage>
    fun getAllUserVideos(accountId: Long, uid: Long, pageSize: Int = 30, maxPages: Int = 50): Flow<VideoItem>
    suspend fun getRootComments(accountId: Long, oid: Long, page: Int, pageSize: Int): ApiResult<CommentPage>
    suspend fun getReplies(accountId: Long, oid: Long, rootId: Long, page: Int, pageSize: Int): ApiResult<CommentPage>
    suspend fun getRelation(accountId: Long, uid: Long): ApiResult<RelationSnapshot>
    suspend fun getRelationStat(accountId: Long, uid: Long): ApiResult<RelationStat>
    suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean): ApiResult<Unit>
    suspend fun getNav(accountId: Long): ApiResult<NavUser>
    suspend fun getUserCard(accountId: Long, uid: Long): ApiResult<SpaceUser>
    suspend fun searchUsers(accountId: Long, keyword: String): ApiResult<List<SpaceUser>>
}
