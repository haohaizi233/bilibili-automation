package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.domain.api.ApiError

object BiliErrorMapper {
    fun fromApiCode(code: Int, message: String, httpStatus: Int? = null): ApiError {
        val text = message.ifBlank { "api code $code" }
        val lowered = text.lowercase()
        return when {
            code == 0 -> ApiError.ApiCode(0, text)
            code == -101 || code == -111 -> ApiError.SessionExpired(text)
            code == -352 || code == -412 || code == 412 -> ApiError.WbiSignFailure(text)
            code == -509 || code == 412 || isRateLimited(code, lowered) -> ApiError.RateLimited(text)
            isRisk(code, lowered) -> ApiError.RiskControl(text)
            isVerification(lowered) -> ApiError.VerificationRequired(text)
            isRestricted(lowered) -> ApiError.AccountRestricted(text)
            httpStatus != null && httpStatus >= 400 -> ApiError.Http(httpStatus, text)
            else -> ApiError.ApiCode(code, text)
        }
    }

    fun fromHttp(status: Int, bodyMessage: String = ""): ApiError {
        val text = bodyMessage.ifBlank { "http $status" }
        return when (status) {
            412 -> ApiError.RiskControl(text)
            429 -> ApiError.RateLimited(text)
            401, 403 -> ApiError.SessionExpired(text)
            else -> ApiError.Http(status, text)
        }
    }

    fun isWbiFailure(error: ApiError): Boolean = error is ApiError.WbiSignFailure

    private fun isRateLimited(code: Int, lowered: String): Boolean {
        if (code == -509) return true
        return listOf("频繁", "rate", "too many", "操作过于频繁", "请求受限").any { it in lowered }
    }

    private fun isRisk(code: Int, lowered: String): Boolean {
        if (code == -352 || code == -412) return false
        return listOf("风控", "risk", "captcha", "验证码", "账号异常").any { it in lowered }
    }

    private fun isVerification(lowered: String): Boolean {
        return listOf("需要验证", "验证码", "captcha", "geetest", "短信验证").any { it in lowered }
    }

    private fun isRestricted(lowered: String): Boolean {
        return listOf("账号异常", "封禁", "限制", "banned", "restricted").any { it in lowered }
    }
}
