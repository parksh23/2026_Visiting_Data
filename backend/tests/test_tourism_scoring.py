import math
import os
import sys
from pathlib import Path


os.environ["DATABASE_URL"] = "sqlite:///:memory:"
APP_DIR = Path(__file__).resolve().parents[1] / "app"
sys.path.insert(0, str(APP_DIR))

from tourism_scoring import (  # noqa: E402
    CATEGORY_BASE_SCORE,
    DistrictScore,
    calculate_accessibility_bonus,
    calculate_district_scores,
    calculate_reward_points,
    haversine_distance_m,
    load_transport_locations,
    update_mission_rewards,
)


def test_category_base_scores_and_reward_formula():
    assert CATEGORY_BASE_SCORE == {
        "장소탐방": 100,
        "먹거리": 120,
        "산책·트레킹": 130,
        "문화·체험": 140,
        "경기·공연": 140,
        "등산": 200,
    }
    assert calculate_reward_points("장소탐방", 20, 10) == 130
    assert calculate_reward_points("등산", 30, 20) == 200


def test_accessibility_boundaries():
    assert calculate_accessibility_bonus(400, 999) == 0
    assert calculate_accessibility_bonus(400.01, 100) == 5
    assert calculate_accessibility_bonus(800, 100.01) == 10
    assert calculate_accessibility_bonus(800, 400.01) == 10
    assert calculate_accessibility_bonus(800.01, 100) == 5
    assert calculate_accessibility_bonus(800.01, 100.01) == 10
    assert calculate_accessibility_bonus(800.01, 200) == 10
    assert calculate_accessibility_bonus(800.01, 200.01) == 15
    assert calculate_accessibility_bonus(800.01, 400) == 15
    assert calculate_accessibility_bonus(800.01, 400.01) == 20


def test_region_activation_formula_and_half_up_rounding():
    scores = calculate_district_scores(
        {
            "낮은지역": {"stay": 0.0, "consumption": 0.0},
            "중간지역": {"stay": 1.0, "consumption": 0.5},
            "높은지역": {"stay": 2.0, "consumption": 2.0},
        }
    )
    middle = scores["중간지역"]
    assert middle.tourism_activity == 0.375
    assert middle.activation_need == 0.625
    assert middle.region_bonus == 19  # 18.75 ROUND_HALF_UP
    assert scores["낮은지역"].region_bonus == 30
    assert scores["높은지역"].region_bonus == 0

    equal = calculate_district_scores(
        {
            "가": {"stay": 1.0, "consumption": 1.0},
            "나": {"stay": 1.0, "consumption": 1.0},
        }
    )
    assert {score.region_bonus for score in equal.values()} == {30}


def test_haversine_and_transport_csv_loading():
    assert math.isclose(haversine_distance_m(35.0, 129.0, 35.0, 129.0), 0.0)
    assert 110_000 < haversine_distance_m(35.0, 129.0, 36.0, 129.0) < 112_000
    buses, rails = load_transport_locations()
    assert buses
    assert rails


class _Mission:
    def __init__(self, mission_id, category):
        self.mission_id = mission_id
        self.mission_category = category
        self.district_name = "중구"
        self.latitude = 35.0
        self.longitude = 129.0
        self.reward_points = 0
        self.difficulty = "기존값"


class _Query:
    def __init__(self, missions):
        self._missions = missions

    def all(self):
        return self._missions


class _Session:
    def __init__(self, missions):
        self._missions = missions
        self.committed = False

    def query(self, _model):
        return _Query(self._missions)

    def commit(self):
        self.committed = True


def test_update_changes_reward_only_and_keeps_difficulty():
    normal = _Mission(1, "장소탐방")
    hiking = _Mission(2, "등산")
    session = _Session([normal, hiking])
    score = DistrictScore("중구", 1.0, 1.0, 1 / 3, 2 / 3, 20)
    result = update_mission_rewards(
        session,
        {"중구": score},
        bus_locations=[(35.0, 129.002)],
        rail_locations=[(35.0, 129.005)],
    )
    assert result == {"updated_missions": 2, "skipped_missions": 0}
    assert normal.reward_points == 130
    assert hiking.reward_points == 200
    assert normal.difficulty == "기존값"
    assert hiking.difficulty == "기존값"
    assert session.committed
