package kr.co.busanquest.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kr.co.busanquest.ui.theme.Coral
import kr.co.busanquest.ui.theme.CoralDark
import kr.co.busanquest.ui.theme.CoralTint
import kr.co.busanquest.ui.theme.Dimens
import kr.co.busanquest.ui.theme.Motion
import kr.co.busanquest.ui.theme.TextSub
import kr.co.busanquest.ui.theme.pressable
import kr.co.busanquest.ui.theme.raisedSurface

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    // 플로팅 흰색 라운드 바
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = 14.dp,
                vertical = 10.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .raisedSurface(
                    RoundedCornerShape(Dimens.radiusPill),
                    elevation = 10.dp
                )
                .padding(
                    horizontal = 6.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomItem(
                title = "홈",
                iconRes = R.drawable.ic_nav_home,
                selected = currentRoute == "home",
                modifier = Modifier.weight(1f)
            ) {
                navigateTab("home")
            }

            BottomItem(
                title = "미션",
                iconRes = R.drawable.ic_nav_flag,
                selected = currentRoute == "mission",
                modifier = Modifier.weight(1f)
            ) {
                navigateTab("mission")
            }

            BottomItem(
                title = "지도",
                iconRes = R.drawable.ic_nav_map,
                selected = currentRoute?.startsWith("map") == true,
                modifier = Modifier.weight(1f)
            ) {
                navigateTab("map/부산")
            }

            BottomItem(
                title = "랭킹",
                iconRes = R.drawable.ic_nav_trophy,
                selected = currentRoute == "ranking",
                modifier = Modifier.weight(1f)
            ) {
                navigateTab("ranking")
            }

            BottomItem(
                title = "내 정보",
                iconRes = R.drawable.ic_nav_person,
                selected = currentRoute == "profile",
                modifier = Modifier.weight(1f)
            ) {
                navigateTab("profile")
            }
        }
    }
}

@Composable
private fun BottomItem(
    title: String,
    iconRes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    // 탭 전환 시 위치 이동 없이 색상만 자연스럽게 변경
    val pillBg by animateColorAsState(
        targetValue = if (selected) {
            CoralTint
        } else {
            Color.Transparent
        },
        animationSpec = tween(
            durationMillis = Motion.DurPress,
            easing = Motion.EaseOut
        ),
        label = "tabPill"
    )

    val fg by animateColorAsState(
        targetValue = if (selected) {
            Coral
        } else {
            TextSub
        },
        animationSpec = tween(
            durationMillis = Motion.DurPress,
            easing = Motion.EaseOut
        ),
        label = "tabFg"
    )

    Column(
        modifier = modifier
            .pressable(
                scaleDown = 0.94f,
                onClick = onClick
            )
            .clip(
                RoundedCornerShape(Dimens.radiusCard)
            )
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 활성 탭 배경
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        Dimens.radiusChip + 4.dp
                    )
                )
                .background(pillBg)
                .padding(
                    horizontal = 12.dp,
                    vertical = 5.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = if (selected) {
                    CoralDark
                } else {
                    fg
                },
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = title,
            fontSize = 11.sp,
            color = if (selected) {
                CoralDark
            } else {
                fg
            },
            fontWeight = if (selected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            maxLines = 1,
            softWrap = false
        )
    }
}