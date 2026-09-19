package com.example.bilibiliautomation.data.api.wbi

import org.json.JSONObject
import java.security.MessageDigest

/**
 * Semantic port of `reference-python-desktop/app/utils/bilibili_wbi.py`.
 *
 * MIXIN_KEY_ENC_TAB and WBI_FILTER_CHARS are copied in original order.
 * MD5 is used only because the WBI protocol requires it.
 */
object WbiSigner {
    val NAV_URL: String = "https://api.bilibili.com/x/web-interface/nav"

    val MIXIN_KEY_ENC_TAB: IntArray = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
    )

    const val WBI_FILTER_CHARS: String = "!'()*"

    private val filterSet: Set<Char> = WBI_FILTER_CHARS.toSet()
    private val unreserved: Set<Char> =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.-".toSet()

    fun extractKeyFromUrl(url: Any?): String {
        val text = url?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return ""
        val tail = text.substringAfterLast('/')
        return tail.substringBefore('.', missingDelimiterValue = tail)
    }

    fun extractWbiKeys(navData: JSONObject?): WbiKeys? {
        if (navData == null) return null
        val wbiImg = navData.optJSONObject("wbi_img") ?: return null
        val imgKey = extractKeyFromUrl(wbiImg.optString("img_url"))
        val subKey = extractKeyFromUrl(wbiImg.optString("sub_url"))
        if (imgKey.isEmpty() || subKey.isEmpty()) return null
        return WbiKeys(imgKey = imgKey, subKey = subKey)
    }

    fun makeMixinKey(imgKey: String, subKey: String): String {
        val rawKey = imgKey + subKey
        val required = (MIXIN_KEY_ENC_TAB.maxOrNull() ?: -1) + 1
        if (rawKey.length < required) return ""
        val mixed = buildString(MIXIN_KEY_ENC_TAB.size) {
            MIXIN_KEY_ENC_TAB.forEach { index -> append(rawKey[index]) }
        }
        return mixed.take(32)
    }

    fun sign(
        params: Map<String, Any?>,
        imgKey: String,
        subKey: String,
        now: Long? = null,
    ): Map<String, String> {
        val mixinKey = makeMixinKey(imgKey, subKey)
        if (mixinKey.isEmpty()) {
            return params.entries.associate { it.key to it.value.toString() }
        }
        val signed = LinkedHashMap<String, String>(params.size + 2)
        params.forEach { (key, value) ->
            signed[key] = filterValue(value?.toString().orEmpty())
        }
        signed["wts"] = (now ?: (System.currentTimeMillis() / 1000L)).toString()
        val query = signed.entries
            .sortedBy { it.key }
            .joinToString("&") { (key, value) ->
                "${wbiQuote(key)}=${wbiQuote(value)}"
            }
        signed["w_rid"] = md5Hex(query + mixinKey)
        return signed
    }

    fun filterValue(raw: String): String = buildString(raw.length) {
        raw.forEach { ch -> if (ch !in filterSet) append(ch) }
    }

    /**
     * Matches Python `urllib.parse.urlencode` (`quote_plus`, `safe=''`).
     * Space becomes `+`; other bytes use `%XX` uppercase.
     */
    fun wbiQuote(value: String): String {
        val out = StringBuilder(value.length)
        value.forEach { ch ->
            when {
                ch == ' ' -> out.append('+')
                ch in unreserved -> out.append(ch)
                else -> {
                    val bytes = ch.toString().toByteArray(Charsets.UTF_8)
                    for (b in bytes) {
                        val v = b.toInt() and 0xFF
                        out.append('%')
                        out.append("0123456789ABCDEF"[v shr 4])
                        out.append("0123456789ABCDEF"[v and 0x0F])
                    }
                }
            }
        }
        return out.toString()
    }

    fun md5Hex(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val v = byte.toInt() and 0xFF
                append("0123456789abcdef"[v shr 4])
                append("0123456789abcdef"[v and 0x0F])
            }
        }
    }
}
