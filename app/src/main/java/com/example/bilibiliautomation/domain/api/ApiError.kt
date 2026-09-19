package com.example.bilibiliautomation.domain.api

sealed interface ApiError {
    val message: String

    data class Network(override val message: String, val causeMessage: String? = null) : ApiError
    data class Timeout(override val message: String = "timeout") : ApiError
    data class Http(val status: Int, override val message: String) : ApiError
    data class ApiCode(val code: Int, override val message: String) : ApiError
    data class SessionExpired(override val message: String = "session expired") : ApiError
    data class SessionInvalid(override val message: String = "session invalid") : ApiError
    data class RateLimited(override val message: String = "rate limited") : ApiError
    data class RiskControl(override val message: String = "risk control") : ApiError
    data class AccountRestricted(override val message: String = "account restricted") : ApiError
    data class VerificationRequired(override val message: String = "verification required") : ApiError
    data class WbiSignFailure(override val message: String = "wbi sign failed") : ApiError
    data class InvalidResponse(override val message: String) : ApiError
    data class CsrfMissing(override val message: String = "csrf missing") : ApiError
    data class UnverifiedOperation(val operation: String, override val message: String) : ApiError
    data class Cancelled(override val message: String = "cancelled") : ApiError
    data class Unknown(override val message: String, val causeMessage: String? = null) : ApiError
}
