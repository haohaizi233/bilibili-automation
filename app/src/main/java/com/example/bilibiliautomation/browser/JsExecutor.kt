package com.example.bilibiliautomation.browser

import android.webkit.WebView
import com.example.bilibiliautomation.domain.model.AutomationResult
import com.example.bilibiliautomation.domain.model.ResultCode
import com.example.bilibiliautomation.util.JsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * All WebView JavaScript evaluation goes through this class.
 * `evaluateJavascript` is always dispatched on the main thread.
 */
class JsExecutor(
    private val webView: WebView,
) {
    suspend fun evaluate(script: String): String? {
        val raw = evaluateRaw(script)
        return JsUtils.unwrapEvaluateJavascript(raw)
    }

    suspend fun evaluateResult(script: String): AutomationResult<JSONObject> {
        val wrapped = """
            (function() {
              try {
                var __out = (function(){ return ($script); })();
                if (__out && typeof __out === 'object' && Object.prototype.hasOwnProperty.call(__out, 'success')) {
                  return __out;
                }
                return {success:true, code:"OK", message:"", data: (__out === undefined ? null : __out)};
              } catch (e) {
                return {success:false, code:"JS_ERROR", message: String(e && e.message ? e.message : e), data:null};
              }
            })();
        """.trimIndent()
        return parseResult(evaluateRaw(wrapped))
    }

    suspend fun evaluateObjectScript(script: String): AutomationResult<JSONObject> {
        return parseResult(evaluateRaw(script))
    }

    private suspend fun evaluateRaw(script: String): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            try {
                webView.evaluateJavascript(script) { value ->
                    if (cont.isActive) cont.resume(value)
                }
            } catch (error: Exception) {
                if (cont.isActive) cont.resumeWithException(error)
            }
        }
    }

    private fun parseResult(raw: String?): AutomationResult<JSONObject> {
        val obj = JsUtils.parseObject(raw)
            ?: return AutomationResult.fail(
                ResultCode.JS_ERROR,
                "JavaScript returned unparsable result: $raw",
            )
        val success = obj.optBoolean("success", false)
        val code = obj.optString("code", if (success) ResultCode.OK else ResultCode.JS_ERROR)
        val message = obj.optString("message", "")
        val data = if (obj.has("data") && !obj.isNull("data") && obj.opt("data") is JSONObject) {
            obj.getJSONObject("data")
        } else {
            obj
        }
        return AutomationResult(
            success = success,
            code = code,
            message = message,
            data = data,
        )
    }

    companion object {
        fun jsonResultScript(body: String): String = """
            (function() {
              function ok(data, message) {
                return {success:true, code:"OK", message: message || "", data: data || null};
              }
              function fail(code, message, data) {
                return {success:false, code:code, message:message || "", data: data || null};
              }
              try {
                $body
              } catch (e) {
                return fail("JS_ERROR", String(e && e.message ? e.message : e));
              }
            })();
        """.trimIndent()
    }
}
