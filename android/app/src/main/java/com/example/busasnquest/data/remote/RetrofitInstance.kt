package com.example.busasnquest.data.remote

import android.content.Context
import com.example.busasnquest.data.local.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

object RetrofitInstance {

    // 에뮬레이터에서 PC localhost로 접근하는 주소
    private const val BASE_URL = "https://visiting-data.onrender.com/"

    // 앱 Context 저장용
    // TokenStore를 만들 때 필요함
    private lateinit var appContext: Context

    // RetrofitInstance를 사용하기 전에 한 번 호출해야 함
    // BusanQuestApp에서 앱 시작 시 호출할 예정
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // HTTP 요청/응답 내용을 Logcat에 보여주는 로거
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient
    // 모든 API 요청 전에 저장된 token을 읽어서 Authorization 헤더에 붙임
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()

            // JWT 자동 첨부 인터셉터
            .addInterceptor { chain ->
                val originalRequest = chain.request()

                // DataStore에 저장된 토큰 읽기
                // Interceptor는 suspend 함수가 아니기 때문에 runBlocking 사용
                val token = runBlocking {
                    TokenStore(appContext).tokenFlow.first()
                }

                Log.d(
                    "AUTH_INTERCEPTOR",
                    "url=${originalRequest.url}, tokenEmpty=${token.isNullOrBlank()}"
                )

                // 토큰이 있으면 Authorization: Bearer <token> 헤더 추가
                val newRequest = if (!token.isNullOrBlank()) {
                    originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }

                chain.proceed(newRequest)
            }

            // 통신 로그 확인용
            .addInterceptor(logger)
            .build()
    }

    // Retrofit 객체
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 랭킹 API
    // 프로필/미션 API와 같은 Retrofit 객체를 사용하므로
    // Authorization 헤더 인터셉터가 똑같이 적용됨
    val api: BusanQuestApi by lazy {
        retrofit.create(BusanQuestApi::class.java)
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val rankingApi: RankingApi by lazy {
        retrofit.create(RankingApi::class.java)
    }
}