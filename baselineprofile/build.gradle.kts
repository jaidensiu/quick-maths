plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.jaidensiu.quickmaths.baselineprofile"
    targetProjectPath = ":app"

    compileSdk {
        version = release(version = 37)
    }

    defaultConfig {
        // Profile collection needs API 28+ (rooted) or 33+ (non-rooted).
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
}
