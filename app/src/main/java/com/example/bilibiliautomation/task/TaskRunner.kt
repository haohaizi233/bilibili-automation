package com.example.bilibiliautomation.task

import com.example.bilibiliautomation.browser.Page
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.logging.AutomationLogger

class TaskRunner(
    private val page: Page,
    private val logger: AutomationLogger = AutomationLogger,
) {
    suspend fun run(task: AutomationTask): AutomationResult<List<String>> {
        logger.i("TASK START ${task.name}")
        val passLines = mutableListOf<String>()
        task.steps.forEachIndexed { index, named ->
            logger.i("STEP ${index + 1}/${task.steps.size} ${named.name}")
            val result = named.step.execute(page)
            if (!result.success) {
                logger.e("TASK FAIL ${task.name} at ${named.name}: ${result.code} ${result.message}")
                return AutomationResult.fail(result.code, "failed at ${named.name}: ${result.message}", passLines)
            }
            val line = "PASS ${named.name}"
            passLines += line
            logger.i(line)
        }
        logger.i("TASK PASS ${task.name}")
        return AutomationResult.ok(passLines, "all steps passed")
    }
}
