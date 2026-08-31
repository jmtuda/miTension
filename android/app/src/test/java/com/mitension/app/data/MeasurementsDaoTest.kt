package com.mitension.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MeasurementsDaoTest {
    private lateinit var database: MiTensionDatabase
    private lateinit var dao: MeasurementsDao

    @Before fun setUp() { database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MiTensionDatabase::class.java).allowMainThreadQueries().build(); dao = database.measurementsDao() }
    @After fun tearDown() = database.close()

    @Test fun `Room stores active records newest first and excludes soft deletes`() = runBlocking {
        dao.insert(item("old", 1, null)); dao.insert(item("deleted", 3, 4)); dao.insert(item("new", 2, null))
        assertEquals(listOf("new", "old"), dao.observeActive().first().map { it.id })
    }

    private fun item(id: String, timestamp: Long, deletedAt: Long?) = LocalMeasurement(id, timestamp, 120, 80, 60, null, deletedAt, SyncState.PENDING_CREATE.name, null, null)
}
