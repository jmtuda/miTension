package com.mitension.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(measurement: LocalMeasurement)

    @Query("SELECT * FROM measurements WHERE deletedAt IS NULL ORDER BY measuredAt DESC, id DESC")
    fun observeActive(): Flow<List<LocalMeasurement>>

    @Query("SELECT * FROM measurements WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun activeById(id: String): LocalMeasurement?
}
