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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.ui.home.HomeViewModel

private enum class PendingLocationAction { PHOTO, CURRENT_LOCATION }
private enum class PendingCameraAction { PHOTO, RECEIPT }

/**
 * 미션 인증(사진/위치/영수증)에 필요한 런처들을 한 곳에 묶은 헬퍼.
 * 화면에서 `val verify = rememberMissionVerifier(viewModel)` 처럼 부르고,
 * `verify(missionId, missionType)` 으로 인증을 시작한다.
 *
 * 사진과 영수증 미션은 "카메라로 촬영 / 갤러리에서 선택"을 고를 수 있다.
 * 사진 미션은 선택한 파일의 EXIF 대신 인증 시점의 현재 위치를 사용한다.
 */
@Composable
fun rememberMissionVerifier(
    viewModel: HomeViewModel
): (Int, MissionType) -> Unit {

    val context = LocalContext.current

    // 어느 미션이 인증을 요청했는지 기억
    val activeId = remember { mutableIntStateOf(0) }
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }
    val pendingPhotoUri = remember { mutableStateOf<Uri?>(null) }
    val pendingLocationAction = remember { mutableStateOf<PendingLocationAction?>(null) }
    val pendingCameraAction = remember { mutableStateOf<PendingCameraAction?>(null) }
    val mediaChooser = remember { mutableStateOf<PendingCameraAction?>(null) }

    val locationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val action = pendingLocationAction.value
        pendingLocationAction.value = null
        if (granted) {
            when (action) {
                PendingLocationAction.PHOTO -> pendingPhotoUri.value?.let { uri ->
                    viewModel.onPhotoSelected(activeId.intValue, context, uri)
                }
                PendingLocationAction.CURRENT_LOCATION ->
                    viewModel.onLocationPermissionGranted(activeId.intValue, context)
                null -> Unit
            }
        } else {
            viewModel.onLocationPermissionDenied(activeId.intValue)
        }
        pendingPhotoUri.value = null
    }

    fun startPhotoVerification(uri: Uri) {
        pendingPhotoUri.value = uri
        pendingLocationAction.value = PendingLocationAction.PHOTO
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onPhotoSelected(activeId.intValue, context, uri)
            pendingLocationAction.value = null
            pendingPhotoUri.value = null
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) startPhotoVerification(uri)
    }

    // 영수증: 갤러리에서 이미지 선택 (GPS 불필요 → 영수증 처리로 바로 전달)
    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onReceiptCaptured(activeId.intValue, context, true, uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri.value
        if (success && uri != null) {
            when (pendingCameraAction.value) {
                PendingCameraAction.PHOTO -> startPhotoVerification(uri)
                PendingCameraAction.RECEIPT ->
                    viewModel.onReceiptCaptured(activeId.intValue, context, true, uri)
                null -> Unit
            }
        }
        pendingCameraUri.value = null
        pendingCameraAction.value = null
    }

    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = com.example.busasnquest.util.createImageUri(context)
            pendingCameraUri.value = uri
            cameraLauncher.launch(uri)
        } else viewModel.onCameraPermissionDenied(activeId.intValue)
    }

    fun startCamera(action: PendingCameraAction) {
        pendingCameraAction.value = action
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = com.example.busasnquest.util.createImageUri(context)
            pendingCameraUri.value = uri
            cameraLauncher.launch(uri)
        } else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    fun startGallery(action: PendingCameraAction) {
        val launcher = if (action == PendingCameraAction.PHOTO) photoPicker else receiptPicker
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    mediaChooser.value?.let { action ->
        val title = if (action == PendingCameraAction.PHOTO) "사진 인증" else "영수증 인증"
        AlertDialog(
            onDismissRequest = { mediaChooser.value = null },
            title = { Text(title) },
            text = { Text("이미지를 어떻게 올릴까요?") },
            confirmButton = {
                TextButton(onClick = {
                    mediaChooser.value = null
                    startCamera(action)
                }) { Text("카메라로 촬영") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mediaChooser.value = null
                    startGallery(action)
                }) { Text("갤러리에서 선택") }
            }
        )
    }

    // 화면이 호출할 함수: 미션 id와 타입을 주면 알맞은 인증을 시작
    return { id, type ->
        activeId.intValue = id
        when (type) {
            MissionType.IMAGE_LOCATION -> mediaChooser.value = PendingCameraAction.PHOTO
            MissionType.CURRENT_LOCATION -> {
                pendingLocationAction.value = PendingLocationAction.CURRENT_LOCATION
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    pendingLocationAction.value = null
                    viewModel.onLocationPermissionGranted(id, context)
                } else {
                    locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            MissionType.RECEIPT -> mediaChooser.value = PendingCameraAction.RECEIPT
        }
    }
}
