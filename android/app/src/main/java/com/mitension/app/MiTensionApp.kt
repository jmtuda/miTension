package com.mitension.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mitension.app.data.MeasurementDetail
import com.mitension.app.export.ExportFormat
import com.mitension.app.export.filterMeasurements
import com.mitension.app.export.shareMeasurements
import java.time.Instant
import java.time.LocalDate

private enum class Screen { HISTORY, FIRST, SECOND, CONFIRM, DETAIL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiTensionApp(viewModel: MeasurementsViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.HISTORY) }
    var flow by remember { mutableStateOf(RegistrationFlow()) }
    var selected by remember { mutableStateOf<MeasurementDetail?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("miTensión", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
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
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            systolic, { systolic = it }, label = { Text("Sistólica") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            diastolic, { diastolic = it }, label = { Text("Diastólica") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            pulse, { pulse = it }, label = { Text("Pulso") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onNext.let { { it(systolic, diastolic, pulse) } }) { Text("Continuar") }
            OutlinedButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}

@Composable private fun ConfirmScreen(flow: RegistrationFlow, onConfirm: (Instant, String) -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    var notes by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf(Instant.now().toString()) }
    val result = runCatching { flow.calculatedMeasurement() }.getOrNull() ?: return
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Confirmar medición", style = MaterialTheme.typography.headlineMedium)
        Text("Medición 1: ${result.first.systolic}/${result.first.diastolic} · ${result.first.pulse} ppm")
        Text("Medición 2: ${result.second.systolic}/${result.second.diastolic} · ${result.second.pulse} ppm")
        Text("Media: ${result.result.systolic}/${result.result.diastolic} · ${result.result.pulse} ppm")
        OutlinedTextField(dateTime, { dateTime = it }, label = { Text("Fecha y hora (ISO-8601)") })
        OutlinedTextField(notes, { if (it.length <= RegistrationFlow.MAX_NOTES_LENGTH) notes = it }, label = { Text("Nota opcional") })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { runCatching { Instant.parse(dateTime) }.getOrNull()?.let { onConfirm(it, notes) } }) { Text("Guardar") }
            OutlinedButton(onClick = onCancel) { Text("Cancelar") }
        }
    }
}

@Composable private fun HistoryScreen(items: List<MeasurementDetail>, onNew: () -> Unit, onDetail: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var filtersExpanded by remember { mutableStateOf(false) }
    var exportExpanded by remember { mutableStateOf(false) }
    val from = fromText.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val to = toText.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val filtered = filterMeasurements(items, from, to)
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("Nueva medición") }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { filtersExpanded = !filtersExpanded }, modifier = Modifier.weight(1f)) {
                Text("Filtrar por fecha")
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { exportExpanded = true },
                    enabled = filtered.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Exportar") }
                DropdownMenu(expanded = exportExpanded, onDismissRequest = { exportExpanded = false }) {
                    DropdownMenuItem(text = { Text("CSV") }, onClick = {
                        exportExpanded = false
                        shareMeasurements(context, filtered, ExportFormat.CSV)
                    })
                    DropdownMenuItem(text = { Text("PDF") }, onClick = {
                        exportExpanded = false
                        shareMeasurements(context, filtered, ExportFormat.PDF)
                    })
                }
            }
        }
        if (filtersExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(fromText, { fromText = it }, label = { Text("Desde (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(toText, { toText = it }, label = { Text("Hasta (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        Text("Historial", style = MaterialTheme.typography.headlineSmall)
        if (filtered.isEmpty()) Text("No hay mediciones en el intervalo seleccionado.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onDetail(item.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            formatHistoryDateTime(item.measuredAt),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${item.systolic}/${item.diastolic}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${item.pulse} ppm", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable private fun DetailScreen(item: MeasurementDetail?, onBack: () -> Unit, onDelete: (MeasurementDetail) -> Unit, modifier: Modifier) {
    var confirmingDelete by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Detalle", style = MaterialTheme.typography.headlineMedium)
        if (item != null) {
            Text(formatDetailDateTime(item.measuredAt)); Text("${item.systolic}/${item.diastolic} · ${item.pulse} ppm"); item.notes?.let { Text(it) }
            Button(onClick = { confirmingDelete = true }) { Text("Eliminar") }
        }
        OutlinedButton(onClick = onBack) { Text("Volver") }
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
