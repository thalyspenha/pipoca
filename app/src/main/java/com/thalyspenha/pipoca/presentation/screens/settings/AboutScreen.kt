package com.thalyspenha.pipoca.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme

private const val TMDB_URL = "https://www.themoviedb.org"

/** Configurações → Sobre, com a atribuição exigida pelos termos da API do TMDB (D-057). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        AboutContent(Modifier.padding(padding))
    }
}

@Composable
private fun AboutContent(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Pipoca", style = MaterialTheme.typography.headlineMedium)
        Text(
            "App pessoal para acompanhar filmes, séries e a coleção física. Tudo fica salvo no aparelho: " +
                "sem conta, sem nuvem e sem anúncios.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dados de filmes e séries", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Títulos, sinopses, elenco, pôsteres e episódios vêm do The Movie Database (TMDB).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                // Texto exigido pelos termos de uso da API, em inglês como o TMDB determina.
                Text(
                    "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { uriHandler.openUri(TMDB_URL) }) { Text("Abrir themoviedb.org") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutPreview() {
    PipocaTheme { AboutContent() }
}
