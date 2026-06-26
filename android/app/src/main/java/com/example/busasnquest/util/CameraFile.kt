package com.example.busasnquest.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// 사진을 저장할 빈 파일을 만들고, 카메라에 건넬 Uri를 돌려준다.
fun createImageUri(context: Context): Uri {
    // 캐시 폴더 안 images/ 디렉터리 준비
    val imagesDir = File(context.cacheDir, "images")
    imagesDir.mkdirs()

    // 빈 사진 파일 생성 (이름은 시간으로 겹치지 않게)
    val imageFile = File(imagesDir, "receipt_${System.currentTimeMillis()}.jpg")

    // FileProvider를 통해 안전한 Uri로 변환해서 돌려줌
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",  // 매니페스트의 authorities와 일치
        imageFile
    )
}