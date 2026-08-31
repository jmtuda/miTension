package com.mitension.app

import android.app.Application
import androidx.room.Room
import com.mitension.app.data.MiTensionDatabase
import com.mitension.app.sync.MeasurementSyncEngine
import com.mitension.app.sync.SharedPreferencesSessionProvider
import com.mitension.app.sync.SupabaseRestMeasurementSyncApi
import com.mitension.app.sync.SyncScheduler

class MiTensionApplication : Application() {
    val database: MiTensionDatabase by lazy {
        Room.databaseBuilder(this, MiTensionDatabase::class.java, "mitension.db")
            .addMigrations(MiTensionDatabase.MIGRATION_1_2)
            .build()
    }
    val sessionProvider by lazy { SharedPreferencesSessionProvider(this) }
    val syncEngine by lazy { MeasurementSyncEngine(database.measurementsDao(), SupabaseRestMeasurementSyncApi()) }

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.ensurePeriodic(this)
        SyncScheduler.enqueueNow(this)
    }
}
