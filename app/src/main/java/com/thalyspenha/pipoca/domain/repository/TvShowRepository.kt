package com.thalyspenha.pipoca.domain.repository

import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import kotlinx.coroutines.flow.Flow

/** Mesmo contrato de [MovieRepository], para séries. */
interface TvShowRepository {
    fun observeTvShowDetails(id: Long): Flow<TvShowDetails?>

    suspend fun refreshTvShowDetails(id: Long, force: Boolean = false): DataResult<Unit>
}
