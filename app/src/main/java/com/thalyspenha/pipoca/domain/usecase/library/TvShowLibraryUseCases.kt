package com.thalyspenha.pipoca.domain.usecase.library

import com.thalyspenha.pipoca.domain.model.LibraryTvShow
import com.thalyspenha.pipoca.domain.model.PERSONAL_RATING_RANGE
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.domain.repository.LibraryRepository
import java.time.Clock
import javax.inject.Inject

/** Mesmas regras dos filmes (D-029): alterar série fora da biblioteca adiciona-a como `WANT_TO_WATCH`. */
private suspend fun LibraryRepository.updateTvShow(
    showId: Long,
    clock: Clock,
    change: (LibraryTvShow) -> LibraryTvShow,
) {
    val now = clock.instant()
    val current = getTvShow(showId)
        ?: LibraryTvShow(showId, TvShowStatus.WANT_TO_WATCH, addedAt = now, updatedAt = now)
    saveTvShow(change(current).copy(updatedAt = now))
}

/** Adiciona à biblioteca. Se já estiver, não muda nada. */
class AddTvShowToLibraryUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(showId: Long, status: TvShowStatus = TvShowStatus.WANT_TO_WATCH) {
        if (repository.getTvShow(showId) != null) return
        repository.updateTvShow(showId, clock) { it.copy(status = status) }
    }
}

class RemoveTvShowFromLibraryUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(showId: Long) = repository.removeTvShow(showId)
}

/**
 * Quero assistir, assistindo ou concluída (adiciona se preciso).
 * Episódios e histórico de séries chegam na fase de episódios.
 */
class SetTvShowStatusUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(showId: Long, status: TvShowStatus) {
        if (repository.getTvShow(showId)?.status == status) return
        repository.updateTvShow(showId, clock) { it.copy(status = status) }
    }
}

class SetTvShowFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(showId: Long, favorite: Boolean) {
        if (!favorite && repository.getTvShow(showId) == null) return
        repository.updateTvShow(showId, clock) { it.copy(isFavorite = favorite) }
    }
}

/** Nota pessoal 1–10; `null` remove a nota. */
class SetTvShowRatingUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(showId: Long, rating: Int?) {
        require(rating == null || rating in PERSONAL_RATING_RANGE) { "Nota fora de $PERSONAL_RATING_RANGE: $rating" }
        if (rating == null && repository.getTvShow(showId) == null) return
        repository.updateTvShow(showId, clock) { it.copy(rating = rating) }
    }
}
