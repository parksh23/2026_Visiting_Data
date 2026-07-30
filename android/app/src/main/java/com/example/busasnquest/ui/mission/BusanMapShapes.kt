package com.example.busasnquest.ui.mission

/**
 * 부산 16개 구·군 실제 행정경계 폴리곤 (GADM 데이터 기반, RDP 단순화).
 * - 좌표계: 0~100(가로) × 0~94.1(세로), y 아래 방향.
 * - 인접 구는 실제 경계 좌표를 공유 → 하나의 부산 형태, 흰 선 = 구·군 경계.
 * - ⚠️ 중구는 원본 데이터에 누락되어 실제 위치에 근사 폴리곤으로 합성함 (맨 위에 그려짐).
 * - 멀티폴리곤 지원 (강서구 가덕도 등 섬 포함).
 */
object BusanMapShapes {

    const val MAP_W = 100f
    const val MAP_H = 94.1f

    val polygons: Map<String, List<List<Pair<Float, Float>>>> = mapOf(
        "북구" to listOf(
            listOf(54.1f to 25.5f, 50.9f to 30.9f, 50.4f to 32.6f, 51.6f to 36.5f, 50.3f to 39.0f, 50.1f to 40.8f, 51.8f to 42.0f, 51.1f to 44.3f, 47.6f to 48.9f, 44.7f to 50.8f, 42.7f to 51.9f, 41.2f to 52.0f, 38.8f to 51.2f, 37.7f to 50.5f, 38.6f to 43.6f, 40.3f to 41.6f, 42.8f to 35.8f, 43.0f to 33.8f, 47.7f to 28.0f)
        ),
        "부산진구" to listOf(
            listOf(48.5f to 50.1f, 49.4f to 51.6f, 50.9f to 51.7f, 51.9f to 52.6f, 53.9f to 52.6f, 54.5f to 54.0f, 54.3f to 56.2f, 53.0f to 58.4f, 50.1f to 59.8f, 49.2f to 61.1f, 45.5f to 60.9f, 42.4f to 63.3f, 41.3f to 62.5f, 39.4f to 62.1f, 41.0f to 60.6f, 41.5f to 58.7f, 41.2f to 55.7f, 42.7f to 51.9f, 47.6f to 48.9f)
        ),
        "동구" to listOf(
            listOf(49.2f to 61.1f, 49.7f to 63.4f, 48.8f to 64.2f, 48.0f to 63.3f, 48.7f to 64.4f, 47.6f to 64.3f, 48.2f to 65.4f, 47.0f to 64.9f, 46.4f to 66.0f, 47.0f to 67.0f, 42.3f to 66.8f, 41.8f to 65.4f, 42.4f to 63.3f, 45.5f to 60.9f)
        ),
        "동래구" to listOf(
            listOf(63.0f to 41.9f, 62.2f to 46.5f, 57.0f to 47.6f, 55.4f to 46.6f, 51.1f to 50.0f, 49.7f to 50.3f, 48.5f to 50.1f, 47.6f to 48.9f, 51.1f to 44.3f, 51.8f to 42.0f, 56.1f to 39.3f, 57.8f to 39.9f, 60.8f to 39.6f, 61.6f to 41.1f)
        ),
        "강서구" to listOf(
            listOf(38.6f to 43.6f, 37.7f to 50.5f, 33.5f to 52.5f, 30.7f to 62.7f, 26.8f to 66.4f, 25.9f to 66.0f, 24.5f to 70.1f, 22.0f to 71.5f, 21.9f to 73.3f, 18.6f to 73.3f, 19.6f to 65.6f, 20.7f to 64.3f, 18.7f to 63.4f, 17.6f to 66.4f, 16.6f to 73.3f, 13.7f to 72.8f, 13.8f to 72.1f, 7.7f to 72.2f, 6.9f to 71.1f, 7.9f to 69.5f, 10.6f to 68.8f, 11.2f to 67.5f, 11.2f to 64.1f, 9.7f to 63.9f, 8.7f to 62.1f, 9.0f to 58.4f, 12.1f to 58.4f, 15.5f to 56.3f, 19.9f to 56.5f, 20.5f to 55.4f, 19.7f to 53.9f, 19.4f to 49.2f, 20.7f to 47.3f, 23.7f to 46.1f, 25.1f to 44.1f, 26.6f to 44.4f, 27.6f to 45.6f, 33.6f to 45.3f),
            listOf(6.4f to 75.9f, 7.4f to 76.2f, 6.9f to 77.9f, 7.4f to 78.8f, 9.5f to 78.2f, 10.4f to 81.8f, 7.2f to 88.6f, 8.5f to 89.7f, 7.9f to 91.3f, 8.4f to 92.2f, 5.9f to 94.1f, 6.4f to 92.7f, 4.8f to 92.7f, 4.8f to 91.2f, 4.2f to 90.8f, 5.2f to 90.1f, 4.4f to 89.2f, 5.8f to 88.4f, 5.3f to 87.7f, 2.7f to 87.2f, 2.7f to 86.4f, 1.7f to 86.6f, 1.5f to 85.8f, 3.1f to 85.9f, 3.6f to 85.0f, 2.1f to 83.7f, 2.9f to 83.4f, 2.8f to 82.0f, 1.8f to 80.6f, 0.3f to 80.3f, 0.0f to 78.9f, 1.7f to 78.3f, 1.8f to 77.6f, 4.9f to 77.4f, 5.2f to 76.0f)
        ),
        "금정구" to listOf(
            listOf(67.2f to 13.5f, 67.8f to 15.2f, 66.6f to 16.8f, 66.2f to 18.3f, 68.5f to 20.6f, 68.7f to 23.2f, 67.8f to 25.3f, 67.5f to 28.1f, 69.3f to 29.0f, 70.1f to 31.6f, 69.4f to 33.1f, 67.2f to 36.4f, 65.8f to 37.3f, 64.5f to 40.4f, 63.0f to 41.9f, 61.6f to 41.1f, 60.8f to 39.6f, 57.8f to 39.9f, 56.1f to 39.3f, 51.8f to 42.0f, 50.1f to 40.8f, 50.3f to 39.0f, 51.6f to 36.5f, 50.4f to 32.6f, 50.9f to 30.9f, 54.1f to 25.5f, 59.1f to 18.7f, 60.9f to 17.6f, 63.6f to 17.3f, 64.8f to 16.8f)
        ),
        "기장군" to listOf(
            listOf(100.0f to 11.4f, 99.2f to 13.4f, 98.5f to 13.1f, 97.8f to 15.3f, 95.7f to 15.4f, 95.8f to 17.3f, 95.2f to 17.8f, 92.8f to 17.5f, 93.0f to 16.6f, 91.7f to 15.9f, 88.5f to 17.9f, 87.4f to 20.7f, 88.1f to 22.6f, 87.0f to 27.9f, 86.1f to 28.8f, 84.8f to 28.6f, 84.5f to 30.5f, 82.8f to 30.7f, 83.7f to 31.8f, 85.2f to 31.5f, 85.2f to 30.8f, 86.5f to 31.2f, 86.6f to 34.4f, 84.9f to 35.0f, 85.9f to 35.9f, 84.7f to 36.5f, 85.1f to 38.0f, 84.2f to 38.4f, 83.0f to 41.6f, 83.0f to 40.5f, 81.7f to 39.6f, 81.8f to 41.1f, 80.6f to 42.3f, 82.4f to 44.9f, 82.0f to 46.0f, 81.1f to 46.0f, 81.1f to 48.5f, 79.9f to 49.0f, 79.5f to 48.1f, 78.9f to 48.2f, 77.4f to 44.5f, 75.0f to 45.7f, 74.3f to 42.4f, 72.8f to 42.6f, 71.7f to 41.7f, 73.0f to 36.5f, 71.7f to 32.7f, 70.1f to 31.6f, 69.3f to 29.0f, 67.5f to 28.1f, 68.7f to 23.2f, 68.5f to 20.6f, 66.2f to 18.3f, 67.8f to 15.2f, 67.2f to 13.5f, 67.9f to 10.6f, 72.7f to 9.9f, 74.7f to 11.3f, 76.5f to 11.2f, 81.7f to 6.2f, 84.0f to 1.7f, 85.2f to 1.3f, 88.3f to 2.2f, 91.6f to 0.0f, 94.7f to 0.4f, 96.9f to 4.5f, 96.7f to 6.6f)
        ),
        "해운대구" to listOf(
            listOf(70.1f to 31.6f, 71.7f to 32.7f, 73.0f to 36.5f, 71.7f to 41.7f, 72.8f to 42.6f, 74.3f to 42.4f, 75.0f to 45.7f, 77.4f to 44.5f, 79.0f to 48.9f, 78.6f to 49.7f, 78.2f to 49.2f, 77.6f to 50.1f, 76.3f to 50.0f, 75.9f to 53.0f, 74.8f to 54.7f, 73.7f to 54.6f, 73.0f to 55.7f, 70.6f to 54.6f, 68.5f to 54.7f, 66.9f to 56.5f, 66.8f to 55.5f, 65.6f to 55.8f, 63.7f to 54.1f, 62.1f to 47.7f, 62.6f to 43.5f, 65.8f to 37.3f, 67.2f to 36.4f)
        ),
        "남구" to listOf(
            listOf(59.5f to 60.1f, 60.7f to 60.4f, 60.7f to 61.2f, 61.4f to 60.4f, 63.1f to 64.1f, 62.0f to 67.7f, 62.5f to 69.0f, 58.6f to 67.7f, 58.4f to 68.7f, 56.9f to 68.4f, 56.4f to 69.1f, 55.9f to 66.3f, 55.5f to 67.6f, 52.9f to 67.7f, 51.6f to 67.1f, 51.4f to 65.1f, 52.2f to 63.2f, 50.8f to 63.0f, 50.9f to 62.2f, 49.8f to 63.2f, 49.3f to 62.5f, 50.1f to 59.8f, 53.0f to 58.4f, 54.3f to 56.2f, 57.0f to 55.9f, 58.0f to 56.5f, 57.9f to 58.5f)
        ),
        "사하구" to listOf(
            listOf(30.9f to 66.1f, 32.1f to 66.6f, 36.8f to 66.5f, 38.9f to 68.1f, 39.4f to 70.5f, 41.9f to 73.7f, 41.6f to 74.4f, 43.0f to 76.9f, 41.0f to 79.3f, 40.6f to 76.5f, 39.4f to 75.1f, 39.3f to 72.0f, 38.6f to 72.2f, 38.9f to 73.1f, 37.2f to 72.4f, 38.3f to 79.3f, 39.0f to 80.2f, 38.3f to 81.3f, 37.1f to 81.0f, 37.4f to 79.8f, 36.8f to 78.1f, 34.9f to 77.4f, 34.3f to 78.7f, 33.4f to 78.9f, 35.5f to 79.1f, 35.3f to 80.7f, 33.0f to 80.5f, 34.3f to 82.3f, 33.5f to 82.0f, 32.3f to 84.0f, 31.7f to 83.0f, 32.4f to 80.7f, 30.7f to 80.0f, 29.1f to 73.0f, 30.2f to 66.9f, 29.0f to 66.8f, 27.2f to 72.6f, 27.3f to 71.1f, 26.0f to 71.8f, 25.7f to 70.5f, 26.8f to 66.4f, 28.8f to 64.9f)
        ),
        "사상구" to listOf(
            listOf(42.7f to 51.9f, 41.2f to 55.7f, 41.5f to 58.7f, 41.0f to 60.6f, 38.2f to 63.5f, 36.8f to 66.5f, 32.1f to 66.6f, 28.8f to 64.9f, 30.7f to 62.7f, 33.5f to 52.5f, 37.7f to 50.5f, 41.2f to 52.0f)
        ),
        "서구" to listOf(
            listOf(42.4f to 63.3f, 41.8f to 65.4f, 42.5f to 66.8f, 41.3f to 68.8f, 43.6f to 71.9f, 42.9f to 74.2f, 42.2f to 73.5f, 41.9f to 73.7f, 39.4f to 70.5f, 38.9f to 68.1f, 36.8f to 66.5f, 38.2f to 63.5f, 39.4f to 62.1f, 41.3f to 62.5f)
        ),
        "수영구" to listOf(
            listOf(61.0f to 50.7f, 64.1f to 55.3f, 63.1f to 56.2f, 61.6f to 55.7f, 60.7f to 56.2f, 60.3f to 57.3f, 60.8f to 59.1f, 59.7f to 60.0f, 57.9f to 58.5f, 58.0f to 56.5f, 57.0f to 55.9f, 58.2f to 52.3f),
            listOf(62.5f to 50.3f, 63.6f to 51.8f, 63.7f to 54.1f, 61.4f to 50.7f)
        ),
        "영도구" to listOf(
            listOf(49.7f to 67.9f, 53.5f to 71.1f, 53.4f to 72.4f, 54.3f to 73.5f, 52.8f to 73.5f, 52.9f to 74.4f, 54.3f to 75.5f, 54.6f to 74.9f, 55.0f to 76.7f, 56.8f to 77.6f, 55.8f to 79.8f, 54.0f to 79.8f, 53.1f to 77.4f, 51.8f to 78.0f, 50.6f to 76.8f, 50.6f to 75.4f, 48.8f to 75.3f, 44.9f to 71.7f, 47.2f to 71.3f, 47.7f to 68.3f),
            listOf(56.5f to 72.8f, 56.6f to 73.8f, 55.4f to 74.6f, 54.6f to 74.2f, 55.5f to 73.1f)
        ),
        "연제구" to listOf(
            listOf(55.4f to 46.6f, 57.0f to 47.6f, 60.1f to 47.1f, 61.0f to 50.7f, 58.2f to 52.3f, 57.0f to 55.9f, 54.3f to 56.2f, 54.5f to 54.0f, 53.9f to 52.6f, 51.9f to 52.6f, 50.9f to 51.7f, 49.4f to 51.6f, 48.5f to 50.1f, 51.1f to 50.0f),
            listOf(62.2f to 47.1f, 62.5f to 50.3f, 61.4f to 50.6f, 60.3f to 47.1f, 60.9f to 46.6f, 62.2f to 46.5f)
        ),
        "중구" to listOf(
            listOf(44.7f to 65.0f, 46.1f to 64.6f, 46.9f to 66.0f, 46.7f to 68.3f, 45.7f to 69.4f, 44.8f to 68.7f)
        )
    )

