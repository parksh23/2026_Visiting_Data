package com.example.busasnquest.ui.mission

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busasnquest.data.repository.DistrictMissionProgress
import com.example.busasnquest.ui.theme.Dimens
import com.example.busasnquest.ui.theme.TextSub
import com.example.busasnquest.ui.theme.occupancyColor
import com.example.busasnquest.ui.theme.occupancyTextColor
import kotlin.math.min

/**
 * 부산 지도 실루엣 히트맵 (둥근 블롭 스타일 + 맞물린 배치).
 *
 * - 각 구를 실제 위치 관계의 폴리곤으로 그리되, 꼭짓점 사이를 곡선으로 이어
 *   둥글둥글한 블롭 모양으로 렌더링 (BusanMapShapes, 멀티폴리곤 지원)
 * - 채움색 = 점령률 코럴 5단계 (완료할수록 진해짐, 색 전환 애니메이션)
 * - 흰 테두리 = 구 경계 / 가덕도·영도 섬 포함
 * - 구 탭 → onSelect (바텀시트) / 진행 중 점 · 점령 완료 ✓ · 선택 테두리 유지
 */
@Composable
fun BusanMap(
    districts: List<DistrictMissionProgress>,
    inProgressSet: Set<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val byName = districts.associateBy { it.name }
    val textMeasurer = rememberTextMeasurer()

    // 구별 점령률 → 애니메이션 채움색 (16개 고정 순서라 recomposition 안정)
    val animatedColors = mutableMapOf<String, Color>()
    for (name in BusanMapShapes.polygons.keys) {
        val p = byName[name]
        val rate = if (p == null || p.total == 0) 0f else p.completed.toFloat() / p.total
        val color by animateColorAsState(
            targetValue = occupancyColor(rate),
            animationSpec = tween(500),
            label = "map_$name"
        )
        animatedColors[name] = color
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val scale = min(
                            size.width / BusanMapShapes.MAP_W,
                            size.height / BusanMapShapes.MAP_H
                        )
                        val dx = (size.width - BusanMapShapes.MAP_W * scale) / 2f
                        val dy = (size.height - BusanMapShapes.MAP_H * scale) / 2f
                        val nx = (tap.x - dx) / scale
                        val ny = (tap.y - dy) / scale
                        BusanMapShapes.hitTest(nx, ny)?.let(onSelect)
                    }
                }
        ) {
            val scale = min(size.width / BusanMapShapes.MAP_W, size.height / BusanMapShapes.MAP_H)
            val dx = (size.width - BusanMapShapes.MAP_W * scale) / 2f
            val dy = (size.height - BusanMapShapes.MAP_H * scale) / 2f

            fun tx(x: Float) = dx + x * scale
            fun ty(y: Float) = dy + y * scale

            // 꼭짓점 사이를 quadratic 곡선으로 통과시키는 스무딩 패스
            // (변의 중점에서 시작해 각 꼭짓점을 제어점으로 사용 → 둥근 블롭)
            fun buildPath(pts: List<Pair<Float, Float>>) = Path().apply {
                val n = pts.size
                fun px(i: Int) = tx(pts[(i + n) % n].first)
                fun py(i: Int) = ty(pts[(i + n) % n].second)

                moveTo((px(0) + px(1)) / 2f, (py(0) + py(1)) / 2f)
                for (i in 1..n) {
                    val midX = (px(i) + px(i + 1)) / 2f
                    val midY = (py(i) + py(i + 1)) / 2f
                    quadraticBezierTo(px(i), py(i), midX, midY)
                }
                close()
            }

            // 1) 구 폴리곤 (멀티폴리곤): 채움 + 흰 테두리
            BusanMapShapes.polygons.forEach { (name, parts) ->
                parts.forEach { pts ->
                    val path = buildPath(pts)
                    drawPath(path, color = animatedColors[name] ?: Color.LightGray)
                    drawPath(path, color = Color.White, style = Stroke(width = 2.5.dp.toPx()))
                    if (name == selected) {
                        drawPath(path, color = Color(0xFF4A1B0C), style = Stroke(width = 3.dp.toPx()))
                    }
                }
            }

            // 2) 라벨 + 상태 표시
            BusanMapShapes.polygons.keys.forEach { name ->
                val p = byName[name]
                val total = p?.total ?: 0
                val completed = p?.completed ?: 0
                val rate = if (total == 0) 0f else completed.toFloat() / total
                val labelColor = occupancyTextColor(rate)
                val (cx, cy) = BusanMapShapes.labelCenter(name)
                val center = Offset(tx(cx), ty(cy))
                val tiny = name in BusanMapShapes.tinyDistricts

                val nameSize = if (tiny) 9.sp else 12.sp
                val pctSize = if (tiny) 8.sp else 10.sp
                val lineGap = if (tiny) 5.dp.toPx() else 6.dp.toPx()

                drawCenteredText(
                    textMeasurer, districtShortName(name),
                    center.copy(y = center.y - lineGap),
                    TextStyle(fontSize = nameSize, fontWeight = FontWeight.Bold, color = labelColor)
                )
                drawCenteredText(
                    textMeasurer,
                    if (total == 0) "-" else "${completed * 100 / total}%",
                    center.copy(y = center.y + lineGap),
                    TextStyle(fontSize = pctSize, color = labelColor.copy(alpha = 0.9f))
                )

                // 점령 완료 ✓ / 진행 중 점
                val cleared = total > 0 && completed == total
                if (cleared) {
                    drawCenteredText(
                        textMeasurer, "✓",
                        center.copy(y = center.y - lineGap - 11.dp.toPx()),
                        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                } else if (name in inProgressSet) {
                    drawCircle(
                        color = if (rate > 0.5f) Color.White else Color(0xFFCE504D),
                        radius = 3.dp.toPx(),
                        center = center.copy(y = center.y - lineGap - 10.dp.toPx())
                    )
                }
            }
        }

        Text(
            "옅음 0% → 진함 100% · 미션을 완료할수록 진해져요",
            fontSize = 11.sp,
            color = TextSub,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 4.dp)
        )
    }
}

// 텍스트를 중심 좌표 기준으로 그리기
private fun DrawScope.drawCenteredText(
    measurer: TextMeasurer,
    text: String,
    center: Offset,
    style: TextStyle
) {
    val layout = measurer.measure(text, style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f
        )
    )
}
