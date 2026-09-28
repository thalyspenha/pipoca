package com.thalyspenha.pipoca.presentation.navigation

import androidx.compose.material.icons.Icons
import com.thalyspenha.pipoca.presentation.components.AppIcons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Abas da bottom bar, na ordem de exibição (D-050). */
enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
) {
    HOME(HomeRoute, "Início", Icons.Filled.Home),
    LIBRARY(LibraryRoute, "Biblioteca", AppIcons.VideoLibrary),
    SEARCH(SearchRoute, "Busca", Icons.Filled.Search),
    COLLECTION(CollectionRoute, "Coleção", AppIcons.Album),
    MORE(MoreRoute, "Mais", Icons.Filled.Menu),
}
