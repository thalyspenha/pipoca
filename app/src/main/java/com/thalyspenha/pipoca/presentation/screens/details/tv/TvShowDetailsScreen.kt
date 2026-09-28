package com.thalyspenha.pipoca.presentation.screens.details.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.CastMember
import com.thalyspenha.pipoca.domain.model.Genre
import com.thalyspenha.pipoca.domain.model.TvShowDetails
import com.thalyspenha.pipoca.domain.model.TvShowStatus
import com.thalyspenha.pipoca.presentation.components.ErrorContent
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.details.CastRow
import com.thalyspenha.pipoca.presentation.components.details.CollectionSection
import com.thalyspenha.pipoca.presentation.components.details.DetailsHeader
import com.thalyspenha.pipoca.presentation.components.details.DetailsScaffold
import com.thalyspenha.pipoca.presentation.components.details.rememberHeaderScrolledPast
import com.thalyspenha.pipoca.presentation.components.details.DETAILS_HEADER_KEY
import com.thalyspenha.pipoca.presentation.components.details.DetailsSection
import com.thalyspenha.pipoca.presentation.components.details.FavoriteButton
import com.thalyspenha.pipoca.presentation.components.details.GenreChips
import com.thalyspenha.pipoca.presentation.components.details.Overview
import com.thalyspenha.pipoca.presentation.components.details.RatingSelector
import com.thalyspenha.pipoca.presentation.components.details.StatusSelector
import com.thalyspenha.pipoca.presentation.components.isRetryable
import com.thalyspenha.pipoca.presentation.components.toMessage
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.formatVote
import java.time.LocalDate

@Composable
fun TvShowDetailsScreen(
    onBack: () -> Unit,
    onSeasonClick: (seasonNumber: Int) -> Unit,
    onAddToCollection: () -> Unit,
    onEditCollectionItem: (itemId: Long) -> Unit,
    viewModel: TvShowDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TvShowDetailsContent(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.refresh() },
        onDismissRefreshError = viewModel::dismissRefreshError,
        onStatusClick = viewModel::onStatusClick,
        onFavoriteClick = viewModel::onFavoriteClick,
        onRatingChange = viewModel::onRatingChange,
        onMarkNextEpisode = viewModel::onMarkNextEpisode,
        onLoadEpisodes = viewModel::loadEpisodes,
        onSeasonClick = onSeasonClick,
        onAddToCollection = onAddToCollection,
        onEditCollectionItem = onEditCollectionItem,
    )
}

