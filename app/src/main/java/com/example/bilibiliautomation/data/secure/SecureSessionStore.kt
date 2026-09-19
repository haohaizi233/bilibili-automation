package com.example.bilibiliautomation.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.bilibiliautomation.domain.model.AccountSession
import com.example.bilibiliautomation.domain.model.SessionCookie
import com.example.bilibiliautomation.logging.AutomationLogger
import com.example.bilibiliautomation.util.TimeMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

interface SessionBlobStore {
    suspend fun write(profileId: String, payload: String)
    suspend fun read(profileId: String): String?
    suspend fun delete(profileId: String)
    suspend fun clear()
}

class MemorySessionBlobStore : SessionBlobStore {
    private val map = ConcurrentHashMap<String, String>()

    override suspend fun write(profileId: String, payload: String) {
        map[profileId] = payload
    }

    override suspend fun read(profileId: String): String? = map[profileId]

    override suspend fun delete(profileId: String) {
        map.remove(profileId)
    }

    override suspend fun clear() {
        map.clear()
    }
}

class EncryptedPrefsSessionBlobStore(
    context: Context,
) : SessionBlobStore {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    override suspend fun write(profileId: String, payload: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key(profileId), payload).apply()
    }

    override suspend fun read(profileId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(key(profileId), null)
    }

    override suspend fun delete(profileId: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(key(profileId)).apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private fun key(profileId: String) = "session_$profileId"

    companion object {
        const val PREFS_NAME = "secure_sessions"

        /**
         * MasterKey uses Android Keystore AES256-GCM. Cookie payloads never
         * live in Room and are never stored with a hardcoded passphrase.
         */
        fun createPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}

class SecureSessionStore(
    private val blobStore: SessionBlobStore,
    private val logger: AutomationLogger = AutomationLogger,
) {
    suspend fun save(profileId: String, session: AccountSession) = withContext(Dispatchers.IO) {
        blobStore.write(profileId, encode(session))
        logger.i("session saved profile=$profileId cookies=${session.cookies.size}")
    }

    suspend fun load(profileId: String): AccountSession? = withContext(Dispatchers.IO) {
        val raw = try {
            blobStore.read(profileId)
        } catch (error: Exception) {
            // EncryptedSharedPreferences can throw GeneralSecurityException
            // after a keystore reset / reinstall. Treat as no session.
            logger.w("session read failed profile=$profileId ${error.javaClass.simpleName} ${error.message}")
            return@withContext null
        } ?: return@withContext null
        try {
            decode(raw)
        } catch (error: Exception) {
            // Corrupted/tampered blob. Treat as no session rather than crashing
            // the process — the caller will just start a fresh login.
            logger.w("session decode failed profile=$profileId ${error.javaClass.simpleName} ${error.message}")
            null
        }
    }

    suspend fun delete(profileId: String) = withContext(Dispatchers.IO) {
        blobStore.delete(profileId)
        logger.i("session deleted profile=$profileId")
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        blobStore.clear()
    }

    private fun encode(session: AccountSession): String {
        val obj = JSONObject()
        obj.put("accountUid", session.accountUid)
        obj.put("createdAt", session.createdAt)
        obj.put("updatedAt", session.updatedAt)
        val cookies = JSONArray()
        session.cookies.forEach { cookie ->
            val item = JSONObject()
            item.put("name", cookie.name)
            item.put("value", cookie.value)
            item.put("domain", cookie.domain)
            item.put("path", cookie.path)
            item.put("secure", cookie.secure)
            item.put("httpOnly", cookie.httpOnly)
            if (cookie.expiresAt != null) {
                item.put("expiresAt", cookie.expiresAt)
            } else {
                item.put("expiresAt", JSONObject.NULL)
            }
            cookies.put(item)
        }
        obj.put("cookies", cookies)
        return obj.toString()
    }

    private fun decode(raw: String): AccountSession {
        val obj = JSONObject(raw)
        val array = obj.optJSONArray("cookies") ?: JSONArray()
        val cookies = buildList {
            for (i in 0 until array.length()) {
                val c = array.getJSONObject(i)
                add(
                    SessionCookie(
                        name = c.optString("name"),
                        value = c.optString("value"),
                        domain = c.optString("domain"),
                        path = c.optString("path", "/"),
                        secure = c.optBoolean("secure", true),
                        httpOnly = c.optBoolean("httpOnly", false),
                        expiresAt = if (c.isNull("expiresAt")) null else c.optLong("expiresAt"),
                    ),
                )
            }
        }
        return AccountSession(
            accountUid = obj.optString("accountUid"),
            cookies = cookies,
            createdAt = obj.optLong("createdAt", TimeMs.wallClock()),
            updatedAt = obj.optLong("updatedAt", TimeMs.wallClock()),
        )
    }
}
