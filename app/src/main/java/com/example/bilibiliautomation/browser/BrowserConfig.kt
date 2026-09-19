package com.example.bilibiliautomation.browser

data class BrowserConfig(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val debugEnabled: Boolean = true,
    val userAgent: String? = null,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    val pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS,
) {
    companion object {
        const val DEFAULT_TIMEOUT_MS: Long = 10_000L
        const val DEFAULT_POLL_INTERVAL_MS: Long = 200L
        const val TEST_PAGE_URL: String = "file:///android_asset/test.html"
    }
}
