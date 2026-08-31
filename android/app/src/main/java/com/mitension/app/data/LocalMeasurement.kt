package com.mitension.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mitension.domain.ConfirmedMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

enum class SyncState { PENDING_CREATE, PENDING_DELETE, SYNCED, ERROR }

@Entity(tableName = "measurements")
data class LocalMeasurement(
    @PrimaryKey val id: String,
    val userId: String,
    val measuredAt: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val notes: String?,
    val deletedAt: Long?,
    val syncState: String,
    val lastSyncError: String?,
    val serverUpdatedAt: String?,
)

@Entity(tableName = "sync_cursors", primaryKeys = ["userId"])
data class SyncCursor(
    val userId: String,
    val updatedAt: String,
    val measurementId: String,
)

data class MeasurementDetail(
    val id: String,
    val measuredAt: Instant,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val notes: String?,
)

interface MeasurementsRepository {
    fun activeMeasurements(): Flow<List<MeasurementDetail>>
    suspend fun measurement(id: String): MeasurementDetail?
    suspend fun save(confirmed: ConfirmedMeasurement, measuredAt: Instant, notes: String?): MeasurementDetail
    suspend fun delete(id: String): Boolean
}

class RoomMeasurementsRepository(
    private val dao: MeasurementsDao,
    private val userId: String,
    private val requestSync: () -> Unit = {},
) : MeasurementsRepository {
    override fun activeMeasurements(): Flow<List<MeasurementDetail>> =
        dao.observeActive(userId).map { measurements -> measurements.map(LocalMeasurement::toDetail) }

    override suspend fun measurement(id: String): MeasurementDetail? = dao.activeById(id, userId)?.toDetail()

    override suspend fun save(
        confirmed: ConfirmedMeasurement,
        measuredAt: Instant,
        notes: String?,
    ): MeasurementDetail {
        val entity = LocalMeasurement(
            id = UUID.randomUUID().toString(),
            userId = userId,
            measuredAt = measuredAt.toEpochMilli(),
            systolic = confirmed.values.systolic,
            diastolic = confirmed.values.diastolic,
            pulse = confirmed.values.pulse,
            notes = notes?.trim()?.ifEmpty { null },
            deletedAt = null,
            syncState = SyncState.PENDING_CREATE.name,
            lastSyncError = null,
            serverUpdatedAt = null,
        )
        dao.insert(entity)
        requestSync()
        return entity.toDetail()
    }

    override suspend fun delete(id: String): Boolean {
        val changed = dao.markPendingDelete(id, userId, Instant.now().toEpochMilli()) > 0
        if (changed) requestSync()
        return changed
    }
}

fun LocalMeasurement.toDetail() = MeasurementDetail(
    id = id,
    measuredAt = Instant.ofEpochMilli(measuredAt),
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    notes = notes,
)
