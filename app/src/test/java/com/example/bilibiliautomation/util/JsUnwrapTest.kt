package com.example.bilibiliautomation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class JsUnwrapTest {
    @Test
    fun unwrapsNull() {
        assertNull(JsUtils.unwrapEvaluateJavascript("null"))
        assertNull(JsUtils.unwrapEvaluateJavascript(null))
    }

    @Test
    fun unwrapsBooleansAndNumbers() {
        assertEquals("true", JsUtils.unwrapEvaluateJavascript("true"))
        assertEquals("false", JsUtils.unwrapEvaluateJavascript("false"))
        assertEquals("123", JsUtils.unwrapEvaluateJavascript("123"))
    }

    @Test
    fun unwrapsJsonStringOnce() {
        assertEquals("hello", JsUtils.unwrapEvaluateJavascript("\"hello\""))
        val once = JsUtils.unwrapEvaluateJavascript("\"{\\\"a\\\":1}\"")
        assertEquals("{\"a\":1}", once)
        val parsed = JSONObject(once!!)
        assertEquals(1, parsed.getInt("a"))
    }

    @Test
    fun doesNotDoubleEncodeObjectsOrArrays() {
        val obj = JsUtils.unwrapEvaluateJavascript("{\"a\":1}")
        assertEquals("{\"a\":1}", obj)
        assertTrue(JsUtils.parseObject("{\"success\":true,\"code\":\"OK\",\"data\":{}}") is JSONObject)
        val array = JsUtils.decodeEvaluateJavascript("[1,2]")
        assertTrue(array is JSONArray)
        assertEquals(2, (array as JSONArray).length())
    }
}
