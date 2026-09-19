package com.example.bilibiliautomation.event

import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.domain.workflow.WorkflowState

sealed interface AutomationEvent {
    data class TaskStarted(val taskId: Long) : AutomationEvent
    data class StateChanged(val taskId: Long, val from: WorkflowState, val to: WorkflowState) : AutomationEvent
    data class ActionStarted(val taskId: Long, val action: String) : AutomationEvent
    data class ActionCompleted(
        val taskId: Long,
        val action: String,
        val resultCode: String = ResultCode.OK,
        val message: String = "",
    ) : AutomationEvent
    data class VideoProgress(
        val taskId: Long,
        val current: Int,
        val total: Int,
        val title: String = "",
        val bvid: String = "",
        val coverUrl: String = "",
    ) : AutomationEvent
    data class CommentProgress(
        val taskId: Long,
        val current: Int,
        val total: Int,
    ) : AutomationEvent
    data class TaskPaused(val taskId: Long, val reason: String) : AutomationEvent
    data class TaskFailed(val taskId: Long, val code: String, val message: String) : AutomationEvent
    data class TaskFinished(val taskId: Long) : AutomationEvent
    data class TaskCancelled(val taskId: Long) : AutomationEvent
    data class NeedsReview(val taskId: Long, val accountId: Long?, val code: String, val message: String) : AutomationEvent
    data class AccountSwitched(val taskId: Long, val oldAccountId: Long?, val newAccountId: Long) : AutomationEvent
}
