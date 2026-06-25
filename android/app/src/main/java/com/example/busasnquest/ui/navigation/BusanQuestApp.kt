package com.example.busasnquest.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.busasnquest.ui.home.HomeScreen
import com.example.busasnquest.ui.map.MapScreen
import com.example.busasnquest.ui.mission.MissionScreen
import com.example.busasnquest.ui.profile.ProfileScreen
import com.example.busasnquest.ui.ranking.RankingScreen
import com.example.busasnquest.ui.theme.BgSoftBlue

@Composable
fun BusanQuestApp() {

    val navController = rememberNavController()

    Scaffold(
        containerColor = BgSoftBlue,
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {

            composable("home") { HomeScreen(navController) }

            composable("mission") { MissionScreen(navController) }

            composable(
                route = "map/{region}",
                arguments = listOf(
                    navArgument("region") { type = NavType.StringType }
                )
            ) {
                val region = it.arguments?.getString("region") ?: ""
                MapScreen(region)
            }

            composable("ranking") { RankingScreen() }

            composable("profile") { ProfileScreen() }
        }
    }
}
