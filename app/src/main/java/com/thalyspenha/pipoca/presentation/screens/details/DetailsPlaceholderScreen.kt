package com.thalyspenha.pipoca.presentation.screens.details

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.thalyspenha.pipoca.presentation.components.PlaceholderScreen
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme

/**
 * Detalhes provisórios da série (D-023); a tela real chega na Fase 5, parte 3.
 */
@Composable
fun TvShowDetailsScreen(id: Long, onBack: () -> Unit) {
    DetailsPlaceholder(title = "Série", id = id, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsPlaceholder(title: String, id: Long, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        PlaceholderScreen(
            title = "Detalhes em breve",
            description = "TMDB #$id. Sinopse, elenco, gêneros e demais informações chegam numa próxima fase.",
            modifier = Modifier.padding(padding),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TvShowDetailsPreview() {
    PipocaTheme { TvShowDetailsScreen(id = 1396, onBack = {}) }
}
