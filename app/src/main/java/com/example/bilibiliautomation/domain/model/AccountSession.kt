package com.example.bilibiliautomation.domain.model

typealias StoredCookie = SessionCookie

data class AccountSession(
    val accountUid: String,
    val cookies: List<SessionCookie>,
    val createdAt: Long,
    val updatedAt: Long,
    val accountId: Long = 0L,
    val uid: Long? = null,
    val uname: String? = null,
    val status: AccountStatus = AccountStatus.LOGIN_REQUIRED,
) {
    val csrf: String?
        get() = cookies.firstOrNull { it.name.equals("bili_jct", ignoreCase = true) }?.value

    val cookieNames: List<String>
        get() = cookies.map { it.name }
}

data class SessionCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val secure: Boolean = true,
    val httpOnly: Boolean = false,
    val expiresAt: Long? = null,
)
