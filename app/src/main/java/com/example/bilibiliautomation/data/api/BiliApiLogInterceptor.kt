package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.logging.AutomationLogger
import okhttp3.Interceptor
import okhttp3.Response

class BiliApiLogInterceptor(
    private val logger: AutomationLogger = AutomationLogger,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val logicalName = original.header(BiliHeaders.HEADER_API_NAME) ?: original.url.encodedPath
        val accountId = original.header(BiliHeaders.HEADER_ACCOUNT_ID)
        val wbiSigned = original.header(BiliHeaders.HEADER_WBI) == "1"
        val sanitized = original.newBuilder()
            .removeHeader(BiliHeaders.HEADER_API_NAME)
            .removeHeader(BiliHeaders.HEADER_ACCOUNT_ID)
            .removeHeader(BiliHeaders.HEADER_WBI)
            .build()
        val started = System.nanoTime()
        val response = try {
            chain.proceed(sanitized)
        } catch (error: Exception) {
            logger.e(
                "api method=${sanitized.method} endpoint=$logicalName accountId=$accountId " +
                    "wbiSigned=$wbiSigned failed=${error.javaClass.simpleName}",
            )
            throw error
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        logger.d(
            "api method=${sanitized.method} endpoint=$logicalName accountId=$accountId " +
                "httpStatus=${response.code} elapsedMs=$elapsedMs wbiSigned=$wbiSigned",
        )
        return response
    }
}
