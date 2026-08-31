package com.mitension.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitension.app.data.MeasurementDetail
import com.mitension.app.data.MeasurementsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class MeasurementsViewModel(private val repository: MeasurementsRepository) : ViewModel() {
    val history: StateFlow<List<MeasurementDetail>> = repository.activeMeasurements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(draft: ConfirmationDraft, onSaved: (MeasurementDetail) -> Unit) = viewModelScope.launch {
        onSaved(repository.save(draft.calculated.confirm(), draft.measuredAt, draft.notes))
    }

    fun load(id: String, onLoaded: (MeasurementDetail?) -> Unit) = viewModelScope.launch {
        onLoaded(repository.measurement(id))
    }
}
