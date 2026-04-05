import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dagger.hilt.android)
    kotlin("kapt")
}

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("key.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}
val apkBaseName = "LongTV"
val apkVersionName = "1.0"
val apkTimestamp: String
    get() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

android {
    namespace = "com.loong.longtv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loong.longtv"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
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
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "META-INF/**"
        }
    }
}

val releaseApkDir = layout.buildDirectory.dir("outputs/apk/release")
val renameReleaseApk = tasks.register("renameReleaseApk") {
    dependsOn("assembleRelease")
    doLast {
        val apkDir = releaseApkDir.get().asFile
        val sourceApk = apkDir.resolve("app-release.apk")
        if (!sourceApk.exists()) return@doLast

        apkDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("${apkBaseName}_v") && it.extension == "apk" }
            ?.forEach { it.delete() }

        val targetApk = apkDir.resolve("${apkBaseName}_v${apkVersionName}_release_${apkTimestamp}.apk")
        check(sourceApk.renameTo(targetApk)) {
            "Failed to rename ${sourceApk.absolutePath} to ${targetApk.absolutePath}"
        }
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(renameReleaseApk)
}

dependencies {
    implementation(project(":spider"))

    implementation(libs.junit)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.sandwich)
    implementation(libs.sandwich.retrofit)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.hilt.android)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.converter.gson)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
