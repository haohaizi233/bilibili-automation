package com.example.bilibiliautomation.data.api.dto

data class NavUser(
    val isLogin: Boolean,
    val mid: Long?,
    val uname: String?,
    val imgUrl: String?,
    val subUrl: String?,
    val face: String? = null,
    val following: Long = 0L,
)

data class QrGenerateData(
    val url: String,
    val qrcodeKey: String,
)

enum class QrPollCode {
    SUCCESS,
    WAITING_SCAN,
    SCANNED,
    EXPIRED,
    FAILED,
    UNKNOWN,
}

data class QrPollData(
    val pollCode: Int,
    val mapped: QrPollCode,
    val message: String,
    val redirectUrl: String?,
)

data class LoginCaptcha(
    val type: String,
    val token: String,
    val gt: String,
    val challenge: String,
)

data class SmsSendResult(
    val captchaKey: String,
)

data class SmsCountry(
    val cid: Int,
    val name: String,
    val countryId: String,
)

data class VideoItem(
    val aid: Long,
    val bvid: String,
    val title: String,
    val createdAt: Long,
    val durationSeconds: Int,
    val pic: String = "",
)

data class VideoPage(
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val items: List<VideoItem>,
    val hasMore: Boolean,
)

data class RootComment(
    val rpid: Long,
    val oid: Long,
    val authorUid: Long,
    val authorName: String,
    val message: String,
    val createdAt: Long,
)

data class CommentPage(
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val comments: List<RootComment>,
    val nextPage: Int?,
    val cursor: String?,
)

data class SpaceUser(
    val mid: Long,
    val name: String,
    val fans: Long,
    val face: String,
)

data class RelationSnapshot(
    val attribute: Int?,
    val beAttribute: Int?,
    val state: com.example.bilibiliautomation.domain.api.RelationState,
)

data class RelationStat(
    val mid: Long,
    val following: Long,
    val follower: Long,
)
