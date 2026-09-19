package com.example.bilibiliautomation.data.api.wbi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

fun interface EpochClock {
    fun epochSeconds(): Long
}

fun interface MonotonicClock {
    fun millis(): Long
}

/**
 * Port of Python `BilibiliWbiSigner`: TTL cache, double-checked refresh, Mutex instead of RLock.
 * Default TTL is 3600s, floor 60s — same as the Python constructor.
 */
class WbiKeyManager(
    ttlSeconds: Int = 3600,
    private val monotonic: MonotonicClock = MonotonicClock { System.nanoTime() / 1_000_000L },
) {
    val ttlSeconds: Int = maxOf(60, ttlSeconds)

    private val mutex = Mutex()

    @Volatile
    private var keys: WbiKeys? = null

    @Volatile
    private var expiresAtMs: Long = 0L

    fun currentKeys(): WbiKeys? {
        val cached = keys
        return if (cached == null || monotonic.millis() >= expiresAtMs) null else cached
    }

    suspend fun invalidate() {
        mutex.withLock {
            keys = null
            expiresAtMs = 0L
        }
    }

    suspend fun setKeys(imgKey: String, subKey: String, ttlSeconds: Int? = null) {
        mutex.withLock {
            keys = WbiKeys(imgKey, subKey)
            val ttl = if (ttlSeconds == null) this.ttlSeconds else maxOf(60, ttlSeconds)
            expiresAtMs = monotonic.millis() + ttl * 1000L
        }
    }

    suspend fun updateFromNavData(navData: JSONObject?): Boolean {
        val parsed = WbiSigner.extractWbiKeys(navData) ?: return false
        setKeys(parsed.imgKey, parsed.subKey)
        return true
    }

    suspend fun ensureKeys(fetch: suspend () -> WbiKeys?): WbiKeys? {
        currentKeys()?.let { return it }
        return mutex.withLock {
            currentKeys()?.let { return@withLock it }
            val fresh = fetch()
            if (fresh != null) {
                keys = fresh
                expiresAtMs = monotonic.millis() + ttlSeconds * 1000L
            }
            fresh
        }
    }

    suspend fun signParams(
        params: Map<String, Any?>,
        now: Long? = null,
        fetch: suspend () -> WbiKeys?,
    ): Pair<Map<String, String>, Boolean> {
        val resolved = ensureKeys(fetch)
        if (resolved == null) {
            return params.entries.associate { it.key to it.value.toString() } to false
        }
        return WbiSigner.sign(params, resolved.imgKey, resolved.subKey, now) to true
    }
}
