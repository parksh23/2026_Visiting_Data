package com.example.busasnquest.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busasnquest.data.local.AppDocuments
import com.example.busasnquest.data.remote.AgreementDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 회원가입 약관 동의 상태.
 *
 * 화면에서 rememberAgreementState() 로 만들어 쓰고,
 * 가입/카카오 로그인 요청 시 toDtoList() 로 서버 전송용 이력을 만든다.
 */
class AgreementState(
    private val versions: Map<String, String>
) {
    // slug -> 동의 여부 (Compose 스냅샷 맵이라 값이 바뀌면 화면이 다시 그려진다)
    private val checked = mutableStateMapOf<String, Boolean>()

    val allRequiredAgreed: Boolean
        get() = AppDocuments.requiredSlugs.all { checked[it] == true }

    fun isAgreed(slug: String): Boolean = checked[slug] == true

    fun toggle(slug: String) {
        checked[slug] = !(checked[slug] ?: false)
    }

    fun setAll(value: Boolean) {
        AppDocuments.requiredSlugs.forEach { checked[it] = value }
    }

    fun versionOf(slug: String): String = versions[slug] ?: "-"

    /** 서버로 보낼 동의 이력. 동의 시각은 전송 시점(UTC ISO-8601). */
    fun toDtoList(): List<AgreementDto> {
        val now = isoUtcNow()
        return AppDocuments.requiredSlugs.map { slug ->
            AgreementDto(
                doc = slug,
                version = versionOf(slug),
                agreed = isAgreed(slug),
                agreedAt = now
            )
        }
    }

    private fun isoUtcNow(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    companion object {
        /**
         * 약관 '보기'로 문서 화면에 다녀오면 로그인 화면 컴포지션이 사라졌다 다시 만들어진다.
         * 그때 체크가 풀리지 않도록 동의한 문서 목록을 저장/복원한다.
         * (Bundle 에 안전하게 담기도록 쉼표로 이어붙인 문자열로 보관)
         */
        fun saver(versions: Map<String, String>): Saver<AgreementState, String> = Saver(
            save = { state ->
                AppDocuments.requiredSlugs.filter { state.isAgreed(it) }.joinToString(",")
            },
            restore = { saved ->
                AgreementState(versions).apply {
                    saved.split(",")
                        .filter { it.isNotBlank() }
                        .forEach { slug -> checked[slug] = true }
                }
            }
        )
    }
}

@Composable
fun rememberAgreementState(): AgreementState {
    val context = LocalContext.current
    // 문서 버전은 assets 에서 한 번만 읽는다
    val versions = remember {
        AppDocuments.requiredSlugs.associateWith { slug -> AppDocuments.version(context, slug) }
    }
    // rememberSaveable — 약관 전문을 보고 돌아와도 체크가 유지된다
    return rememberSaveable(saver = AgreementState.saver(versions)) {
        AgreementState(versions)
    }
}

/**
 * 전체동의 + 개별 필수 항목 3개.
 *
 * @param onOpenDocument 항목 오른쪽 '보기'를 눌렀을 때 문서 전문으로 이동 (slug 전달)
 */
@Composable
fun AgreementSection(
    state: AgreementState,
    onOpenDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color,
    borderColor: Color,
    labelColor: Color,
    subColor: Color
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // ── 전체 동의 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { state.setAll(!state.allRequiredAgreed) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckDot(checked = state.allRequiredAgreed, accent = accent, borderColor = borderColor, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                "약관에 전체 동의합니다",
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = borderColor)
        Spacer(Modifier.height(6.dp))

        // ── 개별 항목 ──
        AppDocuments.requiredSlugs.forEach { slug ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { state.toggle(slug) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckDot(
                        checked = state.isAgreed(slug),
                        accent = accent,
                        borderColor = borderColor,
                        size = 18.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "(필수) ${AppDocuments.title(slug)}",
                        color = if (state.isAgreed(slug)) labelColor else subColor,
                        fontSize = 13.sp
                    )
                }

                Text(
                    "보기",
                    color = subColor,
                    fontSize = 12.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { onOpenDocument(slug) }
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = subColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** 원형 체크 표시 — 체크되면 액센트색으로 채운다. */
@Composable
private fun CheckDot(
    checked: Boolean,
    accent: Color,
    borderColor: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) accent else Color.Transparent)
            .border(1.5.dp, if (checked) accent else borderColor, CircleShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}
