package com.thalyspenha.pipoca.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Aba de seção (Filmes/Séries/Todos) usada em Biblioteca, Favoritos, Busca e Histórico (D-064).
 * Não selecionada em cinza: por padrão o Material 3 usa a mesma cor e só o sublinhado diferencia.
 */
@Composable
fun SectionTab(selected: Boolean, onClick: () -> Unit, text: String) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = { Text(text) },
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
