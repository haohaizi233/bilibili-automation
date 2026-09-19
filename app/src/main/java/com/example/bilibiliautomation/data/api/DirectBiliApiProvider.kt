package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.data.api.dto.CommentPage
import com.example.bilibiliautomation.data.api.dto.NavUser
import com.example.bilibiliautomation.data.api.dto.QrGenerateData
import com.example.bilibiliautomation.data.api.dto.QrPollCode
import com.example.bilibiliautomation.data.api.dto.QrPollData
import com.example.bilibiliautomation.data.api.dto.RelationSnapshot
import com.example.bilibiliautomation.data.api.dto.RelationStat
import com.example.bilibiliautomation.data.api.dto.SpaceUser
import com.example.bilibiliautomation.data.api.dto.VideoItem
import com.example.bilibiliautomation.data.api.dto.VideoPage
import com.example.bilibiliautomation.data.api.session.AccountCookieJar
import com.example.bilibiliautomation.data.api.session.ApiSessionManager
import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.SessionState
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DirectBiliApiProvider(
    private val client: BiliApiClient,
    private val sessions: ApiSessionManager,
    private val logger: AutomationLogger = AutomationLogger,
) : BiliApiProvider {

    override suspend fun getNav(accountId: Long): ApiResult<NavUser> {
        val jar = sessions.jar(accountId)
        return client.get(jar, BiliApiEndpoints.NAV, emptyMap(), "NAV").map { envelope ->
            val nav = BiliJson.parseNav(envelope.data)
            client.ingestNavKeys(envelope.data)
            nav
        }
    }

    override suspend fun getLoginQr(accountId: Long): ApiResult<QrGenerateData> {
        val jar = sessions.jar(accountId)
        return when (val result = client.get(jar, BiliApiEndpoints.QR_GENERATE, emptyMap(), "QR_GENERATE")) {
            is ApiResult.Err -> ApiResult.err(result.error)
            is ApiResult.Ok -> {
                val parsed = BiliJson.parseQrGenerate(result.value.data)
                if (parsed == null) {
                    ApiResult.err(ApiError.InvalidResponse("qr payload missing url/key"))
                } else {
                    ApiResult.ok(parsed)
                }
            }
        }
    }

    override suspend fun pollLogin(accountId: Long, qrcodeKey: String): ApiResult<QrPollData> {
        val jar = sessions.jar(accountId)
        val result = client.get(
            jar,
            BiliApiEndpoints.QR_POLL,
            mapOf("qrcode_key" to qrcodeKey),
            "QR_POLL",
        )
        return result.map { envelope ->
            val poll = BiliJson.parseQrPoll(envelope.data)
            if (poll.mapped == QrPollCode.SUCCESS) {
                sessions.updateCookies(accountId)
                logger.i("qr login cookies captured accountId=$accountId cookieCount=${jar.snapshot().size}")
            }
            poll
        }
    }

    override suspend fun checkSession(accountId: Long): ApiResult<SessionState> {
        val hadCookies = sessions.jar(accountId).snapshot().isNotEmpty()
        return when (val nav = getNav(accountId)) {
            is ApiResult.Ok -> {
                val user = nav.value
                val state = when {
                    user.isLogin && user.mid != null -> SessionState.Valid(user.mid, user.uname.orEmpty(), user.face, user.following)
                    hadCookies -> SessionState.Expired
                    else -> SessionState.NotLoggedIn
                }
                sessions.validate(accountId, state)
                ApiResult.ok(state)
            }
            is ApiResult.Err -> {
                val mapped = when (val error = nav.error) {
                    is ApiError.SessionExpired -> if (hadCookies) SessionState.Expired else SessionState.NotLoggedIn
                    is ApiError.RiskControl, is ApiError.VerificationRequired -> SessionState.RiskControlled
                    is ApiError.Network, is ApiError.Timeout -> SessionState.NetworkError(error.message)
                    else -> SessionState.UnknownError(error.message)
                }
                sessions.validate(accountId, mapped)
                if (mapped is SessionState.NetworkError || mapped is SessionState.UnknownError) {
                    ApiResult.err(nav.error)
                } else {
                    ApiResult.ok(mapped)
                }
            }
        }
    }

    override suspend fun getUserVideos(accountId: Long, uid: Long, page: Int, pageSize: Int): ApiResult<VideoPage> {
        val jar = sessions.jar(accountId)
        val params = mapOf(
            "mid" to uid.toString(),
            "pn" to page.toString(),
            "ps" to pageSize.toString(),
            "order" to "pubdate",
        )
        return client.signedGet(jar, BiliApiEndpoints.USER_VIDEOS, params, "USER_VIDEOS").map { envelope ->
            BiliJson.parseVideoPage(envelope.data, page, pageSize)
        }
    }

    override fun getAllUserVideos(accountId: Long, uid: Long, pageSize: Int, maxPages: Int): Flow<VideoItem> = flow {
        var page = 1
        val cap = maxOf(1, maxPages)
        while (page <= cap) {
            currentCoroutineContext().ensureActive()
            when (val result = getUserVideos(accountId, uid, page, pageSize)) {
                is ApiResult.Err -> throw ApiFlowException(result.error)
                is ApiResult.Ok -> {
                    result.value.items.forEach { emit(it) }
                    if (!result.value.hasMore) break
                    page += 1
                }
            }
        }
    }

    override suspend fun getRootComments(accountId: Long, oid: Long, page: Int, pageSize: Int): ApiResult<CommentPage> {
        val jar = sessions.jar(accountId)
        val params = mapOf(
            "type" to "1",
            "oid" to oid.toString(),
            "pn" to page.toString(),
            "ps" to pageSize.toString(),
            "sort" to "2",
        )
        return client.get(jar, BiliApiEndpoints.ROOT_COMMENTS, params, "ROOT_COMMENTS").map { envelope ->
            BiliJson.parseCommentPage(envelope.data, page, pageSize)
        }
    }

    override suspend fun getReplies(accountId: Long, oid: Long, rootId: Long, page: Int, pageSize: Int): ApiResult<CommentPage> {
        val jar = sessions.jar(accountId)
        val params = mapOf(
            "type" to "1",
            "oid" to oid.toString(),
            "root" to rootId.toString(),
            "pn" to page.toString(),
            "ps" to pageSize.toString(),
        )
        return client.get(jar, BiliApiEndpoints.REPLY_COMMENTS, params, "REPLY_COMMENTS").map { envelope ->
            BiliJson.parseCommentPage(envelope.data, page, pageSize)
        }
    }

    override suspend fun getRelation(accountId: Long, uid: Long): ApiResult<RelationSnapshot> {
        val jar = sessions.jar(accountId)
        if (jar.snapshot().isEmpty()) return ApiResult.err(ApiError.SessionInvalid())
        val params = mapOf("mid" to uid.toString())
        return client.signedGet(jar, BiliApiEndpoints.RELATION_QUERY, params, "RELATION_QUERY").map { envelope ->
            BiliJson.parseRelation(envelope.data)
        }
    }

    override suspend fun getRelationStat(accountId: Long, uid: Long): ApiResult<RelationStat> {
        val jar = sessions.jar(accountId)
        if (jar.snapshot().isEmpty()) return ApiResult.err(ApiError.SessionInvalid())
        val params = mapOf("vmid" to uid.toString())
        return client.get(jar, BiliApiEndpoints.RELATION_STAT, params, "RELATION_STAT").map { envelope ->
            BiliJson.parseRelationStat(envelope.data) ?: RelationStat(mid = uid, following = 0L, follower = 0L)
        }
    }

    override suspend fun followUser(accountId: Long, uid: Long, dryRun: Boolean): ApiResult<Unit> {
        if (dryRun) {
            logger.i("[DRY_RUN] would follow uid=$uid accountId=$accountId")
            return ApiResult.ok(Unit)
        }
        if (!BiliApiEndpoints.RELATION_MODIFY_VERIFIED) {
            return ApiResult.err(
                ApiError.UnverifiedOperation(
                    "followUser",
                    "TODO_UNVERIFIED: relation modify endpoint/act not present in 参考 spider.py",
                ),
            )
        }
        val jar = sessions.jar(accountId)
        val auth = AuthenticatedRequestBuilder(jar)
        auth.requireCsrf()?.let { return ApiResult.err(it) }
        if (jar.snapshot().isEmpty()) return ApiResult.err(ApiError.SessionInvalid())
        val fields = mapOf(
            "fid" to uid.toString(),
            "act" to BiliApiEndpoints.RELATION_ACT_FOLLOW_UNVERIFIED.toString(),
            "re_src" to "11",
        )
        return client.postForm(
            jar = jar,
            url = BiliApiEndpoints.RELATION_MODIFY,
            fields = fields,
            logicalName = "RELATION_MODIFY",
            requireCsrf = true,
            requireSession = true,
        ).map { }
    }

    override suspend fun getUserCard(accountId: Long, uid: Long): ApiResult<SpaceUser> {
        val jar = sessions.jar(accountId)
        val params = mapOf(
            "mid" to uid.toString(),
            "photo" to "true",
        )
        return when (val result = client.get(jar, BiliApiEndpoints.USER_CARD, params, "USER_CARD")) {
            is ApiResult.Err -> ApiResult.err(result.error)
            is ApiResult.Ok -> {
                val parsed = BiliJson.parseUserCard(result.value.data)
                if (parsed == null) {
                    ApiResult.err(ApiError.InvalidResponse("user card missing"))
                } else {
                    val withFace = if (parsed.face.isBlank()) fillFaceFromSpace(jar, parsed) else parsed
                    logger.i("user card uid=$uid name=${withFace.name} fans=${withFace.fans} face=${withFace.face.isNotBlank()}")
                    ApiResult.ok(withFace)
                }
            }
        }
    }

    private suspend fun fillFaceFromSpace(
        jar: AccountCookieJar,
        user: SpaceUser,
    ): SpaceUser {
        val params = mapOf("mid" to user.mid.toString())
        val signed = client.signedGet(jar, BiliApiEndpoints.SPACE_ACC_INFO, params, "SPACE_ACC_INFO")
        val envelope = when (signed) {
            is ApiResult.Ok -> signed
            is ApiResult.Err -> client.get(jar, BiliApiEndpoints.SPACE_ACC_INFO_UNSIGNED, params, "SPACE_ACC_INFO")
        }
        val extra = envelope.getOrNull()?.let { BiliJson.parseSpaceAccInfo(it.data) }
        return if (extra != null && extra.face.isNotBlank()) user.copy(face = extra.face) else user
    }

    override suspend fun searchUsers(accountId: Long, keyword: String): ApiResult<List<SpaceUser>> {
        val jar = sessions.jar(accountId)
        val params = mapOf(
            "search_type" to "bili_user",
            "keyword" to keyword,
            "page" to "1",
        )
        val signed = client.signedGet(jar, BiliApiEndpoints.USER_SEARCH, params, "USER_SEARCH")
        val envelope = when (signed) {
            is ApiResult.Ok -> signed
            is ApiResult.Err -> client.get(jar, BiliApiEndpoints.USER_SEARCH_UNSIGNED, params, "USER_SEARCH")
        }
        return envelope.map { BiliJson.parseUserSearch(it.data) }
    }
}

class ApiFlowException(val error: ApiError) : RuntimeException(error.message)
