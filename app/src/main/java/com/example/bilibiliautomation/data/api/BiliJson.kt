package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.data.api.dto.CommentPage
import com.example.bilibiliautomation.data.api.dto.NavUser
import com.example.bilibiliautomation.data.api.dto.QrGenerateData
import com.example.bilibiliautomation.data.api.dto.QrPollCode
import com.example.bilibiliautomation.data.api.dto.QrPollData
import com.example.bilibiliautomation.data.api.dto.RelationSnapshot
import com.example.bilibiliautomation.data.api.dto.RelationStat
import com.example.bilibiliautomation.data.api.dto.RootComment
import com.example.bilibiliautomation.data.api.dto.SpaceUser
import com.example.bilibiliautomation.data.api.dto.VideoItem
import com.example.bilibiliautomation.data.api.dto.VideoPage
import com.example.bilibiliautomation.domain.api.BiliEnvelope
import com.example.bilibiliautomation.domain.api.RelationState
import com.example.bilibiliautomation.util.MediaUrls
import org.json.JSONArray
import org.json.JSONObject

object BiliJson {
    fun envelope(raw: String): BiliEnvelope<JSONObject> {
        val obj = JSONObject(raw)
        val code = obj.optInt("code", Int.MIN_VALUE)
        val message = obj.optString("message").ifBlank { obj.optString("msg") }
        val ttl = if (obj.has("ttl")) obj.optInt("ttl") else null
        val data = when {
            obj.isNull("data") -> null
            obj.optJSONObject("data") != null -> obj.optJSONObject("data")
            else -> JSONObject().put("value", obj.opt("data"))
        }
        return BiliEnvelope(code = code, message = message, ttl = ttl, data = data)
    }

    fun parseNav(data: JSONObject?): NavUser {
        val obj = data ?: JSONObject()
        val mid = obj.optLong("mid", 0L).takeIf { obj.has("mid") && it != 0L }
        val wbi = obj.optJSONObject("wbi_img")
        return NavUser(
            isLogin = obj.optBoolean("isLogin", false),
            mid = mid,
            uname = obj.optString("uname").takeIf { it.isNotBlank() },
            imgUrl = wbi?.optString("img_url")?.takeIf { it.isNotBlank() },
            subUrl = wbi?.optString("sub_url")?.takeIf { it.isNotBlank() },
            face = obj.optString("face").takeIf { it.isNotBlank() },
            following = obj.optLong("following", 0L),
        )
    }

    fun parseQrGenerate(data: JSONObject?): QrGenerateData? {
        val obj = data ?: return null
        val url = obj.optString("url")
        val key = obj.optString("qrcode_key")
        if (url.isBlank() || key.isBlank()) return null
        return QrGenerateData(url = url, qrcodeKey = key)
    }

    fun parseQrPoll(data: JSONObject?): QrPollData {
        val obj = data ?: JSONObject()
        val code = obj.optInt("code", -1)
        val mapped = when (code) {
            0 -> QrPollCode.SUCCESS
            86101 -> QrPollCode.WAITING_SCAN
            86090 -> QrPollCode.SCANNED
            86038 -> QrPollCode.EXPIRED
            else -> QrPollCode.UNKNOWN
        }
        return QrPollData(
            pollCode = code,
            mapped = mapped,
            message = obj.optString("message"),
            redirectUrl = obj.optString("url").takeIf { it.isNotBlank() },
        )
    }

    fun parseVideoPage(data: JSONObject?, page: Int, pageSize: Int): VideoPage {
        val list = data?.optJSONObject("list")
        val vlist = list?.optJSONArray("vlist") ?: JSONArray()
        val pageInfo = data?.optJSONObject("page")
        val total = pageInfo?.optInt("count") ?: vlist.length()
        val items = buildList {
            for (i in 0 until vlist.length()) {
                val item = vlist.optJSONObject(i) ?: continue
                val bvid = item.optString("bvid")
                if (bvid.isBlank()) continue
                add(
                    VideoItem(
                        aid = item.optLong("aid"),
                        bvid = bvid,
                        title = item.optString("title"),
                        createdAt = item.optLong("created") * 1000L,
                        durationSeconds = parseDuration(item.optString("length")),
                        pic = item.optString("pic"),
                    ),
                )
            }
        }
        val hasMore = page * pageSize < total && items.isNotEmpty()
        return VideoPage(page = page, pageSize = pageSize, total = total, items = items, hasMore = hasMore)
    }

