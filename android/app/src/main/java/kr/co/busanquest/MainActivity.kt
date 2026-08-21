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

        // 카카오맵 초기화 (네이티브 앱 키)
        KakaoMapSdk.init(this, "5f26abd73b4e5c4273ed4ba4ea26aa7e")

        // 카카오 로그인 초기화 (동일한 네이티브 앱 키)
        KakaoSdk.init(this, "5f26abd73b4e5c4273ed4ba4ea26aa7e")

        // 알림을 눌러서 열린 것이라면 이동할 화면을 기억해 둔다.
        // 화면(NavHost)은 아직 만들어지기 전이라 바로 이동할 수 없다 — BusanQuestApp 이 꺼내 쓴다.
        kr.co.busanquest.util.NotificationRoute.capture(intent)

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
     * 앱이 이미 떠 있는 상태에서 알림을 누른 경우.
     *
     * launchMode="singleTop" 이라 액티비티가 새로 만들어지지 않고 여기로 들어온다.
     * setIntent 를 함께 부르지 않으면 getIntent() 가 계속 예전 인텐트를 돌려준다.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        kr.co.busanquest.util.NotificationRoute.capture(intent)
    }
}
