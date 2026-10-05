package com.gtc.app_finance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design system spacing tokens for consistent padding, margins, and gaps.
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val compact: Dp = 6.dp,
    val small: Dp = 8.dp,
    val semiMedium: Dp = 10.dp,
    val medium: Dp = 12.dp,
    val mediumLarge: Dp = 14.dp,
    val normal: Dp = 16.dp,
    val cardPadding: Dp = 18.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp
)

val AppSpacing = Spacing()

val LocalSpacing = staticCompositionLocalOf { AppSpacing }

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
