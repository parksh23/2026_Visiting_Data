package com.example.busasnquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.busasnquest.ui.navigation.BusanQuestApp
import com.kakao.vectormap.KakaoMapSdk

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 카카오맵 초기화 (네이티브 앱 키)
        KakaoMapSdk.init(this, "5f26abd73b4e5c4273ed4ba4ea26aa7e")

        setContent {
            MaterialTheme {
                BusanQuestApp()
            }
        }
    }
}