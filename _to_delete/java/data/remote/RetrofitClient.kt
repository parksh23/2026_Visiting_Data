package com.example.busasnquest.data.remote

/**
 * ⚠️ 더 이상 사용하지 않는 객체 (BASE_URL 이원화 해소).
 *
 * 예전에는 랭킹 API 만 placeholder 주소(https://example.com/api/)의 별도 Retrofit 을 썼지만,
 * 지금은 모든 API 가 RetrofitInstance(단일 BASE_URL + JWT/401 인터셉터)를 사용한다.
 * 혹시 남아있는 참조가 있어도 동작하도록 RetrofitInstance 로 위임만 한다.
 */
@Deprecated(
    message = "RetrofitInstance.rankingApi 를 사용하세요",
    replaceWith = ReplaceWith("RetrofitInstance.rankingApi")
)
object RetrofitClient {
    val rankingApi: RankingApi
        get() = RetrofitInstance.rankingApi
}
