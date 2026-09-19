package com.example.bilibiliautomation.data.secure

import com.example.bilibiliautomation.domain.model.AccountSession
import com.example.bilibiliautomation.domain.model.SessionCookie
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureSessionStoreTest {
    @Test
    fun roundTripDoesNotUseRoom() = runBlocking {
        val store = SecureSessionStore(MemorySessionBlobStore())
        val session = AccountSession(
            accountUid = "10001",
            cookies = listOf(SessionCookie("SESSDATA", "secret", ".example.test")),
            createdAt = 1L,
            updatedAt = 2L,
        )
        store.save("profile-1", session)
        val loaded = store.load("profile-1")
        assertEquals("10001", loaded?.accountUid)
        assertEquals("secret", loaded?.cookies?.single()?.value)
        store.delete("profile-1")
        assertNull(store.load("profile-1"))
    }
}
