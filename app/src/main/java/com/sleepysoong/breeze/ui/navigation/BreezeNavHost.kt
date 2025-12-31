package com.sleepysoong.breeze.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sleepysoong.breeze.ui.history.HistoryScreen
import com.sleepysoong.breeze.ui.home.HomeScreen
import com.sleepysoong.breeze.ui.pace.PaceDialScreen
import com.sleepysoong.breeze.ui.running.RunningScreen
import com.sleepysoong.breeze.ui.settings.SettingsScreen
import com.sleepysoong.breeze.ui.theme.BreezeTheme

object Routes {
    const val PACE_DIAL = "pace_dial"
    const val RUNNING = "running/{paceSeconds}"
    
    fun running(paceSeconds: Int) = "running/$paceSeconds"
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "홈",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object History : BottomNavItem(
        route = "history",
        title = "기록",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    object Settings : BottomNavItem(
        route = "settings",
        title = "설정",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.History,
    BottomNavItem.Settings
)

@Composable
fun BreezeNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 하단 네비게이션 바를 숨길 화면들
    val hideBottomBarRoutes = listOf(Routes.PACE_DIAL, Routes.RUNNING)
    val shouldShowBottomBar = hideBottomBarRoutes.none { currentRoute?.startsWith(it.split("/").first()) == true }
    
    Scaffold(
        containerColor = BreezeTheme.colors.background,
        bottomBar = {
            if (shouldShowBottomBar) {
                BreezeBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onStartRunning = {
                        navController.navigate(Routes.PACE_DIAL)
                    }
                )
            }
            composable(BottomNavItem.History.route) {
                HistoryScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
            composable(Routes.PACE_DIAL) {
                PaceDialScreen(
                    onDismiss = {
                        navController.popBackStack()
                    },
                    onStartRunning = { paceSeconds ->
                        navController.navigate(Routes.running(paceSeconds)) {
                            popUpTo(Routes.PACE_DIAL) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Routes.RUNNING,
                arguments = listOf(
                    navArgument("paceSeconds") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val paceSeconds = backStackEntry.arguments?.getInt("paceSeconds") ?: 390
                RunningScreen(
                    targetPaceSeconds = paceSeconds,
                    onFinish = { distance, time, averagePace ->
                        // TODO: 러닝 완료 화면으로 이동
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = true }
                        }
                    },
                    onStop = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BreezeBottomNavigation(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = BreezeTheme.colors.surface,
        contentColor = BreezeTheme.colors.textPrimary
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = BreezeTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BreezeTheme.colors.primary,
                    selectedTextColor = BreezeTheme.colors.primary,
                    unselectedIconColor = BreezeTheme.colors.textSecondary,
                    unselectedTextColor = BreezeTheme.colors.textSecondary,
                    indicatorColor = BreezeTheme.colors.primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
