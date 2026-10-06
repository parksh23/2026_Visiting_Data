package kr.co.busanquest.ui.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.co.busanquest.ui.theme.CardWhite
import kr.co.busanquest.ui.theme.TextMain
import kr.co.busanquest.ui.theme.TextSub

/** 서버 tourism_scoring.py의 점수 정책이 바뀌면 이 안내도 함께 갱신한다. */
@Composable
internal fun ScoreGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("점수 산정 기준", color = TextMain) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GuideBody("미션을 완료하면 아래 기준으로 포인트를 받아요.")
                GuideHeading("획득 포인트 = 기본 점수 + 지역 보너스 + 접근성 보너스")
                GuideHeading("① 기본 점수")
                GuideTable(
                    "미션 종류", "점수",
                    listOf(
                        "장소탐방" to "100P",
                        "먹거리" to "120P",
                        "산책·트레킹" to "130P",
                        "문화·체험" to "140P",
                        "경기·공연" to "140P",
                        "등산" to "200P"
                    )
                )
                GuideHeading("② 지역 보너스 · 최대 30P")
                GuideBody("관광 활성화가 더 필요한 지역을 방문하면 추가 포인트를 받아요. 지역별 관광 체류·소비 지수를 각각 50%씩 반영하며, 상대적으로 지수가 낮은 지역일수록 보너스가 높아요.")
                GuideHeading("③ 접근성 보너스 · 최대 20P")
                GuideBody("미션 장소가 대중교통에서 멀수록 추가 포인트를 받아요.")
                GuideTable(
                    "가장 가까운 버스정류장까지 거리", "보너스",
                    listOf(
                        "100m 이내" to "5P",
                        "100m 초과 ~ 200m 이내" to "10P",
                        "200m 초과 ~ 400m 이내" to "15P",
                        "400m 초과" to "20P"
                    )
                )
                GuideBody("단, 지하철역이 400m 이내면 0P, 400m 초과 ~ 800m 이내면 최대 10P가 적용돼요.")
                GuideHeading("꼭 확인해 주세요")
                GuideBody("• 등산 미션은 보너스 없이 200P 고정이에요.")
                GuideBody("• 지역 보너스는 관광 데이터 갱신에 따라 달라질 수 있어요.")
                GuideBody("• 포인트 랭킹은 누적 포인트, 지역 랭킹은 해당 지역에서 완료한 미션 수를 기준으로 해요.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("확인") }
        }
    )
}

@Composable
private fun GuideHeading(text: String) {
    Text(text, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

@Composable
private fun GuideBody(text: String) {
    Text(text, color = TextSub, fontSize = 14.sp, lineHeight = 21.sp)
}

@Composable
private fun GuideTable(label: String, value: String, rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, Modifier.weight(1f), color = TextMain, fontWeight = FontWeight.Bold)
            Text(value, color = TextMain, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider()
        rows.forEach { (name, points) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(name, Modifier.weight(1f), color = TextSub, fontSize = 14.sp)
                Text(points, color = TextMain, fontSize = 14.sp)
            }
        }
    }
}
