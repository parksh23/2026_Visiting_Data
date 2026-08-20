package kr.co.busanquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kr.co.busanquest.ui.theme.*

/** 설정 하위 화면 공용 상단 바 (미션 내역 화면과 같은 톤) */
@Composable
fun SubPageHeader(title: String, navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = NavyMain)
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyMain)
    }
}

/** 흰 카드 + 잉크 아웃라인 (앱 공통 박스) */
@Composable
fun SettingsCard(
    label: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        if (label != null) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextSub,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusCard))
                .background(CardWhite)
                .border(1.5.dp, InkBorder, RoundedCornerShape(Dimens.radiusCard)),
            content = content
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = DividerGray, modifier = Modifier.padding(horizontal = 18.dp))
}

/** 스위치 행 */
@Composable
fun ToggleRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                color = if (enabled) TextMain else TextSub,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Spacer(Modifier.height(3.dp))
                Text(description, fontSize = 12.sp, color = TextSub)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Coral,
                checkedBorderColor = InkBorder,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = TrackGray,
                uncheckedBorderColor = InkBorder
            )
        )
    }
}

/**
 * 값 표시 행.
 * @param pending 서버 연동 전이라 아직 못 쓰는 항목 — 눌리되 안내만 뜬다.
 */
@Composable
fun ValueRow(
    title: String,
    value: String? = null,
    pending: Boolean = false,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 15.sp,
            color = if (pending) TextSub else TextMain,
            modifier = Modifier.weight(1f)
        )
        if (pending) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SurfaceGray)
                    .border(1.dp, DividerGray, RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text("준비 중", fontSize = 11.sp, color = TextSub, fontWeight = FontWeight.Medium)
            }
        } else if (value != null) {
            Text(value, fontSize = 14.sp, color = TextSub)
        }
        if (showChevron && onClick != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSub,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
