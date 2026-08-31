package com.mitension.app.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SupabaseAuthManagerTest {
    private val clock = Clock.fixed(Instant.ofEpochSecond(1_000), ZoneOffset.UTC)
    private val signedIn = PersistedAuthSession("access-1", "refresh-1", "user-1", 4_600)

    @Test fun `sign in persists the device session without storing credentials`() = runBlocking {
        val store = MemoryStore()
        val api = FakeAuthApi(signedIn)
        val manager = manager(store, api)

        val session = manager.signIn(" owner@example.com ", "secret")

        assertEquals("owner@example.com", api.email)
        assertEquals("secret", api.password)
        assertEquals(signedIn, store.session)
        assertEquals("user-1", session.userId)
        assertEquals("sb_publishable_test", session.publishableKey)
    }

    @Test fun `a persisted valid session opens without a refresh`() = runBlocking {
        val store = MemoryStore(signedIn)
        val api = FakeAuthApi(signedIn)
        val manager = manager(store, api)

        assertEquals("access-1", manager.validSession()?.accessToken)
        assertEquals(0, api.refreshCount)
        assertEquals("user-1", manager.restoredSession()?.userId)
    }

    @Test fun `an expiring session is refreshed and the rotated refresh token is persisted`() = runBlocking {
        val expiring = signedIn.copy(expiresAtEpochSeconds = 1_030)
        val renewed = PersistedAuthSession("access-2", "refresh-2", "user-1", 8_200)
        val store = MemoryStore(expiring)
        val api = FakeAuthApi(renewed)

        val session = manager(store, api).validSession()

        assertEquals("refresh-1", api.lastRefreshToken)
        assertEquals("access-2", session?.accessToken)
        assertEquals(renewed, store.session)
    }

    @Test fun `a transient refresh failure keeps the offline session`() = runBlocking {
        val stored = signedIn.copy(expiresAtEpochSeconds = 1_000)
        val store = MemoryStore(stored)
        val api = FakeAuthApi(signedIn, refreshFailure = IOException("offline"))

        val result = runCatching { manager(store, api).validSession() }

        assertSame(api.refreshFailure, result.exceptionOrNull())
        assertEquals(stored, store.session)
        assertEquals("user-1", manager(store, api).restoredSession()?.userId)
    }

    @Test fun `sign out removes the persisted session`() {
        val store = MemoryStore(signedIn)
        val manager = manager(store, FakeAuthApi(signedIn))
        manager.signOut()
        assertNull(store.session)
    }

    private fun manager(store: MemoryStore, api: FakeAuthApi) = SupabaseAuthManager(
        "https://example.supabase.co/", "sb_publishable_test", store, api, clock,
    )
}

private class MemoryStore(var session: PersistedAuthSession? = null) : AuthSessionStore {
    override fun read() = session
    override fun write(session: PersistedAuthSession) { this.session = session }
    override fun clear() { session = null }
}

private class FakeAuthApi(
    private val response: PersistedAuthSession,
    val refreshFailure: Throwable? = null,
) : SupabaseAuthApi {
    var email: String? = null
    var password: String? = null
    var refreshCount = 0
    var lastRefreshToken: String? = null

    override suspend fun signIn(email: String, password: String): PersistedAuthSession {
        this.email = email
        this.password = password
        return response
    }

    override suspend fun refresh(refreshToken: String): PersistedAuthSession {
        refreshCount += 1
        lastRefreshToken = refreshToken
        refreshFailure?.let { throw it }
        return response
    }
}
