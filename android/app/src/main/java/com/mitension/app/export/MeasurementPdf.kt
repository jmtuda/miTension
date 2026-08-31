package com.mitension.app.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mitension.app.data.MeasurementDetail
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 36f
private const val TABLE_TOP = 128f
private const val FOOTER_TOP = 806f

fun writeMeasurementsPdf(
    output: OutputStream,
    measurements: List<MeasurementDetail>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    generatedAt: Instant = Instant.now(),
) {
    val document = PdfDocument()
    val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(36, 48, 43); textSize = 9f }
    val bold = Paint(regular).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val rows = measurements.map { it.toPdfRow(zoneId) }
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
    var y = drawHeader(page, pageNumber, generatedAt, zoneId, regular, bold)

    if (rows.isEmpty()) page.canvas.drawText("No hay mediciones en el conjunto filtrado.", MARGIN, y + 28f, regular)
    for (row in rows) {
        val notes = wrapText(row.notes, regular, 188f)
        val height = maxOf(30f, 18f + notes.size * 12f)
        if (y + height > FOOTER_TOP - 18f) {
            drawFooter(page, pageNumber, regular)
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            y = drawHeader(page, pageNumber, generatedAt, zoneId, regular, bold)
        }
        drawRow(page, row, notes, y, height, regular)
        y += height
    }
    drawFooter(page, pageNumber, regular)
    document.finishPage(page)
    document.writeTo(output)
    document.close()
}

data class PdfMeasurementRow(
    val measuredAt: String,
    val systolic: String,
    val diastolic: String,
    val pulse: String,
    val notes: String,
)

fun MeasurementDetail.toPdfRow(zoneId: ZoneId): PdfMeasurementRow = PdfMeasurementRow(
    measuredAt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(zoneId).format(measuredAt),
    systolic = systolic.toString(),
    diastolic = diastolic.toString(),
    pulse = pulse.toString(),
    notes = notes.orEmpty(),
)

private fun drawHeader(page: PdfDocument.Page, pageNumber: Int, generatedAt: Instant, zoneId: ZoneId, regular: Paint, bold: Paint): Float {
    val canvas = page.canvas
    canvas.drawColor(Color.WHITE)
    bold.textSize = 20f; canvas.drawText("miTensión", MARGIN, 46f, bold)
    bold.textSize = 14f; canvas.drawText("Informe de mediciones", MARGIN, 72f, bold)
    regular.textSize = 8f
    val generated = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(zoneId).format(generatedAt)
    canvas.drawText("Generado: $generated", MARGIN, 92f, regular)
    if (pageNumber > 1) canvas.drawText("Continuación", 490f, 72f, regular)
    canvas.drawRect(MARGIN, TABLE_TOP - 20f, PAGE_WIDTH - MARGIN, TABLE_TOP + 4f, Paint().apply { color = Color.rgb(220, 235, 227) })
    bold.textSize = 8f
    listOf("Fecha y hora" to 40f, "Sistólica" to 180f, "Diastólica" to 240f, "Pulso" to 308f, "Notas" to 365f)
        .forEach { (label, x) -> canvas.drawText(label, x, TABLE_TOP - 5f, bold) }
    return TABLE_TOP + 8f
}

private fun drawRow(page: PdfDocument.Page, row: PdfMeasurementRow, notes: List<String>, y: Float, height: Float, paint: Paint) {
    val canvas = page.canvas
    val baseline = y + 14f
    canvas.drawText(row.measuredAt, 40f, baseline, paint)
    canvas.drawText(row.systolic, 184f, baseline, paint)
    canvas.drawText(row.diastolic, 246f, baseline, paint)
    canvas.drawText(row.pulse, 314f, baseline, paint)
    notes.forEachIndexed { index, line -> canvas.drawText(line, 365f, baseline + index * 12f, paint) }
    canvas.drawLine(MARGIN, y + height, PAGE_WIDTH - MARGIN, y + height, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
}

private fun drawFooter(page: PdfDocument.Page, pageNumber: Int, paint: Paint) {
    page.canvas.drawLine(MARGIN, FOOTER_TOP, PAGE_WIDTH - MARGIN, FOOTER_TOP, Paint().apply { color = Color.LTGRAY })
    page.canvas.drawText("Página $pageNumber", 500f, 826f, paint)
}

private fun wrapText(value: String, paint: Paint, width: Float): List<String> {
    val normalized = value.replace(Regex("\\s+"), " ").trim()
    if (normalized.isEmpty()) return listOf("")
    val lines = mutableListOf<String>()
    var remaining = normalized
    while (remaining.isNotEmpty()) {
        var count = paint.breakText(remaining, true, width, null).coerceAtLeast(1)
        if (count < remaining.length) {
            val boundary = remaining.substring(0, count).lastIndexOf(' ')
            if (boundary > 0) count = boundary
        }
        lines += remaining.substring(0, count).trim()
        remaining = remaining.substring(count).trimStart()
    }
    return lines
}
