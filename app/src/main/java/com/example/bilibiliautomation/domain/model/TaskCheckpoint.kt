package com.example.bilibiliautomation.domain.model

import com.example.bilibiliautomation.domain.workflow.WorkflowState

data class TaskCheckpoint(
    val taskId: Long,
    val workflowState: WorkflowState,
    val targetUid: String?,
    val videoId: String?,
    val videoIndex: Int,
    val commentId: String?,
    val commentCursor: String?,
    val commentIndex: Int,
    val currentUserUid: String?,
    val currentUrl: String?,
    val followed: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val videoTotal: Int = 0,
    val commentTotal: Int = 0,
    val currentVideoTitle: String? = null,
    val currentVideoCover: String? = null,
    val updatedAt: Long,
)
