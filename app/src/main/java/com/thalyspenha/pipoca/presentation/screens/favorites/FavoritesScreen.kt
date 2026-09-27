package com.thalyspenha.pipoca.presentation.screens.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.PlaceholderScreen
import com.thalyspenha.pipoca.presentation.components.PosterImage
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onItemClick: (FavoriteItem) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritesContent(
        state = state,
        onBack = onBack,
        onTabChange = viewModel::onTabChange,
        onItemClick = onItemClick,
        onRemove = viewModel::onRemove,
        onUndo = viewModel::undoRemove,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesContent(
    state: FavoritesUiState,
    onBack: () -> Unit,
    onTabChange: (FavoritesTab) -> Unit,
    onItemClick: (FavoriteItem) -> Unit,
    onRemove: (FavoriteItem) -> Unit,
    onUndo: (FavoriteItem) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favoritos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
                Tab(
                    selected = state.tab == FavoritesTab.MOVIES,
                    onClick = { onTabChange(FavoritesTab.MOVIES) },
                    text = { Text("Filmes (${state.movies.size})") },
                )
                Tab(
                    selected = state.tab == FavoritesTab.TV_SHOWS,
                    onClick = { onTabChange(FavoritesTab.TV_SHOWS) },
                    text = { Text("Séries (${state.tvShows.size})") },
                )
            }
            when {
                state.isLoading -> LoadingContent()
                state.items.isEmpty() -> PlaceholderScreen(
                    title = "Nenhum favorito",
                    description = if (state.tab == FavoritesTab.MOVIES) {
                        "Toque no coração nos detalhes de um filme para favoritar."
                    } else {
                        "Toque no coração nos detalhes de uma série para favoritar."
                    },
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(state.items, key = { "${it.tab}-${it.id}" }) { item ->
                        FavoriteRow(
                            item = item,
                            onClick = { onItemClick(item) },
                            onRemove = {
                                onRemove(item)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "${item.title ?: "Item"} removido dos favoritos",
                                        actionLabel = "Desfazer",
                                        duration = SnackbarDuration.Long,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onUndo(item)
                                }
                            },
                        )
                        HorizontalDivider(Modifier.padding(start = 88.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(item: FavoriteItem, onClick: () -> Unit, onRemove: () -> Unit) {
    val title = item.title ?: "Carregando…"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(posterPath = item.posterPath, title = title, modifier = Modifier.width(56.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.year?.toString(), item.statusLabel).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Favorite, contentDescription = "Remover dos favoritos", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoritesPreview() {
    PipocaTheme {
        FavoritesContent(
            state = FavoritesUiState(
                movies = listOf(FavoriteItem(603, FavoritesTab.MOVIES, "Matrix", null, 1999, "Assistido")),
                isLoading = false,
            ),
            onBack = {}, onTabChange = {}, onItemClick = {}, onRemove = {}, onUndo = {},
        )
    }
}
