package com.thalyspenha.pipoca.presentation.components.details

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.presentation.components.isRetryable
import com.thalyspenha.pipoca.presentation.components.toMessage

/**
 * Moldura das telas de detalhes: barra com título e voltar, e snackbar para falha de refresh
 * com cache (não bloqueante, D-032). O conteúdo recebe o `Modifier` com o padding do Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScaffold(
    title: String,
    onBack: () -> Unit,
    refreshError: DataError?,
    onRetry: () -> Unit,
    onDismissRefreshError: () -> Unit,
    /** Falso enquanto o cabeçalho (que já mostra o título) está na tela, para não repetir (D-065). */
    showTitle: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(refreshError) {
        if (refreshError == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Mostrando dados salvos. ${refreshError.toMessage()}",
            actionLabel = if (refreshError.isRetryable) "Tentar" else null,
            duration = SnackbarDuration.Long,
        )
        onDismissRefreshError()
        if (result == SnackbarResult.ActionPerformed) onRetry()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { if (showTitle) Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        content(Modifier.padding(padding))
    }
}

/** Chave do item do cabeçalho na lista dos detalhes (ver [rememberHeaderScrolledPast]). */
const val DETAILS_HEADER_KEY = "details-header"

/**
 * Verdadeiro quando o cabeçalho (item com [DETAILS_HEADER_KEY]) saiu da tela: aí o título passa
 * para a barra de cima. Lista ainda sem layout conta como cabeçalho visível (sem piscar o título).
 */
@Composable
fun rememberHeaderScrolledPast(listState: LazyListState): Boolean {
    val scrolledPast by remember(listState) {
        derivedStateOf {
            val visible = listState.layoutInfo.visibleItemsInfo
            visible.isNotEmpty() && visible.none { it.key == DETAILS_HEADER_KEY }
        }
    }
    return scrolledPast
}
