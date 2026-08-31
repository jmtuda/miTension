package com.mitension.app.export

import com.mitension.app.data.MeasurementDetail
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val CSV_HEADER = "fecha_hora;sistolica;diastolica;pulso;notas"

fun measurementsToCsv(
    measurements: List<MeasurementDetail>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): ByteArray {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").withZone(zoneId)
    val rows = measurements.map { measurement ->
        listOf(
            escapeCsv(formatter.format(measurement.measuredAt)),
            measurement.systolic.toString(),
            measurement.diastolic.toString(),
            measurement.pulse.toString(),
            escapeCsv(measurement.notes.orEmpty()),
        ).joinToString(";")
    }
    return ("\uFEFF" + (listOf(CSV_HEADER) + rows).joinToString("\r\n") + "\r\n")
        .toByteArray(Charsets.UTF_8)
}

fun escapeCsv(value: String): String =
    if (value.any { it == ';' || it == '"' || it == '\r' || it == '\n' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
