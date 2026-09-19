package com.example.bilibiliautomation.domain.recovery

import com.example.bilibiliautomation.data.repository.TaskRepository
import com.example.bilibiliautomation.domain.model.Task
import com.example.bilibiliautomation.domain.model.TaskStatus
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.TimeMs

class CrashRecovery(
    private val taskRepository: TaskRepository,
    private val logger: AutomationLogger = AutomationLogger,
) {
    suspend fun onAppStart(): List<Task> {
        val dangling = taskRepository.findDanglingRuntime()
        dangling.forEach { task ->
            logger.w("crash recovery: task=${task.id} was ${task.status} -> INTERRUPTED")
            taskRepository.updateStatus(
                id = task.id,
                status = TaskStatus.INTERRUPTED,
                lastError = "process died while ${task.status}",
                pausedAt = TimeMs.wallClock(),
            )
        }
        return dangling
    }
}
