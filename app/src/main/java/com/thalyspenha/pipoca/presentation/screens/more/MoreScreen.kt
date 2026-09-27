package com.thalyspenha.pipoca.presentation.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme

/** Aba "Mais": entradas para telas de organização (D-043). Estatísticas e configurações entram depois. */
@Composable
fun MoreScreen(onFavoritesClick: () -> Unit, onHistoryClick: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Mais",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        MoreEntry(Icons.Filled.Favorite, "Favoritos", "Filmes e séries favoritos", onFavoritesClick)
        HorizontalDivider()
        MoreEntry(Icons.Filled.DateRange, "Histórico", "Tudo que você assistiu, por data", onHistoryClick)
        HorizontalDivider()
    }
}

@Composable
private fun MoreEntry(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun MorePreview() {
    PipocaTheme { MoreScreen(onFavoritesClick = {}, onHistoryClick = {}) }
}
