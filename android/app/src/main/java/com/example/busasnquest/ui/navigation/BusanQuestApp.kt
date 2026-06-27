package com.example.busasnquest.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.busasnquest.data.local.TokenStore
import com.example.busasnquest.ui.auth.LoginScreen
import com.example.busasnquest.ui.home.HomeScreen
import com.example.busasnquest.ui.map.MapScreen
import com.example.busasnquest.ui.mission.MissionScreen
import com.example.busasnquest.ui.profile.ProfileScreen
import com.example.busasnquest.ui.ranking.RankingScreen
import com.example.busasnquest.ui.theme.BgSoftBlue
import kotlinx.coroutines.launch
import com.example.busasnquest.ui.detail.MissionDetailScreen
import com.example.busasnquest.ui.profile.MissionHistoryScreen

// 앱 시작 시 로그인 여부
private enum class AuthStatus { Loading, LoggedIn, LoggedOut }

@Composable
fun BusanQuestApp() {

    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val scope = rememberCoroutineScope()

    // DataStore 에서 토큰을 읽어 로그인 여부 판단 (자동 로그인)
    val status by produceState(initialValue = AuthStatus.Loading, tokenStore) {
        tokenStore.tokenFlow.collect { token ->
            value = if (token.isNullOrBlank()) AuthStatus.LoggedOut else AuthStatus.LoggedIn
        }
    }

    when (status) {
        AuthStatus.Loading -> {
            // 토큰 읽는 짧은 순간 동안 로딩 표시
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            val navController = rememberNavController()
            val currentRoute = navController
                .currentBackStackEntryAsState().value?.destination?.route

            // 로그인 화면에서는 하단 탭바를 숨긴다
            val showBottomBar = currentRoute != null && currentRoute != "login"

            val startDestination =
                if (status == AuthStatus.LoggedIn) "home" else "login"

            Scaffold(
                containerColor = BgSoftBlue,
                bottomBar = {
                    if (showBottomBar) BottomNavigationBar(navController)
                }
            ) { padding ->

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(padding)
                ) {

                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

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

                    composable("profile") {
                        ProfileScreen(
                            navController = navController,
                            onLogout = {
                                scope.launch { tokenStore.clear() }
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = "missionDetail/{missionId}",
                        arguments = listOf(
                            navArgument("missionId") { type = NavType.IntType }
                        )
                    ) {
                        val missionId = it.arguments?.getInt("missionId") ?: 0
                        MissionDetailScreen(navController = navController, missionId = missionId)
                    }
                    composable("missionHistory") {
                        MissionHistoryScreen(navController = navController)
                    }
                }
            }
        }
    }
}
