package com.mitension.app

import android.app.Application
import androidx.room.Room
import com.mitension.app.auth.SharedPreferencesAuthSessionStore
import com.mitension.app.auth.SupabaseAuthManager
import com.mitension.app.auth.SupabaseRestAuthApi
import com.mitension.app.data.MiTensionDatabase
import com.mitension.app.sync.MeasurementSyncEngine
import com.mitension.app.sync.SupabaseRestMeasurementSyncApi
import com.mitension.app.sync.SyncScheduler

class MiTensionApplication : Application() {
    val database: MiTensionDatabase by lazy {
        Room.databaseBuilder(this, MiTensionDatabase::class.java, "mitension.db")
            .addMigrations(MiTensionDatabase.MIGRATION_1_2)
            .build()
    }
    val authManager by lazy {
        SupabaseAuthManager(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY,
            SharedPreferencesAuthSessionStore(this),
            SupabaseRestAuthApi(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY),
        )
    }
    val syncEngine by lazy { MeasurementSyncEngine(database.measurementsDao(), SupabaseRestMeasurementSyncApi()) }

    override fun onCreate() {
        super.onCreate()
        if (authManager.isConfigured) SyncScheduler.ensurePeriodic(this)
    }
}
