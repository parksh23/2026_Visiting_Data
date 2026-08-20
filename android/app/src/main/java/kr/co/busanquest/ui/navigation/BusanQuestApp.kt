package kr.co.busanquest.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kr.co.busanquest.data.local.TokenStore
import kr.co.busanquest.data.remote.SessionManager
import kr.co.busanquest.ui.auth.LoginScreen
import kr.co.busanquest.ui.home.HomeScreen
import kr.co.busanquest.ui.map.MapScreen
import kr.co.busanquest.ui.mission.MissionScreen
import kr.co.busanquest.ui.profile.ProfileScreen
import kr.co.busanquest.ui.ranking.RankingScreen
import kr.co.busanquest.ui.theme.BgSoftBlue
import kotlinx.coroutines.launch
import kr.co.busanquest.ui.detail.MissionDetailScreen
import kr.co.busanquest.ui.profile.MissionHistoryScreen
import kr.co.busanquest.ui.profile.SavedMissionScreen
import kr.co.busanquest.data.repository.UserRepository
import kr.co.busanquest.ui.profile.AccountSettingsScreen
import kr.co.busanquest.ui.profile.DocumentScreen
import kr.co.busanquest.ui.profile.NotificationSettingsScreen
import kr.co.busanquest.ui.profile.SupportScreen
import kr.co.busanquest.ui.ranking.DistrictRankingScreen

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
            // 로그인 화면과 "로그인 전 약관 열람" 상태에서는 탭바를 숨긴다.
            // (로그인 전에 탭바가 보이면 토큰 없이 메인 탭으로 들어갈 수 있다)
            val showBottomBar = currentRoute != null &&
                    currentRoute != "login" &&
                    status == AuthStatus.LoggedIn

            val startDestination =
                if (status == AuthStatus.LoggedIn) "home" else "login"

            // 401 세션 만료 이벤트 구독:
            // 어떤 API 든 401 이 발생하면 (토큰은 인터셉터가 이미 삭제함)
            // 백스택을 전부 비우고 로그인 화면으로 보낸다.
            LaunchedEffect(navController) {
                SessionManager.sessionExpired.collect {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            Scaffold(containerColor = BgSoftBlue) { padding ->
                Box(Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding()),
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) {

                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            // 약관 동의 항목의 '보기' → 문서 전문 화면 (로그인 전에도 열람 가능해야 한다)
                            onOpenDocument = { slug -> navController.navigate("doc/$slug") }
                        )
                    }

                    composable("home") { HomeScreen(navController) }

                    composable("mission") { MissionScreen(navController) }

                    composable(
                        route = "map/{region}?focus={focus}",
                        arguments = listOf(
                            navArgument("region") { type = NavType.StringType },
                            navArgument("focus") { type = NavType.BoolType; defaultValue = false }
                        )
                    ) {
                        val region = it.arguments?.getString("region") ?: ""
                        val focusSearch = it.arguments?.getBoolean("focus") ?: false
                        MapScreen(region, navController, focusSearch)
                    }

                    composable("ranking") { RankingScreen(navController) }

                    composable("profile") {
                        ProfileScreen(
                            navController = navController,
                            onLogout = {
                                UserRepository.clear()
                                scope.launch { tokenStore.clear() }
                                // popUpTo(0): 백스택을 통째로 비워 뒤로 가기로 로그인 이전 화면에
                                // 돌아갈 수 없게 한다 (규격서 10장)
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
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
                    composable("savedMission") {
                        SavedMissionScreen(navController = navController)
                    }

                    // ── 내 정보 > 설정 ──
                    composable("settings/notification") {
                        NotificationSettingsScreen(navController = navController)
                    }
                    composable("settings/account") {
                        AccountSettingsScreen(
                            navController = navController,
                            // 비밀번호 변경 · 로그아웃 · 회원 탈퇴가 모두 이 콜백으로 끝난다.
                            // 세 경우 모두 로컬 토큰 삭제가 반드시 동반돼야 한다 (규격서 8장).
                            onSessionEnd = {
                                UserRepository.clear()
                                scope.launch { tokenStore.clear() }
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable("support") {
                        SupportScreen(navController = navController)
                    }
                    // 이용약관 / 개인정보처리방침 / 위치기반서비스 이용약관 공용 뷰어
                    composable(
                        route = "doc/{slug}",
                        arguments = listOf(
                            navArgument("slug") { type = NavType.StringType }
                        )
                    ) {
                        val slug = it.arguments?.getString("slug") ?: "terms"
                        DocumentScreen(navController = navController, slug = slug)
                    }
                    composable(
                        route = "districtRanking/{districtName}",
                        arguments = listOf(
                            navArgument("districtName") { type = NavType.StringType }
                        )
                    ) {
                        val districtName = it.arguments?.getString("districtName") ?: ""
                        DistrictRankingScreen(navController = navController, districtName = districtName)
                    }
                }

                    if (showBottomBar) {
                        BottomNavigationBar(
                            navController = navController,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}
