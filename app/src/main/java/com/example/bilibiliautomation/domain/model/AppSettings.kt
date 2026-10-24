package com.example.bilibiliautomation.domain.model

data class AppSettings(
    val defaultTimeoutMs: Long = 10_000L,
    val pollIntervalMs: Long = 200L,
    val operationIntervalMs: Long = 5_000L,
    val retryCount: Int = 3,
    val cooldownMinutes: Int = 30,
    val dailyLimit: Int = 200,
    val notificationEnabled: Boolean = true,
    val debugEnabled: Boolean = false,
    val parallelTasks: Boolean = false,
    val smartStatus: Boolean = true,
    val autoSwitchAccount: Boolean = true,
    val autoRetry: Boolean = true,
    val unattendedMode: Boolean = false,
)
