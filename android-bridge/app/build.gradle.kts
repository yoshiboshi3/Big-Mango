plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xrealcanvas.proof"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xrealcanvas.scottishtarge.proof"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-proof"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":bridge"))
}
