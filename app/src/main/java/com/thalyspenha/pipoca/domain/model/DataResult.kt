package com.thalyspenha.pipoca.domain.model

/** Resultado da camada de dados. Exceções de rede/HTTP nunca sobem para a UI; viram [DataError]. */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}

/** Erros de domínio (ver ARCHITECTURE.md e TMDB.md). */
sealed interface DataError {
    /** Sem conexão, timeout, DNS. */
    data object Network : DataError

    /** Recurso não existe no TMDB (HTTP 404). */
    data object NotFound : DataError

    /** Token não configurado em `local.properties`; nenhuma chamada é feita. */
    data object MissingApiKey : DataError

    /** Token recusado pelo TMDB (HTTP 401). */
    data object InvalidApiKey : DataError

    /** Qualquer outra falha (HTTP 5xx, resposta inesperada...). */
    data class Unknown(val cause: Throwable? = null) : DataError
}
