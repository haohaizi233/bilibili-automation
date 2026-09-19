package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.browser.BrowserEngine
import com.example.bilibiliautomation.browser.Locator
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.LoadedComment
import com.example.bilibiliautomation.domain.model.LoadedVideo
import com.example.bilibiliautomation.domain.model.PageType
import com.example.bilibiliautomation.domain.model.RelationshipState
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.model.SessionStatus
import com.example.bilibiliautomation.domain.model.Target
import org.json.JSONArray
import org.json.JSONObject

/**
 * Drives the local `assets/test.html` mock site. Selectors are for the test page only.
 */
class MockSiteAdapter(
    private val engine: BrowserEngine,
) : SiteAdapter {
    override suspend fun detectPage(): PageType {
        val attr = engine.getAttribute(Locator.css("#page-type"), "data-page")
        return when (attr.data?.lowercase()) {
            "login" -> PageType.LOGIN
            "target" -> PageType.TARGET
            "video-list" -> PageType.VIDEO_LIST
            "video" -> PageType.VIDEO
            "comments" -> PageType.COMMENTS
            "user" -> PageType.USER_PROFILE
            "rate-limited" -> PageType.RATE_LIMITED
            "restricted" -> PageType.ACCOUNT_RESTRICTED
            else -> PageType.UNKNOWN
        }
    }

    override suspend fun validateSession(): SessionStatus {
        val status = engine.getAttribute(Locator.css("#session-status"), "data-status")
        return when (status.data?.lowercase()) {
            "ok", "valid" -> SessionStatus.VALID
            "expired" -> SessionStatus.LOGIN_EXPIRED
            "required" -> SessionStatus.LOGIN_REQUIRED
            else -> SessionStatus.UNKNOWN
        }
    }

    override suspend fun openTarget(target: Target): AutomationResult<Unit> {
        val result = engine.click(Locator.css("#open-target"))
        if (!result.success) return result
        return engine.waitFor(Locator.css("#target-profile"))
    }

    override suspend fun loadVideos(): AutomationResult<List<LoadedVideo>> {
        val raw = engine.evaluate(
            """
            (function(){
              var items = document.querySelectorAll('.video-item');
              var out = [];
              for (var i = 0; i < items.length; i++) {
                out.push({bvid: items[i].getAttribute('data-bvid') || ('bv'+i), title: items[i].textContent || ''});
              }
              return JSON.stringify(out);
            })()
            """.trimIndent(),
        )
        if (!raw.success) return AutomationResult.fail(raw.code, raw.message)
        val list = parseVideos(raw.data)
        return AutomationResult.ok(list)
    }

    override suspend fun openVideo(bvid: String): AutomationResult<Unit> {
        val result = engine.click(Locator.css(".video-item[data-bvid=\"$bvid\"]"))
        if (!result.success) return result
        return engine.waitFor(Locator.css("#comment-list"))
    }

    override suspend fun loadComments(): AutomationResult<List<LoadedComment>> {
        val banner = engine.getAttribute(Locator.css("#rate-limit-banner"), "data-active")
        if (banner.data == "true") {
            return AutomationResult.fail(ResultCode.RATE_LIMITED, "mock rate limit banner")
        }
        val restricted = engine.getAttribute(Locator.css("#account-restricted-banner"), "data-active")
        if (restricted.data == "true") {
            return AutomationResult.fail(ResultCode.ACCOUNT_RESTRICTED, "mock account restricted")
        }
        val raw = engine.evaluate(
            """
            (function(){
              var items = document.querySelectorAll('.comment-item');
              var out = [];
              for (var i = 0; i < items.length; i++) {
                var el = items[i];
                out.push({
                  commentId: el.getAttribute('data-comment-id') || ('c'+i),
                  userUid: el.getAttribute('data-uid') || '',
                  isRootComment: el.classList.contains('root'),
                  preview: (el.textContent || '').trim()
                });
              }
              return JSON.stringify(out);
            })()
            """.trimIndent(),
        )
        if (!raw.success) return AutomationResult.fail(raw.code, raw.message)
        return AutomationResult.ok(parseComments(raw.data))
    }

    override suspend fun openUser(uid: String): AutomationResult<Unit> {
        val result = engine.click(Locator.css(".comment-item[data-uid=\"$uid\"] .user-link, .comment-item[data-uid=\"$uid\"]"))
        if (!result.success) {
            val fallback = engine.click(Locator.css("#open-user"))
            if (!fallback.success) return fallback
        }
        return engine.waitFor(Locator.css("#user-profile"))
    }

    override suspend fun detectRelationship(): RelationshipState {
        val value = engine.getAttribute(Locator.css("#relationship"), "data-relationship")
        return when (value.data?.uppercase()) {
            "NOT_FOLLOWING" -> RelationshipState.NOT_FOLLOWING
            "FOLLOWING" -> RelationshipState.FOLLOWING
            "MUTUAL" -> RelationshipState.MUTUAL
            "SELF" -> RelationshipState.SELF
            else -> RelationshipState.UNKNOWN
        }
    }

    override suspend fun performMockAction(): AutomationResult<Unit> {
        return engine.click(Locator.css("#mock-action-btn"))
    }

    override suspend fun returnToComments(): AutomationResult<Unit> {
        val click = engine.click(Locator.css("#back-to-comments"))
        if (!click.success) return click
        return engine.waitFor(Locator.css("#comment-list"))
    }

    private fun parseVideos(raw: String?): List<LoadedVideo> {
        val text = raw ?: return emptyList()
        val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(LoadedVideo(obj.optString("bvid"), obj.optString("title")))
            }
        }
    }

    private fun parseComments(raw: String?): List<LoadedComment> {
        val text = raw ?: return emptyList()
        val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj: JSONObject = array.getJSONObject(i)
                add(
                    LoadedComment(
                        commentId = obj.optString("commentId"),
                        userUid = obj.optString("userUid"),
                        isRootComment = obj.optBoolean("isRootComment", true),
                        preview = obj.optString("preview"),
                    ),
                )
            }
        }
    }
}
