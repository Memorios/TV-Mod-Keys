plugins {
    id("com.android.application")
}

android {
    namespace = "com.memoar.tvmodkeys"
    compileSdk = 35   // use a platform you already have under ANDROID_HOME\platforms

    defaultConfig {
        applicationId = "com.memoar.tvmodkeys"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
