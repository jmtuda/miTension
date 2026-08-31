package com.mitension.app.export

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.mitension.app.data.MeasurementDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MeasurementExportTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val measurement = MeasurementDetail(
        id = "internal-id",
        measuredAt = Instant.parse("2026-08-31T10:00:00Z"),
        systolic = 121,
        diastolic = 81,
        pulse = 62,
        notes = "Línea 1; \"bien\"\nLínea 2",
    )

    @Test fun `csv matches the approved format including BOM offset and escaping`() {
        val csv = measurementsToCsv(listOf(measurement), zone).toString(Charsets.UTF_8)
        assertTrue(csv.startsWith("\uFEFF$CSV_HEADER\r\n"))
        assertTrue(csv.contains("2026-08-31T12:00:00+02:00;121;81;62;\"Línea 1; \"\"bien\"\"\nLínea 2\""))
        assertTrue(csv.endsWith("\r\n"))
        assertFalse(csv.contains(measurement.id))
    }

    @Test fun `date filter is inclusive and uses the local day`() {
        assertEquals(1, filterMeasurements(listOf(measurement), LocalDate.parse("2026-08-31"), LocalDate.parse("2026-08-31"), zone).size)
        assertTrue(filterMeasurements(listOf(measurement), LocalDate.parse("2026-09-01"), null, zone).isEmpty())
    }

    @Test fun `pdf is readable and its row excludes internal metadata`() {
        val output = ByteArrayOutputStream()
        writeMeasurementsPdf(output, listOf(measurement), zone, Instant.parse("2026-08-31T12:00:00Z"))
        assertTrue(output.toByteArray().copyOfRange(0, 4).toString(Charsets.US_ASCII).startsWith("%PDF"))
        val row = measurement.toPdfRow(zone)
        assertEquals("31/08/2026 12:00", row.measuredAt)
        assertEquals("121", row.systolic)
        assertEquals(measurement.notes, row.notes)
        assertFalse(row.toString().contains(measurement.id))
    }

    @Test fun `share intent grants temporary read access without provider coupling`() {
        ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = buildShareIntent("content://com.mitension.app.files/exports/report.pdf", ExportFormat.PDF)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals("content", intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.scheme)
    }
}
