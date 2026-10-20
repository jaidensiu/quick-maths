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

    // Only the mobile flavor is profiled for now; add an "automotive" flavor here to profile AAOS.
    flavorDimensions += "device"
    productFlavors {
        create("mobile") {
            dimension = "device"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
}
