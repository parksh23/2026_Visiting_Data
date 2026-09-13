# 기기 위치 판정과 부정 인증 방지

현재 위치와 사진 EXIF GPS를 기기 메모리에서만 비교한다. 서버에는 사용자 좌표·정확도·거리를 보내지 않는다. 기존 계정별 장소 미션 완료 기록, 시각, 사진 심사, 포인트·랭킹은 유지한다. 따라서 이 변경만으로 위치기반서비스사업 신고 제외를 확정할 수 없다. 장소와 계정을 연결한 완료 기록 및 사진 내용에 대한 별도 검토가 필요하다.

## 인증 흐름

1. 로그인한 사용자가 진행 중인 PHOTO/CURRENT_LOCATION 미션의 `POST /api/v1/missions/{id}/location-challenge`를 요청한다.
2. 서버는 계정당 하나의 난수 256비트 challenge를 발급한다. 유효기간 5분, 재발급 간격 15초. 미션의 공개 좌표, 반경(기본 300m), 정확도 한계(기본 100m), Play Cloud 프로젝트 번호를 내려준다. 새 요청은 이전 challenge를 대체한다.
3. 앱이 Play Integrity provider를 준비한 다음 위치를 두 번 측정한다. 측정 간격은 최소 2초이며 두 번째 측정 시각이 첫 번째보다 커야 한다. 캐시 위치는 요청하지 않는다. 위치마다 모의 위치 여부, 유효한 좌표, 정확도, 단조 시계 기준 10초 이내 최신성을 검사한다. `장소까지 거리 + 위치 오차 <= 허용 반경`이어야 한다.
4. 사진 미션은 EXIF 위치도 기기에서 장소와 비교한다. 사진은 원본 메타데이터를 복사하지 않고 픽셀에서 JPEG로 재생성한 후 업로드한다. 서버 업로드에서도 메타데이터를 제거해 저장한다. 사진 내용 자체와 기존 AI 심사는 서버에서 처리된다.
5. 앱은 아래 요청 항목의 SHA-256 해시를 Play Integrity의 `requestHash`로 전달하고, 토큰과 기기 판정 결과를 서버로 보낸다. 위치 좌표의 해시도 보내지 않는다.
6. 서버는 계정·미션·유효기간·현재 정책과 challenge를 대조한다. DB 조건부 갱신으로 먼저 사용 처리하고 커밋한 후 Google에서 토큰을 검증한다. 실패한 요청도 재사용할 수 없다.
7. 패키지, 요청 해시, 시각(발급 이후 및 최근 120초, 5초 시계 여유), 인증서, 최소 앱 버전, `PLAY_RECOGNIZED`, `MEETS_DEVICE_INTEGRITY`, `LICENSED`를 모두 검사한다. Google 오류·설정 누락·판정 누락은 거절한다. 토큰이나 Google 응답 원문은 저장하지 않는다.
8. 진행 중 상태를 완료로 바꾸는 조건부 UPDATE에 성공한 요청만 같은 트랜잭션에서 포인트와 완료 수를 증가시킨다. 동시 인증·취소·계정 탈퇴 시 중복 지급을 막는다.

영수증 미션은 기존 서버 이미지 심사를 유지한다. 위치 인증 challenge는 사용하지 않는다. 공통 보상 중복 방지와 JPEG 메타데이터 제거는 적용된다.

## 요청 계약

`POST /api/v1/missions/verify`:

```json
{
  "mission_id": 1,
  "mission_type": "CURRENT_LOCATION",
  "protocol_version": 1,
  "challenge_id": "64자리 소문자 16진수",
  "local_passed": true,
  "integrity_token": "Google Play Integrity token"
}
```

PHOTO는 `photo_url`을 추가한다. 영수증은 `receipt_image_url`을 쓴다. `image`, `latitude`, `longitude`, `accuracy_m`, `distance_m` 등 계약 외 필드는 거절한다. 기존 앱의 좌표 요청을 받는 호환 우회 경로는 없다.

해시 입력은 다음 7개 문자열을 LF 한 개로 연결한 UTF-8 바이트다. 마지막 항목 뒤 구분자를 추가하지 않는다. null URL은 빈 문자열이다. SHA-256 결과를 URL-safe Base64(패딩 없음)로 인코딩한다.

1. protocol_version의 십진수 문자열
2. challenge_id
3. mission_id의 십진수 문자열
4. mission_type
5. local_passed의 `true` 또는 `false`
6. photo_url 또는 빈 문자열
7. receipt_image_url 또는 빈 문자열

## 운영 연결 및 배포

