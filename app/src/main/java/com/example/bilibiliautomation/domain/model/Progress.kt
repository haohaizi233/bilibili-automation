package com.example.bilibiliautomation.domain.model

data class Progress(
    val current: Int = 0,
    val total: Int = 0,
) {
    val label: String get() = "$current/$total"
}
