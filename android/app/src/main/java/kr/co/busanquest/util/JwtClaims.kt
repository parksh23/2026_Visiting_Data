package kr.co.busanquest.util

import android.util.Base64
import org.json.JSONObject

/**
 * JWT 페이로드에서 클레임 하나를 꺼낸다.
 *
 * ⚠️ **서명을 검증하지 않는다.** 페이로드는 Base64 로 인코딩만 되어 있을 뿐 암호화가 아니어서,
 *    기기에서 얼마든지 위조할 수 있다.
 *
 *    그래서 이 값은 **화면 표시 용도로만** 써야 한다 (예: 랭킹에서 내 행 강조).
 *    "이 사람이 누구인가"를 근거로 무언가를 허용/차단하는 판단에는 절대 쓰지 않는다.
 *    그 판단은 서버가 같은 토큰을 서명 검증한 뒤에 내린다.
 *
 * 서버는 sub 클레임에 USER_CODE 를 담는다(auth_utils.create_access_token).
 * 랭킹 응답의 userId 와 같은 값이라 그대로 비교하면 된다.
 */
fun jwtClaim(token: String?, name: String): String? {
    if (token.isNullOrBlank()) return null
    val parts = token.split(".")
    if (parts.size < 2) return null

    return runCatching {
        val payload = Base64.decode(
            parts[1],
            // JWT 는 URL-safe 알파벳을 쓰고 padding 을 붙이지 않는다
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        JSONObject(String(payload, Charsets.UTF_8))
            .optString(name)
            .takeIf { it.isNotBlank() }
    }.getOrNull()
}
