package com.thalyspenha.pipoca.presentation.components.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thalyspenha.pipoca.domain.model.CollectionItem
import com.thalyspenha.pipoca.presentation.components.label
import com.thalyspenha.pipoca.util.formatDate

/**
 * Itens da coleção deste título (toque edita) e botão de adicionar (D-042).
 * Independente do status pessoal acima (D-039).
 */
@Composable
fun CollectionSection(
    items: List<CollectionItem>,
    onAdd: () -> Unit,
    onItemClick: (CollectionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailsSection(title = "Coleção", modifier = modifier) {
        Column {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            listOfNotNull(item.format.label, item.edition).joinToString(" · "),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        val details = listOfNotNull(
                            item.region?.let { "Região $it" },
                            "${item.quantity} unidades".takeIf { item.quantity > 1 },
                            item.acquiredAt?.let { "Adquirido em ${formatDate(it)}" },
                        )
                        if (details.isNotEmpty()) {
                            Text(
                                details.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(Icons.Filled.Edit, contentDescription = "Editar item")
                }
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.padding(horizontal = 16.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(if (items.isEmpty()) "Adicionar à coleção" else "Adicionar outro formato")
            }
        }
    }
}
