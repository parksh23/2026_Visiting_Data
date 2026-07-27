package com.example.busasnquest

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.busasnquest.ui.navigation.BusanQuestApp
import com.example.busasnquest.ui.theme.AppShapes
import com.example.busasnquest.ui.theme.AppTypography
import com.example.busasnquest.ui.theme.BaeminBaseTextStyle
import com.example.busasnquest.ui.theme.Coral
import com.example.busasnquest.data.remote.RetrofitInstance
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 다크 단일 테마: 상태바/내비바 아이콘을 밝은 색으로 고정
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        // RetrofitInstance가 DataStore에서 토큰을 읽을 수 있게 초기화
        RetrofitInstance.init(this)

        // 카카오맵 초기화 (네이티브 앱 키)
        KakaoMapSdk.init(this, "5f26abd73b4e5c4273ed4ba4ea26aa7e")

        // 카카오 로그인 초기화 (동일한 네이티브 앱 키)
        KakaoSdk.init(this, "5f26abd73b4e5c4273ed4ba4ea26aa7e")

        setContent {
            // 배민식 타이포/형태 토큰을 앱 전체에 적용 (색상은 기존 유지)
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Coral,
                    onPrimary = Color.White,
                    background = com.example.busasnquest.ui.theme.BgSoftBlue,
                    surface = com.example.busasnquest.ui.theme.CardWhite,
                    onBackground = com.example.busasnquest.ui.theme.TextMain,
                    onSurface = com.example.busasnquest.ui.theme.TextMain
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
}