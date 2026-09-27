package com.thalyspenha.pipoca.presentation.screens.details.tv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thalyspenha.pipoca.domain.progress.ShowProgress
import com.thalyspenha.pipoca.presentation.components.PosterImage
import com.thalyspenha.pipoca.util.episodeCode
import com.thalyspenha.pipoca.util.formatDate

/**
 * "37 / 62 episódios", barra e percentual, próximo episódio com atalho para marcar,
 * ou "Em dia"/"Concluída" (D-035, D-037).
 */
@Composable
fun ProgressCard(
    progress: ShowProgress?,
    inLibrary: Boolean,
    isLoading: Boolean,
    onMarkNext: () -> Unit,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                progress == null || (!progress.isComplete && !inLibrary) -> {
                    Text(
                        "Marque episódios ou adicione a série à biblioteca para acompanhar o progresso.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!inLibrary && progress?.isComplete == false) {
                        TextButton(onClick = onLoad, enabled = !isLoading) { Text("Calcular progresso") }
                    }
                }
                else -> ProgressContent(progress, isLoading, onMarkNext)
            }
        }
    }
}

@Composable
private fun ProgressContent(progress: ShowProgress, isLoading: Boolean, onMarkNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${progress.watched} / ${progress.available} episódios",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text("${progress.percent}%", style = MaterialTheme.typography.titleMedium)
    }
    LinearProgressIndicator(
        progress = { progress.percent / 100f },
        modifier = Modifier.fillMaxWidth(),
    )
    if (isLoading || !progress.isComplete) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(Modifier.width(16.dp), strokeWidth = 2.dp)
            Text(
                "Carregando temporadas…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val next = progress.nextEpisode
    when {
        progress.isCompleted -> StatusLine("Série concluída")
        progress.isCaughtUp -> StatusLine(
            "Em dia" + (progress.upcomingEpisode?.airDate?.let { " · próximo em ${formatDate(it)}" } ?: ""),
        )
        next != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Próximo: ${episodeCode(next.seasonNumber, next.episodeNumber)}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(next.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            FilledTonalButton(onClick = onMarkNext) { Text("Assisti") }
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Linha da lista de temporadas: pôster, nome, "5 / 7 assistidos" ou total, seta. */
@Composable
fun SeasonItem(row: SeasonRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PosterImage(posterPath = row.posterPath, title = row.name, modifier = Modifier.width(48.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.titleSmall)
            Text(
                if (row.watched != null && row.aired != null) {
                    "${row.watched} / ${row.aired} assistidos"
                } else {
                    if (row.episodeCount == 1) "1 episódio" else "${row.episodeCount} episódios"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (row.isFullyWatched) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Temporada assistida", tint = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
