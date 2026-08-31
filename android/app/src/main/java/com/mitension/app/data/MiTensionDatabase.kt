package com.mitension.app.data

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LocalMeasurement::class, SyncCursor::class], version = 2, exportSchema = false)
abstract class MiTensionDatabase : RoomDatabase() {
    abstract fun measurementsDao(): MeasurementsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE measurements_v2 (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        measuredAt INTEGER NOT NULL,
                        systolic INTEGER NOT NULL,
                        diastolic INTEGER NOT NULL,
                        pulse INTEGER NOT NULL,
                        notes TEXT,
                        deletedAt INTEGER,
                        syncState TEXT NOT NULL,
                        lastSyncError TEXT,
                        serverUpdatedAt TEXT
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO measurements_v2 (
                        id, userId, measuredAt, systolic, diastolic, pulse, notes,
                        deletedAt, syncState, lastSyncError, serverUpdatedAt
                    )
                    SELECT id, '', measuredAt, systolic, diastolic, pulse, notes,
                        deletedAt, syncState, lastSyncError, NULL
                    FROM measurements
                """.trimIndent())
                database.execSQL("DROP TABLE measurements")
                database.execSQL("ALTER TABLE measurements_v2 RENAME TO measurements")
                database.execSQL("CREATE TABLE IF NOT EXISTS sync_cursors (userId TEXT NOT NULL, updatedAt TEXT NOT NULL, measurementId TEXT NOT NULL, PRIMARY KEY(userId))")
            }
        }
    }
}
