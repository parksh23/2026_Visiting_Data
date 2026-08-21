plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // google-services 는 여기서 선언하지 않는다.
    // 루트 build.gradle.kts 가 apply false 로 클래스패스에만 올려 두었고,
    // 실제 적용은 아래 android{} 블록 뒤에서 조건부로 한다 (google-services.json 유무).
}

android {
    namespace = "kr.co.busanquest"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "kr.co.busanquest"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            // 카카오맵 네이티브 라이브러리(libK3fAndroid.so 등)를 설치 시 풀어주도록 설정
            // (ReLinker가 .so 파일을 찾을 수 있게 함 → MissingLibraryException 해결)
            useLegacyPackaging = true
        }
    }
}

// google-services 플러그인은 app/google-services.json 이 없으면 빌드를 실패시킨다.
// Firebase 콘솔에서 파일을 받기 전에도 프로젝트가 빌드되어야 하므로, 파일이 있을 때만 적용한다.
// 파일을 app/ 아래에 넣는 순간 자동으로 켜지고, FCM 토큰 등록이 동작하기 시작한다.
val googleServicesJson = project.file("google-services.json")
if (googleServicesJson.exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    // ⚠️ 한글로 쓰면 PowerShell(cp949)에서 깨져 읽을 수 없다 — ASCII 로 남긴다
    logger.lifecycle(
        "[BusanQuest] app/google-services.json not found - FCM disabled. " +
            "Drop the file in app/ and rebuild to enable server push."
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Compose Material Icons (버전 명시)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle (ViewModel + Compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // Retrofit / 네트워크
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore (토큰 저장)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 사진 / 위치 (CurrentLocation, PhotoLocation 용)
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("com.kakao.maps.open:android:2.12.18")

    // 카카오 로그인 (카카오맵과 같은 네이티브 앱 키 사용)
    implementation("com.kakao.sdk:v2-user:2.20.1")

    // 이미지 로딩 (미션 히어로 카드 image_url)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // FCM 서버 푸시 — google-services.json 이 없어도 컴파일은 된다.
    // (없으면 런타임에 FirebaseApp 초기화가 실패할 뿐이라 PushRegistrar 가 조용히 넘어간다)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}

