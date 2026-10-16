package com.jaidensiu.quickmaths.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jaidensiu.quickmaths.domain.ThemePreference

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = PaperGrey,
    onPrimaryContainer = Ink,
    inversePrimary = NightText,
    secondary = InkMuted,
    onSecondary = Paper,
    secondaryContainer = PaperGrey,
    onSecondaryContainer = Ink,
    tertiary = InkSoft,
    onTertiary = Paper,
    tertiaryContainer = PaperGrey,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = InkSoft,
    surface = Paper,
    onSurface = InkSoft,
    surfaceVariant = PaperWarm,
    onSurfaceVariant = InkMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    error = Red,
    onError = Paper,
    errorContainer = RedContainer,
    onErrorContainer = Red,
    outline = LineStrong,
    outlineVariant = Line,
    scrim = Color.Black,
    surfaceBright = Paper,
    surfaceDim = PaperGrey,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = PaperWarm,
    surfaceContainer = PaperWarm,
    surfaceContainerHigh = PaperGrey,
    surfaceContainerHighest = Line,
)

private val DarkColorScheme = darkColorScheme(
    primary = NightText,
    onPrimary = NightInk,
    primaryContainer = NightSurfaceHigh,
    onPrimaryContainer = NightText,
    inversePrimary = Ink,
    secondary = NightTextMuted,
    onSecondary = NightInk,
    secondaryContainer = NightSurfaceHigh,
    onSecondaryContainer = NightText,
    tertiary = NightText,
    onTertiary = NightInk,
    tertiaryContainer = NightSurfaceHigh,
    onTertiaryContainer = NightText,
    background = NightInk,
    onBackground = NightText,
    surface = NightInk,
    onSurface = NightText,
    surfaceVariant = NightSurface,
    onSurfaceVariant = NightTextMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = NightText,
    inverseOnSurface = NightInk,
    error = NightRed,
    onError = NightInk,
    errorContainer = NightRedContainer,
    onErrorContainer = NightRed,
    outline = NightLineStrong,
    outlineVariant = NightLine,
    scrim = Color.Black,
    surfaceBright = NightSurfaceHigh,
    surfaceDim = NightInk,
    surfaceContainerLowest = NightInk,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = NightLine,
)

@Composable
fun QuickMathsTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
