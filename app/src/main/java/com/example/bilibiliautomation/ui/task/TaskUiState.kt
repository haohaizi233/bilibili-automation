package com.example.bilibiliautomation.ui.task

import com.example.bilibiliautomation.domain.model.Account
import com.example.bilibiliautomation.domain.model.Progress
import com.example.bilibiliautomation.domain.model.Target
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.domain.workflow.WorkflowState

data class TaskUiState(
    val status: TaskStatus = TaskStatus.IDLE,
    val workflowState: WorkflowState = WorkflowState.IDLE,
    val taskId: Long? = null,
    val target: Target? = null,
    val account: Account? = null,
    val videoProgress: Progress = Progress(),
    val commentProgress: Progress = Progress(),
    val currentAction: String? = null,
    val elapsedTime: Long = 0L,
    val startedAt: Long = 0L,
    val pausedAt: Long? = null,
    val message: String = "",
    val interruptedTaskId: Long? = null,
    val followed: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val currentVideoTitle: String = "",
    val currentBvid: String = "",
    val currentVideoCover: String = "",
)
