package com.thalyspenha.pipoca.presentation.components

import com.thalyspenha.pipoca.domain.model.DataError

/** Mensagem amigável para cada erro da camada de dados. */
fun DataError.toMessage(): String = when (this) {
    DataError.Network -> "Sem conexão com a internet. Verifique a rede e tente novamente."
    DataError.NotFound -> "Não encontrado no TMDB."
    DataError.MissingApiKey ->
        "Configure sua chave TMDB em local.properties (TMDB_API_TOKEN) para buscar filmes e séries."
    DataError.InvalidApiKey -> "A chave TMDB foi recusada. Confira o TMDB_API_TOKEN em local.properties."
    is DataError.Unknown -> "Algo deu errado ao falar com o TMDB. Tente novamente."
}

/** Tentar de novo só faz sentido quando o problema pode ser passageiro. */
val DataError.isRetryable: Boolean
    get() = this is DataError.Network || this is DataError.Unknown