1. Play Console에서 `kr.co.busanquest` 앱에 Google Cloud 프로젝트를 연결하고 해당 프로젝트의 Play Integrity API를 활성화한다.
2. 서버에 `PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER`와 `PLAY_INTEGRITY_CERTIFICATE_DIGESTS`를 설정한다. 인증서는 **Play 앱 서명 인증서**의 SHA-256을 URL-safe Base64로 바꾼 값이며 업로드 키 인증서나 콜론 구분 HEX 값이 아니다. 키 교체 시 쉼표로 여러 인증서를 허용할 수 있다.
3. 해당 프로젝트에서 토큰 해석을 할 수 있는 서비스 계정을 서버의 Application Default Credentials로 제공한다. 예: Secret File을 마운트하고 `GOOGLE_APPLICATION_CREDENTIALS`에 경로를 지정한다. APK, 저장소, 환경 예제에는 비밀 키를 넣지 않는다. Firebase용 자격증명이 자동으로 같은 권한을 가진다고 가정하지 않는다.
4. `PLAY_INTEGRITY_MIN_VERSION_CODE=6` 이상을 설정한다. 기기 판정이 구현된 앱은 versionCode 6 / versionName 1.0.5다. 6 미만은 서버 설정 자체가 거절된다.
5. `backend/app/requirements.txt`의 의존성을 설치하고 Oracle에 `migrations/013_location_challenges.sql`을 적용한다. 이미 앱 초기화가 해당 테이블을 생성했다면 다시 CREATE하지 않는다. 운영 DB에는 이번 로컬 작업에서 접속하거나 마이그레이션하지 않았다.
6. Play 내부 테스트 트랙에서 앱을 설치해 실제 Google 응답과 GPS로 확인한 뒤 서버·앱을 함께 배포한다. 새 서버에 기존 앱을 연결하면 위치 인증이 거절되므로 업데이트 안내/전환 시점을 조율한다. 미설정 상태에서도 목록 등은 열리지만 위치·사진 인증은 503으로 거절된다. 디버그 APK, 직접 설치한 미인식 버전, 무결성 기준을 만족하지 않는 에뮬레이터는 통과하지 않는다.
7. 개인정보 안내 변경과 재동의 필요 여부, 신고 해당 여부를 확인하고 실제 시행일을 조정한다. 앱 assets와 docs는 동일한 변경 초안을 포함한다. 기존 서버 저장 사진·로그·백업에 남아 있을 수 있는 위치정보는 자동 삭제하지 않았으며 별도 보존 근거와 정리 범위를 확인해야 한다.

만료된 challenge는 서버 스케줄러가 10분마다 삭제한다(프로세스 중단 시 다음 실행까지 지연). 계정 탈퇴 시 즉시 삭제한다. 서버는 계정별 미션 완료 기록을 계속 보유한다.

## 검증과 한계

- 자동 테스트: 정상 인증, 모의 위치, 오래된/미래 위치, 정확도 및 반경 경계, Android/Python 해시 일치, 좌표 필드 거절, 다른 계정·미션·요청 해시, 만료·재사용·과도한 발급, 인증서/앱 버전/기기/라이선스 오류, Google 장애, 취소 경합, 중복 보상, EXIF 제거.
- 실제 연결 확인: Play 내부 테스트 설치본의 정상 GPS, 모의 위치 앱, 권한 철회, 네트워크 단절, Google 설정 오류, 인증 도중 화면 종료, 사진 GPS 유무, API 트래픽에 좌표가 없는지 확인한다. 실제 기기·Google 운영 계정 검증은 별도로 필요하다.
- Play Integrity는 앱·기기의 신뢰 신호이며 GPS가 사실이라는 암호학적 증명이 아니다. 고급 루팅/후킹 우회, 외부 GNSS 신호 조작, 현장 기기 중계, 조작한 사진 EXIF를 완전히 막지는 못한다. 좌표 없는 방식으로 이동 속도/동선의 서버 교차 검증도 할 수 없다.
- 금전성 보상을 강하게 보호해야 한다면 현장 단말의 회전 QR/NFC 확인 등 별도 방문 증거가 필요하다. 그 방식도 장소·사용자 기록에 대한 법적 검토는 별도다.

공식 자료: [Play Integrity 표준 요청](https://developer.android.com/google/play/integrity/standard), [검증 결과](https://developer.android.com/google/play/integrity/verdicts), [설정](https://developer.android.com/google/play/integrity/setup), [위치정보지원센터 신고 제외 안내](https://www.lbsc.kr/front/content/contentViewer.do?contentId=CONTENT_0000081).
