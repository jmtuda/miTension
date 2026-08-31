package com.mitension.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mitension.app.data.MeasurementDetail
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class Screen { HISTORY, FIRST, SECOND, CONFIRM, DETAIL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiTensionApp(viewModel: MeasurementsViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.HISTORY) }
    var flow by remember { mutableStateOf(RegistrationFlow()) }
    var selected by remember { mutableStateOf<MeasurementDetail?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("miTensión") }) }) { padding ->
        when (screen) {
            Screen.HISTORY -> HistoryScreen(history, onNew = { flow = RegistrationFlow(); screen = Screen.FIRST }, onDetail = {
                viewModel.load(it) { detail -> selected = detail; screen = Screen.DETAIL }
            }, Modifier.padding(padding))
            Screen.FIRST -> ReadingScreen("Medición 1", onNext = { s, d, p ->
                runCatching { flow.captureFirst(s, d, p) }.onSuccess { screen = Screen.SECOND }
            }, onCancel = { flow.cancel(); screen = Screen.HISTORY }, Modifier.padding(padding))
            Screen.SECOND -> ReadingScreen("Medición 2", onNext = { s, d, p ->
                runCatching { flow.captureSecond(s, d, p) }.onSuccess { screen = Screen.CONFIRM }
            }, onCancel = { flow.cancel(); screen = Screen.HISTORY }, Modifier.padding(padding))
            Screen.CONFIRM -> ConfirmScreen(flow, onConfirm = { measuredAt, notes ->
                runCatching { flow.confirmation(measuredAt, notes) }.onSuccess { draft ->
                    viewModel.save(draft) { screen = Screen.HISTORY }
                }
            }, onCancel = { flow.cancel(); screen = Screen.HISTORY }, Modifier.padding(padding))
            Screen.DETAIL -> DetailScreen(selected, onBack = { screen = Screen.HISTORY }, onDelete = { item ->
                viewModel.delete(item.id) { selected = null; screen = Screen.HISTORY }
            }, Modifier.padding(padding))
        }
    }
}

@Composable private fun ReadingScreen(title: String, onNext: (String, String, String) -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    var systolic by remember { mutableStateOf("") }; var diastolic by remember { mutableStateOf("") }; var pulse by remember { mutableStateOf("") }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title)
        OutlinedTextField(systolic, { systolic = it }, label = { Text("Sistólica") })
        OutlinedTextField(diastolic, { diastolic = it }, label = { Text("Diastólica") })
        OutlinedTextField(pulse, { pulse = it }, label = { Text("Pulso") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onNext.let { { it(systolic, diastolic, pulse) } }) { Text("Continuar") }; Button(onClick = onCancel) { Text("Cancelar") } }
    }
}

@Composable private fun ConfirmScreen(flow: RegistrationFlow, onConfirm: (Instant, String) -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    var notes by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf(Instant.now().toString()) }
    val result = runCatching { flow.calculatedMeasurement() }.getOrNull() ?: return
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Confirmar medición")
        Text("Medición 1: ${result.first.systolic}/${result.first.diastolic} · ${result.first.pulse} ppm")
        Text("Medición 2: ${result.second.systolic}/${result.second.diastolic} · ${result.second.pulse} ppm")
        Text("Media: ${result.result.systolic}/${result.result.diastolic} · ${result.result.pulse} ppm")
        OutlinedTextField(dateTime, { dateTime = it }, label = { Text("Fecha y hora (ISO-8601)") })
        OutlinedTextField(notes, { if (it.length <= RegistrationFlow.MAX_NOTES_LENGTH) notes = it }, label = { Text("Nota opcional") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { runCatching { Instant.parse(dateTime) }.getOrNull()?.let { onConfirm(it, notes) } }) { Text("Guardar") }; Button(onClick = onCancel) { Text("Cancelar") } }
    }
}

@Composable private fun HistoryScreen(items: List<MeasurementDetail>, onNew: () -> Unit, onDetail: (String) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) { Button(onClick = onNew) { Text("Nueva medición") }; LazyColumn { items(items, key = { it.id }) { item -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onDetail(item.id) }) { Text("${format(item.measuredAt)} · ${item.systolic}/${item.diastolic} · ${item.pulse} ppm", Modifier.padding(16.dp)) } } } }
}

@Composable private fun DetailScreen(item: MeasurementDetail?, onBack: () -> Unit, onDelete: (MeasurementDetail) -> Unit, modifier: Modifier) {
    var confirmingDelete by remember { mutableStateOf(false) }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detalle")
        if (item != null) {
            Text(format(item.measuredAt)); Text("${item.systolic}/${item.diastolic} · ${item.pulse} ppm"); item.notes?.let { Text(it) }
            Button(onClick = { confirmingDelete = true }) { Text("Eliminar") }
        }
        Button(onClick = onBack) { Text("Volver") }
    }
    if (confirmingDelete && item != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Eliminar medición") },
            text = { Text("La medición se ocultará ahora y se eliminará de forma lógica al sincronizar.") },
            confirmButton = { Button(onClick = { confirmingDelete = false; onDelete(item) }) { Text("Eliminar") } },
            dismissButton = { Button(onClick = { confirmingDelete = false }) { Text("Cancelar") } },
        )
    }
}

private fun format(instant: Instant): String = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(instant)