    // 면적이 작아 라벨을 축소 표기하는 구
    val tinyDistricts = setOf("중구", "동구", "서구", "연제구", "수영구", "영도구")

    // 라벨 위치 (본토 폴리곤의 면적 무게중심, 생성 스크립트가 계산)
    private val labelCenters: Map<String, Pair<Float, Float>> = mapOf(
        "북구" to (45.6f to 40.5f),
        "부산진구" to (47.0f to 56.1f),
        "동구" to (45.6f to 63.9f),
        "동래구" to (55.8f to 44.5f),
        "강서구" to (22.8f to 58.5f),
        "금정구" to (60.4f to 29.9f),
        "기장군" to (81.4f to 20.9f),
        "해운대구" to (69.5f to 45.9f),
        "남구" to (56.4f to 62.9f),
        "사하구" to (34.0f to 73.0f),
        "사상구" to (36.1f to 58.8f),
        "서구" to (40.4f to 67.4f),
        "수영구" to (60.1f to 55.0f),
        "영도구" to (51.0f to 73.5f),
        "연제구" to (55.7f to 50.8f),
        "중구" to (45.7f to 66.9f)
    )

    fun labelCenter(name: String): Pair<Float, Float> =
        labelCenters[name] ?: (MAP_W / 2 to MAP_H / 2)

