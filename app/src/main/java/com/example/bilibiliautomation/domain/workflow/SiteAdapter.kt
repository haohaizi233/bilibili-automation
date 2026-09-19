package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.LoadedComment
import com.example.bilibiliautomation.domain.model.LoadedVideo
import com.example.bilibiliautomation.domain.model.PageType
import com.example.bilibiliautomation.domain.model.RelationshipState
import com.example.bilibiliautomation.domain.model.SessionStatus
import com.example.bilibiliautomation.domain.model.Target

interface SiteAdapter {
    suspend fun detectPage(): PageType

    suspend fun validateSession(): SessionStatus

    suspend fun openTarget(target: Target): AutomationResult<Unit>

    suspend fun loadVideos(): AutomationResult<List<LoadedVideo>>

    suspend fun openVideo(bvid: String): AutomationResult<Unit>

    suspend fun loadComments(): AutomationResult<List<LoadedComment>>

    suspend fun openUser(uid: String): AutomationResult<Unit>

    suspend fun detectRelationship(): RelationshipState

    suspend fun performMockAction(): AutomationResult<Unit>

    suspend fun returnToComments(): AutomationResult<Unit>
}
