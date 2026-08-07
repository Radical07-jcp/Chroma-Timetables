plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jpagdi.cromascheduler"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jpagdi.cromascheduler"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0" // Phase 1: architecture skeleton, not yet a usable app
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    // XML/View-based screens (Home, Timetable Detail, nav drawer) alongside the existing Compose
    // screens hosted by ComposeHostActivity — see MIXED_UI.md for why the app is intentionally
    // mixed rather than converting everything to one or the other.
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
}