    // 점(nx, ny)이 어느 구 안에 있는지 — 나중에 그려진(위에 있는) 구가 우선
    fun hitTest(nx: Float, ny: Float): String? {
        polygons.entries.reversed().forEach { (name, parts) ->
            if (parts.any { pointInPolygon(nx, ny, it) }) return name
        }
        return null
    }

    /**
     * 스마트 히트 슬롭: 정확히 어느 구 안이면 그 구를,
     * 아니면 maxDist(지도 좌표 단위) 안에서 경계가 가장 가까운 구를 돌려준다.
     * → 중구처럼 작은 구도 근처만 탭하면 선택됨.
     */
    fun hitTestNearest(nx: Float, ny: Float, maxDist: Float): String? {
        hitTest(nx, ny)?.let { return it }

        var best: String? = null
        var bestDist = maxDist
        polygons.forEach { (name, parts) ->
            parts.forEach { pts ->
                val n = pts.size
                for (i in 0 until n) {
                    val d = distToSegment(
                        nx, ny,
                        pts[i].first, pts[i].second,
                        pts[(i + 1) % n].first, pts[(i + 1) % n].second
                    )
                    if (d < bestDist) {
                        bestDist = d
                        best = name
                    }
                }
            }
        }
        return best
    }

    // 점 (px,py) 와 선분 (ax,ay)-(bx,by) 사이 거리
    private fun distToSegment(
        px: Float, py: Float,
        ax: Float, ay: Float,
        bx: Float, by: Float
    ): Float {
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        val t = if (lenSq == 0f) 0f
        else (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0f, 1f)
        val cx = ax + t * dx
        val cy = ay + t * dy
        return kotlin.math.hypot((px - cx).toDouble(), (py - cy).toDouble()).toFloat()
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
