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
    error = SoftCoral,
    onError = TextPrimary,
    background = CupertinoBackground,
    onBackground = TextPrimary,
    surface = CupertinoSurface,
    onSurface = TextPrimary,
    surfaceVariant = CupertinoSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = TextTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD6E4FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001B3E),
    secondary = EmeraldGreen,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFD1F2D9),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF063B13),
    tertiary = SoftCoral,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD8D6),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF410002),
    error = SoftCoral,
    onError = androidx.compose.ui.graphics.Color.White,
    background = androidx.compose.ui.graphics.Color(0xFFF2F2F7),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE5E5EA),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF8E8E93),
    outline = androidx.compose.ui.graphics.Color(0xFFD1D1D6),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFC7C7CC)
)

@Composable
fun AppfinanceTheme(
    darkTheme: Boolean = true, // Default to iOS dark style as requested in plan
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalSpacing provides AppSpacing) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}