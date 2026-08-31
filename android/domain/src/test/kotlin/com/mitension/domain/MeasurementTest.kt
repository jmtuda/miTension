package com.mitension.domain

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MeasurementTest {
    @Test
    fun `mean follows the shared ROUND_HALF_UP contract`() {
        contractCases().forEach { (first, second, expected) ->
            assertEquals(expected, roundHalfUpMean(first, second), "$first, $second")
        }
    }

    @Test
    fun `calculates all fields and only confirmation creates persistible type`() {
        val firstStep = FirstReadingCaptured(Reading(121, 79, 61))
        val calculated = firstStep.addSecondReading(Reading(122, 82, 62))

        assertEquals(MeasurementValues(122, 81, 62), calculated.result)
        assertEquals(ConfirmedMeasurement(MeasurementValues(122, 81, 62)), calculated.confirm())
    }

    @Test
    fun `confirmed measurement does not retain original readings`() {
        val confirmed = FirstReadingCaptured(Reading(120, 80, 60))
            .addSecondReading(Reading(122, 82, 62))
            .confirm()

        assertEquals(MeasurementValues(121, 81, 61), confirmed.values)
    }

    @Test
    fun `validates only documented unequivocal invariants`() {
        assertFailsWith<IllegalArgumentException> { Reading(0, 80, 60) }
        assertFailsWith<IllegalArgumentException> { Reading(120, 0, 60) }
        assertFailsWith<IllegalArgumentException> { Reading(120, 80, 0) }
        assertFailsWith<IllegalArgumentException> { Reading(80, 80, 60) }
    }

    private fun contractCases(): List<Triple<Int, Int, Int>> {
        val path = Path.of(requireNotNull(System.getProperty("measurement.contract.path")))
        return Files.readAllLines(path)
            .drop(1)
            .filter(String::isNotBlank)
            .map { line ->
                val (first, second, expected) = line.split(',').map(String::toInt)
                Triple(first, second, expected)
            }
    }
}
