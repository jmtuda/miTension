package com.mitension.app

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class UiFormattingTest {
    @Test
    fun `history uses compact two digit year`() {
        val instant = Instant.parse("2026-08-31T18:30:00Z")

        assertEquals("31/08/26 20:30", formatHistoryDateTime(instant, ZoneId.of("Europe/Madrid")))
    }

    @Test
    fun `detail keeps full year`() {
        val instant = Instant.parse("2026-08-31T18:30:00Z")

        assertEquals("31/08/2026 20:30", formatDetailDateTime(instant, ZoneId.of("Europe/Madrid")))
    }
}
