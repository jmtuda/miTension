package com.mitension.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(measurement: LocalMeasurement)

    @Query("SELECT * FROM measurements WHERE userId = :userId AND deletedAt IS NULL ORDER BY measuredAt DESC, id DESC")
    fun observeActive(userId: String): Flow<List<LocalMeasurement>>

    @Query("SELECT * FROM measurements WHERE id = :id AND userId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun activeById(id: String, userId: String): LocalMeasurement?

    @Query("SELECT * FROM measurements WHERE userId = :userId AND syncState IN ('PENDING_CREATE', 'PENDING_DELETE', 'ERROR') ORDER BY measuredAt, id")
    suspend fun pending(userId: String): List<LocalMeasurement>

    @Query("SELECT * FROM measurements WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun byId(id: String, userId: String): LocalMeasurement?

    @Query("UPDATE measurements SET deletedAt = :deletedAt, syncState = 'PENDING_DELETE', lastSyncError = NULL WHERE id = :id AND userId = :userId AND deletedAt IS NULL")
    suspend fun markPendingDelete(id: String, userId: String, deletedAt: Long): Int

    @Query("UPDATE measurements SET syncState = 'SYNCED', lastSyncError = NULL, serverUpdatedAt = :serverUpdatedAt, deletedAt = COALESCE(:deletedAt, deletedAt) WHERE id = :id AND userId = :userId")
    suspend fun markSynced(id: String, userId: String, serverUpdatedAt: String, deletedAt: Long?): Int

    @Query("UPDATE measurements SET syncState = 'ERROR', lastSyncError = :message WHERE id = :id AND userId = :userId")
    suspend fun markError(id: String, userId: String, message: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCursor(cursor: SyncCursor)

    @Query("SELECT * FROM sync_cursors WHERE userId = :userId LIMIT 1")
    suspend fun cursor(userId: String): SyncCursor?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemote(measurement: LocalMeasurement): Long

    @Query("UPDATE measurements SET deletedAt = :deletedAt, syncState = 'SYNCED', lastSyncError = NULL, serverUpdatedAt = :serverUpdatedAt WHERE id = :id AND userId = :userId AND syncState = 'SYNCED'")
    suspend fun updateRemoteTombstone(id: String, userId: String, deletedAt: Long?, serverUpdatedAt: String)

    @Transaction
    suspend fun applyRemotePage(userId: String, measurements: List<LocalMeasurement>, cursor: SyncCursor) {
        measurements.forEach { remote ->
            val inserted = insertRemote(remote)
            if (inserted == -1L) {
                updateRemoteTombstone(remote.id, userId, remote.deletedAt, remote.serverUpdatedAt!!)
            }
        }
        putCursor(cursor)
    }
}
