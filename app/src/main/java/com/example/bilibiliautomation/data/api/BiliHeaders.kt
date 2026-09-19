package com.example.bilibiliautomation.data.api

/**
 * Port of Python spider HEADERS: User-Agent + Referer only.
 * Screens / WorkflowEngine must not add their own headers.
 */
object BiliHeaders {
    const val USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/139.0.0.0 Safari/537.36"

    const val REFERER: String = "https://www.bilibili.com"
    const val ORIGIN: String = "https://www.bilibili.com"
    const val ACCEPT: String = "application/json, text/plain, */*"

    const val HEADER_API_NAME: String = "X-Bili-Api-Name"
    const val HEADER_ACCOUNT_ID: String = "X-Bili-Account-Id"
    const val HEADER_WBI: String = "X-Bili-Wbi-Signed"

    fun defaultHeaders(): Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Referer" to REFERER,
        "Origin" to ORIGIN,
        "Accept" to ACCEPT,
    )
}
