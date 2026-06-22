import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// === Read secrets.properties ===
fun loadSecrets(file: File): Properties {
    return Properties().apply {
        if (file.exists()) {
            file.inputStream().use { load(it) }
        }
    }
}
val secrets = loadSecrets(rootProject.file("secrets.properties"))

android {
    namespace = "com.aichat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aichat"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // === API keys from secrets.properties → BuildConfig ===
        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${secrets.getProperty("OPENROUTER_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "DEEPSEEK_API_KEY",
            "\"${secrets.getProperty("DEEPSEEK_API_KEY", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:di"))
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":features:chat"))
    implementation(project(":features:chats"))
    implementation(project(":features:models"))
    implementation(project(":features:chat-create"))
    implementation(project(":features:settings"))
    implementation(project(":features:profile"))

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}