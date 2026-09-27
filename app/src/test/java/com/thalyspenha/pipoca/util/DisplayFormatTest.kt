package com.thalyspenha.pipoca.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatTest {

    @Test
    fun `duracao em horas e minutos`() {
        assertEquals("2h 16min", formatRuntime(136))
        assertEquals("45min", formatRuntime(45))
        assertEquals("2h", formatRuntime(120))
        assertEquals("0min", formatRuntime(0))
    }

    @Test
    fun `nota TMDB com uma casa e virgula`() {
        assertEquals("8,2", formatVote(8.237))
        assertEquals("10,0", formatVote(10.0))
    }

    @Test
    fun `decimal com virgula e inteiro com ponto de milhar`() {
        assertEquals("2,3", formatDecimal(2.345))
        assertEquals("0,0", formatDecimal(0.0))
        assertEquals("12.345", formatInt(12_345))
        assertEquals("7", formatInt(7))
    }
}
