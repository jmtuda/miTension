package com.mitension.app.sync

import com.mitension.app.data.LocalMeasurement
import com.mitension.app.data.MeasurementsDao
import com.mitension.app.data.SyncCursor
import com.mitension.app.data.SyncState
import java.time.Instant

class MeasurementSyncEngine(
    private val dao: MeasurementsDao,
    private val api: MeasurementSyncApi,
    private val pageSize: Int = 100,
) {
    suspend fun synchronize(session: SupabaseSession) {
        uploadPending(session)
        downloadChanges(session)
    }

    private suspend fun uploadPending(session: SupabaseSession) {
        dao.pending(session.userId).forEach { local ->
            try {
                val remote = if (local.deletedAt != null || local.syncState == SyncState.PENDING_DELETE.name) {
                    // A measurement may be deleted before its first upload. Creating the immutable
                    // row first guarantees that the server retains a tombstone for every client UUID.
                    if (local.serverUpdatedAt == null) api.create(session, local)
                    api.delete(session, local)
                } else {
                    api.create(session, local)
                }
                dao.markSynced(local.id, session.userId, remote.updatedAt, remote.deletedAt?.let(Instant::parse)?.toEpochMilli())
            } catch (error: Exception) {
                dao.markError(local.id, session.userId, error.safeMessage())
                throw error
            }
        }
    }

    private suspend fun downloadChanges(session: SupabaseSession) {
        while (true) {
            val stored = dao.cursor(session.userId)
            val page = api.changes(session, stored?.let { SyncPosition(it.updatedAt, it.measurementId) }, pageSize)
            if (page.isEmpty()) return
            val localRows = page.map { it.toLocal(session.userId) }
            val last = page.last()
            dao.applyRemotePage(session.userId, localRows, SyncCursor(session.userId, last.updatedAt, last.id))
            if (page.size < pageSize) return
        }
    }

    private fun RemoteMeasurement.toLocal(userId: String) = LocalMeasurement(
        id = id, userId = userId, measuredAt = Instant.parse(measuredAt).toEpochMilli(),
        systolic = systolic, diastolic = diastolic, pulse = pulse, notes = notes,
        deletedAt = deletedAt?.let(Instant::parse)?.toEpochMilli(), syncState = SyncState.SYNCED.name,
        lastSyncError = null, serverUpdatedAt = updatedAt,
    )

    private fun Throwable.safeMessage(): String = when (this) {
        is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
        is java.io.IOException -> "No se pudo completar la sincronización"
        else -> "La sincronización rechazó la operación"
    }
}
