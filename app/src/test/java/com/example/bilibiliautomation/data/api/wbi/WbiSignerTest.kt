package com.example.bilibiliautomation.data.api.wbi

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WbiSignerTest {
    private val imgKey = "7cd084941338484aae1ad9423b19e27a"
    private val subKey = "39001e36c5574c2ab04d4f5ea80ce0cd"
    private val now = 1_700_000_000L

    @Test
    fun mixinKeyMatchesPython() {
        assertEquals("2a1df123a43a00c09740935a2e464358", WbiSigner.makeMixinKey(imgKey, subKey))
        assertEquals(32, WbiSigner.makeMixinKey(imgKey, subKey).length)
        assertEquals("", WbiSigner.makeMixinKey("short", "x"))
    }

    @Test
    fun extractKeysFromNavUrl() {
        val nav = JSONObject(
            """
            {"wbi_img":{"img_url":"https://i0.hdslb.com/bfs/wbi/$imgKey.png",
            "sub_url":"https://i0.hdslb.com/bfs/wbi/$subKey.png"}}
            """.trimIndent(),
        )
        val keys = WbiSigner.extractWbiKeys(nav)!!
        assertEquals(imgKey, keys.imgKey)
        assertEquals(subKey, keys.subKey)
        assertNull(WbiSigner.extractWbiKeys(JSONObject()))
    }

    @Test
    fun filterCharsAndPythonCrossCheck() {
        val signed = WbiSigner.sign(
            mapOf("foo" to "hello!()*world", "a" to "1"),
            imgKey,
            subKey,
            now,
        )
        assertEquals("helloworld", signed["foo"])
        assertEquals("1700000000", signed["wts"])
        assertEquals("8245359d2a794853e469240352dfe280", signed["w_rid"])
    }

    @Test
    fun simpleParamsMatchPython() {
        val signed = WbiSigner.sign(mapOf("foo" to "bar", "baz" to "qux"), imgKey, subKey, now)
        assertEquals("b7409628f2fbcf5c6936ec6ec76885bd", signed["w_rid"])
    }

    @Test
    fun spaceEncodesAsPlusLikePythonQuotePlus() {
        val signed = WbiSigner.sign(mapOf("keyword" to "hello world"), imgKey, subKey, now)
        assertEquals("b960f4e19e9e6a19521451b39301170b", signed["w_rid"])
        assertEquals("keyword=hello+world", WbiSigner.wbiQuote("keyword") + "=" + WbiSigner.wbiQuote("hello world"))
    }

    @Test
    fun chineseUrlEncodeMatchesPython() {
        val signed = WbiSigner.sign(mapOf("keyword" to "测试"), imgKey, subKey, now)
        assertEquals("e6d8e381036c1f169bbecc933d70878d", signed["w_rid"])
    }

    @Test
    fun sortOrderMatchesPython() {
        val signed = WbiSigner.sign(mapOf("z" to "9", "a" to "1", "m" to "5"), imgKey, subKey, now)
        assertEquals("58694691e43be910d2174bd8a2dbe346", signed["w_rid"])
    }

    @Test
    fun videoParamsMatchPython() {
        val signed = WbiSigner.sign(
            mapOf("mid" to "123456", "pn" to "1", "ps" to "30", "order" to "pubdate"),
            imgKey,
            subKey,
            now,
        )
        assertEquals("a092038fe1976d803e45e013d37e2030", signed["w_rid"])
        assertTrue(signed.containsKey("wts"))
        assertTrue(signed.containsKey("w_rid"))
    }

    @Test
    fun mixinTableIsUnmodified64Entries() {
        assertEquals(64, WbiSigner.MIXIN_KEY_ENC_TAB.size)
        assertEquals(46, WbiSigner.MIXIN_KEY_ENC_TAB.first())
        assertEquals(52, WbiSigner.MIXIN_KEY_ENC_TAB.last())
        assertEquals("!'()*", WbiSigner.WBI_FILTER_CHARS)
    }
}
