package com.example.bilibiliautomation.data.api.sms

import com.example.bilibiliautomation.domain.model.SessionCookie

enum class SmsLoginPhase {
    IDLE,
    LOADING_CAPTCHA,
    WAITING_GEEST,
    SENDING,
    CODE_SENT,
    LOGGING_IN,
    HITL_WEBVIEW,
    SUCCESS,
    FAILED,
}

data class SmsLoginUiState(
    val phase: SmsLoginPhase = SmsLoginPhase.IDLE,
    val accountId: Long? = null,
    val tel: String = "",
    val cid: Int = 1,
    val countryLabel: String = "+86 中国",
    val captcha: com.example.bilibiliautomation.data.api.dto.LoginCaptcha? = null,
    val captchaKey: String? = null,
    val cooldownUntil: Long = 0L,
    val message: String = "",
    val uid: Long? = null,
    val uname: String? = null,
    val face: String? = null,
    val countries: List<com.example.bilibiliautomation.data.api.dto.SmsCountry> = emptyList(),
    val webCookies: List<SessionCookie> = emptyList(),
)
