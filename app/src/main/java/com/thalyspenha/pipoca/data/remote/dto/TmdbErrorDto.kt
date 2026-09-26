package com.thalyspenha.pipoca.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Corpo de erro do TMDB (ex.: 34 = não encontrado, 7 = chave inválida). */
@Serializable
data class TmdbErrorDto(
    @SerialName("status_code") val statusCode: Int = 0,
    @SerialName("status_message") val statusMessage: String = "",
)
