"""프론트 연동 화면 검증용 시드 데이터.

실행:
    cd backend/app
    python seed_data.py
"""

from auth_utils import hash_password
from database import Base, SessionLocal, engine
from models import AppUser, District, Mission, UserMission
from routers.api_v1 import BUSAN_DISTRICTS


MISSION_SEEDS = [
    (1, "국제시장 로컬 맛집 방문", "중구", "중구 신창동", 35.1011, 129.0304, "RECEIPT", 150),
    (2, "용두산공원 정상 인증", "중구", "중구 용두산길", 35.1007, 129.0326, "PHOTO", 120),
    (3, "오륙도 해안길 걷기", "남구", "남구 용호동", 35.1015, 129.1237, "CURRENT_LOCATION", 100),
    (4, "광안리 야경 인증", "수영구", "수영구 광안동", 35.1532, 129.1187, "PHOTO", 120),
    (5, "민락회타운 영수증", "수영구", "수영구 민락동", 35.1555, 129.1311, "RECEIPT", 150),
    (6, "해운대 해변 도착", "해운대구", "해운대구 우동", 35.1587, 129.1604, "CURRENT_LOCATION", 100),
    (7, "달맞이길 사진 인증", "해운대구", "해운대구 중동", 35.1584, 129.1810, "PHOTO", 120),
    (8, "전포 카페거리 방문", "부산진구", "부산진구 전포동", 35.1551, 129.0631, "RECEIPT", 150),
    (9, "시민공원 산책", "부산진구", "부산진구 연지동", 35.1664, 129.0571, "CURRENT_LOCATION", 100),
    (10, "감천문화마을 인증", "사하구", "사하구 감천동", 35.0975, 129.0106, "PHOTO", 120),
    (11, "다대포 일몰 도착", "사하구", "사하구 다대동", 35.0467, 128.9650, "CURRENT_LOCATION", 100),
    (12, "흰여울문화마을 인증", "영도구", "영도구 영선동", 35.0788, 129.0443, "PHOTO", 120),
    (13, "태종대 전망대 도착", "영도구", "영도구 동삼동", 35.0532, 129.0871, "CURRENT_LOCATION", 100),
    (14, "동래시장 한 끼", "동래구", "동래구 복천동", 35.2050, 129.0838, "RECEIPT", 150),
    (15, "금정산성 도착", "금정구", "금정구 금성동", 35.2505, 129.0556, "CURRENT_LOCATION", 100),
    (16, "온천천 산책 인증", "연제구", "연제구 연산동", 35.1911, 129.0822, "PHOTO", 120),
    (17, "송도해수욕장 인증", "서구", "서구 암남동", 35.0759, 129.0178, "PHOTO", 120),
    (18, "화명생태공원 도착", "북구", "북구 금곡동", 35.2304, 129.0086, "CURRENT_LOCATION", 100),
    (19, "기장시장 영수증", "기장군", "기장군 기장읍", 35.2446, 129.2156, "RECEIPT", 150),
    (20, "대저생태공원 인증", "강서구", "강서구 대저동", 35.2124, 128.9838, "PHOTO", 120),
]


def seed() -> None:
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        for district_name in BUSAN_DISTRICTS:
            if db.get(District, district_name) is None:
                db.add(District(name=district_name))

        for index in range(1, 13):
            code = f"U{index:03d}"
            user = db.get(AppUser, code)
            if user is None:
                user = AppUser(
                    user_code=code,
                    login_id=f"test{index}@busan.quest",
                    email=f"test{index}@busan.quest",
                    password_hash=hash_password("testpass123"),
                    nickname=f"부산탐험가{index}",
                    district_name=BUSAN_DISTRICTS[(index - 1) % len(BUSAN_DISTRICTS)],
                    total_points=(13 - index) * 320,
                    account_status="ACTIVE",
                )
                db.add(user)

        for row in MISSION_SEEDS:
            mission_id, title, district, location, lat, lng, kind, reward = row
            if db.get(Mission, mission_id) is None:
                db.add(
                    Mission(
                        mission_id=mission_id,
                        title=title,
                        district_name=district,
                        location=location,
                        latitude=lat,
                        longitude=lng,
                        radius_m=300,
                        mission_type=kind,
                        reward_points=reward,
                        image_url=None if mission_id == 1 else f"https://picsum.photos/seed/busan{mission_id}/1280/720",
                    )
                )
        db.commit()

        # U006 기준: 중구 100%, 수영구 50%, 해운대구 50%, 부산진구 50% 등
        for mission_id in [1, 2, 4, 6, 8, 10, 12]:
            exists = (
                db.query(UserMission)
                .filter_by(user_code="U006", mission_id=mission_id)
                .first()
            )
            if not exists:
                db.add(
                    UserMission(
                        user_code="U006",
                        mission_id=mission_id,
                        status="completed",
                    )
                )
        user = db.get(AppUser, "U006")
        user.completed_missions = 7
        db.commit()
    finally:
        db.close()


if __name__ == "__main__":
    seed()
    print("시드 데이터 생성 완료")
