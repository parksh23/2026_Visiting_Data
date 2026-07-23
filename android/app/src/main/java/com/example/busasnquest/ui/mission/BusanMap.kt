package com.example.busasnquest.ui.mission

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
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
import com.example.busasnquest.ui.theme.AppFontFamily
import com.example.busasnquest.ui.theme.OccupancyTextDarker
import com.example.busasnquest.ui.theme.TextSub
import com.example.busasnquest.ui.theme.occupancyColor
import kotlin.math.min

/**
 * 부산 지도 실루엣 히트맵 (실제 행정경계 + 줌/팬).
 *
 * - 실경계 폴리곤 (BusanMapShapes) / 채움색 = 점령률 코럴 5단계 (애니메이션)
 * - 핀치 줌(1~5배) + 드래그 팬 + 더블탭(확대↔원위치)
 * - 스마트 히트 슬롭: 작은 구(중구 등)는 근처만 탭해도 선택됨
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

    // 구별 점령률 → 애니메이션 채움색 (컴포지션 단계에서 계산, Canvas 가 캡처)
    val animatedColors = rememberAnimatedMapColors(byName)

    // ── 줌/팬 상태 ──
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    fun clampPan(p: Offset, z: Float): Offset {
        val maxX = canvasSize.width * (z - 1f) / 2f
        val maxY = canvasSize.height * (z - 1f) / 2f
        return Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 5f)
        pan = clampPan(pan + panChange, zoom)
    }

    // 화면 좌표 → 지도 좌표 역변환 (탭 판정용)
    fun toMapCoords(tap: Offset): Pair<Float, Float> {
        val base = min(canvasSize.width / BusanMapShapes.MAP_W, canvasSize.height / BusanMapShapes.MAP_H)
        val dx = (canvasSize.width - BusanMapShapes.MAP_W * base) / 2f
        val dy = (canvasSize.height - BusanMapShapes.MAP_H * base) / 2f
        val c = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        // 줌/팬 이전의 기준 좌표로 되돌림: S = C + (B - C)*Z + P  →  B = C + (S - C - P)/Z
        val bx = c.x + (tap.x - c.x - pan.x) / zoom
        val by = c.y + (tap.y - c.y - pan.y) / zoom
        return (bx - dx) / base to (by - dy) / base
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
                // 확대 시 지도가 캔버스 영역 밖(검색창·세그먼트 위)으로 그려지지 않게 잘라냄
                .clipToBounds()
                .transformable(transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tap ->
                            val (nx, ny) = toMapCoords(tap)
                            // 히트 슬롭: 화면 16dp 를 지도 좌표로 환산 (줌 배율만큼 좁아짐)
                            val base = min(
                                size.width / BusanMapShapes.MAP_W,
                                size.height / BusanMapShapes.MAP_H
                            )
                            val slop = 16.dp.toPx() / (base * zoom)
                            BusanMapShapes.hitTestNearest(nx, ny, slop)?.let(onSelect)
                        },
                        onDoubleTap = { tap ->
                            if (zoom < 1.5f) {
                                // 탭 지점을 중심으로 2.5배 확대
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val target = 2.5f
                                val bx = c.x + (tap.x - c.x - pan.x) / zoom
                                val by = c.y + (tap.y - c.y - pan.y) / zoom
                                zoom = target
                                pan = clampPan(
                                    Offset(
                                        tap.x - c.x - (bx - c.x) * target,
                                        tap.y - c.y - (by - c.y) * target
                                    ),
                                    target
                                )
                            } else {
                                // 원위치
                                zoom = 1f
                                pan = Offset.Zero
                            }
                        }
                    )
                }
        ) {
            canvasSize = size

            val base = min(size.width / BusanMapShapes.MAP_W, size.height / BusanMapShapes.MAP_H)
            val dx0 = (size.width - BusanMapShapes.MAP_W * base) / 2f
            val dy0 = (size.height - BusanMapShapes.MAP_H * base) / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // 지도 좌표 → 화면 좌표 (줌/팬 반영: S = C + (B - C)*Z + P)
            fun tx(x: Float): Float {
                val b = dx0 + x * base
                return c.x + (b - c.x) * zoom + pan.x
            }

            fun ty(y: Float): Float {
                val b = dy0 + y * base
                return c.y + (b - c.y) * zoom + pan.y
            }

            // 모서리를 살짝 둥글리는 스무딩 패스
            fun buildPath(pts: List<Pair<Float, Float>>) = Path().apply {
                val n = pts.size
                val r = 0.12f
                fun x(i: Int) = tx(pts[(i + n) % n].first)
                fun y(i: Int) = ty(pts[(i + n) % n].second)
                fun lerpX(i: Int, j: Int, t: Float) = x(i) + (x(j) - x(i)) * t
                fun lerpY(i: Int, j: Int, t: Float) = y(i) + (y(j) - y(i)) * t

                for (i in 0 until n) {
                    val entryX = lerpX(i, i - 1, r)
                    val entryY = lerpY(i, i - 1, r)
                    val exitX = lerpX(i, i + 1, r)
                    val exitY = lerpY(i, i + 1, r)
                    if (i == 0) moveTo(entryX, entryY) else lineTo(entryX, entryY)
                    quadraticBezierTo(x(i), y(i), exitX, exitY)
                }
                close()
            }

            // 1) 구 폴리곤: 채움 + 흰 테두리
            BusanMapShapes.polygons.forEach { (name, parts) ->
                val color = animatedColors[name] ?: Color.LightGray
                parts.forEach { pts ->
                    val path = buildPath(pts)
                    drawPath(path, color = color)
                    drawPath(path, color = Color.White, style = Stroke(width = 2.dp.toPx()))
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
                // 모든 구 동일한 글자색 — 흰 외곽선(halo)이 있어 어떤 배경에서도 잘 보임
                val labelColor = OccupancyTextDarker
                val (cx, cy) = BusanMapShapes.labelCenter(name)
                val center = Offset(tx(cx), ty(cy))
                val tiny = name in BusanMapShapes.tinyDistricts

                // 축소 상태에서 작은 구 라벨은 겹치므로 숨기고, 확대하면 표시
                if (tiny && zoom < 1.8f) return@forEach

                val pct = if (total == 0) "0%" else "${completed * 100 / total}%"
                drawCenteredText(
                    textMeasurer, "$name $pct",
                    center,
                    TextStyle(
                        fontSize = if (tiny) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor
                    )
                )

                val cleared = total > 0 && completed == total
                if (cleared) {
                    drawCenteredText(
                        textMeasurer, "✓",
                        center.copy(y = center.y - 11.dp.toPx()),
                        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = labelColor)
                    )
                } else if (name in inProgressSet) {
                    // 진행 중 점: 흰 테두리 + 코럴 — 배경 농도와 무관하게 동일
                    val dotCenter = center.copy(y = center.y - 10.dp.toPx())
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = dotCenter)
                    drawCircle(Color(0xFFCE504D), radius = 2.5.dp.toPx(), center = dotCenter)
                }
            }
        }

        Text(
            if (zoom > 1.05f) "더블탭으로 원위치 · 드래그로 이동"
            else "핀치·더블탭으로 확대 · 미션을 완료할수록 진해져요",
            fontSize = 11.sp,
            color = TextSub,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 4.dp)
        )
    }
}

// 구별 점령률 → 애니메이션 채움색 (컴포지션 단계에서 계산)
@Composable
private fun rememberAnimatedMapColors(byName: Map<String, DistrictMissionProgress>): Map<String, Color> {
    val colors = mutableMapOf<String, Color>()
    for (name in BusanMapShapes.polygons.keys) {
        val p = byName[name]
        val rate = if (p == null || p.total == 0) 0f else p.completed.toFloat() / p.total
        val color by animateColorAsState(
            targetValue = occupancyColor(rate),
            animationSpec = tween(500),
            label = "map_$name"
        )
        colors[name] = color
    }
    return colors
}

// 텍스트를 중심 좌표 기준으로 그리기
// - 앱 공통 폰트(Pretendard, AppFontFamily) 적용 — Canvas drawText 는 테마를 상속 안 받아서 직접 지정
// - 흰 글로우(그림자)로 배경 대비 확보 — 외곽선 방식과 달리 획이 꽉 찬 채로 또렷함
private fun DrawScope.drawCenteredText(
    measurer: TextMeasurer,
    text: String,
    center: Offset,
    style: TextStyle
) {
    val layout = measurer.measure(
        text,
        style.copy(
            fontFamily = AppFontFamily,
            shadow = Shadow(
                color = Color.White,
                offset = Offset.Zero,
                blurRadius = style.fontSize.toPx() * 0.35f
            )
        )
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            center.x - layout.size.width / 2f,
            center.y - layout.size.height / 2f
        )
    )
}
