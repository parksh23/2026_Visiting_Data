package com.example.busasnquest.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // ⚠️ 서버 주소. 팀원 서버가 준비되면 이 주소를 바꿔야 함!
    private const val BASE_URL = "http://10.0.2.2:8000/"

    // 통신 내용을 로그로 보여주는 도구
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    // 전화기 본체 조립
    val api: BusanQuestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())  // JSON 번역기 장착
            .build()
            .create(BusanQuestApi::class.java)
    }

    // 로그인용 API 추가
    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}