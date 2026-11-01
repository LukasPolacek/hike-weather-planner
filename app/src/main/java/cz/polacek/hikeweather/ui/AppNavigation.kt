package cz.polacek.hikeweather.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.polacek.hikeweather.AppContainer
import cz.polacek.hikeweather.R
import cz.polacek.hikeweather.ui.dashboard.DashboardScreen
import cz.polacek.hikeweather.ui.profile.ProfileScreen
import cz.polacek.hikeweather.ui.search.SearchScreen
import cz.polacek.hikeweather.ui.setup.SetupScreen
import cz.polacek.hikeweather.ui.trip.TripDetailScreen
import cz.polacek.hikeweather.ui.trip.TripDetailViewModel

/** Hlavni zalozky dolni navigace */
private enum class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    OVERVIEW("overview", R.string.nav_overview, Icons.Filled.Home),
    SEARCH("search", R.string.nav_search, Icons.Filled.Search),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person)
}

private const val TRIP_ROUTE = "trip/{${TripDetailViewModel.ARG_TRIP_ID}}"
private fun tripRoute(id: String) = "trip/$id"

/**
 * Koren UI: bez jmena uzivatele uvodni nastaveni, jinak hlavni cast s navigaci
 * @param pendingTripId vylet k otevreni (po kliknuti na upozorneni)
 */
@Composable
fun HikeWeatherRoot(container: AppContainer, pendingTripId: String?, onTripOpened: () -> Unit) {
    val userName by container.preferences.userName.collectAsStateWithLifecycle()
    val name = userName
    if (name.isNullOrBlank()) {
        SetupScreen(onDone = { container.preferences.setUserName(it) })
    } else {
        MainScaffold(name, pendingTripId, onTripOpened)
    }
}

@Composable
private fun MainScaffold(userName: String, pendingTripId: String?, onTripOpened: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    LaunchedEffect(pendingTripId) {
        pendingTripId?.let {
            navController.navigate(tripRoute(it))
            onTripOpened()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.OVERVIEW.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Tab.OVERVIEW.route) {
                DashboardScreen(
                    userName = userName,
                    onOpenTrip = { navController.navigate(tripRoute(it)) },
                    onSearch = { navController.navigateToTab(Tab.SEARCH.route) }
                )
            }
            composable(Tab.SEARCH.route) {
                SearchScreen(onOpenTrip = { navController.navigate(tripRoute(it)) })
            }
            composable(Tab.PROFILE.route) {
                // po smazani dat se zobrazi uvodni nastaveni (jmeno je prazdne)
                ProfileScreen(onDataDeleted = {})
            }
            composable(
                TRIP_ROUTE,
                arguments = listOf(navArgument(TripDetailViewModel.ARG_TRIP_ID) { type = NavType.StringType })
            ) {
                TripDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** prepnuti zalozky bez hromadeni obrazovek v historii, stav zalozek se zachova */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
