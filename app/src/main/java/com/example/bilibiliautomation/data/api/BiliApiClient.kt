package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.data.api.session.AccountCookieJar
import com.example.bilibiliautomation.data.api.wbi.WbiKeyManager
import com.example.bilibiliautomation.data.api.wbi.WbiKeys
import com.example.bilibiliautomation.data.api.wbi.WbiSigner
import com.example.bilibiliautomation.domain.api.ApiError
import com.example.bilibiliautomation.domain.api.ApiResult
import com.example.bilibiliautomation.domain.api.BiliEnvelope
import com.example.bilibiliautomation.logging.AutomationLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class BiliApiClient(
    private val wbi: WbiKeyManager = WbiKeyManager(),
    private val logger: AutomationLogger = AutomationLogger,
    private val timeoutMs: Long = 15_000L,
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(BiliApiLogInterceptor(logger))
        .build(),
) {
    private val clients = ConcurrentHashMap<Long, OkHttpClient>()

    fun clientFor(jar: AccountCookieJar): OkHttpClient {
        return clients.getOrPut(jar.accountId) {
            baseClient.newBuilder().cookieJar(jar).build()
        }
    }

    suspend fun get(
        jar: AccountCookieJar,
        url: String,
        params: Map<String, String> = emptyMap(),
        logicalName: String,
    ): ApiResult<BiliEnvelope<JSONObject>> = execute(jar, logicalName, wbiSigned = false, retryCount = 0) { http ->
        http.newCall(request(jar, url, params, logicalName, wbiSigned = false, method = "GET", body = null)).execute()
    }

    suspend fun signedGet(
        jar: AccountCookieJar,
        url: String,
        params: Map<String, String>,
        logicalName: String,
        retry: Boolean = true,
    ): ApiResult<BiliEnvelope<JSONObject>> {
        val signed = wbi.signParams(params, fetch = { fetchWbiKeys(jar) })
        val first = execute(jar, logicalName, wbiSigned = signed.second, retryCount = 0) { http ->
            http.newCall(request(jar, url, signed.first, logicalName, signed.second, "GET", null)).execute()
        }
        val error = first.errorOrNull()
        if (retry && error != null && BiliErrorMapper.isWbiFailure(error)) {
            wbi.invalidate()
            val resigned = wbi.signParams(params, fetch = { fetchWbiKeys(jar) })
            return execute(jar, logicalName, wbiSigned = resigned.second, retryCount = 1) { http ->
                http.newCall(request(jar, url, resigned.first, logicalName, resigned.second, "GET", null)).execute()
            }
        }
        return first
    }

    suspend fun postForm(
        jar: AccountCookieJar,
        url: String,
        fields: Map<String, String>,
        logicalName: String,
        requireCsrf: Boolean,
        requireSession: Boolean,
    ): ApiResult<BiliEnvelope<JSONObject>> {
        val auth = AuthenticatedRequestBuilder(jar)
        if (requireSession && jar.snapshot().isEmpty()) {
            return ApiResult.err(ApiError.SessionInvalid())
        }
        val body = if (requireCsrf) {
            val (form, error) = auth.formWithCsrf(fields)
            if (error != null) return ApiResult.err(error)
            form!!
        } else {
            val builder = FormBody.Builder()
            fields.forEach { (k, v) -> builder.add(k, v) }
            builder.build()
        }
        return execute(jar, logicalName, wbiSigned = false, retryCount = 0) { http ->
            http.newCall(request(jar, url, emptyMap(), logicalName, false, "POST", body)).execute()
        }
    }

    suspend fun refreshWbi(jar: AccountCookieJar): WbiKeys? = wbi.ensureKeys { fetchWbiKeys(jar) }

    suspend fun ingestNavKeys(data: JSONObject?): Boolean = wbi.updateFromNavData(data)

    private suspend fun fetchWbiKeys(jar: AccountCookieJar): WbiKeys? {
        val result = get(jar, WbiSigner.NAV_URL, emptyMap(), "NAV")
        val envelope = result.getOrNull() ?: return null
        return WbiSigner.extractWbiKeys(envelope.data)
    }

    private fun request(
        jar: AccountCookieJar,
        url: String,
        params: Map<String, String>,
        logicalName: String,
        wbiSigned: Boolean,
        method: String,
        body: FormBody?,
    ): Request {
        val httpUrl = if (wbiSigned && params.isNotEmpty()) {
            val query = params.entries.joinToString("&") { (key, value) ->
                "${com.example.bilibiliautomation.data.api.wbi.WbiSigner.wbiQuote(key)}=" +
                    com.example.bilibiliautomation.data.api.wbi.WbiSigner.wbiQuote(value)
            }
            val joined = if (url.contains('?')) "$url&$query" else "$url?$query"
            joined.toHttpUrl()
        } else {
            url.toHttpUrl().newBuilder().apply {
                params.forEach { (k, v) -> addQueryParameter(k, v) }
            }.build()
        }
        val builder = Request.Builder().url(httpUrl)
        AuthenticatedRequestBuilder(jar).applyHeaders(builder, logicalName, jar.accountId, wbiSigned)
        if (method == "POST") {
            builder.post(body ?: FormBody.Builder().build())
        } else {
            builder.get()
        }
        return builder.build()
    }

    private suspend fun execute(
        jar: AccountCookieJar,
        logicalName: String,
        wbiSigned: Boolean,
        retryCount: Int,
        call: (OkHttpClient) -> Response,
    ): ApiResult<BiliEnvelope<JSONObject>> = withContext(Dispatchers.IO) {
        val http = clientFor(jar)
        val started = System.nanoTime()
        try {
            call(http).use { response ->
                val elapsedMs = (System.nanoTime() - started) / 1_000_000L
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful && body.isBlank()) {
                    logger.i(
                        "api endpoint=$logicalName accountId=${jar.accountId} httpStatus=${response.code} " +
                            "apiCode=null elapsedMs=$elapsedMs wbiSigned=$wbiSigned retryCount=$retryCount",
                    )
                    return@withContext ApiResult.err(BiliErrorMapper.fromHttp(response.code))
                }
                val envelope = runCatching { BiliJson.envelope(body) }.getOrElse {
                    return@withContext ApiResult.err(ApiError.InvalidResponse("invalid json"))
                }
                logger.i(
                    "api endpoint=$logicalName accountId=${jar.accountId} httpStatus=${response.code} " +
                        "apiCode=${envelope.code} elapsedMs=$elapsedMs wbiSigned=$wbiSigned retryCount=$retryCount",
                )
                if (!envelope.isSuccess) {
                    return@withContext ApiResult.err(
                        BiliErrorMapper.fromApiCode(envelope.code, envelope.message, response.code),
                    )
                }
                ApiResult.ok(envelope)
            }
        } catch (_: SocketTimeoutException) {
            ApiResult.err(ApiError.Timeout())
        } catch (io: IOException) {
            ApiResult.err(ApiError.Network(io.message ?: "network", io.javaClass.simpleName))
        } catch (error: Exception) {
            ApiResult.err(ApiError.Unknown(error.message ?: "unknown", error.javaClass.simpleName))
        }
    }
}
