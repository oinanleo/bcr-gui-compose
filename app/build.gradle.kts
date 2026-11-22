plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.leo.bcrgui"
    compileSdk = 37
    compileSdkMinor = 1
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.leo.bcrgui"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "0.1-slice"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.compose.ui:ui:1.13.0")
    implementation("androidx.compose.ui:ui-graphics:1.13.0")
    implementation("androidx.compose.foundation:foundation:1.13.0")
    implementation("androidx.compose.material3:material3:1.5.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation(files("libs/sherpa-onnx-2.0.2.aar"))
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
