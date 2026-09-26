package com.thalyspenha.pipoca.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thalyspenha.pipoca.presentation.screens.collection.CollectionScreen
import com.thalyspenha.pipoca.presentation.screens.home.HomeScreen
import com.thalyspenha.pipoca.presentation.screens.library.LibraryScreen
import com.thalyspenha.pipoca.presentation.screens.more.MoreScreen
import com.thalyspenha.pipoca.presentation.screens.search.SearchScreen

@Composable
fun PipocaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.hasRoute(destination.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Padrão de abas: uma pilha por aba, sem duplicar destinos.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> { HomeScreen() }
            composable<SearchRoute> { SearchScreen() }
            composable<LibraryRoute> { LibraryScreen() }
            composable<CollectionRoute> { CollectionScreen() }
            composable<MoreRoute> { MoreScreen() }
        }
    }
}
