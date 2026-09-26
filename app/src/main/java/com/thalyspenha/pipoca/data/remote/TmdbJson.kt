package com.thalyspenha.pipoca.data.remote

import kotlinx.serialization.json.Json

/**
 * Configuração JSON do TMDB, compartilhada entre Retrofit e testes.
 * O TMDB adiciona campos com frequência e às vezes manda `null` onde não se espera.
 */
val TmdbJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
