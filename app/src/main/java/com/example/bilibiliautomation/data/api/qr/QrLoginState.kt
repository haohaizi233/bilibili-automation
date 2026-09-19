package com.example.bilibiliautomation.data.api.qr

enum class QrLoginPhase {
    IDLE,
    FETCHING_QR,
    WAITING_SCAN,
    SCANNED,
    WAITING_CONFIRM,
    SUCCESS,
    EXPIRED,
    FAILED,
}

data class QrLoginUiState(
    val phase: QrLoginPhase = QrLoginPhase.IDLE,
    val accountId: Long? = null,
    val qrUrl: String? = null,
    val qrcodeKey: String? = null,
    val message: String = "",
    val uid: Long? = null,
    val uname: String? = null,
    val face: String? = null,
    val fansCount: Long? = null,
)
