import csv
import logging
import math
import os
from calendar import monthrange
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Callable

import httpx
from sqlalchemy.orm import Session

from models import Mission

logger = logging.getLogger(__name__)

TOURISM_API_BASE_URL = "https://apis.data.go.kr/B551011/AreaTarDemDsService"
STAY_API_URL = f"{TOURISM_API_BASE_URL}/areaTarSjrnDsList"
CONSUMPTION_API_URL = f"{TOURISM_API_BASE_URL}/areaTarExpDsList"
BUSAN_AREA_CODE = "26"
BUSAN_DISTRICT_CODES = {
    "중구": "26110", "서구": "26140", "동구": "26170", "영도구": "26200",
    "부산진구": "26230", "동래구": "26260", "남구": "26290", "북구": "26320",
    "해운대구": "26350", "사하구": "26380", "금정구": "26410", "강서구": "26440",
    "연제구": "26470", "수영구": "26500", "사상구": "26530", "기장군": "26710",
}
INDICATORS = {"stay": "21", "consumption": "22"}
CATEGORY_BASE_SCORE = {
    "장소탐방": 100,
    "먹거리": 120,
    "산책·트레킹": 130,
    "문화·체험": 140,
    "경기·공연": 140,
    "등산": 200,
}
# 이전 이름을 가져오는 코드가 있더라도 새 기준표를 사용한다.
MISSION_BASE_SCORES = CATEGORY_BASE_SCORE
TRANSPORT_DATA_DIR = Path(__file__).resolve().parent / "data" / "transport"
BUS_STOPS_CSV = TRANSPORT_DATA_DIR / "busan_bus_stops.csv"
RAIL_STATIONS_CSV = TRANSPORT_DATA_DIR / "busan_rail_stations.csv"


@dataclass(frozen=True)
class DistrictScore:
    district_name: str
    stay_intensity: float
    consumption_intensity: float
    tourism_activity: float
    activation_need: float
    region_bonus: int


def previous_month(base_ym: str) -> str:
    year = int(base_ym[:4])
    month = int(base_ym[4:])
    return f"{year - 1}12" if month == 1 else f"{year}{month - 1:02d}"


def default_target_month(today: date | None = None) -> str:
    current = today or date.today()
    return previous_month(f"{current.year}{current.month:02d}")


def month_candidates(base_ym: str, count: int = 12) -> list[str]:
    result = [base_ym]
    for _ in range(count - 1):
        result.append(previous_month(result[-1]))
    return result


def _normalize(values: dict[str, float]) -> dict[str, float]:
    if not values:
        return {}
    minimum, maximum = min(values.values()), max(values.values())
    if math.isclose(minimum, maximum):
        return {key: 0.0 for key in values}
    width = maximum - minimum
    return {key: (value - minimum) / width for key, value in values.items()}


