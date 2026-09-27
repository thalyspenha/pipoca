package com.thalyspenha.pipoca.presentation.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thalyspenha.pipoca.presentation.components.PosterImage
import com.thalyspenha.pipoca.util.TmdbImageUrl

private const val BACKDROP_ASPECT_RATIO = 16f / 9f
private val POSTER_OVERLAP = 48.dp

/**
 * Cabeçalho comum a filme e série: backdrop 16:9 com degradê, pôster sobreposto
 * e, ao lado, título e linhas de apoio (título original, ano · duração...).
 */
@Composable
fun DetailsHeader(
    title: String,
    posterPath: String?,
    backdropPath: String?,
    modifier: Modifier = Modifier,
    supportingLines: List<String> = emptyList(),
) {
    // Box em vez de Column + offset: o pôster sobe sobre o backdrop sem deixar espaço vazio abaixo.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val backdropHeight = maxWidth / BACKDROP_ASPECT_RATIO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BACKDROP_ASPECT_RATIO)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            TmdbImageUrl.build(backdropPath, TmdbImageUrl.BACKDROP)?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    ),
            )
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = backdropHeight - POSTER_OVERLAP),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            PosterImage(
                posterPath = posterPath,
                title = title,
                size = TmdbImageUrl.POSTER_SMALL,
                modifier = Modifier.width(112.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                supportingLines.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
