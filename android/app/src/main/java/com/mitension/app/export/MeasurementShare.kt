package com.mitension.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mitension.app.data.MeasurementDetail
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    PDF("pdf", "application/pdf"),
}

fun shareMeasurements(context: Context, measurements: List<MeasurementDetail>, format: ExportFormat) {
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(directory, "mitension-${LocalDate.now()}.${format.extension}")
    when (format) {
        ExportFormat.CSV -> file.writeBytes(measurementsToCsv(measurements))
        ExportFormat.PDF -> file.outputStream().use { writeMeasurementsPdf(it, measurements) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    context.startActivity(Intent.createChooser(buildShareIntent(uri.toString(), format), "Compartir informe"))
}

fun buildShareIntent(uri: String, format: ExportFormat): Intent = Intent(Intent.ACTION_SEND).apply {
    type = format.mimeType
    putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uri))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
