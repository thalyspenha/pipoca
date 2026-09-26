package com.thalyspenha.pipoca.domain.model

/** Configuração de acesso ao TMDB. Token vazio = chave não configurada (o app segue funcionando offline). */
data class TmdbConfig(val apiToken: String) {
    val isConfigured: Boolean get() = apiToken.isNotBlank()
}
