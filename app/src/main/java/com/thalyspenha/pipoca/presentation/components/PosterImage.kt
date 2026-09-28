package com.thalyspenha.pipoca.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.thalyspenha.pipoca.util.TmdbImageUrl

private const val POSTER_ASPECT_RATIO = 2f / 3f

/**
 * Poster 2:3. Sem imagem (ou enquanto carrega), mostra a inicial do título.
 * Decorativo para leitores de tela: o título sempre aparece ao lado.
 */
@Composable
fun PosterImage(
    posterPath: String?,
    title: String,
    modifier: Modifier = Modifier,
    size: String = TmdbImageUrl.POSTER_THUMB,
) {
    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .aspectRatio(POSTER_ASPECT_RATIO)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TmdbImageUrl.build(posterPath, size)?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
