package com.mitension.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mitension.app.data.MeasurementsRepository

class MeasurementsViewModelFactory(private val repository: MeasurementsRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MeasurementsViewModel(repository) as T
}
