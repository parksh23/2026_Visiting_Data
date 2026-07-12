package com.example.busasnquest.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.busasnquest.ui.theme.*

@Composable
fun BottomNavigationBar(navController: NavHostController) {

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(CardWhite)
            .border(1.dp, DividerGray, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BottomItem("홈", Icons.Outlined.Home, currentRoute == "home") { navigateTab("home") }

        BottomItem("미션", Icons.Outlined.Flag, currentRoute == "mission") { navigateTab("mission") }

        BottomItem(
            "지도",
            Icons.Outlined.Map,
            currentRoute?.startsWith("map") == true
        ) { navigateTab("map/부산") }

        BottomItem("랭킹", Icons.Outlined.EmojiEvents, currentRoute == "ranking") { navigateTab("ranking") }

        BottomItem("내 정보", Icons.Outlined.Person, currentRoute == "profile") { navigateTab("profile") }
    }
}

@Composable
fun BottomItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) Coral else TextSub

    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = title, tint = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            fontSize = 11.sp,
            color = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
