package com.kreedaankana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.kreedaankana.navigation.Screen
import com.kreedaankana.viewmodel.MainViewModel

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(rootNavController: NavController, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem("Calendar",   Icons.Default.CalendarMonth,  Screen.Calendar.route),
        BottomNavItem("Challenges", Icons.Default.EmojiEvents,    Screen.ChallengeBoard.route),
        BottomNavItem("Scores",     Icons.Default.Scoreboard,     Screen.ScoreWall.route),
        BottomNavItem("Rankings",   Icons.Default.Leaderboard,    Screen.Leaderboard.route),
        BottomNavItem("Profile",    Icons.Default.Person,         Screen.Profile.route),
    )
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick  = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Screen.Calendar.route) {
                composable(Screen.Calendar.route)       { CalendarScreen(navController, viewModel) }
                composable(Screen.BookSlot.route)       { BookSlotScreen(navController, viewModel) }
                composable(Screen.ChallengeBoard.route) { ChallengeBoardScreen(navController, viewModel) }
                composable(Screen.PostChallenge.route)  { PostChallengeScreen(navController, viewModel) }
                composable(Screen.ScoreWall.route)      { ScoreWallScreen(navController, viewModel) }
                composable(Screen.PostResult.route)     { PostResultScreen(navController, viewModel) }
                composable(Screen.Leaderboard.route)    { LeaderboardScreen(viewModel) }
                composable(Screen.Profile.route)        { ProfileScreen(viewModel, navController) }
            }
        }
    }
}
