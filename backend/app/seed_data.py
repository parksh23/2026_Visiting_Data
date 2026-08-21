"""프론트 연동 화면 검증용 시드 데이터.

실행:
    cd backend/app
    python seed_data.py
"""

import logging
import math
import os

import httpx

from auth_utils import hash_password
from database import Base, SessionLocal, engine
from document_seed import seed_documents
from models import AppUser, District, Friendship, Mission, UserMission
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

logger = logging.getLogger(__name__)
TOURISM_IMAGE_API_URL = (
    "https://apis.data.go.kr/B551011/KorService2/areaBasedList2"
)


def _distance_m(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    radius = 6_371_000
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lng2 - lng1)
    value = (
        math.sin(d_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    )
    return radius * 2 * math.atan2(math.sqrt(value), math.sqrt(1 - value))


def fetch_tourism_images() -> dict[int, str]:
    """TourAPI 부산 관광정보 중 각 미션 좌표와 가장 가까운 대표 이미지를 찾는다."""
    service_key = os.getenv("TOURISM_DATA_SERVICE_KEY")
    if not service_key:
        logger.warning("TOURISM_DATA_SERVICE_KEY가 없어 미션 대표 이미지를 비웁니다.")
        return {}
    try:
        response = httpx.get(
            TOURISM_IMAGE_API_URL,
            params={
                "serviceKey": service_key,
                "MobileOS": "ETC",
                "MobileApp": "BusanQuest",
                "_type": "json",
                "areaCode": "6",
                "numOfRows": 1000,
                "pageNo": 1,
                "arrange": "C",
            },
            timeout=30.0,
        )
        response.raise_for_status()
        items = (
            response.json()
            .get("response", {})
            .get("body", {})
            .get("items", {})
            .get("item", [])
        )
        if isinstance(items, dict):
            items = [items]
    except Exception:
        logger.exception("TourAPI 미션 대표 이미지 조회에 실패했습니다.")
        return {}

    candidates = []
    for item in items if isinstance(items, list) else []:
        image_url = item.get("firstimage") or item.get("firstimage2")
        try:
            candidates.append(
                (float(item["mapy"]), float(item["mapx"]), image_url)
            )
        except (KeyError, TypeError, ValueError):
            continue
        if not image_url:
            candidates.pop()

    result = {}
    for mission_id, _, _, _, lat, lng, _, _ in MISSION_SEEDS:
        if not candidates:
            break
        distance, image_url = min(
            (_distance_m(lat, lng, item_lat, item_lng), url)
            for item_lat, item_lng, url in candidates
        )
        # 멀리 떨어진 관광지 사진을 억지로 붙이지 않는다.
        if distance <= 5_000:
            result[mission_id] = image_url
    return result


def seed() -> None:
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        seed_documents(db)
        mission_images = fetch_tourism_images()
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
            mission = db.get(Mission, mission_id)
            if mission is None:
                mission = Mission(mission_id=mission_id)
                db.add(mission)
            mission.title = title
            mission.district_name = district
            mission.location = location
            mission.latitude = lat
            mission.longitude = lng
            mission.radius_m = 300
            mission.mission_type = kind
            mission.reward_points = reward
            mission.image_url = mission_images.get(mission_id)

        for friend_code in ["U001", "U002", "U003"]:
            exists = (
                db.query(Friendship)
                .filter_by(user_code="U006", friend_user_code=friend_code)
                .first()
            )
            if not exists:
                db.add(
                    Friendship(
                        user_code="U006",
                        friend_user_code=friend_code,
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
