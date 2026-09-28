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

/**
 * Erro que a tela deve mostrar (D-055). Sem internet, um refresh automático com dados já salvos
 * não avisa nada: a tela funciona com o cache. Pedido pelo usuário ("Tentar") ou sem cache, avisa.
 */
fun DataError.shownFor(hasCache: Boolean, userInitiated: Boolean): DataError? =
    if (this == DataError.Network && hasCache && !userInitiated) null else this
