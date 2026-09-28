package com.thalyspenha.pipoca.presentation.screens.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.presentation.components.ErrorContent
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.PosterImage
import com.thalyspenha.pipoca.presentation.components.isRetryable
import com.thalyspenha.pipoca.presentation.components.toMessage
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme

@Composable
fun SearchScreen(
    onResultClick: (SearchResultItem) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContentView(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onTypeChange = viewModel::onTypeChange,
        onRetry = viewModel::retry,
        onResultClick = onResultClick,
    )
}

@Composable
private fun SearchContentView(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onTypeChange: (SearchType) -> Unit,
    onRetry: () -> Unit,
    onResultClick: (SearchResultItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        )
        TypeSelector(
            selected = state.type,
            onTypeChange = onTypeChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        when (val content = state.content) {
            SearchContent.Idle -> SearchMessage(
                title = "Busca",
                description = "Digite pelo menos 2 letras para pesquisar filmes e séries no TMDB.",
            )
            SearchContent.Loading -> LoadingContent()
            is SearchContent.Empty -> {
                val other = if (state.type == SearchType.MOVIES) SearchType.TV_SHOWS else SearchType.MOVIES
                SearchMessage(
                    title = "Nada encontrado",
                    description = "Nenhum resultado para \"${content.query}\". Confira o nome ou procure em " +
                        (if (other == SearchType.TV_SHOWS) "séries." else "filmes."),
                    action = (if (other == SearchType.TV_SHOWS) "Procurar em séries" else "Procurar em filmes") to
                        { onTypeChange(other) },
                )
            }
            is SearchContent.Error -> ErrorContent(
                message = content.error.toMessage(),
                onRetry = onRetry.takeIf { content.error.isRetryable },
            )
            is SearchContent.Results -> ResultList(content.items, onResultClick)
        }
    }
}

/**
 * Mensagem da Busca alinhada ao topo, logo abaixo do seletor: centralizada, ficava atrás do teclado (D-061).
 */
@Composable
private fun SearchMessage(title: String, description: String, action: Pair<String, () -> Unit>? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Buscar filmes e séries") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpar busca")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // A busca já acontece enquanto digita; o botão do teclado só fecha o teclado.
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun TypeSelector(selected: SearchType, onTypeChange: (SearchType) -> Unit, modifier: Modifier = Modifier) {
    val options = SearchType.entries
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onTypeChange(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(type.tabLabel)
            }
        }
    }
}

@Composable
private fun ResultList(items: List<SearchResultItem>, onResultClick: (SearchResultItem) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(items, key = { "${it.type}-${it.id}" }) { item ->
            ResultRow(item, onClick = { onResultClick(item) })
            HorizontalDivider(modifier = Modifier.padding(start = 88.dp))
        }
    }
}

@Composable
private fun ResultRow(item: SearchResultItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(posterPath = item.posterPath, title = item.title, modifier = Modifier.width(56.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(item.year?.toString(), item.type.itemLabel).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SearchType.tabLabel: String
    get() = when (this) {
        SearchType.MOVIES -> "FILMES"
        SearchType.TV_SHOWS -> "SÉRIES"
    }

private val SearchType.itemLabel: String
    get() = when (this) {
        SearchType.MOVIES -> "Filme"
        SearchType.TV_SHOWS -> "Série"
    }

@Preview(showBackground = true)
@Composable
private fun SearchResultsPreview() {
    PipocaTheme {
        SearchContentView(
            state = SearchUiState(
                query = "matrix",
                content = SearchContent.Results(
                    listOf(
                        SearchResultItem(603, SearchType.MOVIES, "Matrix", 1999, null),
                        SearchResultItem(604, SearchType.MOVIES, "Matrix Reloaded", 2003, null),
                        SearchResultItem(1, SearchType.MOVIES, "Filme sem data", null, null),
                    ),
                ),
            ),
            onQueryChange = {},
            onTypeChange = {},
            onRetry = {},
            onResultClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchErrorPreview() {
    PipocaTheme {
        SearchContentView(
            state = SearchUiState(query = "matrix", content = SearchContent.Error(DataError.Network)),
            onQueryChange = {},
            onTypeChange = {},
            onRetry = {},
            onResultClick = {},
        )
    }
}
