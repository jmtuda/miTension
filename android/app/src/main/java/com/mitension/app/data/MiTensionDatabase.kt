package com.mitension.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalMeasurement::class], version = 1, exportSchema = false)
abstract class MiTensionDatabase : RoomDatabase() {
    abstract fun measurementsDao(): MeasurementsDao
}
