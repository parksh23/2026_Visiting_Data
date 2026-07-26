package com.example.busasnquest.ui.mission

/**
 * 부산 16개 구·군의 지도 폴리곤 데이터 (테셀레이션 — 틈 없이 맞물림).
 *
 * - 좌표계: 0~100(가로) × 0~112(세로), y 아래 방향.
 * - ⚠️ 인접한 구는 경계 꼭짓점을 "완전히 동일하게" 공유한다.
 *   렌더러의 스무딩 곡선은 변의 중점을 지나므로, 양쪽 구가 같은 꼭짓점을 쓰면
 *   경계 곡선이 정확히 일치해 흰 테두리 선만 남고 공백이 생기지 않는다.
 *   → 좌표를 수정할 때는 반드시 인접 구의 같은 점도 함께 수정할 것.
 * - 멀티폴리곤 지원 (강서구 = 본토 + 가덕도, 영도구 = 섬).
 */
object BusanMapShapes {

    const val MAP_W = 100f
    const val MAP_H = 112f

    // 구 이름 → 멀티폴리곤 (각 폴리곤은 시계 방향)
    val polygons: Map<String, List<List<Pair<Float, Float>>>> = mapOf(
        "북구" to listOf(listOf(
            36f to 28f, 42f to 25f, 45f to 30f, 48f to 42f, 46f to 52f,
            42f to 57f, 33f to 52f, 33f to 44f, 34f to 35f
        )),
        "금정구" to listOf(listOf(
            50f to 16f, 58f to 12f, 63f to 16f, 64f to 24f, 63f to 34f, 62f to 42f,
            56f to 44f, 48f to 42f, 45f to 30f, 46f to 22f
        )),
        "기장군" to listOf(listOf(
            64f to 24f, 68f to 12f, 75f to 5f, 84f to 3f, 88f to 8f, 94f to 6f,
            96f to 14f, 92f to 22f, 96f to 30f, 90f to 38f, 92f to 46f, 84f to 50f,
            76f to 46f, 70f to 48f, 64f to 46f, 62f to 42f, 63f to 34f
        )),
        "동래구" to listOf(listOf(
            48f to 42f, 56f to 44f, 62f to 42f, 64f to 46f, 60f to 56f,
            55f to 58f, 46f to 52f
        )),
        "해운대구" to listOf(listOf(
            64f to 46f, 70f to 48f, 76f to 46f, 84f to 50f, 88f to 56f, 84f to 62f,
            76f to 64f, 70f to 62f, 68f to 58f, 60f to 56f
        )),
        "연제구" to listOf(listOf(
            55f to 58f, 60f to 56f, 68f to 58f, 62f to 68f, 56f to 66f
        )),
        "수영구" to listOf(listOf(
            68f to 58f, 70f to 62f, 76f to 64f, 74f to 70f, 68f to 74f,
            61f to 72f, 62f to 68f
        )),
        "부산진구" to listOf(listOf(
            42f to 57f, 46f to 52f, 55f to 58f, 56f to 66f, 46f to 68f, 43f to 66f
        )),
        "사상구" to listOf(listOf(
            33f to 52f, 42f to 57f, 43f to 66f, 40f to 72f, 31f to 68f
        )),
        "동구" to listOf(listOf(
            46f to 68f, 56f to 66f, 62f to 68f, 61f to 72f, 56f to 76f,
            52f to 82f, 48f to 82f, 46f to 74f
        )),
        "서구" to listOf(listOf(
            43f to 66f, 46f to 68f, 46f to 74f, 48f to 82f, 44f to 88f,
            41f to 82f, 40f to 72f
        )),
        "중구" to listOf(listOf(
            48f to 82f, 52f to 82f, 53f to 88f, 49f to 94f, 45f to 92f, 44f to 88f
        )),
        "남구" to listOf(listOf(
            52f to 82f, 56f to 76f, 61f to 72f, 68f to 74f, 67f to 82f,
            62f to 88f, 56f to 90f, 53f to 88f
        )),
        "영도구" to listOf(listOf(
            49f to 94f, 53f to 88f, 56f to 90f, 62f to 92f, 64f to 98f,
            60f to 106f, 53f to 108f, 48f to 102f
        )),
        "사하구" to listOf(listOf(
            31f to 68f, 40f to 72f, 41f to 82f, 44f to 88f, 41f to 94f, 36f to 100f,
            30f to 104f, 26f to 98f, 26f to 86f, 28f to 76f
        )),
        "강서구" to listOf(
            listOf(
                33f to 52f, 31f to 68f, 28f to 76f, 26f to 86f, 22f to 92f, 14f to 94f,
                8f to 88f, 4f to 78f, 3f to 66f, 8f to 58f, 16f to 52f, 24f to 48f, 30f to 46f
            ),
            // 가덕도
            listOf(8f to 96f, 13f to 94f, 16f to 100f, 12f to 106f, 7f to 103f)
        )
    )

    // 면적이 작아 라벨을 축소 표기하는 구
    val tinyDistricts = setOf("중구", "서구", "연제구")

    // 라벨 위치 수동 보정 (무게중심이 어색한 구)
    private val labelOverrides = mapOf(
        "강서구" to (15f to 72f),
        "기장군" to (77f to 24f),
        "사하구" to (33f to 86f),
        "북구" to (40f to 42f),
        "동구" to (53f to 74f)
    )

    // 라벨 위치: 보정값 → 없으면 본토(첫 폴리곤) 무게중심
    fun labelCenter(name: String): Pair<Float, Float> {
        labelOverrides[name]?.let { return it }
        val pts = polygons[name]?.firstOrNull() ?: return MAP_W / 2 to MAP_H / 2
        val x = pts.map { it.first }.average().toFloat()
        val y = pts.map { it.second }.average().toFloat()
        return x to y
    }

    // 점(nx, ny)이 어느 구 안에 있는지 — 멀티폴리곤 ray casting
    fun hitTest(nx: Float, ny: Float): String? {
        polygons.forEach { (name, parts) ->
            if (parts.any { pointInPolygon(nx, ny, it) }) return name
        }
        return null
    }

    private fun pointInPolygon(x: Float, y: Float, pts: List<Pair<Float, Float>>): Boolean {
        var inside = false
        var j = pts.size - 1
        for (i in pts.indices) {
            val (xi, yi) = pts[i]
            val (xj, yj) = pts[j]
            val intersects = (yi > y) != (yj > y) &&
                x < (xj - xi) * (y - yi) / (yj - yi) + xi
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }
}
