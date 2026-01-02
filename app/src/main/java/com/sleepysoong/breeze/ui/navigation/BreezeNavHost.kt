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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sleepysoong.breeze.ui.detail.RecordDetailScreen
import com.sleepysoong.breeze.ui.history.HistoryScreen
import com.sleepysoong.breeze.ui.home.HomeScreen
import com.sleepysoong.breeze.ui.pace.PaceDialScreen
import com.sleepysoong.breeze.ui.result.RunningResultScreen
import com.sleepysoong.breeze.ui.running.RunningScreen
import com.sleepysoong.breeze.ui.settings.SettingsScreen
import com.sleepysoong.breeze.ui.theme.BreezeTheme
import com.sleepysoong.breeze.ui.viewmodel.RunningViewModel
import com.sleepysoong.breeze.ui.viewmodel.SaveResult

object Routes {
    const val PACE_DIAL = "pace_dial"
    const val RUNNING = "running/{paceSeconds}"
    const val RESULT = "result/{distance}/{time}/{averagePace}/{targetPace}"
    const val RECORD_DETAIL = "record_detail/{recordId}"
    
    fun running(paceSeconds: Int) = "running/$paceSeconds"
    fun result(distance: Double, time: Long, averagePace: Int, targetPace: Int) = 
        "result/${distance.toFloat()}/$time/$averagePace/$targetPace"
    fun recordDetail(recordId: Long) = "record_detail/$recordId"
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
    navController: NavHostController = rememberNavController(),
    viewModel: RunningViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val saveResult by viewModel.saveResult.collectAsState()
    
    // 하단 네비게이션 바를 숨길 화면들
    val hideBottomBarRoutes = listOf(Routes.PACE_DIAL, Routes.RUNNING, Routes.RESULT, Routes.RECORD_DETAIL)
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
                val latestRecord by viewModel.latestRecord.collectAsState()
                val weeklyRecords by viewModel.weeklyRecords.collectAsState()
                
                HomeScreen(
                    latestRecord = latestRecord,
                    weeklyRecords = weeklyRecords,
                    onStartRunning = {
                        navController.navigate(Routes.PACE_DIAL)
                    }
                )
            }
            composable(BottomNavItem.History.route) {
                val allRecords by viewModel.allRecords.collectAsState()
                
                HistoryScreen(
                    records = allRecords,
                    onRecordClick = { record ->
                        navController.navigate(Routes.recordDetail(record.id))
                    },
                    onDeleteRecord = { record ->
                        viewModel.deleteRecord(record)
                    }
                )
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
                    viewModel = viewModel,
                    onFinish = { distance, time, averagePace ->
                        navController.navigate(Routes.result(distance, time, averagePace, paceSeconds)) {
                            popUpTo(BottomNavItem.Home.route)
                        }
                    },
                    onStop = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Routes.RESULT,
                arguments = listOf(
                    navArgument("distance") { type = NavType.FloatType },
                    navArgument("time") { type = NavType.LongType },
                    navArgument("averagePace") { type = NavType.IntType },
                    navArgument("targetPace") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val distance = backStackEntry.arguments?.getFloat("distance")?.toDouble() ?: 0.0
                val time = backStackEntry.arguments?.getLong("time") ?: 0L
                val averagePace = backStackEntry.arguments?.getInt("averagePace") ?: 0
                val targetPace = backStackEntry.arguments?.getInt("targetPace") ?: 390
                
                // 저장 결과 처리
                LaunchedEffect(saveResult) {
                    if (saveResult is SaveResult.Success) {
                        viewModel.resetSaveResult()
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = true }
                        }
                    }
                }
                
                RunningResultScreen(
                    distanceMeters = distance,
                    elapsedTimeMs = time,
                    averagePaceSeconds = averagePace,
                    targetPaceSeconds = targetPace,
                    onSave = {
                        viewModel.saveRunningRecord(
                            distanceMeters = distance,
                            elapsedTimeMs = time,
                            targetPaceSeconds = targetPace,
                            averagePaceSeconds = averagePace
                        )
                    },
                    onDiscard = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Routes.RECORD_DETAIL,
                arguments = listOf(
                    navArgument("recordId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                val allRecords by viewModel.allRecords.collectAsState()
                val record = allRecords.find { it.id == recordId }
                
                RecordDetailScreen(
                    record = record,
                    onBack = {
                        navController.popBackStack()
                    },
                    onDelete = { recordToDelete ->
                        viewModel.deleteRecord(recordToDelete)
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
