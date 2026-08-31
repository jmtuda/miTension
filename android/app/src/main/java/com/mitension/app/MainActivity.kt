package com.mitension.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.room.Room
import com.mitension.app.data.MiTensionDatabase
import com.mitension.app.data.RoomMeasurementsRepository
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MeasurementsViewModel by viewModels {
        val database = Room.databaseBuilder(applicationContext, MiTensionDatabase::class.java, "mitension.db").build()
        MeasurementsViewModelFactory(RoomMeasurementsRepository(database.measurementsDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MiTensionApp(viewModel)
            }
        }
    }
}