def _half_up(value: float) -> int:
    return int(Decimal(str(value)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def calculate_accessibility_bonus(rail_distance: float, bus_distance: float) -> int:
    if rail_distance <= 400:
        return 0
    if bus_distance <= 100:
        bonus = 5
    elif bus_distance <= 200:
        bonus = 10
    elif bus_distance <= 400:
        bonus = 15
    else:
        bonus = 20
    return min(bonus, 10) if rail_distance <= 800 else bonus


def calculate_reward_points(
    category: str, region_bonus: int = 0, accessibility_bonus: int = 0
) -> int:
    if category == "등산":
        return CATEGORY_BASE_SCORE["등산"]
    return CATEGORY_BASE_SCORE[category] + region_bonus + accessibility_bonus


def calculate_district_scores(
    raw_values: dict[str, dict[str, float | None]],
) -> dict[str, DistrictScore]:
    filled = {district: values.copy() for district, values in raw_values.items()}
    for indicator in INDICATORS:
        available = [
            float(values[indicator])
            for values in filled.values()
            if values.get(indicator) is not None
        ]
        if not available:
            raise RuntimeError(f"{indicator} 관광지수가 없어 기존 점수를 유지합니다.")
        average = sum(available) / len(available)
        for values in filled.values():
            if values.get(indicator) is None:
                values[indicator] = average

    stays = {district: float(values["stay"]) for district, values in filled.items()}
    consumptions = {
        district: float(values["consumption"]) for district, values in filled.items()
    }
    stay_norm = _normalize(stays)
    consumption_norm = _normalize(consumptions)
    result = {}
    for district in raw_values:
        activity = stay_norm[district] * 0.5 + consumption_norm[district] * 0.5
        activation_need = 1 - activity
        result[district] = DistrictScore(
            district_name=district,
            stay_intensity=stays[district],
            consumption_intensity=consumptions[district],
            tourism_activity=activity,
            activation_need=activation_need,
            region_bonus=_half_up(activation_need * 30),
        )
    return result


def _extract_value(response: httpx.Response, value_key: str) -> float | None:
    response.raise_for_status()
    body = response.json().get("response", {}).get("body", {})
    items = body.get("items") if isinstance(body, dict) else None
    if not isinstance(items, dict):
        return None
    item = items.get("item")
    if isinstance(item, list):
        item = item[0] if item else None
    if not isinstance(item, dict) or item.get(value_key) in (None, ""):
        return None
    return float(item[value_key])


def fetch_indicator_value(
    client: httpx.Client,
    service_key: str,
    base_ym: str,
    district_code: str,
    indicator: str,
) -> float | None:
    if indicator == "stay":
        url, code_key, code, value_key = STAY_API_URL, "tarSjrnDsIxCd", "21", "tarSjrnDsIxVal"
    elif indicator == "consumption":
        url, code_key, code, value_key = CONSUMPTION_API_URL, "tarExpDsIxCd", "22", "tarExpDsIxVal"
    else:
        raise ValueError(f"지원하지 않는 관광지표입니다: {indicator}")
    response = client.get(
        url,
        params={
            "serviceKey": service_key,
            "MobileApp": "BusanQuest",
            "MobileOS": "ETC",
            "pageNo": 1,
            "numOfRows": 10,
            "baseYm": base_ym,
            "areaCd": BUSAN_AREA_CODE,
            "signguCd": district_code,
            code_key: code,
            "_type": "json",
        },
    )
    return _extract_value(response, value_key)


def collect_tourism_values(
    base_ym: str,
    fetcher: Callable[[str, str, str], float | None],
) -> tuple[str, dict[str, dict[str, float | None]]]:
    """두 지표가 함께 존재하는 가장 최근 월만 선택한다."""
    for candidate in month_candidates(base_ym):
        values = {
            district: {"stay": None, "consumption": None}
            for district in BUSAN_DISTRICT_CODES
        }
        for district, district_code in BUSAN_DISTRICT_CODES.items():
            for indicator in INDICATORS:
                try:
                    values[district][indicator] = fetcher(
                        candidate, district_code, indicator
                    )
                except (httpx.HTTPError, ValueError, KeyError, TypeError):
                    logger.warning(
                        "관광지수 조회 실패: base_ym=%s district=%s indicator=%s",
                        candidate, district, indicator, exc_info=True,
                    )
        has_stay = any(row["stay"] is not None for row in values.values())
        has_consumption = any(
            row["consumption"] is not None for row in values.values()
        )
        if has_stay and has_consumption:
            return candidate, values
    raise RuntimeError("동일 기준연월의 관광지수를 가져오지 못해 기존 점수를 유지합니다.")


def load_transport_locations(
    bus_path: Path = BUS_STOPS_CSV, rail_path: Path = RAIL_STATIONS_CSV
) -> tuple[list[tuple[float, float]], list[tuple[float, float]]]:
    def load(path: Path, name_column: str) -> list[tuple[float, float]]:
        try:
            with path.open("r", encoding="utf-8-sig", newline="") as stream:
                reader = csv.DictReader(stream)
                required = {name_column, "LATITUDE", "LONGITUDE"}
                if not reader.fieldnames or not required.issubset(reader.fieldnames):
                    raise ValueError(f"필수 컬럼이 없습니다: {sorted(required)}")
                rows = []
                for line_number, row in enumerate(reader, start=2):
                    latitude = float(row["LATITUDE"])
                    longitude = float(row["LONGITUDE"])
                    if not (-90 <= latitude <= 90 and -180 <= longitude <= 180):
                        raise ValueError(f"{line_number}행 좌표 범위가 올바르지 않습니다.")
                    rows.append((latitude, longitude))
        except (OSError, csv.Error, TypeError, ValueError) as exc:
            raise RuntimeError(f"교통 CSV를 읽을 수 없습니다: {path}") from exc
        if not rows:
            raise RuntimeError(f"교통 CSV가 비어 있습니다: {path}")
        return rows

    return load(bus_path, "STOP_NAME"), load(rail_path, "STATION_NAME")


def haversine_distance_m(
    lat1: float, lon1: float, lat2: float, lon2: float
) -> float:
    radius = 6_371_000
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    a = (
        math.sin(delta_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    )
    return radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def _nearest_distance(
    latitude: float, longitude: float, locations: list[tuple[float, float]]
) -> float:
    return min(
        haversine_distance_m(latitude, longitude, other_lat, other_lon)
        for other_lat, other_lon in locations
    )


def update_mission_rewards(
    db: Session,
    scores: dict[str, DistrictScore],
    bus_locations: list[tuple[float, float]] | None = None,
    rail_locations: list[tuple[float, float]] | None = None,
) -> dict[str, int]:
    if bus_locations is None or rail_locations is None:
        bus_locations, rail_locations = load_transport_locations()
    updated = skipped = 0
    for mission in db.query(Mission).all():
        category = (mission.mission_category or "").strip()
        district_score = scores.get(mission.district_name)
        if (
            category not in CATEGORY_BASE_SCORE
            or district_score is None
            or mission.latitude is None
            or mission.longitude is None
        ):
            logger.warning(
                "미션 점수 갱신 제외: mission_id=%s category=%r district=%r",
                mission.mission_id, category, mission.district_name,
            )
            skipped += 1
            continue
        if category == "등산":
            mission.reward_points = 200
        else:
            rail_distance = _nearest_distance(
                mission.latitude, mission.longitude, rail_locations
            )
            bus_distance = _nearest_distance(
                mission.latitude, mission.longitude, bus_locations
            )
            accessibility_bonus = calculate_accessibility_bonus(
                rail_distance, bus_distance
            )
            mission.reward_points = calculate_reward_points(
                category, district_score.region_bonus, accessibility_bonus
            )
        updated += 1
    db.commit()
    return {"updated_missions": updated, "skipped_missions": skipped}


def refresh_tourism_scores(db: Session, base_ym: str | None = None) -> dict:
    service_key = os.getenv("TOURISM_DATA_SERVICE_KEY")
    if not service_key:
        raise RuntimeError("TOURISM_DATA_SERVICE_KEY 환경변수가 필요합니다.")
    target_month = base_ym or default_target_month()
    if len(target_month) != 6 or not target_month.isdigit():
        raise ValueError("base_ym은 YYYYMM 형식이어야 합니다.")
    year, month = int(target_month[:4]), int(target_month[4:])
    if month not in range(1, 13):
        raise ValueError("base_ym의 월은 01~12여야 합니다.")
    monthrange(year, month)

    # 외부/정적 입력을 모두 검증한 뒤에만 MISSIONS를 갱신한다.
    bus_locations, rail_locations = load_transport_locations()
    with httpx.Client(timeout=15) as client:
        actual_month, raw_values = collect_tourism_values(
            target_month,
            lambda ym, district_code, indicator: fetch_indicator_value(
                client, service_key, ym, district_code, indicator
            ),
        )
    scores = calculate_district_scores(raw_values)
    result = update_mission_rewards(db, scores, bus_locations, rail_locations)
    return {"base_ym": actual_month, "district_count": len(scores), **result}
