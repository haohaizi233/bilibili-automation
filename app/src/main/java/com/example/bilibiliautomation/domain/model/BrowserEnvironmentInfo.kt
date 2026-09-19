package com.example.bilibiliautomation.domain.model

data class BrowserEnvironmentInfo(
    val userAgent: String,
    val innerWidth: Int,
    val innerHeight: Int,
    val screenWidth: Int,
    val screenHeight: Int,
    val devicePixelRatio: Double,
    val maxTouchPoints: Int,
    val href: String,
    val title: String,
    val readyState: String,
    val webViewVersion: String,
)
