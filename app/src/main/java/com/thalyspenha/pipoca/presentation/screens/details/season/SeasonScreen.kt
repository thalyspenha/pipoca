package com.thalyspenha.pipoca.presentation.screens.details.season

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.thalyspenha.pipoca.domain.model.Episode
import com.thalyspenha.pipoca.presentation.components.ErrorContent
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.PlaceholderScreen
import com.thalyspenha.pipoca.presentation.components.details.DetailsScaffold
import com.thalyspenha.pipoca.presentation.components.isRetryable
import com.thalyspenha.pipoca.presentation.components.toMessage
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.TmdbImageUrl
import com.thalyspenha.pipoca.util.episodeCode
import com.thalyspenha.pipoca.util.formatDate
import com.thalyspenha.pipoca.util.formatRuntime
import java.time.LocalDate

@Composable
fun SeasonScreen(onBack: () -> Unit, viewModel: SeasonViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SeasonContent(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.refresh() },
        onDismissRefreshError = viewModel::dismissRefreshError,
        onEpisodeToggle = viewModel::onEpisodeToggle,
        onMarkSeason = viewModel::onMarkSeason,
        onUnmarkSeason = viewModel::onUnmarkSeason,
    )
}

@Composable
private fun SeasonContent(
    state: SeasonUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDismissRefreshError: () -> Unit,
    onEpisodeToggle: (Episode, Boolean) -> Unit,
    onMarkSeason: () -> Unit,
    onUnmarkSeason: () -> Unit,
) {
    val success = state as? SeasonUiState.Success
    DetailsScaffold(
        title = success?.seasonName ?: "Temporada",
        onBack = onBack,
        refreshError = success?.refreshError,
        onRetry = onRetry,
        onDismissRefreshError = onDismissRefreshError,
    ) { modifier ->
        when (state) {
            SeasonUiState.Loading -> LoadingContent(modifier)
            is SeasonUiState.Error -> ErrorContent(
                message = state.error.toMessage(),
                onRetry = onRetry.takeIf { state.error.isRetryable },
                modifier = modifier,
            )
            is SeasonUiState.Success -> if (state.episodes.isEmpty()) {
                PlaceholderScreen(
                    title = "Sem episódios",
                    description = "O TMDB ainda não listou episódios para esta temporada.",
                    modifier = modifier,
                )
            } else {
                SeasonBody(state, onEpisodeToggle, onMarkSeason, onUnmarkSeason, modifier)
            }
        }
    }
}

@Composable
private fun SeasonBody(
    state: SeasonUiState.Success,
    onEpisodeToggle: (Episode, Boolean) -> Unit,
    onMarkSeason: () -> Unit,
    onUnmarkSeason: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmUnmark by rememberSaveable { mutableStateOf(false) }
    if (confirmUnmark) {
        val count = state.episodes.count { it.isWatched }
        AlertDialog(
            onDismissRequest = { confirmUnmark = false },
            title = { Text(if (count == 1) "Desmarcar 1 episódio?" else "Desmarcar $count episódios?") },
            text = { Text("As datas em que você assistiu e os registros no histórico desta temporada serão apagados.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnmark = false
                    onUnmarkSeason()
                }) { Text("Desmarcar") }
            },
            dismissButton = { TextButton(onClick = { confirmUnmark = false }) { Text("Cancelar") } },
        )
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (state.isRefreshing) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    state.showName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    Text(
                        "${state.watched} / ${state.aired} assistidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.allAiredWatched) {
                    // Menos destaque e com confirmação: apaga datas e histórico da temporada (D-061).
                    TextButton(onClick = { confirmUnmark = true }) { Text("Desmarcar todos") }
                } else if (state.aired > 0) {
                    OutlinedButton(onClick = onMarkSeason) { Text("Marcar todos") }
                }
            }
        }
        items(state.episodes, key = { it.episode.id }) { row ->
            EpisodeItem(row, onToggle = { onEpisodeToggle(row.episode, it) })
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}

/**
 * Episódio: imagem menor, título/data e checkbox numa linha; sinopse embaixo com a largura toda
 * (antes ficava espremida entre a imagem de 128 dp e o checkbox, D-067).
 */
@Composable
private fun EpisodeItem(row: EpisodeRow, onToggle: (Boolean) -> Unit) {
    val episode = row.episode
    var expanded by rememberSaveable(episode.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EpisodeStill(episode, Modifier.width(96.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${episode.episodeNumber}. ${episode.name.ifBlank { episodeCode(episode.seasonNumber, episode.episodeNumber) }}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    episodeInfo(episode, row.isAired),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val code = episodeCode(episode.seasonNumber, episode.episodeNumber)
            Checkbox(
                checked = row.isWatched,
                onCheckedChange = onToggle,
                enabled = row.isAired || row.isWatched,
                // Sem isso o leitor de tela diz só "caixa de seleção", sem qual episódio (D-061).
                modifier = Modifier.semantics { contentDescription = "Assistido: $code ${episode.name}".trim() },
            )
        }
        episode.overview?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EpisodeStill(episode: Episode, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            episodeCode(episode.seasonNumber, episode.episodeNumber),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TmdbImageUrl.build(episode.stillPath, TmdbImageUrl.STILL)?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** "20/01/2008 · 58min", "Estreia 01/01/2030" ou "Sem data". */
private fun episodeInfo(episode: Episode, isAired: Boolean): String {
    val date = episode.airDate?.let(::formatDate)
    val parts = when {
        date == null -> listOf("Sem data")
        !isAired -> listOf("Estreia $date")
        else -> listOf(date)
    } + listOfNotNull(episode.runtimeMinutes?.let(::formatRuntime))
    return parts.joinToString(" · ")
}

@Preview(showBackground = true)
@Composable
private fun SeasonPreview() {
    fun ep(n: Int, date: LocalDate?) = Episode(
        id = n.toLong(), showId = 1396, seasonNumber = 1, episodeNumber = n, name = "Episódio $n",
        overview = "Walter começa uma nova vida.", stillPath = null, airDate = date, runtimeMinutes = 47,
    )
    PipocaTheme {
        SeasonContent(
            state = SeasonUiState.Success(
                showName = "Breaking Bad",
                seasonName = "Temporada 1",
                episodes = listOf(
                    EpisodeRow(ep(1, LocalDate.of(2008, 1, 20)), isWatched = true, isAired = true),
                    EpisodeRow(ep(2, LocalDate.of(2008, 1, 27)), isWatched = false, isAired = true),
                    EpisodeRow(ep(3, LocalDate.of(2030, 1, 1)), isWatched = false, isAired = false),
                ),
            ),
            onBack = {}, onRetry = {}, onDismissRefreshError = {},
            onEpisodeToggle = { _, _ -> }, onMarkSeason = {}, onUnmarkSeason = {},
        )
    }
}
