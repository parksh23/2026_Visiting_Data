package com.example.busasnquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.ui.theme.*

@Composable
fun SavedMissionScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saved = uiState.savedMissions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = NavyMain)
            }
            Text("찜한 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyMain)
        }

        if (saved.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "아직 찜한 미션이 없어요.\n미션 카드의 하트를 눌러보세요!",
                    color = TextSub,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = Dimens.bottomBarSpace)
            ) {
                item {
                    Text(
                        "찜한 미션 ${saved.size}개",
                        color = TextSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(saved, key = { it.mission.id }) { item ->
                    SavedRow(item) { MissionRepository.toggleSaved(item.mission.id) }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun SavedRow(item: MissionWithState, onUnsave: () -> Unit) {
    val mission = item.mission

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextMain)
            Spacer(modifier = Modifier.height(2.dp))
            Text(mission.region, color = TextSub, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = PointOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("+${mission.reward}P", fontWeight = FontWeight.Bold, color = PointOrange, fontSize = 13.sp)
            }
        }

        // 찜 해제 하트
        Icon(
            Icons.Filled.Favorite,
            contentDescription = "찜 해제",
            tint = PointRed,
            modifier = Modifier
                .size(24.dp)
                .clickable { onUnsave() }
        )
    }
}