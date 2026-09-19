package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.data.api.session.AccountCookieJar
import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.model.SessionCookie
import okhttp3.FormBody
import okhttp3.Request

class AuthenticatedRequestBuilder(
    private val jar: AccountCookieJar,
) {
    fun csrf(): String? = jar.csrf

    fun requireCsrf(): ApiError? {
        return if (csrf().isNullOrBlank()) ApiError.CsrfMissing() else null
    }

    fun applyHeaders(builder: Request.Builder, logicalName: String, accountId: Long, wbiSigned: Boolean) {
        BiliHeaders.defaultHeaders().forEach { (k, v) -> builder.header(k, v) }
        builder.header(BiliHeaders.HEADER_API_NAME, logicalName)
        builder.header(BiliHeaders.HEADER_ACCOUNT_ID, accountId.toString())
        builder.header(BiliHeaders.HEADER_WBI, if (wbiSigned) "1" else "0")
    }

    fun formWithCsrf(fields: Map<String, String>): Pair<FormBody?, ApiError?> {
        val token = csrf() ?: return null to ApiError.CsrfMissing()
        val body = FormBody.Builder()
        fields.forEach { (k, v) -> body.add(k, v) }
        body.add("csrf", token)
        return body.build() to null
    }

    fun cookieSummary(): Pair<Int, List<String>> {
        val cookies: List<SessionCookie> = jar.snapshot()
        return cookies.size to cookies.map { it.name }
    }
}
