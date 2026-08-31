package com.mitension.app.export

import com.mitension.app.data.MeasurementDetail
import java.time.LocalDate
import java.time.ZoneId

fun filterMeasurements(
    measurements: List<MeasurementDetail>,
    from: LocalDate?,
    to: LocalDate?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<MeasurementDetail> = measurements.filter { measurement ->
    val date = measurement.measuredAt.atZone(zoneId).toLocalDate()
    (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to))
}
