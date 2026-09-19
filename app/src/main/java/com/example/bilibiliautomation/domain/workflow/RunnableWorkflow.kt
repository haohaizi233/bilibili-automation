package com.example.bilibiliautomation.domain.workflow

import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.Task
import kotlinx.coroutines.flow.StateFlow

interface RunnableWorkflow {
    val state: StateFlow<WorkflowState>
    var pauseRequested: Boolean
    var cancelRequested: Boolean
    suspend fun run(
        task: Task,
        startState: WorkflowState = WorkflowState.VALIDATE_ACCOUNT,
        initial: WorkflowContext = WorkflowContext(task),
    ): AutomationResult<Unit>
}
