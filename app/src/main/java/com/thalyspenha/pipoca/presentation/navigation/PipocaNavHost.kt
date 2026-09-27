package com.thalyspenha.pipoca.presentation.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.thalyspenha.pipoca.presentation.screens.collection.CollectionScreen
import com.thalyspenha.pipoca.presentation.screens.collection.detailsRoute
import com.thalyspenha.pipoca.presentation.screens.details.movie.MovieDetailsScreen
import com.thalyspenha.pipoca.presentation.screens.details.season.SeasonScreen
import com.thalyspenha.pipoca.presentation.screens.details.tv.TvShowDetailsScreen
import com.thalyspenha.pipoca.presentation.screens.home.HomeScreen
import com.thalyspenha.pipoca.presentation.screens.home.detailsRoute
import com.thalyspenha.pipoca.presentation.screens.library.LibraryScreen
import com.thalyspenha.pipoca.presentation.screens.more.MoreScreen
import com.thalyspenha.pipoca.presentation.screens.search.SearchScreen
import com.thalyspenha.pipoca.presentation.screens.search.detailsRoute

@Composable
fun PipocaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Em telas empilhadas (detalhes), a aba de origem continua marcada.
    var lastTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    val currentTab = TopLevelDestination.entries
        .firstOrNull { tab -> currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    LaunchedEffect(currentTab) {
        if (currentTab != null) lastTab = currentTab
    }
    val selectedTab = currentTab ?: lastTab

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selectedTab,
                        onClick = {
                            // A aba pode reabrir direto numa tela empilhada (restoreState); marca já.
                            lastTab = destination
                            navController.navigateToTab(destination)
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            // Insets já aplicados aqui não são reaplicados pelos Scaffolds internos (detalhes).
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onItemClick = { item -> navController.navigate(item.detailsRoute()) },
                    onSearchClick = { navController.navigateToTab(TopLevelDestination.SEARCH) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(onResultClick = { item -> navController.navigate(item.detailsRoute()) })
            }
            composable<LibraryRoute> { LibraryScreen() }
            composable<CollectionRoute> {
                CollectionScreen(onItemClick = { item -> navController.navigate(item.detailsRoute()) })
            }
            composable<MoreRoute> { MoreScreen() }
            composable<MovieDetailsRoute> {
                MovieDetailsScreen(onBack = navController::navigateUp)
            }
            composable<SeasonRoute> {
                SeasonScreen(onBack = navController::navigateUp)
            }
            composable<TvShowDetailsRoute> {
                TvShowDetailsScreen(
                    onBack = navController::navigateUp,
                    onSeasonClick = { seasonNumber ->
                        navController.navigate(SeasonRoute(it.toRoute<TvShowDetailsRoute>().id, seasonNumber))
                    },
                )
            }
        }
    }
}

/** Padrão de abas: uma pilha por aba, sem duplicar destinos. */
private fun NavController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
