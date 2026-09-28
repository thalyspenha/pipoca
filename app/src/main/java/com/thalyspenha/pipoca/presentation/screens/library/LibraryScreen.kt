package com.thalyspenha.pipoca.presentation.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.LibrarySort
import com.thalyspenha.pipoca.domain.model.MovieLibraryCounts
import com.thalyspenha.pipoca.domain.model.MovieLibraryFilter
import com.thalyspenha.pipoca.domain.model.TvShowLibraryCounts
import com.thalyspenha.pipoca.domain.model.TvShowLibraryFilter
import com.thalyspenha.pipoca.domain.repository.LibraryViewMode
import com.thalyspenha.pipoca.presentation.components.AppIcons
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.MediaCardData
import com.thalyspenha.pipoca.presentation.components.MediaCardProgress
import com.thalyspenha.pipoca.presentation.components.MediaPosterCard
import com.thalyspenha.pipoca.presentation.components.MediaStatusBadge
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme

@Composable
fun LibraryScreen(
    onItemClick: (MediaCardData) -> Unit,
    onSearchTitlesClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryContent(
        state = state,
        actions = LibraryActions(
            onTabChange = viewModel::onTabChange,
            onMovieFilterChange = viewModel::onMovieFilterChange,
            onTvShowFilterChange = viewModel::onTvShowFilterChange,
            onSortChange = viewModel::onSortChange,
            onQueryChange = viewModel::onQueryChange,
            onViewModeChange = viewModel::onViewModeChange,
            onItemClick = onItemClick,
            onSearchTitlesClick = onSearchTitlesClick,
        ),
    )
}

private class LibraryActions(
    val onTabChange: (LibraryTab) -> Unit,
    val onMovieFilterChange: (MovieLibraryFilter) -> Unit,
    val onTvShowFilterChange: (TvShowLibraryFilter) -> Unit,
    val onSortChange: (LibrarySort) -> Unit,
    val onQueryChange: (String) -> Unit,
    val onViewModeChange: (LibraryViewMode) -> Unit,
    val onItemClick: (MediaCardData) -> Unit,
    val onSearchTitlesClick: () -> Unit,
)

@Composable
private fun LibraryContent(state: LibraryUiState, actions: LibraryActions) {
    when {
        state.isLoading -> LoadingContent()
        state.isLibraryEmpty -> EmptyLibrary(actions.onSearchTitlesClick)
        else -> Column(Modifier.fillMaxSize()) {
            Header(state, actions)
            TabRow(state, actions.onTabChange)
            FilterRow(state, actions)
            val grid = state.viewMode == LibraryViewMode.GRID
            if (state.items.isEmpty()) {
                EmptyFilter(state.emptyMessage)
            } else if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.items, key = MediaCardData::id) { item ->
                        MediaPosterCard(item, grid = true, onClick = { actions.onItemClick(item) })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(state.items, key = MediaCardData::id) { item ->
                        MediaPosterCard(item, grid = false, onClick = { actions.onItemClick(item) })
                        HorizontalDivider(Modifier.padding(start = 96.dp))
                    }
                }
            }
        }
    }
}

/** Título, pesquisa local (nunca TMDB), ordenação e Grid/Lista. */
@Composable
private fun Header(state: LibraryUiState, actions: LibraryActions) {
    val query = state.selection.query
    var searching by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    val closeSearch = {
        searching = false
        actions.onQueryChange("")
    }
    BackHandler(enabled = searching, onBack = closeSearch)

    if (searching) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        OutlinedTextField(
            value = query,
            onValueChange = actions.onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Pesquisar na biblioteca") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = closeSearch) { Icon(Icons.Filled.Clear, contentDescription = "Fechar pesquisa") }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Minha Biblioteca", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { searching = true }) {
            Icon(Icons.Filled.Search, contentDescription = "Pesquisar na biblioteca")
        }
        if (state.viewMode == LibraryViewMode.GRID) {
            IconButton(onClick = { actions.onViewModeChange(LibraryViewMode.LIST) }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Ver em lista")
            }
        } else {
            IconButton(onClick = { actions.onViewModeChange(LibraryViewMode.GRID) }) {
                Icon(AppIcons.GridView, contentDescription = "Ver em grade")
            }
        }
    }
}

@Composable
private fun TabRow(state: LibraryUiState, onTabChange: (LibraryTab) -> Unit) {
    val tab = state.selection.tab
    PrimaryTabRow(selectedTabIndex = tab.ordinal) {
        Tab(
            selected = tab == LibraryTab.MOVIES,
            onClick = { onTabChange(LibraryTab.MOVIES) },
            text = { Text("Filmes (${state.movieCounts.all})") },
        )
        Tab(
            selected = tab == LibraryTab.TV_SHOWS,
            onClick = { onTabChange(LibraryTab.TV_SHOWS) },
            text = { Text("Séries (${state.tvShowCounts.all})") },
        )
    }
}

/** Chips com contagem vinda do banco + menu de ordenação. */
@Composable
private fun FilterRow(state: LibraryUiState, actions: LibraryActions) {
    val selection = state.selection
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item { SortMenu(selection.sort, state.sortOptions, actions.onSortChange) }
        if (selection.tab == LibraryTab.MOVIES) {
            items(MovieLibraryFilter.entries) { filter ->
                FilterChip(
                    selected = filter == selection.movieFilter,
                    onClick = { actions.onMovieFilterChange(filter) },
                    label = { Text("${filter.label} (${state.movieCounts.of(filter)})") },
                )
            }
        } else {
            items(TvShowLibraryFilter.entries) { filter ->
                FilterChip(
                    selected = filter == selection.tvShowFilter,
                    onClick = { actions.onTvShowFilterChange(filter) },
                    label = { Text("${filter.label} (${state.tvShowCounts.of(filter)})") },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(selected: LibrarySort, options: List<LibrarySort>, onSortChange: (LibrarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selected.label)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Ordenar")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label) },
                    onClick = {
                        onSortChange(sort)
                        expanded = false
                    },
                    trailingIcon = if (sort == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyFilter(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyLibrary(onSearchTitlesClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sua biblioteca está vazia", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "Pesquise um filme ou série para começar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onSearchTitlesClick) { Text("Buscar títulos") }
    }
}

private val previewActions = LibraryActions({}, {}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true)
@Composable
private fun LibraryGridPreview() {
    PipocaTheme {
        LibraryContent(
            state = LibraryUiState(
                selection = LibrarySelection(tab = LibraryTab.TV_SHOWS),
                movieCounts = MovieLibraryCounts(3, 1, 2, 1),
                tvShowCounts = TvShowLibraryCounts(2, 0, 1, 1, 0, 0, 1),
                items = listOf(
                    MediaCardData(1396, false, "Breaking Bad", null, 2008, MediaStatusBadge.WATCHING, true, MediaCardProgress(36, 50, 72)),
                    MediaCardData(1399, false, "Game of Thrones", null, 2011, MediaStatusBadge.COMPLETED, false, MediaCardProgress(73, 73, 100)),
                ),
                isLoading = false,
            ),
            actions = previewActions,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryEmptyPreview() {
    PipocaTheme { LibraryContent(state = LibraryUiState(isLoading = false), actions = previewActions) }
}
