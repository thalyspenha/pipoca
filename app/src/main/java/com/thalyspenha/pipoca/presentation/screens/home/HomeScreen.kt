package com.thalyspenha.pipoca.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.thalyspenha.pipoca.presentation.components.PlaceholderScreen
import com.thalyspenha.pipoca.presentation.theme.PipocaTheme

@Composable
fun HomeScreen() {
    PlaceholderScreen(
        title = "Pipoca",
        description = "Continue assistindo, próximos episódios e novidades aparecem aqui.",
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PipocaTheme { HomeScreen() }
}
