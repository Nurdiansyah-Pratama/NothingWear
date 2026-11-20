package com.nothingwear.app.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nothingwear.app.features.calendar.CalendarRoute
import com.nothingwear.app.features.calendar.EventDetailRoute
import com.nothingwear.app.features.dashboard.DashboardRoute
import com.nothingwear.app.features.device.DevicesScreen
import com.nothingwear.app.features.settings.SettingsRoute
import com.nothingwear.app.features.watchface.WatchFaceStudioScreen

private const val EVENT_DETAIL_ROUTE_PATTERN = "calendar/event/{instanceKey}"

@Composable
fun NothingWearApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = NothingWearDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    } || currentDestination == null

    fun navigateTopLevel(destination: NothingWearDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openEventDetail(instanceKey: String) {
        navController.navigate("calendar/event/${Uri.encode(instanceKey)}")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        NothingWearDestination.entries.forEach { destination ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == destination.route } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = destination.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NothingWearDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(NothingWearDestination.HOME.route) {
                DashboardRoute(
                    onOpenCalendar = { navigateTopLevel(NothingWearDestination.CALENDAR) },
                    onOpenEventDetail = ::openEventDetail,
                    onOpenWatchFaceStudio = { navigateTopLevel(NothingWearDestination.WATCH_FACE_STUDIO) },
                    onOpenDeviceDiagnostics = { navigateTopLevel(NothingWearDestination.DEVICES) },
                )
            }
            composable(NothingWearDestination.CALENDAR.route) {
                CalendarRoute(
                    onOpenEventDetail = ::openEventDetail,
                )
            }
            composable(
                route = EVENT_DETAIL_ROUTE_PATTERN,
                arguments = listOf(
                    navArgument("instanceKey") { type = NavType.StringType },
                ),
            ) {
                EventDetailRoute(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(NothingWearDestination.WATCH_FACE_STUDIO.route) {
                WatchFaceStudioScreen()
            }
            composable(NothingWearDestination.DEVICES.route) {
                DevicesScreen()
            }
            composable(NothingWearDestination.SETTINGS.route) {
                SettingsRoute()
            }
        }
    }
}
