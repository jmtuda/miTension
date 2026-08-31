package com.mitension.app

import com.mitension.domain.ConfirmedMeasurement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import java.time.Instant

class RegistrationFlowTest {
    @Test fun `confirmation persists only calculated average`() {
        val flow = RegistrationFlow()
        flow.captureFirst("121", "79", "61")
        flow.captureSecond("122", "80", "62")

        val draft = flow.confirmation(Instant.parse("2026-01-01T12:00:00Z"), " note ")
        val confirmed: ConfirmedMeasurement = draft.calculated.confirm()

        assertEquals(122, confirmed.values.systolic)
        assertEquals(80, confirmed.values.diastolic)
        assertEquals(62, confirmed.values.pulse)
        assertEquals("note", draft.notes)
    }

    @Test fun `flow validates readings and note length`() {
        val flow = RegistrationFlow()
        assertFailsWith<IllegalArgumentException> { flow.captureFirst("120", "120", "60") }
        flow.captureFirst("120", "80", "60")
        flow.captureSecond("122", "82", "62")
        assertFailsWith<IllegalArgumentException> { flow.confirmation(Instant.now(), "x".repeat(1001)) }
    }

    @Test fun `cancelling leaves no confirmation draft`() {
        val flow = RegistrationFlow()
        flow.captureFirst("120", "80", "60")
        flow.cancel()
        assertFailsWith<IllegalStateException> { flow.confirmation(Instant.now(), "") }
    }
}
