package com.thalyspenha.pipoca.presentation.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.TextButton
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thalyspenha.pipoca.domain.model.PERSONAL_RATING_RANGE

/**
 * Status pessoal como botões segmentados. Nenhum selecionado = fora da biblioteca;
 * tocar no selecionado desmarca (a tela decide o que isso significa, D-032).
 */
@Composable
fun <T> StatusSelector(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onClick(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                // Sem o ✓: com 3 opções o texto não cabe; o preenchimento já indica a seleção.
                icon = {},
            ) { Text(label(option), maxLines = 1) }
        }
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalIconToggleButton(checked = isFavorite, onCheckedChange = { onClick() }, modifier = modifier) {
        Icon(
            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Remover dos favoritos" else "Favoritar",
        )
    }
}

/**
 * Nota pessoal 1–10 como estrelas. Cada estrela ocupa 1/10 da largura com 48 dp de altura, sem vãos
 * entre alvos (D-061). Remover a nota é um botão explícito, não tocar de novo na estrela.
 */
@Composable
fun RatingSelector(rating: Int?, onRatingChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            PERSONAL_RATING_RANGE.forEach { value ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .selectable(selected = value == rating, role = Role.RadioButton) { onRatingChange(value) }
                        .semantics { contentDescription = "Nota $value" },
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (rating != null && value <= rating) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                }
            }
        }
        if (rating != null) {
            TextButton(onClick = { onRatingChange(null) }) { Text("Remover nota") }
        }
    }
}
