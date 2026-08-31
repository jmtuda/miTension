package com.mitension.app

import com.mitension.domain.CalculatedMeasurement
import com.mitension.domain.FirstReadingCaptured
import com.mitension.domain.Reading
import java.time.Instant

/** Temporary in-memory flow. Only [confirmation] can be sent to persistence. */
class RegistrationFlow {
    private var first: FirstReadingCaptured? = null
    private var calculated: CalculatedMeasurement? = null

    fun captureFirst(systolic: String, diastolic: String, pulse: String) {
        first = FirstReadingCaptured(parseReading(systolic, diastolic, pulse))
        calculated = null
    }

    fun captureSecond(systolic: String, diastolic: String, pulse: String): CalculatedMeasurement {
        val firstReading = checkNotNull(first) { "first reading is required" }
        return firstReading.addSecondReading(parseReading(systolic, diastolic, pulse)).also { calculated = it }
    }

    fun confirmation(measuredAt: Instant, notes: String): ConfirmationDraft {
        require(notes.length <= MAX_NOTES_LENGTH) { "notes must contain at most $MAX_NOTES_LENGTH characters" }
        return ConfirmationDraft(checkNotNull(calculated) { "second reading is required" }, measuredAt, notes.trim().ifEmpty { null })
    }

    fun calculatedMeasurement(): CalculatedMeasurement =
        checkNotNull(calculated) { "second reading is required" }

    fun cancel() {
        first = null
        calculated = null
    }

    private fun parseReading(systolic: String, diastolic: String, pulse: String): Reading = Reading(
        systolic = systolic.toIntOrNull() ?: throw IllegalArgumentException("systolic must be an integer"),
        diastolic = diastolic.toIntOrNull() ?: throw IllegalArgumentException("diastolic must be an integer"),
        pulse = pulse.toIntOrNull() ?: throw IllegalArgumentException("pulse must be an integer"),
    )

    companion object { const val MAX_NOTES_LENGTH = 1000 }
}

data class ConfirmationDraft(
    val calculated: CalculatedMeasurement,
    val measuredAt: Instant,
    val notes: String?,
)
