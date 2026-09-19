package com.example.bilibiliautomation.domain.api

import com.example.bilibiliautomation.domain.model.SessionStatus

sealed interface SessionState {
    data class Valid(val uid: Long, val uname: String, val face: String? = null, val following: Long = 0L) : SessionState
    data object Expired : SessionState
    data object NotLoggedIn : SessionState
    data object RiskControlled : SessionState
    data class NetworkError(val message: String) : SessionState
    data class UnknownError(val message: String) : SessionState

    fun toSessionStatus(): SessionStatus = when (this) {
        is Valid -> SessionStatus.VALID
        Expired -> SessionStatus.LOGIN_EXPIRED
        NotLoggedIn -> SessionStatus.LOGIN_REQUIRED
        RiskControlled, is NetworkError, is UnknownError -> SessionStatus.UNKNOWN
    }
}
