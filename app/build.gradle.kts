plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //Room
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

android {
    namespace = "ddwu.com.mobile.a01_20230820"
    compileSdk = 36

    defaultConfig {
        applicationId = "ddwu.com.mobile.a01_20230820"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = project.findProperty("GOOGLE_MAPS_API_KEY")?.toString() ?: ""
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
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding.enable = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ROOOM
    val room_version = "2.7.2"

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Coroutine + Flow 지원
    ksp("androidx.room:room-compiler:$room_version")

    // Google play service 위치 관련 정보 추가
    implementation ("com.google.android.gms:play-services-location:21.3.0")

    // GoogleMap 관련 정보 추가
    implementation("com.google.android.gms:play-services-maps:19.2.0")


}