    fun parseCommentPage(data: JSONObject?, page: Int, pageSize: Int): CommentPage {
        val replies = data?.optJSONArray("replies") ?: JSONArray()
        val pageObj = data?.optJSONObject("page")
        val cursor = data?.optJSONObject("cursor")
        val total = pageObj?.optInt("count") ?: cursor?.optInt("all_count") ?: replies.length()
        val comments = buildList {
            for (i in 0 until replies.length()) {
                val item = replies.optJSONObject(i) ?: continue
                val member = item.optJSONObject("member")
                val content = item.optJSONObject("content")
                val rpid = item.optLong("rpid")
                val uid = member?.optLong("mid") ?: item.optLong("mid")
                if (rpid == 0L || uid == 0L) continue
                add(
                    RootComment(
                        rpid = rpid,
                        oid = item.optLong("oid"),
                        authorUid = uid,
                        authorName = member?.optString("uname").orEmpty(),
                        message = content?.optString("message").orEmpty(),
                        createdAt = item.optLong("ctime") * 1000L,
                    ),
                )
            }
        }
        val next = if (page * pageSize < total && comments.isNotEmpty()) page + 1 else null
        val cursorNext = cursor?.optString("pagination_reply")?.takeIf { it.isNotBlank() }
            ?: cursor?.optString("next")?.takeIf { it.isNotBlank() }
        return CommentPage(
            page = page,
            pageSize = pageSize,
            total = total,
            comments = comments,
            nextPage = next,
            cursor = cursorNext,
        )
    }

    fun parseRelation(data: JSONObject?): RelationSnapshot {
        val relation = data?.optJSONObject("relation") ?: data
        val be = data?.optJSONObject("be_relation")
        val attribute = if (relation?.has("attribute") == true) relation.optInt("attribute") else null
        val beAttribute = if (be?.has("attribute") == true) be.optInt("attribute") else null
        val state = when {
            RelationState.fromAttribute(attribute) == RelationState.Mutual -> RelationState.Mutual
            RelationState.fromAttribute(attribute) == RelationState.Following -> RelationState.Following
            beAttribute == 2 || beAttribute == 6 -> RelationState.Follower
            attribute == 0 -> RelationState.None
            else -> RelationState.fromAttribute(attribute)
        }
        return RelationSnapshot(attribute = attribute, beAttribute = beAttribute, state = state)
    }

    fun parseRelationStat(data: JSONObject?): RelationStat? {
        val obj = data ?: return null
        val mid = if (obj.has("mid")) obj.optLong("mid") else 0L
        val following = obj.optLong("following", 0L)
        val follower = obj.optLong("follower", 0L)
        return RelationStat(mid = mid, following = following, follower = follower)
    }

    fun parseUserCard(data: JSONObject?): SpaceUser? {
        val card = data?.optJSONObject("card") ?: return null
        val mid = parseMid(card) ?: return null
        val fans = when {
            data.has("follower") && !data.isNull("follower") -> data.optLong("follower")
            else -> card.optLong("fans")
        }
        return SpaceUser(
            mid = mid,
            name = cleanString(card, "name").ifBlank { cleanString(card, "uname") },
            fans = fans,
            face = firstFace(card, data),
        )
    }

    fun parseSpaceAccInfo(data: JSONObject?): SpaceUser? {
        val obj = data ?: return null
        val mid = parseMid(obj) ?: return null
        return SpaceUser(
            mid = mid,
            name = cleanString(obj, "name").ifBlank { cleanString(obj, "uname") },
            fans = obj.optLong("follower").takeIf { it > 0L } ?: obj.optLong("fans"),
            face = firstFace(obj),
        )
    }

    fun parseUserSearch(data: JSONObject?): List<SpaceUser> {
        val result = data?.optJSONArray("result") ?: JSONArray()
        return buildList {
            for (i in 0 until result.length()) {
                val item = result.optJSONObject(i) ?: continue
                val mid = item.optLong("mid").takeIf { it != 0L }
                    ?: item.optString("mid").toLongOrNull()
                    ?: continue
                add(
                    SpaceUser(
                        mid = mid,
                        name = item.optString("uname").ifBlank { item.optString("name") },
                        fans = item.optLong("fans"),
                        face = firstFace(item),
                    ),
                )
            }
        }
    }

    private fun parseMid(obj: JSONObject): Long? {
        return obj.optLong("mid").takeIf { it != 0L }
            ?: cleanString(obj, "mid").toLongOrNull()
    }

    private fun firstFace(vararg objs: JSONObject?): String {
        val keys = listOf("face", "avatar", "upic")
        for (obj in objs) {
            if (obj == null) continue
            for (key in keys) {
                val value = MediaUrls.httpsFace(cleanString(obj, key))
                if (!value.isNullOrBlank()) return value
            }
        }
        return ""
    }

    private fun cleanString(obj: JSONObject, key: String): String {
        if (!obj.has(key) || obj.isNull(key)) return ""
        val text = obj.opt(key)?.toString()?.trim().orEmpty()
        return if (text.isEmpty() || text.equals("null", ignoreCase = true)) "" else text
    }

    private fun parseDuration(raw: String): Int {
        if (raw.isBlank()) return 0
        val parts = raw.split(':').mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0
        }
    }
}
