package kr.co.busanquest.data.model

// 미션을 어떻게 완료하는지(검증 방식)
enum class MissionType {
    IMAGE_LOCATION,   // 사진 위치 인증 (사진의 GPS로 판정)
    CURRENT_LOCATION, // 현재 위치 인증 (지금 내 위치로 판정)
    RECEIPT           // 결제 영수증 인증
}

/**
 * 인증 제출 시 서버에 보내는 mission_type 문자열.
 *
 * 백엔드 계약상 정규 값은 이 셋뿐이다.
 *   PHOTO · CURRENT_LOCATION · RECEIPT
 *
 * 예전에 보내던 "IMAGE" / "IMAGE_LOCATION" 은 더 이상 나가지 않는다.
 * (읽는 방향은 toMissionTypeOrNull 이 옛 표기까지 계속 받아준다)
 */
fun MissionType.toServerType(): String = when (this) {
    MissionType.IMAGE_LOCATION -> "PHOTO"
    MissionType.CURRENT_LOCATION -> "CURRENT_LOCATION"
    MissionType.RECEIPT -> "RECEIPT"
}

/**
 * 서버가 내려준 mission_type 문자열을 앱 타입으로 읽는다.
 *
 * 읽기는 과거 표기까지 계속 받아준다 — 서버 DB 에 옛 값이 남아 있어도
 * 미션 목록이 깨지면 안 되기 때문이다. 되돌려 보낼 때만 정규 값을 쓴다.
 * 모르는 값이면 null 을 돌려주고, 기본값은 호출부가 정한다.
 */
fun String.toMissionTypeOrNull(): MissionType? = when (uppercase().trim()) {
    "PHOTO", "PHOTO_LOCATION", "IMAGE", "IMAGE_LOCATION" -> MissionType.IMAGE_LOCATION
    "CURRENT_LOCATION" -> MissionType.CURRENT_LOCATION
    "RECEIPT" -> MissionType.RECEIPT
    else -> null
}
