package com.gtc.app_finance.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CupertinoCardShape = RoundedCornerShape(20.dp)
val CupertinoSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val CupertinoPillShape = RoundedCornerShape(50)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = CupertinoCardShape,
    extraLarge = CupertinoSheetShape
)
