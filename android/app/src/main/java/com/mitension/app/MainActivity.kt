package com.mitension.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mitension.app.data.RoomMeasurementsRepository
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.mitension.app.sync.SyncScheduler

class MainActivity : ComponentActivity() {
    private val viewModel: MeasurementsViewModel by viewModels {
        val app = application as MiTensionApplication
        val session = checkNotNull(app.sessionProvider.currentSession()) { "Supabase Auth session is required" }
        MeasurementsViewModelFactory(RoomMeasurementsRepository(app.database.measurementsDao(), session.userId) {
            SyncScheduler.enqueueNow(applicationContext)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = (application as MiTensionApplication).sessionProvider.currentSession()
        setContent {
            MaterialTheme {
                if (session == null) Text("Se necesita una sesión autenticada de Supabase")
                else MiTensionApp(viewModel)
            }
        }
    }
}
