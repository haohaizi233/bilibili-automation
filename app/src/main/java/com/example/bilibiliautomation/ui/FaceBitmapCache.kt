package com.example.bilibiliautomation.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.bilibiliautomation.data.api.BiliHeaders
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.MediaUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object FaceBitmapCache {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val cache = LruCache<String, Bitmap>(64)
    private val logger = AutomationLogger

    suspend fun load(url: String?): Bitmap? {
        val original = MediaUrls.httpsFace(url) ?: return null
        cache.get(original)?.let { return it }
        val candidates = MediaUrls.faceCandidates(original)
        candidates.forEach { key ->
            cache.get(key)?.let { bitmap ->
                cache.put(original, bitmap)
                return bitmap
            }
        }
        return withContext(Dispatchers.IO) {
            cache.get(original)?.let { return@withContext it }
            for (candidate in candidates) {
                val bitmap = download(candidate)
                if (bitmap != null) {
                    cache.put(original, bitmap)
                    cache.put(candidate, bitmap)
                    return@withContext bitmap
                }
            }
            logger.w("face decode failed url=$original")
            null
        }
    }

    private fun download(url: String): Bitmap? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BiliHeaders.USER_AGENT)
            .header("Referer", BiliHeaders.REFERER)
            .header("Origin", BiliHeaders.ORIGIN)
            // Prefer formats BitmapFactory can decode on all API levels.
            // Declaring image/avif first makes the CDN negotiate to AVIF, which
            // BitmapFactory cannot decode below API 31 — the avatar then falls
            // back to text. Keep AVIF/WebP out of the preferred position.
            .header("Accept", "image/png,image/jpeg,image/webp,image/*,*/*;q=0.8")
            .get()
            .build()
        return runCatching {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.w("face http=${response.code} url=$url")
                    return@use null
                }
                val bytes = response.body?.bytes() ?: return@use null
                if (bytes.isEmpty()) return@use null
                val contentType = response.header("Content-Type").orEmpty()
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inScaled = false
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (bitmap == null) {
                    logger.w("face decode failed url=$url contentType=$contentType bytes=${bytes.size}")
                }
                bitmap
            }
        }.onFailure { error ->
            logger.w("face request failed url=$url ${error.javaClass.simpleName} ${error.message}")
        }.getOrNull()
    }
}
