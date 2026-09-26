package com.thalyspenha.pipoca.data.remote

import com.thalyspenha.pipoca.data.remote.dto.MovieDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.MovieSummaryDto
import com.thalyspenha.pipoca.data.remote.dto.PagedResponseDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowDetailsDto
import com.thalyspenha.pipoca.data.remote.dto.TvShowSummaryDto
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.TmdbConfig
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Único ponto de acesso ao [TmdbApi]. Converte exceções em [DataError];
 * sem token configurado, nem chama a rede.
 */
@Singleton
class TmdbRemoteDataSource @Inject constructor(
    private val api: TmdbApi,
    private val tmdbConfig: TmdbConfig,
) {
    suspend fun searchMovies(query: String, page: Int = 1): DataResult<PagedResponseDto<MovieSummaryDto>> =
        call { api.searchMovies(query, page) }

    suspend fun searchTvShows(query: String, page: Int = 1): DataResult<PagedResponseDto<TvShowSummaryDto>> =
        call { api.searchTvShows(query, page) }

    suspend fun getMovieDetails(id: Long): DataResult<MovieDetailsDto> =
        call { api.getMovieDetails(id) }

    suspend fun getTvShowDetails(id: Long): DataResult<TvShowDetailsDto> =
        call { api.getTvShowDetails(id) }

    private suspend fun <T> call(block: suspend () -> T): DataResult<T> {
        if (!tmdbConfig.isConfigured) return DataResult.Failure(DataError.MissingApiKey)
        return try {
            DataResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            DataResult.Failure(e.toDataError())
        } catch (_: IOException) {
            DataResult.Failure(DataError.Network)
        } catch (e: SerializationException) {
            DataResult.Failure(DataError.Unknown(e))
        } catch (e: RuntimeException) {
            DataResult.Failure(DataError.Unknown(e))
        }
    }

    private fun HttpException.toDataError(): DataError = when (code()) {
        401 -> DataError.InvalidApiKey
        404 -> DataError.NotFound
        else -> DataError.Unknown(this)
    }
}
