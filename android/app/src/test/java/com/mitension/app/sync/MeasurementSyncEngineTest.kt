package com.mitension.app.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mitension.app.data.LocalMeasurement
import com.mitension.app.data.MeasurementsDao
import com.mitension.app.data.MiTensionDatabase
import com.mitension.app.data.SyncState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class MeasurementSyncEngineTest {
    private lateinit var database: MiTensionDatabase
    private lateinit var dao: MeasurementsDao
    private lateinit var api: FakeApi
    private lateinit var engine: MeasurementSyncEngine
    private val session = SupabaseSession("https://example.supabase.co", "anon", "user-token", USER)

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MiTensionDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = database.measurementsDao(); api = FakeApi(); engine = MeasurementSyncEngine(dao, api, 2)
    }
    @After fun tearDown() = database.close()

    @Test fun `offline create uploads later and retries idempotently without duplicates`() = runBlocking {
        dao.insert(local("a"))
        assertEquals(SyncState.PENDING_CREATE.name, dao.byId("a", USER)?.syncState)

        engine.synchronize(session)
        assertEquals(SyncState.SYNCED.name, dao.byId("a", USER)?.syncState)
        assertNotNull(dao.byId("a", USER)?.serverUpdatedAt)
        assertEquals(1, api.rows(USER).size)

        dao.markError("a", USER, "retry")
        engine.synchronize(session)
        assertEquals(1, api.rows(USER).size)
    }

    @Test fun `offline soft delete hides immediately and propagates tombstone`() = runBlocking {
        dao.insert(local("a")); engine.synchronize(session)
        assertEquals(1, dao.markPendingDelete("a", USER, Instant.parse("2026-08-31T10:00:00Z").toEpochMilli()))
        assertNull(dao.activeById("a", USER))

        engine.synchronize(session)
        assertNotNull(api.rows(USER).single().deletedAt)
        assertEquals(SyncState.SYNCED.name, dao.byId("a", USER)?.syncState)
    }

    @Test fun `delete before first upload still creates one server tombstone`() = runBlocking {
        dao.insert(local("never-uploaded"))
        dao.markPendingDelete("never-uploaded", USER, Instant.parse("2026-08-31T10:00:00Z").toEpochMilli())

        engine.synchronize(session)

        val remote = api.rows(USER).single()
        assertEquals("never-uploaded", remote.id)
        assertNotNull(remote.deletedAt)
        assertEquals(SyncState.SYNCED.name, dao.byId("never-uploaded", USER)?.syncState)
    }

    @Test fun `incremental pages include creates and deletes and preserve cursor`() = runBlocking {
        api.seed(USER, remote("one", "2026-08-31T10:00:00.000001Z"))
        api.seed(USER, remote("two", "2026-08-31T10:00:00.000001Z", "2026-08-31T11:00:00Z"))
        api.seed(USER, remote("three", "2026-08-31T10:00:01Z"))

        engine.synchronize(session)
        assertNotNull(dao.activeById("one", USER)); assertNull(dao.activeById("two", USER)); assertNotNull(dao.activeById("three", USER))
        assertEquals("three", dao.cursor(USER)?.measurementId)

        api.seed(USER, remote("four", "2026-08-31T10:00:02Z"))
        engine.synchronize(session)
        assertNotNull(dao.activeById("four", USER))
        assertEquals(1, api.changeRequests.count { it?.id == "three" })
    }

    @Test fun `interruption after upload recovers without duplicate or lost download`() = runBlocking {
        dao.insert(local("local")); api.seed(USER, remote("remote", "2026-08-31T10:00:03Z")); api.failNextChanges = true
        runCatching { engine.synchronize(session) }
        assertEquals(1, api.rows(USER).count { it.id == "local" })

        engine.synchronize(session)
        assertEquals(1, api.rows(USER).count { it.id == "local" })
        assertNotNull(dao.activeById("remote", USER))
    }

    @Test fun `server and Room isolation prevent another user from leaking into sync`() = runBlocking {
        dao.insert(local("other-local", "other-user")); api.seed("other-user", remote("other-remote", "2026-08-31T10:00:04Z"))
        engine.synchronize(session)
        assertNull(dao.byId("other-remote", USER))
        assertEquals(SyncState.PENDING_CREATE.name, dao.byId("other-local", "other-user")?.syncState)
    }

    private fun local(id: String, userId: String = USER) = LocalMeasurement(
        id, userId, Instant.parse("2026-08-31T08:00:00Z").toEpochMilli(), 120, 80, 60,
        null, null, SyncState.PENDING_CREATE.name, null, null,
    )

    private fun remote(id: String, updatedAt: String, deletedAt: String? = null) = RemoteMeasurement(
        id, "2026-08-31T08:00:00Z", 120, 80, 60, null, deletedAt, updatedAt,
    )

    companion object { private const val USER = "user-one" }
}

private class FakeApi : MeasurementSyncApi {
    private val data = mutableMapOf<String, MutableMap<String, RemoteMeasurement>>()
    private var version = 10
    var failNextChanges = false
    val changeRequests = mutableListOf<SyncPosition?>()

    fun seed(userId: String, remote: RemoteMeasurement) { data.getOrPut(userId, ::mutableMapOf)[remote.id] = remote }
    fun rows(userId: String) = data[userId]?.values.orEmpty()

    override suspend fun create(session: SupabaseSession, local: LocalMeasurement): RemoteMeasurement =
        data.getOrPut(session.userId, ::mutableMapOf).getOrPut(local.id) {
            RemoteMeasurement(local.id, Instant.ofEpochMilli(local.measuredAt).toString(), local.systolic, local.diastolic, local.pulse, local.notes, null, nextVersion())
        }

    override suspend fun delete(session: SupabaseSession, local: LocalMeasurement): RemoteMeasurement {
        val rows = data.getOrPut(session.userId, ::mutableMapOf)
        val existing = rows[local.id] ?: create(session, local)
        return if (existing.deletedAt != null) existing else existing.copy(deletedAt = Instant.ofEpochMilli(local.deletedAt!!).toString(), updatedAt = nextVersion()).also { rows[local.id] = it }
    }

    override suspend fun changes(session: SupabaseSession, after: SyncPosition?, limit: Int): List<RemoteMeasurement> {
        if (failNextChanges) { failNextChanges = false; throw IOException("interrupted") }
        changeRequests += after
        return rows(session.userId).sortedWith(compareBy(RemoteMeasurement::updatedAt, RemoteMeasurement::id))
            .filter { after == null || it.updatedAt > after.updatedAt || (it.updatedAt == after.updatedAt && it.id > after.id) }.take(limit)
    }

    private fun nextVersion() = "2026-08-31T12:00:${(version++).toString().padStart(2, '0')}Z"
}
