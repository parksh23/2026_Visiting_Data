package com.example.busasnquest.ui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.busasnquest.ui.theme.*

/**
 * 약관·방침 공용 뷰어.
 *
 * 문서는 assets/{slug}.md 에서 읽는다. 서버에 GET /api/v1/documents/{slug} 가 생기면
 * loadDocument() 안에서 "서버 우선 → 실패 시 assets 폴백" 으로만 바꾸면 되고
 * 화면 코드는 그대로 쓸 수 있다.
 *
 * 문서 형식:
 *   # 제목
 *   version: 1.0
 *   effective: 2026-08-11
 *   ---
 *   ## 제1조 (목적)
 *   본문...
 */
private data class AppDocument(
    val title: String,
    val version: String,
    val effective: String,
    val body: List<String>
)

@Composable
fun DocumentScreen(navController: NavHostController, slug: String) {
    val context = LocalContext.current
    val document by remember(slug) { mutableStateOf(context.loadDocument(slug)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.bottomBarClearance)
    ) {
        SubPageHeader(document?.title ?: "문서", navController)

        if (document == null) {
            Spacer(Modifier.height(40.dp))
            Text(
                "문서를 불러오지 못했어요.",
                fontSize = 14.sp,
                color = TextSub,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            return@Column
        }

        Spacer(Modifier.height(4.dp))

        // 버전·시행일 (개정 이력 확인용 — 약관 표기에 필요)
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusChip))
                .background(SurfaceGray)
                .border(1.dp, DividerGray, RoundedCornerShape(Dimens.radiusChip))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("버전 ${document!!.version}", fontSize = 12.sp, color = TextSub)
            Spacer(Modifier.weight(1f))
            Text("시행일 ${document!!.effective}", fontSize = 12.sp, color = TextSub)
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusCard))
                .background(CardWhite)
                .border(1.5.dp, InkBorder, RoundedCornerShape(Dimens.radiusCard))
                .padding(18.dp)
        ) {
            document!!.body.forEach { line -> DocumentLine(line) }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 아주 단순한 마크다운 렌더링 (##, -, >, 빈 줄만 처리) */
@Composable
private fun DocumentLine(raw: String) {
    val line = raw.trimEnd()
    when {
        line.isBlank() -> Spacer(Modifier.height(10.dp))

        line.startsWith("## ") -> {
            Spacer(Modifier.height(8.dp))
            Text(
                line.removePrefix("## "),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(6.dp))
        }

        line.startsWith("- ") -> Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text("·", fontSize = 13.sp, color = TextSub)
            Spacer(Modifier.width(8.dp))
            Text(line.removePrefix("- "), fontSize = 13.sp, color = TextSub, lineHeight = 21.sp)
        }

        line.startsWith("> ") -> Box(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CoralTint)
                .padding(12.dp)
        ) {
            Text(line.removePrefix("> "), fontSize = 12.sp, color = CoralInk, lineHeight = 19.sp)
        }

        else -> Text(line, fontSize = 13.sp, color = TextSub, lineHeight = 21.sp)
    }
}

private fun Context.loadDocument(slug: String): AppDocument? = runCatching {
    val text = assets.open("$slug.md").bufferedReader().use { it.readText() }
    val lines = text.lines()
    val separator = lines.indexOfFirst { it.trim() == "---" }
    val head = if (separator >= 0) lines.take(separator) else emptyList()
    val body = if (separator >= 0) lines.drop(separator + 1) else lines

    AppDocument(
        title = head.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim() ?: "문서",
        version = head.firstOrNull { it.startsWith("version:") }
            ?.removePrefix("version:")?.trim() ?: "-",
        effective = head.firstOrNull { it.startsWith("effective:") }
            ?.removePrefix("effective:")?.trim() ?: "-",
        body = body
    )
}.getOrNull()
