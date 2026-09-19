package com.example.bilibiliautomation.util

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object JsUtils {
    fun quote(value: String): String = JSONObject.quote(value)

    fun parseObject(raw: String?): JSONObject? {
        if (raw.isNullOrBlank() || raw == "null") return null
        val text = unwrapEvaluateJavascript(raw) ?: return null
        return runCatching { JSONObject(text) }.getOrNull()
    }

    /**
     * `WebView.evaluateJavascript` JSON-encodes the JS return value exactly once.
     *
     * Examples of the raw callback string:
     * - JS `null` -> `null`
     * - JS `true` -> `true`
     * - JS `123` -> `123`
     * - JS `"hello"` -> `"hello"`
     * - JS `[]` / `{}` -> `[]` / `{}`
     * - JS `JSON.stringify({a:1})` -> `"{\"a\":1}"` (a JSON string, unwrapped once to `{"a":1}`)
     *
     * This method unwraps that single encoding. It does not parse-and-restringify
     * objects, which would produce a double-encoded payload.
     */
    fun unwrapEvaluateJavascript(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "null") return null
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            return decodeJsonStringLiteral(trimmed)
        }
        return trimmed
    }

    fun decodeEvaluateJavascript(raw: String?): Any? {
        val unwrapped = unwrapEvaluateJavascript(raw) ?: return null
        return parseJsonToken(unwrapped).let { token ->
            if (token === JSONObject.NULL) null else token
        } ?: unwrapped
    }

    private fun parseJsonToken(trimmed: String): Any? {
        return runCatching { JSONTokener(trimmed).nextValue() }.getOrNull()
    }

    private fun decodeJsonStringLiteral(quoted: String): String {
        val inner = quoted.substring(1, quoted.length - 1)
        val out = StringBuilder(inner.length)
        var i = 0
        while (i < inner.length) {
            val ch = inner[i]
            if (ch == '\\' && i + 1 < inner.length) {
                when (val next = inner[i + 1]) {
                    '"' -> out.append('"')
                    '\\' -> out.append('\\')
                    '/' -> out.append('/')
                    'b' -> out.append('\b')
                    'f' -> out.append('\u000C')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'u' -> {
                        if (i + 5 < inner.length) {
                            val hex = inner.substring(i + 2, i + 6)
                            out.append(hex.toInt(16).toChar())
                            i += 6
                            continue
                        }
                        out.append('u')
                    }
                    else -> out.append(next)
                }
                i += 2
            } else {
                out.append(ch)
                i += 1
            }
        }
        return out.toString()
    }

    fun jsonStringOrNull(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.opt(key)?.toString()
    }

    fun asString(value: Any?): String? = when (value) {
        null, JSONObject.NULL -> null
        is String -> value
        else -> value.toString()
    }
}
