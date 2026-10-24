package com.example.bilibiliautomation.data.api

/**
 * Central URL registry. Do not scatter these in Screen / ViewModel / WorkflowEngine.
 *
 * Verification:
 * - VERIFIED_FROM_REFERENCE: present in `reference-python-desktop/app/spiders/bilibili/spider.py`
 * - OFFICIAL_WEB_PASSPORT: Bilibili web passport QR login (not in spider.py; spider uses browser cookies)
 * - UNVERIFIED: not present in 参考; implemented from documented web API shapes, live status PENDING
 */
object BiliApiEndpoints {
    const val NAV = "https://api.bilibili.com/x/web-interface/nav"
    const val VIDEO_VIEW = "https://api.bilibili.com/x/web-interface/view"
    const val PLAYURL_WBI = "https://api.bilibili.com/x/player/wbi/playurl"
    const val PLAYURL = "https://api.bilibili.com/x/player/playurl"

    const val QR_GENERATE = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    const val QR_POLL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"
    const val LOGIN_CAPTCHA = "https://passport.bilibili.com/x/passport-login/captcha"
    const val SMS_SEND = "https://passport.bilibili.com/x/passport-login/web/sms/send"
    const val SMS_LOGIN = "https://passport.bilibili.com/x/passport-login/web/login/sms"
    const val COUNTRY_LIST = "https://passport.bilibili.com/web/generic/country/list"
    /** Official passport page used as HITL fallback when GeeTest must be completed by a human. */
    const val PASSPORT_LOGIN = "https://passport.bilibili.com/login"
    const val PASSPORT_H5_LOGIN = "https://passport.bilibili.com/h5-app/passport/login"

    const val USER_CARD = "https://api.bilibili.com/x/web-interface/card"
    const val SPACE_ACC_INFO = "https://api.bilibili.com/x/space/wbi/acc/info"
    const val SPACE_ACC_INFO_UNSIGNED = "https://api.bilibili.com/x/space/acc/info"
    const val USER_SEARCH = "https://api.bilibili.com/x/web-interface/wbi/search/type"
    const val USER_SEARCH_UNSIGNED = "https://api.bilibili.com/x/web-interface/search/type"
    const val USER_VIDEOS = "https://api.bilibili.com/x/space/wbi/arc/search"
    const val ROOT_COMMENTS = "https://api.bilibili.com/x/v2/reply"
    const val REPLY_COMMENTS = "https://api.bilibili.com/x/v2/reply/reply"
    const val RELATION_QUERY = "https://api.bilibili.com/x/space/wbi/acc/relation"
    const val RELATION_STAT = "https://api.bilibili.com/x/relation/stat"

    /** Official Bilibili web relation modify endpoint. act=1 follow, act=2 unfollow. */
    const val RELATION_MODIFY = "https://api.bilibili.com/x/relation/modify"
    const val RELATION_MODIFY_VERIFIED: Boolean = true
    const val RELATION_ACT_FOLLOW_UNVERIFIED: Int = 1

    enum class Trust {
        VERIFIED_FROM_REFERENCE,
        OFFICIAL_WEB_PASSPORT,
        UNVERIFIED,
    }

    data class Descriptor(
        val logicalName: String,
        val method: String,
        val url: String,
        val requiresLogin: Boolean,
        val requiresWbi: Boolean,
        val requiresCsrf: Boolean,
        val trust: Trust,
    )

    val registry: List<Descriptor> = listOf(
        Descriptor("NAV", "GET", NAV, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.VERIFIED_FROM_REFERENCE),
        Descriptor("VIDEO_VIEW", "GET", VIDEO_VIEW, requiresLogin = false, requiresWbi = true, requiresCsrf = false, trust = Trust.VERIFIED_FROM_REFERENCE),
        Descriptor("PLAYURL_WBI", "GET", PLAYURL_WBI, requiresLogin = false, requiresWbi = true, requiresCsrf = false, trust = Trust.VERIFIED_FROM_REFERENCE),
        Descriptor("QR_GENERATE", "GET", QR_GENERATE, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.OFFICIAL_WEB_PASSPORT),
        Descriptor("QR_POLL", "GET", QR_POLL, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.OFFICIAL_WEB_PASSPORT),
        Descriptor("LOGIN_CAPTCHA", "GET", LOGIN_CAPTCHA, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.OFFICIAL_WEB_PASSPORT),
        Descriptor("SMS_SEND", "POST", SMS_SEND, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.OFFICIAL_WEB_PASSPORT),
        Descriptor("SMS_LOGIN", "POST", SMS_LOGIN, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.OFFICIAL_WEB_PASSPORT),
        Descriptor("COUNTRY_LIST", "GET", COUNTRY_LIST, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.OFFICIAL_WEB_PASSPORT),
        Descriptor("USER_CARD", "GET", USER_CARD, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("SPACE_ACC_INFO", "GET", SPACE_ACC_INFO, requiresLogin = false, requiresWbi = true, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("USER_SEARCH", "GET", USER_SEARCH, requiresLogin = false, requiresWbi = true, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("USER_VIDEOS", "GET", USER_VIDEOS, requiresLogin = false, requiresWbi = true, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("ROOT_COMMENTS", "GET", ROOT_COMMENTS, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("REPLY_COMMENTS", "GET", REPLY_COMMENTS, requiresLogin = false, requiresWbi = false, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("RELATION_QUERY", "GET", RELATION_QUERY, requiresLogin = true, requiresWbi = true, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("RELATION_STAT", "GET", RELATION_STAT, requiresLogin = true, requiresWbi = false, requiresCsrf = false, trust = Trust.UNVERIFIED),
        Descriptor("RELATION_MODIFY", "POST", RELATION_MODIFY, requiresLogin = true, requiresWbi = false, requiresCsrf = true, trust = Trust.UNVERIFIED),
    )
}