@Composable
private fun TvShowDetailsContent(
    state: TvShowDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDismissRefreshError: () -> Unit,
    onStatusClick: (TvShowStatus) -> Unit,
    onFavoriteClick: () -> Unit,
    onRatingChange: (Int?) -> Unit,
    onMarkNextEpisode: () -> Unit,
    onLoadEpisodes: () -> Unit,
    onSeasonClick: (Int) -> Unit,
    onAddToCollection: () -> Unit,
    onEditCollectionItem: (Long) -> Unit,
) {
    val success = state as? TvShowDetailsUiState.Success
    val listState = rememberLazyListState()
    val headerScrolledPast = rememberHeaderScrolledPast(listState)
    DetailsScaffold(
        title = success?.show?.name ?: "Série",
        onBack = onBack,
        refreshError = success?.refreshError,
        onRetry = onRetry,
        onDismissRefreshError = onDismissRefreshError,
        showTitle = success == null || headerScrolledPast,
    ) { modifier ->
        when (state) {
            TvShowDetailsUiState.Loading -> LoadingContent(modifier)
            is TvShowDetailsUiState.Error -> ErrorContent(
                message = state.error.toMessage(),
                onRetry = onRetry.takeIf { state.error.isRetryable },
                modifier = modifier,
            )
            is TvShowDetailsUiState.Success -> TvShowDetailsBody(
                state = state,
                listState = listState,
                onStatusClick = onStatusClick,
                onFavoriteClick = onFavoriteClick,
                onRatingChange = onRatingChange,
                onMarkNextEpisode = onMarkNextEpisode,
                onLoadEpisodes = onLoadEpisodes,
                onSeasonClick = onSeasonClick,
                onAddToCollection = onAddToCollection,
                onEditCollectionItem = onEditCollectionItem,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun TvShowDetailsBody(
    state: TvShowDetailsUiState.Success,
    listState: LazyListState,
    onStatusClick: (TvShowStatus) -> Unit,
    onFavoriteClick: () -> Unit,
    onRatingChange: (Int?) -> Unit,
    onMarkNextEpisode: () -> Unit,
    onLoadEpisodes: () -> Unit,
    onSeasonClick: (Int) -> Unit,
    onAddToCollection: () -> Unit,
    onEditCollectionItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val show = state.show
    val personal = state.personal
    LazyColumn(state = listState, modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (state.isRefreshing) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        item(key = DETAILS_HEADER_KEY) {
            DetailsHeader(
                title = show.name,
                posterPath = show.posterPath,
                backdropPath = show.backdropPath,
                supportingLines = listOfNotNull(
                    show.originalName.takeIf { it != show.name },
                    listOfNotNull(show.firstAirDate?.year?.toString(), show.tmdbStatus?.let(::tmdbStatusLabel))
                        .joinToString(" · ").ifEmpty { null },
                    seasonsLine(show),
                    show.voteAverage?.let { "★ ${formatVote(it)} TMDB" },
                ),
            )
        }
        item {
            DetailsSection(
                title = if (personal.inLibrary) "Na sua biblioteca" else "Adicionar à biblioteca",
                action = { FavoriteButton(isFavorite = personal.isFavorite, onClick = onFavoriteClick) },
            ) {
                Column {
                    StatusSelector(
                        options = TV_SHOW_SELECTABLE_STATUSES,
                        selected = personal.status,
                        label = TvShowStatus::label,
                        onClick = onStatusClick,
                    )
                    MoreStatusMenu(current = personal.status, onStatusClick = onStatusClick)
                }
            }
        }
        item {
            DetailsSection("Progresso") {
                ProgressCard(
                    progress = state.progress,
                    inLibrary = personal.inLibrary,
                    isLoading = state.isLoadingEpisodes,
                    onMarkNext = onMarkNextEpisode,
                    onLoad = onLoadEpisodes,
                )
            }
        }
        item {
            DetailsSection(personal.rating?.let { "Sua nota: $it/10" } ?: "Avaliar") {
                RatingSelector(rating = personal.rating, onRatingChange = onRatingChange)
            }
        }
        if (show.genres.isNotEmpty()) {
            item { GenreChips(show.genres.map(Genre::name)) }
        }
        show.overview?.takeIf { it.isNotBlank() }?.let { overview ->
            item { DetailsSection("Sinopse") { Overview(overview) } }
        }
        // Temporadas depois da sinopse, como no filme: nota, gêneros e sinopse ficam perto do topo (D-063).
        if (state.seasons.isNotEmpty()) {
            item {
                DetailsSection("Temporadas") {
                    Column {
                        state.seasons.forEach { row -> SeasonItem(row, onClick = { onSeasonClick(row.seasonNumber) }) }
                    }
                }
            }
        }
        if (show.creators.isNotEmpty()) {
            item {
                DetailsSection("Criação") {
                    Text(
                        show.creators.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        if (show.cast.isNotEmpty()) {
            item { DetailsSection("Elenco") { CastRow(show.cast) } }
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

/**
 * Pausada/Abandonada fora dos segmentos (não cabem) (D-050). O botão mostra o status quando é
 * um desses; escolher o atual não faz nada (tirar da biblioteca fica nos segmentos).
 */
@Composable
private fun MoreStatusMenu(current: TvShowStatus?, onStatusClick: (TvShowStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val extra = current?.takeIf { it in TV_SHOW_EXTRA_STATUSES }
    Box(Modifier.padding(start = 8.dp)) {
        TextButton(onClick = { expanded = true }) {
            Text(extra?.label ?: "Mais status")
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TV_SHOW_EXTRA_STATUSES.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label) },
                    onClick = {
                        if (status != current) onStatusClick(status)
                        expanded = false
                    },
                    trailingIcon = if (status == current) {
                        { Icon(Icons.Filled.Check, contentDescription = "Selecionado") }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** "5 temporadas · 62 episódios"; temporada 0 (especiais) não conta (TMDB já exclui em `number_of_seasons`). */
private fun seasonsLine(show: TvShowDetails): String? = listOfNotNull(
    show.numberOfSeasons?.let { if (it == 1) "1 temporada" else "$it temporadas" },
    show.numberOfEpisodes?.let { if (it == 1) "1 episódio" else "$it episódios" },
).joinToString(" · ").ifEmpty { null }

/** Status de produção do TMDB em português; valor desconhecido aparece como veio. */
internal fun tmdbStatusLabel(status: String): String = when (status) {
    "Returning Series" -> "Em exibição"
    "Ended" -> "Finalizada"
    "Canceled" -> "Cancelada"
    "In Production" -> "Em produção"
    "Planned" -> "Planejada"
    "Pilot" -> "Piloto"
    else -> status
}

private val TvShowStatus.label: String
    get() = when (this) {
        TvShowStatus.WANT_TO_WATCH -> "Quero ver"
        TvShowStatus.WATCHING -> "Assistindo"
        TvShowStatus.COMPLETED -> "Concluída"
        TvShowStatus.PAUSED -> "Pausada"
        TvShowStatus.DROPPED -> "Abandonada"
    }

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun TvShowDetailsPreview() {
    PipocaTheme {
        TvShowDetailsContent(
            state = TvShowDetailsUiState.Success(
                show = TvShowDetails(
                    id = 1396, name = "Breaking Bad", originalName = "Breaking Bad",
                    overview = "Um professor de química vira fabricante de metanfetamina.",
                    posterPath = null, backdropPath = null, firstAirDate = LocalDate.of(2008, 1, 20),
                    tmdbStatus = "Ended", numberOfSeasons = 5, numberOfEpisodes = 62, episodeRunTime = 47,
                    voteAverage = 8.9, genres = listOf(Genre(18, "Drama")), creators = listOf("Vince Gilligan"),
                    seasons = emptyList(), cast = listOf(CastMember(17419, "Bryan Cranston", "Walter White", null, 0)),
                ),
                personal = PersonalTvShow(status = TvShowStatus.WATCHING, isFavorite = true),
            ),
            onBack = {}, onRetry = {}, onDismissRefreshError = {},
            onStatusClick = {}, onFavoriteClick = {}, onRatingChange = {},
            onMarkNextEpisode = {}, onLoadEpisodes = {}, onSeasonClick = {},
            onAddToCollection = {}, onEditCollectionItem = {},
        )
    }
}
