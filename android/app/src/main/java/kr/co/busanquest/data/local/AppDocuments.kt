package kr.co.busanquest.data.local

import android.content.Context

/**
 * 앱에 동봉된 약관·방침 문서(assets 폴더의 md 파일) 메타 정보.
 *
 * ⚠️ 이 주석 안에 슬래시+별표 조합을 쓰지 말 것 — Kotlin 은 블록 주석 중첩을 허용해서
 *    KDoc 안에서 새 주석이 열리고 파일 끝까지 주석으로 먹힌다.
 *
 * 동의 이력에 "몇 버전 문서에 동의했는지"를 남겨야 하므로,
 * 화면에 하드코딩하지 않고 문서 머리말의 version 값을 읽어서 쓴다.
 *
 *   # 이용약관
 *   version: 1.0        ← 이 값
 *   effective: 2026-08-11
 *   ---
 */
object AppDocuments {

    // 회원가입 시 필수 동의 문서 (표시 순서대로)
    val requiredSlugs = listOf(SLUG_TERMS, SLUG_PRIVACY, SLUG_LOCATION)

    fun title(slug: String): String = when (slug) {
        SLUG_TERMS -> "이용약관"
        SLUG_PRIVACY -> "개인정보 처리방침"
        SLUG_LOCATION -> "위치기반서비스 이용약관"
        else -> "약관"
    }

    /** assets/{slug}.md 의 version 값. 읽지 못하면 "-" */
    fun version(context: Context, slug: String): String = runCatching {
        context.assets.open("$slug.md").bufferedReader().use { reader ->
            reader.lineSequence()
                .take(10)   // 머리말만 훑는다
                .firstOrNull { it.startsWith("version:") }
                ?.removePrefix("version:")
                ?.trim()
        }
    }.getOrNull() ?: "-"

    const val SLUG_TERMS = "terms"
    const val SLUG_PRIVACY = "privacy"
    const val SLUG_LOCATION = "location"
}
