package com.thalyspenha.pipoca.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/** Abas da bottom bar, na ordem de exibição. */
enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
) {
    HOME(HomeRoute, "Início", Icons.Filled.Home),
    SEARCH(SearchRoute, "Busca", Icons.Filled.Search),
    LIBRARY(LibraryRoute, "Biblioteca", Icons.AutoMirrored.Filled.List),
    COLLECTION(CollectionRoute, "Coleção", Icons.Filled.Star),
    MORE(MoreRoute, "Mais", Icons.Filled.Menu),
}
