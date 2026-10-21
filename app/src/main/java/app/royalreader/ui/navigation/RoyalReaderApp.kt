package app.royalreader.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private val TOP_LEVEL_ROUTES = setOf(Routes.LIBRARY, Routes.DISCOVER, Routes.STATS, Routes.SETTINGS)

private fun navigateToTab(navController: androidx.navigation.NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun RoyalReaderApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TOP_LEVEL_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.LIBRARY,
                        onClick = { navigateToTab(navController, Routes.LIBRARY) },
                        icon = { }, // TODO: add icon
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.DISCOVER,
                        onClick = { navigateToTab(navController, Routes.DISCOVER) },
                        icon = { }, // TODO: add icon
                        label = { Text("Discover") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.STATS,
                        onClick = { navigateToTab(navController, Routes.STATS) },
                        icon = { }, // TODO: add icon
                        label = { Text("Stats") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = { navigateToTab(navController, Routes.SETTINGS) },
                        icon = { }, // TODO: add icon
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LIBRARY) {
                // TODO: LibraryScreen
            }
            composable(Routes.DISCOVER) {
                // TODO: DiscoverScreen
            }
            composable(Routes.STATS) {
                // TODO: StatsScreen
            }
            composable(Routes.SETTINGS) {
                // TODO: SettingsScreen
            }
            composable(
                route = Routes.BOOK_DETAIL,
                arguments = listOf(navArgument("novelID") { type = NavType.LongType }),
            ) {
                // TODO: BookDetailScreen
            }
            composable(
                route = Routes.READER,
                arguments = listOf(
                    navArgument("novelID") { type = NavType.LongType },
                    navArgument("chapterID") { type = NavType.LongType },
                )
            ) {
                // TODO: ReaderScreen
            }
        }
    }
}
