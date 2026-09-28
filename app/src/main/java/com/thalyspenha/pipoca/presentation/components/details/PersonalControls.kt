package com.thalyspenha.pipoca.presentation.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
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
 * Nota pessoal 1–10 como estrelas (10 × 32 dp cabem na largura do S25). Tocar na nota atual remove a nota.
 */
@Composable
fun RatingSelector(rating: Int?, onRatingChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PERSONAL_RATING_RANGE.forEach { value ->
            IconButton(
                onClick = { onRatingChange(if (value == rating) null else value) },
                modifier = Modifier
                    .size(32.dp)
                    .semantics {
                        contentDescription = "Nota $value"
                        selected = value == rating
                    },
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
}
