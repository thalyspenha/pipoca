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
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.HistoryType
import com.thalyspenha.pipoca.presentation.screens.collection.CollectionScreen
import com.thalyspenha.pipoca.presentation.screens.collection.form.CollectionItemFormScreen
import com.thalyspenha.pipoca.presentation.screens.collection.detailsRoute
import com.thalyspenha.pipoca.presentation.screens.details.movie.MovieDetailsScreen
import com.thalyspenha.pipoca.presentation.screens.details.season.SeasonScreen
import com.thalyspenha.pipoca.presentation.screens.details.tv.TvShowDetailsScreen
import com.thalyspenha.pipoca.presentation.screens.favorites.FavoritesScreen
import com.thalyspenha.pipoca.presentation.screens.favorites.FavoritesTab
import com.thalyspenha.pipoca.presentation.screens.history.HistoryScreen
import com.thalyspenha.pipoca.presentation.screens.home.HomeScreen
import com.thalyspenha.pipoca.presentation.screens.home.detailsRoute
import com.thalyspenha.pipoca.presentation.screens.library.LibraryScreen
import com.thalyspenha.pipoca.presentation.screens.more.MoreScreen
import com.thalyspenha.pipoca.presentation.screens.search.SearchScreen
import com.thalyspenha.pipoca.presentation.screens.stats.StatsScreen
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
                    onShowClick = { showId -> navController.navigate(TvShowDetailsRoute(showId)) },
                    onFavoritesClick = { navController.navigate(FavoritesRoute) },
                    onSearchClick = { navController.navigateToTab(TopLevelDestination.SEARCH) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(onResultClick = { item -> navController.navigate(item.detailsRoute()) })
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    onItemClick = { item ->
                        navController.navigate(if (item.isMovie) MovieDetailsRoute(item.id) else TvShowDetailsRoute(item.id))
                    },
                    onSearchTitlesClick = { navController.navigateToTab(TopLevelDestination.SEARCH) },
                )
            }
            composable<CollectionRoute> {
                CollectionScreen(
                    onItemClick = { item ->
                        navController.navigate(CollectionItemFormRoute(item.tmdbId, item.mediaType.name, item.id))
                    },
                    onOpenDetails = { item -> navController.navigate(item.detailsRoute()) },
                )
            }
            composable<CollectionItemFormRoute> {
                CollectionItemFormScreen(onDone = navController::navigateUp)
            }
            composable<MoreRoute> {
                MoreScreen(
                    onFavoritesClick = { navController.navigate(FavoritesRoute) },
                    onHistoryClick = { navController.navigate(HistoryRoute) },
                    onStatsClick = { navController.navigate(StatsRoute) },
                )
            }
            composable<StatsRoute> { StatsScreen(onBack = navController::navigateUp) }
            composable<HistoryRoute> {
                HistoryScreen(
                    onBack = navController::navigateUp,
                    onEntryClick = { entry ->
                        navController.navigate(
                            if (entry.type == HistoryType.MOVIE) MovieDetailsRoute(entry.tmdbId) else TvShowDetailsRoute(entry.tmdbId),
                        )
                    },
                )
            }
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onBack = navController::navigateUp,
                    onItemClick = { item ->
                        navController.navigate(
                            if (item.tab == FavoritesTab.MOVIES) MovieDetailsRoute(item.id) else TvShowDetailsRoute(item.id),
                        )
                    },
                )
            }
            composable<MovieDetailsRoute> {
                val id = it.toRoute<MovieDetailsRoute>().id
                MovieDetailsScreen(
                    onBack = navController::navigateUp,
                    onAddToCollection = { navController.navigate(CollectionItemFormRoute(id, CollectionMediaType.MOVIE.name)) },
                    onEditCollectionItem = { itemId ->
                        navController.navigate(CollectionItemFormRoute(id, CollectionMediaType.MOVIE.name, itemId))
                    },
                )
            }
            composable<SeasonRoute> {
                SeasonScreen(onBack = navController::navigateUp)
            }
            composable<TvShowDetailsRoute> {
                val id = it.toRoute<TvShowDetailsRoute>().id
                TvShowDetailsScreen(
                    onBack = navController::navigateUp,
                    onSeasonClick = { seasonNumber -> navController.navigate(SeasonRoute(id, seasonNumber)) },
                    onAddToCollection = { navController.navigate(CollectionItemFormRoute(id, CollectionMediaType.TV_SHOW.name)) },
                    onEditCollectionItem = { itemId ->
                        navController.navigate(CollectionItemFormRoute(id, CollectionMediaType.TV_SHOW.name, itemId))
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
