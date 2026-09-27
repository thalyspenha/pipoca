package com.thalyspenha.pipoca.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Duração em minutos como "2h 16min", "45min" ou "2h". */
fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}min"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}min"
    }
}

/** Nota TMDB (0–10) com uma casa e vírgula: 8.237 → "8,2". */
fun formatVote(vote: Double): String = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", vote)

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Data curta brasileira: 2008-01-20 → "20/01/2008". */
fun formatDate(date: LocalDate): String = DATE_FORMAT.format(date)

/** Código do episódio: T2E5 (temporada 0 aparece como "Especial 5"). */
fun episodeCode(seasonNumber: Int, episodeNumber: Int): String =
    if (seasonNumber == 0) "Especial $episodeNumber" else "T${seasonNumber}E$episodeNumber"

/** Número com uma casa e vírgula: 2.345 → "2,3". */
fun formatDecimal(value: Double): String = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", value)

/** Inteiro com separador de milhar pt-BR: 12345 → "12.345". */
fun formatInt(value: Long): String = String.format(Locale.forLanguageTag("pt-BR"), "%,d", value)
