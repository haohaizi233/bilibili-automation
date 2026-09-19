package com.example.bilibiliautomation.domain.api

data class BiliEnvelope<T>(
    val code: Int,
    val message: String,
    val ttl: Int?,
    val data: T?,
) {
    val isSuccess: Boolean get() = code == 0
}
