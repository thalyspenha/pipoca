package com.thalyspenha.pipoca.presentation.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.HistoryEntry
import com.thalyspenha.pipoca.domain.model.HistoryFilter
import com.thalyspenha.pipoca.domain.model.HistoryType
import com.thalyspenha.pipoca.presentation.components.SectionTab
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.PlaceholderScreen
import com.thalyspenha.pipoca.presentation.components.PosterImage
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.episodeCode
import com.thalyspenha.pipoca.util.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onEntryClick: (HistoryEntry) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state, onBack, viewModel::onFilterChange, onEntryClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onEntryClick: (HistoryEntry) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when {
                state.isLoading -> LoadingContent()
                state.isHistoryEmpty -> PlaceholderScreen(
                    title = "Nada assistido ainda",
                    description = "Marque filmes como assistidos ou episódios nas temporadas para montar seu histórico.",
                )
                else -> {
                    FilterRow(state.filter, state.counts, onFilterChange)
                    if (state.days.isEmpty()) {
                        PlaceholderScreen(
                            title = "Nada aqui",
                            description = if (state.filter == HistoryFilter.MOVIES) "Nenhum filme no histórico." else "Nenhum episódio no histórico.",
                        )
                    } else {
                        HistoryList(state.days, onEntryClick)
                    }
                }
            }
        }
    }
}

/** Abas Todos/Filmes/Séries, no mesmo estilo de Biblioteca, Favoritos e Busca (D-064). */
@Composable
private fun FilterRow(selected: HistoryFilter, counts: Map<HistoryFilter, Int>, onFilterChange: (HistoryFilter) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected.ordinal) {
        HistoryFilter.entries.forEach { filter ->
            SectionTab(selected = filter == selected, onClick = { onFilterChange(filter) }, text = "${filter.label} (${counts[filter] ?: 0})")
        }
    }
}

@Composable
private fun HistoryList(days: List<HistoryDay>, onEntryClick: (HistoryEntry) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        days.forEach { day ->
            item(key = "day-${day.date}") {
                Text(
                    dayLabel(day.date),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(day.items, key = { it.key }) { item ->
                when (item) {
                    is HistoryItem.Single -> HistoryRow(item.entry, onClick = { onEntryClick(item.entry) })
                    is HistoryItem.EpisodeBatch -> BatchRow(item, onClick = { onEntryClick(item.latest) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val title = entry.title ?: "Carregando…"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(posterPath = entry.posterPath, title = title, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            entry.episodeLine()?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                listOf(if (entry.type == HistoryType.MOVIE) "Filme" else "Episódio", timeLabel(entry.watchedAt)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Episódios da mesma série marcados juntos (D-062): "22 episódios · T8E1–T8E22". Toque abre a série;
 * a seta mostra a lista dos episódios.
 */
@Composable
private fun BatchRow(batch: HistoryItem.EpisodeBatch, onClick: () -> Unit) {
    val latest = batch.latest
    val title = latest.title ?: "Carregando…"
    var expanded by rememberSaveable(batch.key) { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PosterImage(posterPath = latest.posterPath, title = title, modifier = Modifier.width(48.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull("${batch.entries.size} episódios", batch.rangeLabel()).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    timeLabel(latest.watchedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Esconder episódios" else "Mostrar episódios",
                )
            }
        }
        if (expanded) {
            // Ordem de exibição: do primeiro ao último episódio, como numa temporada.
            batch.entries.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber })).forEach { entry ->
                entry.episodeLine()?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 80.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** "T8E1–T8E22" (menor e maior código); nulo se faltar temporada/número. */
private fun HistoryItem.EpisodeBatch.rangeLabel(): String? {
    val codes = entries.mapNotNull { e -> e.seasonNumber?.let { s -> e.episodeNumber?.let { n -> s to n } } }
    if (codes.size != entries.size) return null
    val sorted = codes.sortedWith(compareBy({ it.first }, { it.second }))
    return episodeCode(sorted.first().first, sorted.first().second) + "–" +
        episodeCode(sorted.last().first, sorted.last().second)
}

private val HistoryFilter.label: String
    get() = when (this) {
        HistoryFilter.ALL -> "Todos"
        HistoryFilter.MOVIES -> "Filmes"
        HistoryFilter.TV_SHOWS -> "Séries"
    }

/** "T2E1 · Nome" para episódios; nulo para filmes. */
private fun HistoryEntry.episodeLine(): String? {
    if (type != HistoryType.EPISODE) return null
    val code = if (seasonNumber != null && episodeNumber != null) episodeCode(seasonNumber, episodeNumber) else null
    return listOfNotNull(code, episodeName).joinToString(" · ").ifEmpty { null }
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

private fun timeLabel(instant: Instant): String = "às " + TIME_FORMAT.format(instant.atZone(ZoneId.systemDefault()))

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> formatDate(date)
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    val now = Instant.now()
    PipocaTheme {
        HistoryContent(
            state = HistoryUiState(
                days = listOf(
                    HistoryDay(
                        LocalDate.now(),
                        listOf(
                            HistoryEntry(1, HistoryType.EPISODE, 1396, "Breaking Bad", null, "Seven Thirty-Seven", 2, 1, now),
                            HistoryEntry(2, HistoryType.MOVIE, 603, "Matrix", null, watchedAt = now),
                        ),
                    ),
                ),
                totalCount = 2,
                isLoading = false,
            ),
            onBack = {}, onFilterChange = {}, onEntryClick = {},
        )
    }
}
