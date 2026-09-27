package com.thalyspenha.pipoca.domain.usecase.library

import com.thalyspenha.pipoca.domain.model.LibraryMovie
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.domain.model.PERSONAL_RATING_RANGE
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Regras comuns: favoritar ou dar nota a um filme fora da biblioteca adiciona-o como `WANT_TO_WATCH` (D-029);
 * toda alteração atualiza `updatedAt`.
 */
private suspend fun LibraryRepository.updateMovie(
    movieId: Long,
    clock: Clock,
    change: (LibraryMovie) -> LibraryMovie,
) {
    val now = clock.instant()
    val current = getMovie(movieId)
        ?: LibraryMovie(movieId, MovieStatus.WANT_TO_WATCH, addedAt = now, updatedAt = now)
    saveMovie(change(current).copy(updatedAt = now))
}

/** Adiciona à biblioteca. Se já estiver, não muda nada (não sobrescreve status, nota nem favorito). */
class AddMovieToLibraryUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(movieId: Long, status: MovieStatus = MovieStatus.WANT_TO_WATCH) {
        if (repository.getMovie(movieId) != null) return
        if (status == MovieStatus.WATCHED) {
            val now = clock.instant()
            repository.saveMovieWatched(LibraryMovie(movieId, status, addedAt = now, updatedAt = now), watchedAt = now)
        } else {
            repository.updateMovie(movieId, clock) { it.copy(status = status) }
        }
    }
}

class RemoveMovieFromLibraryUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(movieId: Long) = repository.removeMovie(movieId)
}

/**
 * Marca assistido ou quero assistir (adiciona se preciso).
 * Passar a `WATCHED` registra uma visualização no histórico; marcar de novo quem já está `WATCHED` não duplica.
 * Voltar a `WANT_TO_WATCH` mantém o histórico (ex.: quer reassistir).
 */
class SetMovieStatusUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(movieId: Long, status: MovieStatus) {
        val current = repository.getMovie(movieId)
        if (current?.status == status) return
        if (status == MovieStatus.WATCHED) {
            val now = clock.instant()
            val movie = current?.copy(status = status, updatedAt = now)
                ?: LibraryMovie(movieId, status, addedAt = now, updatedAt = now)
            repository.saveMovieWatched(movie, watchedAt = now)
        } else {
            repository.updateMovie(movieId, clock) { it.copy(status = status) }
        }
    }
}

class SetMovieFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(movieId: Long, favorite: Boolean) {
        if (!favorite && repository.getMovie(movieId) == null) return
        repository.updateMovie(movieId, clock) { it.copy(isFavorite = favorite) }
    }
}

/** Nota pessoal 1–10; `null` remove a nota. */
class SetMovieRatingUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(movieId: Long, rating: Int?) {
        require(rating == null || rating in PERSONAL_RATING_RANGE) { "Nota fora de $PERSONAL_RATING_RANGE: $rating" }
        if (rating == null && repository.getMovie(movieId) == null) return
        repository.updateMovie(movieId, clock) { it.copy(rating = rating) }
    }
}
