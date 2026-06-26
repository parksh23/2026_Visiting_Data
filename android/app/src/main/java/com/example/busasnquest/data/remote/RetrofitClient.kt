package com.example.busasnquest.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // TODO: 실제 서버 주소로 교체하세요. 반드시 끝에 "/" 가 있어야 합니다.
    private const val BASE_URL = "https://example.com/api/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val rankingApi: RankingApi = retrofit.create(RankingApi::class.java)
}
