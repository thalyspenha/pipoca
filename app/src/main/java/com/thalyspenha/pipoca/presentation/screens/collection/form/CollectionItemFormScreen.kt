package com.thalyspenha.pipoca.presentation.screens.collection.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.presentation.components.LoadingContent
import com.thalyspenha.pipoca.presentation.components.label
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme
import com.thalyspenha.pipoca.util.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun CollectionItemFormScreen(onDone: () -> Unit, viewModel: CollectionItemFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    CollectionItemFormContent(
        state = state,
        today = viewModel.today,
        onBack = onDone,
        onFormatChange = viewModel::onFormatChange,
        onEditionChange = viewModel::onEditionChange,
        onRegionChange = viewModel::onRegionChange,
        onQuantityChange = viewModel::onQuantityChange,
        onAcquiredAtChange = viewModel::onAcquiredAtChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::onSave,
        onDelete = viewModel::onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionItemFormContent(
    state: CollectionItemFormState,
    today: LocalDate,
    onBack: () -> Unit,
    onFormatChange: (MediaFormat) -> Unit,
    onEditionChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onAcquiredAtChange: (LocalDate?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar item" else "Adicionar à coleção") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remover da coleção")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading && state.isEditing && !state.showErrors) {
            LoadingContent(Modifier.padding(padding))
        } else {
            FormFields(
                state, today, onFormatChange, onEditionChange, onRegionChange, onQuantityChange,
                onAcquiredAtChange, onNotesChange, onSave,
                Modifier.padding(padding),
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remover da coleção?") },
            text = { Text("Só este item sai da coleção. Status assistido e favoritos não mudam.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormFields(
    state: CollectionItemFormState,
    today: LocalDate,
    onFormatChange: (MediaFormat) -> Unit,
    onEditionChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onAcquiredAtChange: (LocalDate?) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.title?.let { Text(it, style = MaterialTheme.typography.titleLarge) }

        Text("Formato", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaFormat.entries.forEach { format ->
                FilterChip(
                    selected = format == state.format,
                    onClick = { onFormatChange(format) },
                    label = { Text(format.label) },
                )
            }
        }

        OutlinedTextField(
            value = state.edition,
            onValueChange = onEditionChange,
            label = { Text("Edição") },
            placeholder = { Text("Ex.: Steelbook") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.region,
                onValueChange = onRegionChange,
                label = { Text("Região") },
                placeholder = { Text("Ex.: A/B") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            val quantityError = state.quantityError.takeIf { state.showErrors }
            OutlinedTextField(
                value = state.quantity,
                onValueChange = onQuantityChange,
                label = { Text("Quantidade") },
                singleLine = true,
                isError = quantityError != null,
                supportingText = quantityError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        AcquiredAtField(
            date = state.acquiredAt,
            today = today,
            error = state.dateError(today).takeIf { state.showErrors },
            onChange = onAcquiredAtChange,
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text("Observações") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = onSave, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isEditing) "Salvar" else "Adicionar")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcquiredAtField(date: LocalDate?, today: LocalDate, error: String?, onChange: (LocalDate?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Data de aquisição", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { open = true }) { Text(date?.let(::formatDate) ?: "Escolher data") }
            if (date != null) TextButton(onClick = { onChange(null) }) { Text("Limpar") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
    if (open) {
        // DatePicker trabalha em millis UTC.
        val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = remember(todayMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                    override fun isSelectableYear(year: Int) = year <= today.year
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionItemFormPreview() {
    PipocaTheme {
        CollectionItemFormContent(
            state = CollectionItemFormState(
                tmdbId = 603, mediaType = CollectionMediaType.MOVIE, isEditing = true, title = "Matrix",
                format = MediaFormat.UHD_4K_BLURAY, edition = "Steelbook", acquiredAt = LocalDate.of(2024, 5, 1),
            ),
            today = LocalDate.of(2026, 9, 27),
            onBack = {}, onFormatChange = {}, onEditionChange = {}, onRegionChange = {}, onQuantityChange = {},
            onAcquiredAtChange = {}, onNotesChange = {}, onSave = {}, onDelete = {},
        )
    }
}
