package com.example.bilibiliautomation.task

data class AutomationTask(
    val name: String,
    val steps: List<NamedStep>,
) {
    data class NamedStep(
        val name: String,
        val step: AutomationStep,
    )
}
