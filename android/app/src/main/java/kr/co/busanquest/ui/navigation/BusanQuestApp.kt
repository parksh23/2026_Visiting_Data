package kr.co.busanquest.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kr.co.busanquest.data.local.SettingsStore
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
import androidx.compose.runtime.collectAsState
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
import kr.co.busanquest.data.repository.NotificationSettingsRepository
import kr.co.busanquest.data.repository.UserRepository
import kr.co.busanquest.util.NotificationRoute
import kr.co.busanquest.util.PushRegistrar
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

    // 로그인 상태가 되면(직접 로그인 · 자동 로그인 모두) 서버와 알림 관련 상태를 맞춘다.
    //   1) 내 USER_CODE 확보 — 랭킹에서 내 행을 찾는 기준이라 화면보다 먼저 있어야 한다
    //   2) FCM 토큰 등록 — 서버는 PUSH_TOKENS 에 있는 토큰으로만 푸시를 보낼 수 있다
    //   3) 알림 설정 내려받기 — 서버가 원본이라 다른 기기에서 바꾼 설정도 여기 반영된다
    // 전부 실패해도 앱 흐름을 막지 않는다.
    // Android 13+ 알림 권한 요청창.
    // 허용/거부 어느 쪽이든 앱 흐름은 그대로 간다 — 거부해도 앱은 정상 동작하고
    // 나중에 내 정보 > 알림 설정에서 다시 켤 수 있다.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 결과를 따로 처리하지 않는다 */ }

    LaunchedEffect(status) {
        if (status == AuthStatus.LoggedIn) {
            // 0) 알림 권한 — Android 13+ 는 이 권한이 없으면 서버 푸시가 와도 화면에 안 뜬다.
            //    로그인 직후 딱 한 번만 물어본다. 매번 띄우면 성가시고,
            //    두 번 거절당하면 시스템이 영구 차단해 버려 되돌리기가 더 어려워진다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val settings = SettingsStore(context)
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted && !settings.pushPermissionAsked()) {
                    settings.setPushPermissionAsked()
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            UserRepository.refreshUserCode(context)
            PushRegistrar.register()
            NotificationSettingsRepository.refresh(context)
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

            // 알림을 눌러서 들어왔다면 해당 화면으로 보낸다.
            //   · 앱이 떠 있을 때  → Notifier 의 PendingIntent 에 실린 화면 이름
            //   · 앱이 꺼져 있을 때 → 시스템이 전달한 푸시 data["type"] 을 변환한 값
            // 로그인 전이면 소비하지 않고 남겨 둔다 → 로그인 직후 그 화면으로 이어진다.
            val pendingRoute by NotificationRoute.pending.collectAsState()
            LaunchedEffect(pendingRoute, status) {
                val route = pendingRoute ?: return@LaunchedEffect
                if (status != AuthStatus.LoggedIn) return@LaunchedEffect
                navController.navigate(route) {
                    // 탭 이동과 같은 규칙 — 홈 위에 한 장만 쌓아 뒤로가기가 홈으로 가게 한다
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
                NotificationRoute.consume()
            }

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
