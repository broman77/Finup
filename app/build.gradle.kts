plugins {
    id("com.android.application")
}

android {
    namespace = "com.ivan.finflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ivan.finflow"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.5.1"
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
