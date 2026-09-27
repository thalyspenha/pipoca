package com.thalyspenha.pipoca.util

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
