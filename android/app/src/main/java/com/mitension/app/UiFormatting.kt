package com.mitension.app

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val historyFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
private val detailFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

internal fun formatHistoryDateTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
    historyFormatter.withZone(zoneId).format(instant)

internal fun formatDetailDateTime(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
    detailFormatter.withZone(zoneId).format(instant)
