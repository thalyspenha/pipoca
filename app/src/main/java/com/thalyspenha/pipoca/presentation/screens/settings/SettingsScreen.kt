package com.thalyspenha.pipoca.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.AppInfo
import com.thalyspenha.pipoca.domain.model.DatabaseInfo
import com.thalyspenha.pipoca.domain.model.ThemeMode
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.formatDecimal
import com.thalyspenha.pipoca.util.formatInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAboutClick: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clear = state.cacheClear
    LaunchedEffect(clear) {
        val message = when (clear) {
            is CacheClearState.Done -> cacheClearedMessage(clear.result.movies + clear.result.tvShows)
            CacheClearState.Failed -> "Não foi possível limpar o cache."
            else -> null
        }
        if (message != null) {
            // Marcar como mostrada só depois: mudar o estado antes troca a chave do efeito e cancela o aviso.
            snackbarHostState.showSnackbar(message)
            viewModel.onCacheClearMessageShown()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        SettingsContent(
            state = state,
            onThemeChange = viewModel::setThemeMode,
            onClearCache = viewModel::clearCache,
            onAboutClick = onAboutClick,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onThemeChange: (ThemeMode) -> Unit,
    onClearCache: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionTitle("Tema")
        ThemeOptions(selected = state.themeMode, onSelect = onThemeChange)
        HorizontalDivider()

        SectionTitle("Armazenamento")
        val running = state.cacheClear == CacheClearState.Running
        ListItem(
            headlineContent = { Text("Limpar cache") },
            supportingContent = {
                Text("Apaga imagens salvas e dados do TMDB de títulos fora da biblioteca, coleção e histórico.")
            },
            leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
            trailingContent = { if (running) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) },
            modifier = Modifier.clickable(enabled = !running) { confirmClear = true },
        )
        HorizontalDivider()

        SectionTitle("Banco de dados")
        DatabaseInfoRows(state.databaseInfo)
        HorizontalDivider()

        SectionTitle("Aplicativo")
        ListItem(
            headlineContent = { Text("Sobre") },
            supportingContent = { Text("Créditos e atribuição ao TMDB") },
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onAboutClick),
        )
        InfoRow("Versão", "${state.appInfo.versionName} (${state.appInfo.versionCode})")
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Limpar cache?") },
            text = {
                Text(
                    "Imagens serão baixadas de novo quando aparecerem. Sua biblioteca, coleção, histórico, " +
                        "episódios e notas não são afetados.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearCache()
                }) { Text("Limpar") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun ThemeOptions(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            ListItem(
                headlineContent = { Text(mode.label()) },
                leadingContent = { RadioButton(selected = mode == selected, onClick = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = mode == selected, role = Role.RadioButton) { onSelect(mode) },
            )
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Padrão do sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}

@Composable
private fun DatabaseInfoRows(info: DatabaseInfo?) {
    if (info == null) {
        InfoRow("Carregando…", "")
        return
    }
    InfoRow("Filmes na biblioteca", formatInt(info.movies.toLong()))
    InfoRow("Séries na biblioteca", formatInt(info.tvShows.toLong()))
    InfoRow("Episódios assistidos", formatInt(info.watchedEpisodes.toLong()))
    InfoRow("Itens na coleção", formatInt(info.collectionItems.toLong()))
    InfoRow("Registros no histórico", formatInt(info.historyEntries.toLong()))
    InfoRow(
        "Em cache (TMDB)",
        "${info.cachedMovies} filmes · ${info.cachedTvShows} séries · ${info.cachedEpisodes} episódios",
    )
    InfoRow("Tamanho", formatBytes(info.fileSizeBytes))
    InfoRow("Versão do banco", info.schemaVersion.toString())
}

@Composable
private fun InfoRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium) },
    )
}

private fun cacheClearedMessage(removed: Int): String = when (removed) {
    0 -> "Cache de imagens limpo."
    1 -> "Cache limpo: 1 título removido."
    else -> "Cache limpo: $removed títulos removidos."
}

internal fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) "${formatDecimal(kb)} KB" else "${formatDecimal(kb / 1024)} MB"
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    PipocaTheme {
        SettingsContent(
            state = SettingsUiState(
                databaseInfo = DatabaseInfo(4, 12, 5, 230, 8, 240, 20, 9, 410, 1_572_864),
                appInfo = AppInfo("0.1.0", 1),
            ),
            onThemeChange = {},
            onClearCache = {},
            onAboutClick = {},
        )
    }
}
