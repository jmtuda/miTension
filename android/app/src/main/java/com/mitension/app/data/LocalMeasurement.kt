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
    val measuredAt: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val notes: String?,
    val deletedAt: Long?,
    val syncState: String,
    val lastSyncError: String?,
    val serverUpdatedAt: Long?,
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
}

class RoomMeasurementsRepository(private val dao: MeasurementsDao) : MeasurementsRepository {
    override fun activeMeasurements(): Flow<List<MeasurementDetail>> =
        dao.observeActive().map { measurements -> measurements.map(LocalMeasurement::toDetail) }

    override suspend fun measurement(id: String): MeasurementDetail? = dao.activeById(id)?.toDetail()

    override suspend fun save(
        confirmed: ConfirmedMeasurement,
        measuredAt: Instant,
        notes: String?,
    ): MeasurementDetail {
        val entity = LocalMeasurement(
            id = UUID.randomUUID().toString(),
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
        return entity.toDetail()
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
