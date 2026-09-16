plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.xrealcanvas.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xrealcanvas.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}
