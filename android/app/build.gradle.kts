import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Sideload imzası:
//  - CI'da KEYSTORE_BASE64 / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD secret'ları varsa onlar kullanılır.
//  - Yoksa repo içindeki keystore/sideload.jks kullanılır (kişisel sideload için, Play Store için değil).
val sideloadStore = rootProject.file("keystore/sideload.jks")
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/sideload.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

fun prop(name: String, fallback: String): String =
    System.getenv(name) ?: keystoreProps.getProperty(name) ?: fallback

android {
    namespace = "com.bigfalkon.karakterevreni"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bigfalkon.karakterevreni"
        minSdk = 24
        targetSdk = 35
        // CI her derlemede artan bir sürüm kodu geçer (ORG_GRADLE_PROJECT_versionCodeOverride).
        versionCode = (project.findProperty("versionCodeOverride") as String?)?.toIntOrNull() ?: 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("sideload") {
            val envStore = System.getenv("KEYSTORE_FILE")?.let { file(it) }
            storeFile = envStore ?: sideloadStore
            storePassword = prop("KEYSTORE_PASSWORD", "karakterevreni")
            keyAlias = prop("KEY_ALIAS", "sideload")
            keyPassword = prop("KEY_PASSWORD", "karakterevreni")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes += setOf("META-INF/*.version", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.browser:browser:1.8.0")
}
