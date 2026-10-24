package com.example.bilibiliautomation.data.api.sms

import org.junit.Assert.assertTrue
import org.junit.Test

class PassportTakeoverJsTest {
    @Test
    fun hookWatchesOfficialSmsSend() {
        assertTrue(PassportTakeoverJs.hook.contains("sms/send"))
        assertTrue(PassportTakeoverJs.hook.contains("BiliGee.onPageEvent"))
    }

    @Test
    fun fillPhoneUsesNativeSetter() {
        val js = PassportTakeoverJs.fillPhone("13800138000")
        assertTrue(js.contains("13800138000"))
        assertTrue(js.contains("HTMLInputElement"))
    }

    @Test
    fun queryTargetsFindsSendAgreeAndSliderCss() {
        val js = PassportTakeoverJs.queryTargets("send")
        assertTrue(js.contains("获取验证码"))
        assertTrue(js.contains("我已阅读"))
        assertTrue(js.contains("bili-slider-only"))
        assertTrue(js.contains("geetest"))
        assertTrue(js.contains("scrollIntoView"))
    }

    @Test
    fun injectsCodeAndClicksLogin() {
        val js = PassportTakeoverJs.injectCodeAndSubmit("123456")
        assertTrue(js.contains("123456"))
        assertTrue(js.contains("登录"))
    }
}
