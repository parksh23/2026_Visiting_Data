package kr.co.busanquest

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import kr.co.busanquest.ui.navigation.BusanQuestApp
import kr.co.busanquest.ui.theme.AppShapes
import kr.co.busanquest.ui.theme.AppTypography
import kr.co.busanquest.ui.theme.BaeminBaseTextStyle
import kr.co.busanquest.ui.theme.Coral
import kr.co.busanquest.data.remote.RetrofitInstance
import kr.co.busanquest.util.PushNavigation
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 라이트 단일 테마: 상태바/내비바 아이콘을 어두운 색으로 고정
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )

        // RetrofitInstance가 DataStore에서 토큰을 읽을 수 있게 초기화
        RetrofitInstance.init(this)

        // 알림 채널 생성 + 앱 Context 보관 (알림 설정 스위치는 Notifier 내부에서 확인)
        kr.co.busanquest.util.Notifier.init(this)

        // FCM 토큰 등록/해제에 앱 Context 가 필요하다 (등록 시점은 BusanQuestApp 이 잡는다)
        kr.co.busanquest.util.PushRegistrar.init(this)

        // 알림을 눌러 앱이 켜진 경우 목적지를 보관해 둔다 (실제 화면 이동은 BusanQuestApp 이 한다)
        handlePushIntent(intent)

        // 카카오맵 초기화 (네이티브 앱 키)
        KakaoMapSdk.init(this, "5f26abd73b4e5c4273ed4ba4ea26aa7e")

        // 카카오 로그인 초기화 (동일한 네이티브 앱 키)
        KakaoSdk.init(this, "5f26abd73b4e5c4273ed4ba4ea26aa7e")

        setContent {
            // 배민식 타이포/형태 토큰을 앱 전체에 적용 (색상은 기존 유지)
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Coral,
                    onPrimary = Color.White,
                    background = kr.co.busanquest.ui.theme.BgSoftBlue,
                    surface = kr.co.busanquest.ui.theme.CardWhite,
                    onBackground = kr.co.busanquest.ui.theme.TextMain,
                    onSurface = kr.co.busanquest.ui.theme.TextMain,
                    outline = kr.co.busanquest.ui.theme.InkBorder,
                    error = kr.co.busanquest.ui.theme.PointRed
                ),
                typography = AppTypography,
                shapes = AppShapes
            ) {
                // 폰트·자간을 전역 상속 → 모든 탭의 Text 에 공통 적용
                CompositionLocalProvider(LocalTextStyle provides BaeminBaseTextStyle) {
                    BusanQuestApp()
                }
            }
        }
    }

    /**
     * 앱이 이미 떠 있는 상태에서 알림을 누른 경우 여기로 들어온다.
     * (매니페스트의 launchMode="singleTop" 덕분에 Activity 가 새로 만들어지지 않는다)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    /**
     * 알림에서 들어온 Intent 인지 보고, 열어야 할 화면을 PushNavigation 에 넘긴다.
     *
     *   · 앱이 화면에 떠 있을 때 Notifier 가 띄운 알림
     *       → Notifier 가 심어 둔 EXTRA_ROUTE 가 들어 있다.
     *   · 앱이 백그라운드/종료 상태라 시스템이 대신 띄운 FCM 알림
     *       → Notifier 를 거치지 않아 EXTRA_ROUTE 가 없다. 대신 서버가 보낸 data
     *         페이로드가 그대로 extras 에 실려 오므로 "type" 을 보고 판단한다.
     *         (서버는 이미 type 을 보내고 있어 백엔드는 손댈 것이 없다)
     */
    private fun handlePushIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val route = extras.getString(PushNavigation.EXTRA_ROUTE)
            ?: PushNavigation.routeForType(extras.getString("type"))

        // 한 번 읽은 목적지는 Intent 에서 지운다.
        // 화면 회전 등으로 Activity 가 다시 만들어질 때 같은 Intent 가 그대로 들어오는데,
        // 지우지 않으면 그때마다 다시 이동한다.
        intent.removeExtra(PushNavigation.EXTRA_ROUTE)
        intent.removeExtra("type")

        PushNavigation.request(route)
    }
}