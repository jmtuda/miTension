package com.mitension.domain

/** A temporary reading captured during the two-step input flow. */
data class Reading(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
) {
    init {
        require(systolic > 0) { "systolic must be positive" }
        require(diastolic > 0) { "diastolic must be positive" }
        require(pulse > 0) { "pulse must be positive" }
        require(diastolic < systolic) { "diastolic must be lower than systolic" }
    }
}

/** First flow state. It deliberately exposes no persistible result. */
data class FirstReadingCaptured(val reading: Reading) {
    fun addSecondReading(second: Reading): CalculatedMeasurement =
        CalculatedMeasurement(
            first = reading,
            second = second,
            result = MeasurementValues(
                systolic = roundHalfUpMean(reading.systolic, second.systolic),
                diastolic = roundHalfUpMean(reading.diastolic, second.diastolic),
                pulse = roundHalfUpMean(reading.pulse, second.pulse),
            ),
        )
}

/** Calculated values that still require explicit user confirmation. */
data class CalculatedMeasurement internal constructor(
    val first: Reading,
    val second: Reading,
    val result: MeasurementValues,
) {
    fun confirm(): ConfirmedMeasurement = ConfirmedMeasurement(result)
}

data class MeasurementValues(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
)

/** The only domain type eligible for persistence; it contains no original readings. */
data class ConfirmedMeasurement internal constructor(val values: MeasurementValues)

fun roundHalfUpMean(first: Int, second: Int): Int {
    require(first > 0 && second > 0) { "mean inputs must be positive" }

    val lower = minOf(first, second)
    val difference = maxOf(first, second) - lower
    return lower + difference / 2 + difference % 2
}

