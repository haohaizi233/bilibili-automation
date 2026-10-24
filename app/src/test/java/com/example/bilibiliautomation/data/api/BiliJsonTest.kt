package com.example.bilibiliautomation.data.api

import com.example.bilibiliautomation.domain.api.RelationState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiliJsonTest {
    @Test
    fun parseNavAndEnvelope() {
        val raw = """{"code":0,"message":"OK","ttl":1,"data":{"isLogin":true,"mid":88,"uname":"n","wbi_img":{"img_url":"https://i0.hdslb.com/bfs/wbi/abc.png","sub_url":"https://i0.hdslb.com/bfs/wbi/def.png"}}}"""
        val env = BiliJson.envelope(raw)
        assertTrue(env.isSuccess)
        val nav = BiliJson.parseNav(env.data)
        assertEquals(88L, nav.mid)
        assertEquals("n", nav.uname)
        assertTrue(nav.isLogin)
    }

    @Test
    fun parseUserCardFansAndFace() {
        val data = JSONObject(
            """{"card":{"mid":88,"name":"n","face":"http://i0.hdslb.com/bfs/face/a.jpg","fans":12},"follower":3201000}""",
        )
        val user = BiliJson.parseUserCard(data)!!
        assertEquals(88L, user.mid)
        assertEquals("n", user.name)
        assertEquals(3201000L, user.fans)
        assertEquals("https://i0.hdslb.com/bfs/face/a.jpg", user.face)
    }

    @Test
    fun parseUserCardMidAsStringAndHttpsFace() {
        val data = JSONObject(
            """{"card":{"mid":"62081047","name":"小嵘哥美食测评","face":"https://i1.hdslb.com/bfs/face/a1b3161b8784c832ec6d06d8e32c4497826b412c.jpg"},"follower":491104}""",
        )
        val user = BiliJson.parseUserCard(data)!!
        assertEquals(62081047L, user.mid)
        assertEquals("小嵘哥美食测评", user.name)
        assertEquals(491104L, user.fans)
        assertEquals(
            "https://i1.hdslb.com/bfs/face/a1b3161b8784c832ec6d06d8e32c4497826b412c.jpg",
            user.face,
        )
    }

    @Test
    fun parseVideos() {
        val data = JSONObject(
            """{"list":{"vlist":[{"aid":1,"bvid":"BV1xx","title":"t","created":1700000000,"length":"1:02"}]},"page":{"count":1,"pn":1,"ps":30}}""",
        )
        val page = BiliJson.parseVideoPage(data, 1, 30)
        assertEquals(1, page.items.size)
        assertEquals("BV1xx", page.items[0].bvid)
        assertEquals(62, page.items[0].durationSeconds)
        assertEquals(false, page.hasMore)
    }

    @Test
    fun parseRootCommentsAndAuthorUid() {
        val data = JSONObject(
            """{"replies":[{"rpid":9,"oid":11,"ctime":1700000000,"member":{"mid":12345,"uname":"u"},"content":{"message":"hi"}}],"page":{"num":1,"size":20,"count":1}}""",
        )
        val page = BiliJson.parseCommentPage(data, 1, 20)
        assertEquals(1, page.comments.size)
        assertEquals(12345L, page.comments[0].authorUid)
        assertEquals(9L, page.comments[0].rpid)
        assertEquals("hi", page.comments[0].message)
    }

    @Test
    fun parseRelationAttribute() {
        val data = JSONObject("""{"relation":{"attribute":2},"be_relation":{"attribute":0}}""")
        val snap = BiliJson.parseRelation(data)
        assertEquals(RelationState.Following, snap.state)
        assertEquals(RelationState.Mutual, RelationState.fromAttribute(6))
        assertEquals(RelationState.None, RelationState.fromAttribute(0))
    }

    @Test
    fun parseQrPollCodes() {
        assertEquals(
            com.example.bilibiliautomation.data.api.dto.QrPollCode.WAITING_SCAN,
            BiliJson.parseQrPoll(JSONObject("""{"code":86101,"message":"waiting"}""")).mapped,
        )
        assertEquals(
            com.example.bilibiliautomation.data.api.dto.QrPollCode.SUCCESS,
            BiliJson.parseQrPoll(JSONObject("""{"code":0,"url":"https://www.bilibili.com"}""")).mapped,
        )
    }

    @Test
    fun parseLoginCaptchaAndSmsSend() {
        val captcha = BiliJson.parseLoginCaptcha(
            JSONObject(
                """{"type":"geetest","token":"tok","geetest":{"gt":"gt-1","challenge":"ch-1"},"tencent":{"appid":""}}""",
            ),
        )!!
        assertEquals("tok", captcha.token)
        assertEquals("gt-1", captcha.gt)
        assertEquals("ch-1", captcha.challenge)
        val sent = BiliJson.parseSmsSend(JSONObject("""{"captcha_key":"ck-9"}"""))!!
        assertEquals("ck-9", sent.captchaKey)
        val countries = BiliJson.parseSmsCountries(
            JSONObject("""{"common":[{"id":1,"cname":"中国","country_id":"86"}],"others":[{"id":5,"cname":"美国","country_id":"1"}]}"""),
        )
        assertEquals(1, countries.first().cid)
        assertEquals("86", countries.first().countryId)
        assertEquals(2, countries.size)
        org.junit.Assert.assertNull(
            BiliJson.parseLoginCaptcha(JSONObject("""{"type":"geetest","token":"tok","geetest":{"gt":"gt-1"}}""")),
        )
    }
}
