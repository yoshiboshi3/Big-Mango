plugins {
    id("com.android.application")
}

android {
    namespace = "com.ifoldnt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ifoldnt"
        minSdk = 30
        targetSdk = 33
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
