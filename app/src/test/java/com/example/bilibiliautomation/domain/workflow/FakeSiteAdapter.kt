package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.LoadedComment
import com.example.bilibiliautomation.domain.model.LoadedVideo
import com.example.bilibiliautomation.domain.model.PageType
import com.example.bilibiliautomation.domain.model.RelationshipState
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.model.SessionStatus
import com.example.bilibiliautomation.domain.model.Target

class FakeSiteAdapter(
    private var session: SessionStatus = SessionStatus.VALID,
    private var videos: List<LoadedVideo> = listOf(
        LoadedVideo("BV1mock01", "Video One"),
        LoadedVideo("BV1mock02", "Video Two"),
    ),
    private var comments: List<LoadedComment> = listOf(
        LoadedComment("c1", "30001", true, "root one"),
        LoadedComment("c2", "30002", true, "root two"),
        LoadedComment("c1-reply", "30003", false, "reply"),
    ),
    private var relationship: RelationshipState = RelationshipState.NOT_FOLLOWING,
    var restrictCode: String? = null,
    val actions: MutableList<String> = mutableListOf(),
) : SiteAdapter {
    override suspend fun detectPage(): PageType = PageType.TARGET

    override suspend fun validateSession(): SessionStatus {
        actions += "VALIDATE_ACCOUNT"
        return session
    }

    override suspend fun openTarget(target: Target): AutomationResult<Unit> {
        actions += "OPEN_TARGET"
        return AutomationResult.ok()
    }

    override suspend fun loadVideos(): AutomationResult<List<LoadedVideo>> {
        actions += "LOAD_VIDEOS"
        return AutomationResult.ok(videos)
    }

    override suspend fun openVideo(bvid: String): AutomationResult<Unit> {
        actions += "OPEN_VIDEO:$bvid"
        return AutomationResult.ok()
    }

    override suspend fun loadComments(): AutomationResult<List<LoadedComment>> {
        actions += "LOAD_COMMENTS"
        restrictCode?.let { return AutomationResult.fail(it, "restricted") }
        return AutomationResult.ok(comments)
    }

    override suspend fun openUser(uid: String): AutomationResult<Unit> {
        actions += "OPEN_USER:$uid"
        return AutomationResult.ok()
    }

    override suspend fun detectRelationship(): RelationshipState {
        actions += "CHECK_RELATIONSHIP:$relationship"
        return relationship
    }

    override suspend fun performMockAction(): AutomationResult<Unit> {
        actions += "PERFORM_ACTION"
        if (relationship == RelationshipState.UNKNOWN) {
            return AutomationResult.fail(ResultCode.UNSAFE_STATE, "unknown")
        }
        relationship = RelationshipState.FOLLOWING
        return AutomationResult.ok()
    }

    override suspend fun returnToComments(): AutomationResult<Unit> {
        actions += "RETURN_TO_COMMENTS"
        return AutomationResult.ok()
    }

    fun setUnknownRelationship() {
        relationship = RelationshipState.UNKNOWN
    }
}
