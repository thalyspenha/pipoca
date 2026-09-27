package com.thalyspenha.pipoca.presentation.screens.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.CollectionFilter
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.CollectionSort
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.PlaceholderScreen
import com.thalyspenha.pipoca.presentation.components.PosterImage
import com.thalyspenha.pipoca.presentation.components.label
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme

@Composable
fun CollectionScreen(
    onItemClick: (CollectionListItem) -> Unit,
    onOpenDetails: (CollectionListItem) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionContent(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onSortChange = viewModel::onSortChange,
        onItemClick = onItemClick,
        onOpenDetails = onOpenDetails,
    )
}

@Composable
private fun CollectionContent(
    state: CollectionUiState,
    onFilterChange: (CollectionFilter) -> Unit,
    onSortChange: (CollectionSort) -> Unit,
    onItemClick: (CollectionListItem) -> Unit,
    onOpenDetails: (CollectionListItem) -> Unit,
) {
    when {
        state.isLoading -> LoadingContent()
        state.isCollectionEmpty -> PlaceholderScreen(
            title = "Minha Coleção",
            description = "Sua coleção está vazia. Adicione Blu-ray, 4K, DVD ou digital na seção Coleção dos detalhes de um filme ou série.",
        )
        else -> Column(Modifier.fillMaxSize()) {
            Header(state, onSortChange)
            FilterRow(selected = state.filter, onFilterChange = onFilterChange)
            if (state.items.isEmpty()) {
                PlaceholderScreen(
                    title = "Nada aqui",
                    description = "Nenhum item em ${state.filter.label}.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(state.items, key = CollectionListItem::id) { item ->
                        CollectionRow(item, onClick = { onItemClick(item) }, onOpenDetails = { onOpenDetails(item) })
                        HorizontalDivider(Modifier.padding(start = 88.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: CollectionUiState, onSortChange: (CollectionSort) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Minha Coleção", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (state.totalCount == 1) "1 item" else "${state.totalCount} itens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SortMenu(selected = state.sort, onSortChange = onSortChange)
    }
}

@Composable
private fun SortMenu(selected: CollectionSort, onSortChange: (CollectionSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selected.label)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Ordenar")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CollectionSort.entries.forEach { sort ->
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
private fun FilterRow(selected: CollectionFilter, onFilterChange: (CollectionFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(CollectionFilter.entries) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

/** Toque edita o item (D-042); a seta abre os detalhes do título. */
@Composable
private fun CollectionRow(item: CollectionListItem, onClick: () -> Unit, onOpenDetails: () -> Unit) {
    val title = item.title ?: "Carregando…"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(posterPath = item.posterPath, title = title, modifier = Modifier.width(56.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.format.label, item.edition).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                listOfNotNull(
                    if (item.mediaType == CollectionMediaType.MOVIE) "Filme" else "Série",
                    "${item.quantity} unidades".takeIf { item.quantity > 1 },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenDetails) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ver detalhes")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionPreview() {
    PipocaTheme {
        CollectionContent(
            state = CollectionUiState(
                items = listOf(
                    CollectionListItem(1, 603, CollectionMediaType.MOVIE, "Matrix", null, MediaFormat.UHD_4K_BLURAY, "Steelbook", 1),
                    CollectionListItem(2, 1396, CollectionMediaType.TV_SHOW, "Breaking Bad", null, MediaFormat.BLURAY, null, 2),
                ),
                totalCount = 2,
                isLoading = false,
            ),
            onFilterChange = {}, onSortChange = {}, onItemClick = {}, onOpenDetails = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionEmptyPreview() {
    PipocaTheme {
        CollectionContent(
            state = CollectionUiState(isLoading = false),
            onFilterChange = {}, onSortChange = {}, onItemClick = {}, onOpenDetails = {},
        )
    }
}
