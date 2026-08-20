package kr.co.busanquest.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kr.co.busanquest.R
import kr.co.busanquest.ui.theme.*

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }

    // 플로팅 흰색 라운드 바
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 떠 있는 탭바: 드롭섀도 + 상하 그라데이션 + 가장자리 하이라이트
                .raisedSurface(RoundedCornerShape(Dimens.radiusPill), elevation = 10.dp)
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem("홈", R.drawable.ic_nav_home,
                currentRoute == "home") { navigateTab("home") }

            BottomItem("미션", R.drawable.ic_nav_flag,
                currentRoute == "mission") { navigateTab("mission") }

            BottomItem("지도", R.drawable.ic_nav_map,
                currentRoute?.startsWith("map") == true) { navigateTab("map/부산") }

            BottomItem("랭킹", R.drawable.ic_nav_trophy,
                currentRoute == "ranking") { navigateTab("ranking") }

            BottomItem("내 정보", R.drawable.ic_nav_person,
                currentRoute == "profile") { navigateTab("profile") }
        }
    }
}

@Composable
private fun BottomItem(
    title: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    // 탭 전환은 하루에 수십 번 일어나는 동작이다.
    // 스킬 기준상 이 빈도에서는 "거의 감지되지 않을 정도"만 허용 → 위치 이동·팝 없이 색만 넘긴다.
    val pillBg by animateColorAsState(
        targetValue = if (selected) CoralTint else Color.Transparent,
        animationSpec = tween(Motion.DurPress, easing = Motion.EaseOut),
        label = "tabPill"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Coral else TextSub,
        animationSpec = tween(Motion.DurPress, easing = Motion.EaseOut),
        label = "tabFg"
    )

    Column(
        modifier = Modifier
            // 눌림은 아주 얕게 (0.97 은 탭바에서 과하게 튄다)
            .pressable(scaleDown = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 활성 탭은 코럴 틴트 알약 하이라이트
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.radiusChip + 4.dp))
                .background(pillBg)
                .padding(horizontal = 18.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = if (selected) CoralDark else TextSub,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            title,
            fontSize = 11.sp,
            color = if (selected) CoralDark else TextSub,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
