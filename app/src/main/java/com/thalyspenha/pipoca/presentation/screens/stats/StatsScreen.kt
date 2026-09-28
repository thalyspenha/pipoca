package com.thalyspenha.pipoca.presentation.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.stats.EpisodeStats
import com.thalyspenha.pipoca.domain.stats.FormatCount
import com.thalyspenha.pipoca.domain.stats.GenreShare
import com.thalyspenha.pipoca.domain.stats.MovieStats
import com.thalyspenha.pipoca.domain.stats.RatingBucket
import com.thalyspenha.pipoca.domain.stats.Statistics
import com.thalyspenha.pipoca.domain.stats.StatsCalculator
import com.thalyspenha.pipoca.domain.stats.TvShowStats
import com.thalyspenha.pipoca.domain.stats.WatchTime
import com.thalyspenha.pipoca.presentation.components.UiStateContent
import com.thalyspenha.pipoca.presentation.components.label
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.formatDecimal
import com.thalyspenha.pipoca.util.formatInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estatísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        UiStateContent(state = state, modifier = Modifier.padding(padding)) { stats ->
            StatsContent(stats, Modifier.padding(padding))
        }
    }
}

@Composable
private fun StatsContent(stats: Statistics, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { TimeSection(stats.watchTime) }
        item { MoviesSection(stats.movies) }
        item { TvShowsSection(stats.tvShows) }
        item { EpisodesSection(stats.episodes) }
        item { GenresSection(stats.genres) }
        item { RatingsSection(stats) }
        item { CollectionSection(stats.collection) }
        item { LimitationsNote() }
    }
}

// ---- Seções

@Composable
private fun TimeSection(time: WatchTime) {
    Section("Tempo assistido") {
        TileRow(
            "Horas" to formatInt(time.hours),
            "Dias" to formatDecimal(time.days),
            "Minutos" to formatInt(time.minutes),
        )
        Text(
            buildString {
                append(if (time.views == 1) "1 visualização" else "${formatInt(time.views.toLong())} visualizações")
                if (time.withoutRuntime > 0) append(" · ${time.withoutRuntime} sem duração conhecida (fora do total)")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MoviesSection(movies: MovieStats) {
    Section("Filmes") {
        TileRow("Assistidos" to movies.watched.toString(), "Quero assistir" to movies.wantToWatch.toString())
        TileRow("Na coleção" to movies.inCollection.toString(), "Favoritos" to movies.favorites.toString())
    }
}

@Composable
private fun TvShowsSection(shows: TvShowStats) {
    Section("Séries") {
        TileRow("Total" to shows.total.toString(), "Em andamento" to shows.watching.toString())
        TileRow("Concluídas" to shows.completed.toString(), "Quero assistir" to shows.wantToWatch.toString())
    }
}

@Composable
private fun EpisodesSection(episodes: EpisodeStats) {
    Section("Episódios") {
        TileRow(
            "Total" to formatInt(episodes.total.toLong()),
            "Este mês" to episodes.thisMonth.toString(),
            "Este ano" to episodes.thisYear.toString(),
        )
    }
}

@Composable
private fun GenresSection(genres: List<GenreShare>) {
    Section("Gêneros") {
        if (genres.isEmpty()) {
            EmptyNote("Assista filmes ou episódios para ver seus gêneros.")
        } else {
            val max = genres.maxOf { it.count }
            genres.take(MAX_GENRES).forEach { genre ->
                BarRow(label = genre.name, fraction = genre.count / max.toFloat(), trailing = "${genre.count} · ${genre.percent}%")
            }
            if (genres.size > MAX_GENRES) EmptyNote("+ ${genres.size - MAX_GENRES} gêneros com menos títulos")
        }
    }
}

@Composable
private fun RatingsSection(stats: Statistics) {
    Section("Suas notas") {
        if (stats.ratedCount == 0) {
            EmptyNote("Avalie filmes e séries nos detalhes para ver a distribuição.")
        } else {
            Text(
                "Média ${formatDecimal(stats.averageRating ?: 0.0)} · " +
                    if (stats.ratedCount == 1) "1 avaliação" else "${stats.ratedCount} avaliações",
                style = MaterialTheme.typography.bodyMedium,
            )
            RatingChart(stats.ratings)
        }
    }
}

@Composable
private fun CollectionSection(collection: List<FormatCount>) {
    Section("Coleção") {
        val max = collection.maxOfOrNull { it.count } ?: 0
        if (max == 0) {
            EmptyNote("Nenhum item na coleção ainda.")
        } else {
            collection.filter { it.format != MediaFormat.OTHER || it.count > 0 }.forEach { item ->
                BarRow(label = item.format.label, fraction = item.count / max.toFloat(), trailing = item.count.toString())
            }
        }
    }
}

@Composable
private fun LimitationsNote() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Como calculamos", style = MaterialTheme.typography.titleSmall)
            Text(
                "Tempo é aproximado: soma a duração do TMDB de cada vez que você marcou um filme ou episódio " +
                    "(reassistir conta de novo). Episódio sem duração usa a média da série. Itens sem duração ou " +
                    "ainda não baixados do TMDB ficam fora do total. Gêneros consideram filmes assistidos e séries " +
                    "com algum episódio visto ou concluídas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Blocos visuais

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun TileRow(vararg tiles: Pair<String, String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BarRow(label: String, fraction: Float, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(112.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(trailing, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
    }
}

/** Colunas 1–10 com a quantidade em cima. */
@Composable
private fun RatingChart(ratings: List<RatingBucket>) {
    val max = ratings.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        ratings.forEach { bucket ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (bucket.count > 0) Text(bucket.count.toString(), style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(BAR_MAX_HEIGHT * (bucket.count / max.toFloat()))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(bucket.rating.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val MAX_GENRES = 10
private val CHART_HEIGHT = 140.dp
private val BAR_MAX_HEIGHT = 96.dp

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun StatsPreview() {
    PipocaTheme {
        StatsContent(
            Statistics(
                movies = MovieStats(12, 30, 5, 4),
                tvShows = TvShowStats(8, 2, 3, 3),
                episodes = EpisodeStats(250, 12, 140),
                watchTime = WatchTime(minutes = 14_000, views = 262, withoutRuntime = 3),
                genres = StatsCalculator.genreShares(listOf("Drama" to 9, "Ação" to 6, "Crime" to 4)),
                ratings = StatsCalculator.ratingBuckets(mapOf(7 to 2, 8 to 5, 9 to 3, 10 to 1)),
                collection = StatsCalculator.formatCounts(mapOf(MediaFormat.UHD_4K_BLURAY to 3, MediaFormat.BLURAY to 7)),
            ),
        )
    }
}
