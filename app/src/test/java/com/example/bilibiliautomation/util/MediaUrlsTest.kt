package com.example.bilibiliautomation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUrlsTest {
    @Test
    fun httpsFaceUpgradesHttpAndProtocolRelative() {
        assertEquals(
            "https://i0.hdslb.com/bfs/face/a.jpg",
            MediaUrls.httpsFace("http://i0.hdslb.com/bfs/face/a.jpg"),
        )
        assertEquals(
            "https://i0.hdslb.com/bfs/face/a.jpg",
            MediaUrls.httpsFace("//i0.hdslb.com/bfs/face/a.jpg"),
        )
        assertEquals(null, MediaUrls.httpsFace("null"))
        assertEquals(null, MediaUrls.httpsFace(" "))
    }

    @Test
    fun faceCandidatesIncludeThumbnails() {
        val url = "https://i1.hdslb.com/bfs/face/a1b3161b8784c832ec6d06d8e32c4497826b412c.jpg"
        val candidates = MediaUrls.faceCandidates(url)
        assertEquals(url, candidates.first())
        assertTrue(candidates.any { it.endsWith("@120w_120h_1c.jpg") })
    }
}
