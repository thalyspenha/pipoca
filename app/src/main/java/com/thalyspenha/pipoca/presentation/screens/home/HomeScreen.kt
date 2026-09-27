package com.thalyspenha.pipoca.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.presentation.components.PosterImage
import com.thalyspenha.pipoca.presentation.components.UiStateContent
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import java.time.Instant

@Composable
fun HomeScreen(
    onItemClick: (HomeItem) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateContent(state = state) { content ->
        HomeContentView(content, onItemClick = onItemClick, onSearchClick = onSearchClick)
    }
}

@Composable
private fun HomeContentView(
    content: HomeContent,
    onItemClick: (HomeItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (content.isLibraryEmpty) {
        EmptyLibrary(content.tmdbConfigured, onSearchClick, modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pipoca", style = MaterialTheme.typography.headlineMedium)
                Text(
                    content.stats.summary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!content.tmdbConfigured) {
            item { MissingApiKeyCard(Modifier.padding(horizontal = 16.dp)) }
        }
        section("Assistindo", content.watching, onItemClick)
        section("Quero assistir", content.wantToWatch, onItemClick)
        section("Favoritos", content.favorites, onItemClick)
        section("Assistidos recentemente", content.recentlyWatched, onItemClick)
    }
}

private fun LazyListScope.section(
    title: String,
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = title) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = HomeItem::key) { item -> PosterCard(item, onClick = { onItemClick(item) }) }
            }
        }
    }
}

@Composable
private fun PosterCard(item: HomeItem, onClick: () -> Unit) {
    val title = item.title ?: "Carregando…"
    Column(
        modifier = Modifier.width(112.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PosterImage(posterPath = item.posterPath, title = title, modifier = Modifier.fillMaxWidth())
        Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        item.year?.let {
            Text(
                it.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyLibrary(tmdbConfigured: Boolean, onSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pipoca", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Sua biblioteca está vazia. Busque filmes e séries para começar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (tmdbConfigured) {
            Button(onClick = onSearchClick) { Text("Buscar") }
        } else {
            MissingApiKeyCard()
        }
    }
}

@Composable
private fun MissingApiKeyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Text(
            "Configure sua chave TMDB em local.properties (TMDB_API_TOKEN) para buscar filmes e séries.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun HomeStats.summary(): String = listOf(
    plural(total, "título", "títulos") + " na biblioteca",
    plural(moviesWatched, "filme assistido", "filmes assistidos"),
    plural(showsCompleted, "série concluída", "séries concluídas"),
).joinToString(" · ")

private fun plural(count: Int, one: String, many: String) = "$count ${if (count == 1) one else many}"

@Preview(showBackground = true)
@Composable
private fun HomeEmptyPreview() {
    PipocaTheme { HomeContentView(HomeContent(tmdbConfigured = true), onItemClick = {}, onSearchClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    val items = listOf(
        HomeItem(603, HomeItemType.MOVIE, "Matrix", null, 1999, Instant.EPOCH),
        HomeItem(1396, HomeItemType.TV_SHOW, "Breaking Bad", null, 2008, Instant.EPOCH),
        HomeItem(1, HomeItemType.MOVIE, null, null, null, Instant.EPOCH),
    )
    PipocaTheme {
        HomeContentView(
            HomeContent(
                tmdbConfigured = true,
                watching = items.take(1),
                wantToWatch = items,
                stats = HomeStats(moviesWatched = 1, showsCompleted = 0, total = 3),
            ),
            onItemClick = {},
            onSearchClick = {},
        )
    }
}
