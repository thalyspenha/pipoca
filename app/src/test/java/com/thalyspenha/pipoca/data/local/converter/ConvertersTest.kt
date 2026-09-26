package com.thalyspenha.pipoca.data.local.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `LocalDate vira string ISO e volta igual`() {
        val date = LocalDate.of(2026, 9, 26)

        val stored = converters.localDateToString(date)

        assertEquals("2026-09-26", stored)
        assertEquals(date, converters.stringToLocalDate(stored))
    }

    @Test
    fun `null continua null nos dois sentidos`() {
        assertNull(converters.localDateToString(null))
        assertNull(converters.stringToLocalDate(null))
    }
}
