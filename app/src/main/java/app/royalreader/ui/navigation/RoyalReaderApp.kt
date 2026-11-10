package app.royalreader.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.royalreader.R
import app.royalreader.core.LocalAutoBrightness
import app.royalreader.core.LocalReducedAnimations
import app.royalreader.ui.bookdetail.BookDetailScreen
import app.royalreader.ui.discover.DiscoverScreen
import app.royalreader.ui.library.LibraryScreen
import app.royalreader.ui.reader.ReaderScreen
import app.royalreader.ui.settings.SettingsScreen
import app.royalreader.ui.settings.SettingsViewModel
import app.royalreader.ui.stats.StatsScreen

private val TOP_LEVEL_ROUTES = setOf(Routes.LIBRARY, Routes.DISCOVER, Routes.STATS, Routes.SETTINGS)

private fun navigateToTab(navController: NavHostController, route: String) {
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
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.uiState.collectAsState()

    CompositionLocalProvider(
        LocalReducedAnimations provides settings.reducedAnimations,
        LocalAutoBrightness provides settings.autoBrightness,
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == Routes.LIBRARY,
                            onClick = { navigateToTab(navController, Routes.LIBRARY) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.shelves_24px),
                                    contentDescription = null
                                )
                            },
                            label = { Text("Library") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.DISCOVER,
                            onClick = { navigateToTab(navController, Routes.DISCOVER) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.explore_24px),
                                    contentDescription = null
                                )
                            },
                            label = { Text("Discover") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.STATS,
                            onClick = { navigateToTab(navController, Routes.STATS) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.calendar_month_24px),
                                    contentDescription = null
                                )
                            },
                            label = { Text("Stats") }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Routes.SETTINGS,
                            onClick = { navigateToTab(navController, Routes.SETTINGS) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.settings_24px),
                                    contentDescription = null
                                )
                            },
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
                    LibraryScreen(
                        onBookClick = { novelID -> navController.navigate(Routes.bookDetail(novelID)) },
                        onDiscoverClick = { navigateToTab(navController, Routes.DISCOVER) },
                        onReadChapter = { novelID, chapterID ->
                            navController.navigate(Routes.reader(novelID, chapterID))
                        },
                    )
                }
                composable(Routes.DISCOVER) {
                    DiscoverScreen(
                        onBookClick = { novelID -> navController.navigate(Routes.bookDetail(novelID)) },
                    )
                }
                composable(Routes.STATS) {
                    StatsScreen()
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }
                composable(
                    route = Routes.BOOK_DETAIL,
                    arguments = listOf(navArgument("novelID") { type = NavType.LongType }),
                ) {
                    BookDetailScreen(
                        onBackClick = { navController.popBackStack() },
                        onReadChapter = { novelID, chapterID ->
                            navController.navigate(Routes.reader(novelID, chapterID))
                        },
                        onDiscoverClick = { navigateToTab(navController, Routes.DISCOVER) },
                    )
                }
                composable(
                    route = Routes.READER,
                    arguments = listOf(
                        navArgument("novelID") { type = NavType.LongType },
                        navArgument("chapterID") { type = NavType.LongType },
                    )
                ) {
                    ReaderScreen(
                        onBackClick = { navController.popBackStack() },
                        onOpenChapter = { novelID, chapterID ->
                            navController.navigate(Routes.reader(novelID, chapterID)) {
                                popUpTo(Routes.READER) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
