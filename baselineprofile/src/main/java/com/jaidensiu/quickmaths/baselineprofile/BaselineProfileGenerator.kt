package com.jaidensiu.quickmaths.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the app's baseline + startup profiles for cold start. Run on a device or emulator with:
 *
 * `./gradlew :app:generateMobileReleaseBaselineProfile`
 *
 * The output lands in `app/src/mobileRelease/generated/baselineProfiles/` and must be committed.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = "com.jaidensiu.quickmaths",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
