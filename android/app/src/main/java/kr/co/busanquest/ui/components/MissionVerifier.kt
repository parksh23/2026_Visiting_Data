package kr.co.busanquest.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import kr.co.busanquest.data.model.MissionType
import kr.co.busanquest.ui.home.HomeViewModel

private enum class PendingLocationAction { PICK_PHOTO, VERIFY_CURRENT_LOCATION }

/**
 * 미션 인증(사진/위치/영수증)에 필요한 런처들을 한 곳에 묶은 헬퍼.
 * 화면에서 `val verify = rememberMissionVerifier(viewModel)` 처럼 부르고,
 * `verify(missionId, missionType)` 으로 인증을 시작한다.
 *
 * 영수증(RECEIPT) 미션은 바로 카메라로 가지 않고,
 * "카메라로 촬영 / 갤러리에서 선택" 을 고르는 다이얼로그를 먼저 띄운다.
 *
 * 사진(PHOTO) 미션은 사진을 고르기 전에 위치 권한을 먼저 받는다.
 * 앱이 현재 위치와 미션 장소를 기기에서 판정한 뒤 좌표 없이 인증 요청을 보내기 때문이다.
 */
@Composable
fun rememberMissionVerifier(
    viewModel: HomeViewModel
): (Int, MissionType) -> Unit {

    val context = LocalContext.current

    // 어느 미션이 인증을 요청했는지 기억
    val activeId = remember { mutableStateOf(0) }
    val pendingReceiptUri = remember { mutableStateOf<Uri?>(null) }
    val pendingLocationAction = remember { mutableStateOf<PendingLocationAction?>(null) }
    // 영수증 인증 방식(촬영/갤러리) 선택 다이얼로그 표시 여부
    val showReceiptChooser = remember { mutableStateOf(false) }
    val showLocationDisclosure = remember { mutableStateOf(false) }
    val showLocationSettings = remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onImagePicked(activeId.value, context, uri)
    }

    // 영수증: 갤러리에서 이미지 선택 (GPS 불필요 → 영수증 처리로 바로 전달)
    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onReceiptCaptured(activeId.value, context, true, uri)
    }

    // Android 12+ 에서는 대략적/정확한 위치 선택을 위해 두 권한을 함께 요청해야 한다.
    val locationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val preciseGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (preciseGranted) {
            when (pendingLocationAction.value) {
                PendingLocationAction.PICK_PHOTO -> imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                PendingLocationAction.VERIFY_CURRENT_LOCATION ->
                    viewModel.onLocationPermissionGranted(activeId.value, context)
                null -> Unit
            }
        } else {
            viewModel.onLocationPermissionDenied(activeId.value)
            showLocationSettings.value = true
        }
        pendingLocationAction.value = null
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

    // 외부 기본 카메라 앱을 호출하므로 CAMERA 런타임 권한이 필요하지 않다.
    fun startReceiptCamera() {
        val uri = kr.co.busanquest.util.createImageUri(context)
        pendingReceiptUri.value = uri
        cameraLauncher.launch(uri)
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

    if (showLocationDisclosure.value) {
        LocationPermissionDisclosureDialog(
            onAllow = {
                showLocationDisclosure.value = false
                locationPermission.launch(arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ))
            },
            onCancel = {
                showLocationDisclosure.value = false
                pendingLocationAction.value = null
                viewModel.onLocationPermissionDenied(activeId.value)
            },
        )
    }

    if (showLocationSettings.value) {
        PermissionSettingsDialog(
            title = "정확한 위치 권한이 필요해요",
            message = "위치·사진 미션 인증을 사용하려면 기기 설정에서 정확한 위치 권한을 허용해주세요. 다른 앱 기능은 계속 이용할 수 있습니다.",
            onOpenSettings = {
                showLocationSettings.value = false
                context.openAppPermissionSettings()
            },
            onCancel = { showLocationSettings.value = false },
        )
    }

    // 화면이 호출할 함수: 미션 id와 타입을 주면 알맞은 인증을 시작
    return { id, type ->
        activeId.value = id
        when (type) {
            MissionType.IMAGE_LOCATION -> {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    pendingLocationAction.value = PendingLocationAction.PICK_PHOTO
                    showLocationDisclosure.value = true
                }
            }
            MissionType.CURRENT_LOCATION -> {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.onLocationPermissionGranted(id, context)
                else {
                    pendingLocationAction.value = PendingLocationAction.VERIFY_CURRENT_LOCATION
                    showLocationDisclosure.value = true
                }
            }
            // 촬영/갤러리 선택 다이얼로그를 띄운다
            MissionType.RECEIPT -> showReceiptChooser.value = true
        }
    }
}

private fun android.content.Context.openAppPermissionSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
