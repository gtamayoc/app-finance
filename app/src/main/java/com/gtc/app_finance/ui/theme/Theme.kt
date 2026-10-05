package com.gtc.app_finance.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = IndigoBlue,
    onPrimary = TextPrimary,
    primaryContainer = IndigoBlueContainer,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldGreen,
    onSecondary = TextPrimary,
    secondaryContainer = EmeraldGreenContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = SoftCoral,
    onTertiary = TextPrimary,
    tertiaryContainer = SoftCoralContainer,
    onTertiaryContainer = TextPrimary,
    background = CupertinoBackground,
    onBackground = TextPrimary,
    surface = CupertinoSurface,
    onSurface = TextPrimary,
    surfaceVariant = CupertinoSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = TextTertiary
)

private val LightColorScheme = darkColorScheme(
    primary = IndigoBlue,
    onPrimary = TextPrimary,
    primaryContainer = IndigoBlueContainer,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldGreen,
    onSecondary = TextPrimary,
    secondaryContainer = EmeraldGreenContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = SoftCoral,
    onTertiary = TextPrimary,
    tertiaryContainer = SoftCoralContainer,
    onTertiaryContainer = TextPrimary,
    background = CupertinoBackground,
    onBackground = TextPrimary,
    surface = CupertinoSurface,
    onSurface = TextPrimary,
    surfaceVariant = CupertinoSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = TextTertiary
)

@Composable
fun AppfinanceTheme(
    darkTheme: Boolean = true, // Default to iOS dark style as requested in plan
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}