@file:OptIn(ExperimentalTextApi::class)

package com.jaidensiu.quickmaths.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.jaidensiu.quickmaths.R

private fun fredoka(weight: FontWeight) = Font(
    resId = R.font.fredoka,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(value = weight.weight)),
)

val Fredoka = FontFamily(
    fonts = arrayOf(
        fredoka(weight = FontWeight.Light),
        fredoka(weight = FontWeight.Normal),
        fredoka(weight = FontWeight.Medium),
        fredoka(weight = FontWeight.SemiBold),
        fredoka(weight = FontWeight.Bold),
    ),
)

private val baseline = Typography()

val Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = Fredoka),
    displayMedium = baseline.displayMedium.copy(fontFamily = Fredoka),
    displaySmall = baseline.displaySmall.copy(fontFamily = Fredoka),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = Fredoka),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = Fredoka),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = Fredoka),
    titleLarge = baseline.titleLarge.copy(fontFamily = Fredoka),
    titleMedium = baseline.titleMedium.copy(fontFamily = Fredoka),
    titleSmall = baseline.titleSmall.copy(fontFamily = Fredoka),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = Fredoka),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = Fredoka),
    bodySmall = baseline.bodySmall.copy(fontFamily = Fredoka),
    labelLarge = baseline.labelLarge.copy(fontFamily = Fredoka),
    labelMedium = baseline.labelMedium.copy(fontFamily = Fredoka),
    labelSmall = baseline.labelSmall.copy(fontFamily = Fredoka),
)
