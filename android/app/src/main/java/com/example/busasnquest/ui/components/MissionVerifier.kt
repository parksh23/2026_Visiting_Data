package com.example.busasnquest.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.ui.home.HomeViewModel

/**
 * 미션 인증(사진/위치/영수증)에 필요한 런처들을 한 곳에 묶은 헬퍼.
 * 화면에서 `val verify = rememberMissionVerifier(viewModel)` 처럼 부르고,
 * `verify(missionId, missionType)` 으로 인증을 시작한다.
 *
 * 영수증(RECEIPT) 미션은 바로 카메라로 가지 않고,
 * "카메라로 촬영 / 갤러리에서 선택" 을 고르는 다이얼로그를 먼저 띄운다.
 */
@Composable
fun rememberMissionVerifier(
    viewModel: HomeViewModel
): (Int, MissionType) -> Unit {

    val context = LocalContext.current

    // 어느 미션이 인증을 요청했는지 기억
    val activeId = remember { mutableStateOf(0) }
    val pendingReceiptUri = remember { mutableStateOf<Uri?>(null) }
    // 영수증 인증 방식(촬영/갤러리) 선택 다이얼로그 표시 여부
    val showReceiptChooser = remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onPhotoPicked(activeId.value, context, uri)
    }

    // 영수증: 갤러리에서 이미지 선택 (GPS 불필요 → 영수증 처리로 바로 전달)
    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onReceiptCaptured(activeId.value, context, true, uri)
    }

    val locationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onLocationPermissionGranted(activeId.value, context)
        else viewModel.onLocationPermissionDenied(activeId.value)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // 촬영한 영수증 이미지 uri 를 함께 넘겨 서버 인증에 사용
        viewModel.onReceiptCaptured(
            activeId.value,
            context,
            success,
            pendingReceiptUri.value
        )
    }

    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = com.example.busasnquest.util.createImageUri(context)
            pendingReceiptUri.value = uri
            cameraLauncher.launch(uri)
        } else viewModel.onCameraPermissionDenied(activeId.value)
    }

    // 영수증: 카메라 촬영 시작 (권한 확인 포함)
    fun startReceiptCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = com.example.busasnquest.util.createImageUri(context)
            pendingReceiptUri.value = uri
            cameraLauncher.launch(uri)
        } else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    // 영수증: 갤러리에서 이미지 선택 시작
    fun startReceiptGallery() {
        receiptPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // 영수증 인증 방식 선택 다이얼로그
    if (showReceiptChooser.value) {
        AlertDialog(
            onDismissRequest = { showReceiptChooser.value = false },
            title = { Text("영수증 인증") },
            text = { Text("영수증을 어떻게 올릴까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showReceiptChooser.value = false
                    startReceiptCamera()
                }) { Text("카메라로 촬영") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReceiptChooser.value = false
                    startReceiptGallery()
                }) { Text("갤러리에서 선택") }
            }
        )
    }

    // 화면이 호출할 함수: 미션 id와 타입을 주면 알맞은 인증을 시작
    return { id, type ->
        activeId.value = id
        when (type) {
            MissionType.PHOTO_LOCATION -> photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            MissionType.CURRENT_LOCATION -> {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.onLocationPermissionGranted(id, context)
                else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // 촬영/갤러리 선택 다이얼로그를 띄운다
            MissionType.RECEIPT -> showReceiptChooser.value = true
        }
    }
}
