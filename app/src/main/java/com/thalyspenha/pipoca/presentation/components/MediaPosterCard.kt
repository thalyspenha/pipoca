package com.thalyspenha.pipoca.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.TmdbImageUrl

/** Status exibido no card: ícone Material + texto (D-050: sem emoji). */
enum class MediaStatusBadge(val label: String, val icon: ImageVector) {
    WANT_TO_WATCH("Quero assistir", Icons.Filled.DateRange),
    WATCHED("Assistido", Icons.Filled.CheckCircle),
    WATCHING("Assistindo", Icons.Filled.PlayArrow),
    COMPLETED("Concluída", Icons.Filled.CheckCircle),
    PAUSED("Pausada", AppIcons.Pause),
    DROPPED("Abandonada", Icons.Filled.Close),
}

/** Progresso de série já calculado para exibir ("36/50 episódios", 72%). */
@Immutable
data class MediaCardProgress(val watched: Int, val available: Int, val percent: Int)

/** Dados de um card. `title` nulo = ainda sem cache TMDB. */
@Immutable
data class MediaCardData(
    val id: Long,
    val isMovie: Boolean,
    val title: String?,
    val posterPath: String?,
    val year: Int?,
    val status: MediaStatusBadge,
    val isFavorite: Boolean,
    val progress: MediaCardProgress? = null,
)

/**
 * Card de filme/série com o pôster como elemento principal. [grid] = pôster grande com
 * título abaixo; senão, linha de lista (pôster pequeno à esquerda).
 */
@Composable
fun MediaPosterCard(
    data: MediaCardData,
    grid: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = data.title ?: "Carregando…"
    if (grid) {
        Column(
            modifier = modifier.clickable(onClick = onClick).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box {
                PosterImage(
                    posterPath = data.posterPath,
                    title = title,
                    size = TmdbImageUrl.POSTER_SMALL,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (data.isFavorite) FavoriteBadge(Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            data.year?.let { YearText(it) }
            data.progress?.let { ProgressLine(it, compact = true) } ?: StatusLine(data.status)
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PosterImage(posterPath = data.posterPath, title = title, modifier = Modifier.width(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                data.year?.let { YearText(it) }
                StatusLine(data.status)
                data.progress?.let { ProgressLine(it, compact = false) }
            }
            if (data.isFavorite) {
                Icon(Icons.Filled.Favorite, contentDescription = "Favorito", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun YearText(year: Int) {
    Text(year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun StatusLine(status: MediaStatusBadge) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(status.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(status.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun ProgressLine(progress: MediaCardProgress, compact: Boolean) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = "${progress.watched} de ${progress.available} episódios, ${progress.percent}%"
        },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.weight(1f))
            Text("${progress.percent}%", style = MaterialTheme.typography.labelSmall)
        }
        Text(
            if (compact) "${progress.watched}/${progress.available}" else "${progress.watched}/${progress.available} episódios",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteBadge(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = "Favorito",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(4.dp).size(16.dp),
        )
    }
}

private val previewShow = MediaCardData(
    id = 1396, isMovie = false, title = "Breaking Bad", posterPath = null, year = 2008,
    status = MediaStatusBadge.WATCHING, isFavorite = true, progress = MediaCardProgress(36, 50, 72),
)

@Preview(showBackground = true, widthDp = 140)
@Composable
private fun MediaPosterCardGridPreview() {
    PipocaTheme { MediaPosterCard(previewShow, grid = true, onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MediaPosterCardListPreview() {
    PipocaTheme {
        MediaPosterCard(
            MediaCardData(242, true, "Predator", null, 1987, MediaStatusBadge.WATCHED, isFavorite = false),
            grid = false,
            onClick = {},
        )
    }
}
