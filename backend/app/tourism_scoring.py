import math
import logging
import os
from calendar import monthrange
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, ROUND_HALF_UP
from typing import Callable

import httpx
from sqlalchemy.orm import Session

from models import Mission

logger = logging.getLogger(__name__)

TOURISM_API_URL = (
    "https://apis.data.go.kr/B551011/AreaTarResDemService/areaTarSvcDemList"
)
BUSAN_AREA_CODE = "26"
BUSAN_DISTRICT_CODES = {
    "중구": "26110",
    "서구": "26140",
    "동구": "26170",
    "영도구": "26200",
    "부산진구": "26230",
    "동래구": "26260",
    "남구": "26290",
    "북구": "26320",
    "해운대구": "26350",
    "사하구": "26380",
    "금정구": "26410",
    "강서구": "26440",
    "연제구": "26470",
    "수영구": "26500",
    "사상구": "26530",
    "기장군": "26710",
}
INDICATORS = {
    "food_spending": "1106",
    "lodging_spending": "1107",
    "leisure_spending": "1108",
    "lodging_search": "1110",
    "food_search": "1111",
    "shopping_search": "1112",
}
SPENDING_KEYS = ("food_spending", "lodging_spending", "leisure_spending")
SEARCH_KEYS = ("lodging_search", "food_search", "shopping_search")
MISSION_BASE_SCORES = {
    "식당방문": 100,
    "관광지방문": 120,
    "체험": 150,
    "등산": 170,
    "경기관람": 200,
}


@dataclass(frozen=True)
class DistrictScore:
    district_name: str
    spending_total: float | None
    search_total: float | None
    activity_score: float
    difficulty_score: float
    difficulty_level: str


def previous_month(base_ym: str) -> str:
    year = int(base_ym[:4])
    month = int(base_ym[4:])
    if month == 1:
        return f"{year - 1}12"
    return f"{year}{month - 1:02d}"


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
    minimum = min(values.values())
    maximum = max(values.values())
    if math.isclose(minimum, maximum):
        return {key: 0.0 for key in values}
    width = maximum - minimum
    return {key: (value - minimum) / width for key, value in values.items()}


def _half_up(value: float) -> int:
    return int(Decimal(str(value)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


def calculate_reward_points(category: str, difficulty_score: float) -> int:
    base_score = MISSION_BASE_SCORES[category]
    return _half_up(base_score * (1 + difficulty_score * 0.5))


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
        average = sum(available) / len(available) if available else None
        for values in filled.values():
            if values.get(indicator) is None:
                values[indicator] = average

    spending_totals: dict[str, float] = {}
    search_totals: dict[str, float] = {}
    missing_groups: set[str] = set()
    for district, values in filled.items():
        spending = [values.get(key) for key in SPENDING_KEYS]
        searches = [values.get(key) for key in SEARCH_KEYS]
        if any(value is None for value in spending + searches):
            missing_groups.add(district)
            continue
        spending_totals[district] = sum(float(value) for value in spending)
        search_totals[district] = sum(float(value) for value in searches)

    spending_normalized = _normalize(spending_totals)
    search_normalized = _normalize(search_totals)
    result: dict[str, DistrictScore] = {}
    for district in raw_values:
        if district in missing_groups:
            activity = 1.0
            difficulty = 0.0
            spending_total = None
            search_total = None
        else:
            spending_total = spending_totals[district]
            search_total = search_totals[district]
            activity = (
                spending_normalized[district] * 0.5
                + search_normalized[district] * 0.5
            )
            difficulty = 1 - activity
        level = "어려움" if difficulty >= 0.67 else ("보통" if difficulty >= 0.34 else "쉬움")
        result[district] = DistrictScore(
            district_name=district,
            spending_total=spending_total,
            search_total=search_total,
            activity_score=activity,
            difficulty_score=difficulty,
            difficulty_level=level,
        )
    return result


def fetch_indicator_value(
    client: httpx.Client,
    service_key: str,
    base_ym: str,
    district_code: str,
    indicator_code: str,
) -> float | None:
    response = client.get(
        TOURISM_API_URL,
        params={
            "serviceKey": service_key,
            "MobileApp": "BusanQuest",
            "MobileOS": "ETC",
            "pageNo": 1,
            "numOfRows": 10,
            "baseYm": base_ym,
            "areaCd": BUSAN_AREA_CODE,
            "signguCd": district_code,
            "tarSvcDemIxCd": indicator_code,
            "_type": "json",
        },
    )
    response.raise_for_status()
    body = response.json().get("response", {}).get("body", {})
    items = body.get("items") if isinstance(body, dict) else None
    if not isinstance(items, dict):
        return None
    item = items.get("item")
    if isinstance(item, list):
        item = item[0] if item else None
    if not isinstance(item, dict) or item.get("tarSvcDemIxVal") in (None, ""):
        return None
    return float(item["tarSvcDemIxVal"])


def collect_tourism_values(
    base_ym: str,
    fetcher: Callable[[str, str, str], float | None],
) -> dict[str, dict[str, float | None]]:
    candidates = month_candidates(base_ym)
    values: dict[str, dict[str, float | None]] = {}
    for district, district_code in BUSAN_DISTRICT_CODES.items():
        district_values: dict[str, float | None] = {}
        for indicator_name, indicator_code in INDICATORS.items():
            value = None
            for candidate in candidates:
                try:
                    value = fetcher(candidate, district_code, indicator_code)
                except (httpx.HTTPError, ValueError, KeyError):
                    value = None
                if value is not None:
                    break
            district_values[indicator_name] = value
        values[district] = district_values
    return values


def update_mission_rewards(
    db: Session,
    scores: dict[str, DistrictScore],
) -> dict[str, int]:
    updated = 0
    skipped = 0
    for mission in db.query(Mission).all():
        category = (mission.mission_category or "").strip()
        district_score = scores.get(mission.district_name)
        if category not in MISSION_BASE_SCORES or district_score is None:
            logger.warning(
                "미션 점수 갱신 제외: mission_id=%s category=%r district=%r",
                mission.mission_id,
                category,
                mission.district_name,
            )
            skipped += 1
            continue
        mission.reward_points = calculate_reward_points(
            category, district_score.difficulty_score
        )
        mission.difficulty = district_score.difficulty_level
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

    with httpx.Client(timeout=15) as client:
        raw_values = collect_tourism_values(
            target_month,
            lambda ym, district_code, indicator_code: fetch_indicator_value(
                client, service_key, ym, district_code, indicator_code
            ),
        )
    if not any(
        value is not None
        for district_values in raw_values.values()
        for value in district_values.values()
    ):
        raise RuntimeError(
            "관광지수 데이터를 한 건도 가져오지 못해 기존 점수를 유지합니다."
        )
    scores = calculate_district_scores(raw_values)
    result = update_mission_rewards(db, scores)
    return {"base_ym": target_month, "district_count": len(scores), **result}
