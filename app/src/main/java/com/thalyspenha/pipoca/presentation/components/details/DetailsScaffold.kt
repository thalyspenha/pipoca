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
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
