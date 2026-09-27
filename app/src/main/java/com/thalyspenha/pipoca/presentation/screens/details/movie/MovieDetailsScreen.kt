package com.thalyspenha.pipoca.presentation.screens.details.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.CastMember
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.Genre
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.model.MovieStatus
import com.thalyspenha.pipoca.presentation.components.ErrorContent
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.details.CastRow
import com.thalyspenha.pipoca.presentation.components.details.CollectionSection
import com.thalyspenha.pipoca.presentation.components.details.DetailsHeader
import com.thalyspenha.pipoca.presentation.components.details.DetailsScaffold
import com.thalyspenha.pipoca.presentation.components.details.DetailsSection
import com.thalyspenha.pipoca.presentation.components.details.FavoriteButton
import com.thalyspenha.pipoca.presentation.components.details.GenreChips
import com.thalyspenha.pipoca.presentation.components.details.Overview
import com.thalyspenha.pipoca.presentation.components.details.RatingSelector
import com.thalyspenha.pipoca.presentation.components.details.StatusSelector
import com.thalyspenha.pipoca.presentation.components.isRetryable
import com.thalyspenha.pipoca.presentation.components.toMessage
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.formatRuntime
import com.thalyspenha.pipoca.util.formatVote
import java.time.LocalDate

@Composable
fun MovieDetailsScreen(
    onBack: () -> Unit,
    onAddToCollection: () -> Unit,
    onEditCollectionItem: (itemId: Long) -> Unit,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MovieDetailsContent(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.refresh() },
        onDismissRefreshError = viewModel::dismissRefreshError,
        onStatusClick = viewModel::onStatusClick,
        onFavoriteClick = viewModel::onFavoriteClick,
        onRatingChange = viewModel::onRatingChange,
        onAddToCollection = onAddToCollection,
        onEditCollectionItem = onEditCollectionItem,
    )
}

@Composable
private fun MovieDetailsContent(
    state: MovieDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDismissRefreshError: () -> Unit,
    onStatusClick: (MovieStatus) -> Unit,
    onFavoriteClick: () -> Unit,
    onRatingChange: (Int?) -> Unit,
    onAddToCollection: () -> Unit,
    onEditCollectionItem: (Long) -> Unit,
) {
    val success = state as? MovieDetailsUiState.Success
    DetailsScaffold(
        title = success?.movie?.title ?: "Filme",
        onBack = onBack,
        refreshError = success?.refreshError,
        onRetry = onRetry,
        onDismissRefreshError = onDismissRefreshError,
    ) { modifier ->
        when (state) {
            MovieDetailsUiState.Loading -> LoadingContent(modifier)
            is MovieDetailsUiState.Error -> ErrorContent(
                message = state.error.toMessage(),
                onRetry = onRetry.takeIf { state.error.isRetryable },
                modifier = modifier,
            )
            is MovieDetailsUiState.Success -> MovieDetailsBody(
                state = state,
                onStatusClick = onStatusClick,
                onFavoriteClick = onFavoriteClick,
                onRatingChange = onRatingChange,
                onAddToCollection = onAddToCollection,
                onEditCollectionItem = onEditCollectionItem,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun MovieDetailsBody(
    state: MovieDetailsUiState.Success,
    onStatusClick: (MovieStatus) -> Unit,
    onFavoriteClick: () -> Unit,
    onRatingChange: (Int?) -> Unit,
    onAddToCollection: () -> Unit,
    onEditCollectionItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val movie = state.movie
    val personal = state.personal
    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (state.isRefreshing) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        item {
            DetailsHeader(
                title = movie.title,
                posterPath = movie.posterPath,
                backdropPath = movie.backdropPath,
                supportingLines = listOfNotNull(
                    movie.originalTitle.takeIf { it != movie.title },
                    listOfNotNull(
                        movie.releaseDate?.year?.toString(),
                        movie.runtimeMinutes?.let(::formatRuntime),
                    ).joinToString(" · ").ifEmpty { null },
                    movie.voteAverage?.let { "★ ${formatVote(it)} TMDB" },
                ),
            )
        }
        item {
            DetailsSection(
                title = if (personal.inLibrary) "Na sua biblioteca" else "Adicionar à biblioteca",
                action = { FavoriteButton(isFavorite = personal.isFavorite, onClick = onFavoriteClick) },
            ) {
                StatusSelector(
                    options = MovieStatus.entries,
                    selected = personal.status,
                    label = MovieStatus::label,
                    onClick = onStatusClick,
                )
            }
        }
        item {
            DetailsSection(personal.rating?.let { "Sua nota: $it/10" } ?: "Avaliar") {
                RatingSelector(rating = personal.rating, onRatingChange = onRatingChange)
            }
        }
        if (movie.genres.isNotEmpty()) {
            item { GenreChips(movie.genres.map(Genre::name)) }
        }
        movie.overview?.takeIf { it.isNotBlank() }?.let { overview ->
            item { DetailsSection("Sinopse") { Overview(overview) } }
        }
        if (movie.directors.isNotEmpty()) {
            item {
                DetailsSection(if (movie.directors.size == 1) "Direção" else "Direção (${movie.directors.size})") {
                    Text(
                        movie.directors.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        if (movie.cast.isNotEmpty()) {
            item { DetailsSection("Elenco") { CastRow(movie.cast) } }
        }
        item {
            CollectionSection(
                items = state.collection,
                onAdd = onAddToCollection,
                onItemClick = { onEditCollectionItem(it.id) },
            )
        }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}

private val MovieStatus.label: String
    get() = when (this) {
        MovieStatus.WANT_TO_WATCH -> "Quero assistir"
        MovieStatus.WATCHED -> "Assistido"
    }

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun MovieDetailsPreview() {
    PipocaTheme {
        MovieDetailsContent(
            state = MovieDetailsUiState.Success(
                movie = MovieDetails(
                    id = 603, title = "Matrix", originalTitle = "The Matrix",
                    overview = "Um hacker descobre que a realidade é uma simulação.",
                    posterPath = null, backdropPath = null, releaseDate = LocalDate.of(1999, 3, 31),
                    runtimeMinutes = 136, voteAverage = 8.2,
                    genres = listOf(Genre(28, "Ação"), Genre(878, "Ficção científica")),
                    directors = listOf("Lana Wachowski", "Lilly Wachowski"),
                    cast = listOf(CastMember(6384, "Keanu Reeves", "Neo", null, 0)),
                ),
                personal = PersonalMovie(status = MovieStatus.WATCHED, isFavorite = true, rating = 9),
            ),
            onBack = {}, onRetry = {}, onDismissRefreshError = {},
            onStatusClick = {}, onFavoriteClick = {}, onRatingChange = {},
            onAddToCollection = {}, onEditCollectionItem = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MovieDetailsErrorPreview() {
    PipocaTheme {
        MovieDetailsContent(
            state = MovieDetailsUiState.Error(DataError.Network),
            onBack = {}, onRetry = {}, onDismissRefreshError = {},
            onStatusClick = {}, onFavoriteClick = {}, onRatingChange = {},
            onAddToCollection = {}, onEditCollectionItem = {},
        )
    }
}
