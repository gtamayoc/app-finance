package com.gtc.app_finance.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hardware-accelerated shimmer effect modifier running on GPU linear gradient.
 * Provides instant tactile visual feedback while components and data are hydrating.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val gradientWidth = screenWidthPx * 0.75f

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = -gradientWidth,
        targetValue = screenWidthPx + gradientWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) {
        Color(0xFF22242D)
    } else {
        Color(0xFFE2E5EB)
    }
    val highlightColor = if (isDark) {
        Color(0xFF3E4356)
    } else {
        Color(0xFFFFFFFF)
    }

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(x = translateAnimation, y = translateAnimation * 0.35f),
        end = Offset(x = translateAnimation + gradientWidth, y = (translateAnimation + gradientWidth) * 0.35f)
    )

    this.background(brush)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect()
    )
}

/**
 * Shimmer skeleton loading state for Dashboard screen.
 */
@Composable
fun DashboardSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // User Header Card skeleton
        item {
            CupertinoCard(elevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(modifier = Modifier.size(44.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 14.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.size(width = 140.dp, height = 18.dp))
                    }
                }
            }
        }

        // Wallet Balance Card skeleton
        item {
            CupertinoCard(elevation = 6.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(modifier = Modifier.size(36.dp), shape = CircleShape)
                        Spacer(modifier = Modifier.width(10.dp))
                        SkeletonBox(modifier = Modifier.size(width = 120.dp, height = 16.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    SkeletonBox(modifier = Modifier.size(width = 220.dp, height = 36.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeletonBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )
                        SkeletonBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )
                    }
                }
            }
        }

        // Active Credits Card skeleton
        item {
            CupertinoCard(elevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(modifier = Modifier.size(44.dp), shape = CircleShape)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            SkeletonBox(modifier = Modifier.size(width = 150.dp, height = 18.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(modifier = Modifier.size(width = 100.dp, height = 14.dp))
                        }
                    }
                    SkeletonBox(modifier = Modifier.size(width = 80.dp, height = 22.dp))
                }
            }
        }

        // Recent items header skeleton
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.size(width = 160.dp, height = 22.dp))
                SkeletonBox(modifier = Modifier.size(width = 60.dp, height = 18.dp))
            }
        }

        // Recent transaction rows skeleton
        items(3) {
            CupertinoCard(elevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            SkeletonBox(modifier = Modifier.size(width = 130.dp, height = 16.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 12.dp))
                        }
                    }
                    SkeletonBox(modifier = Modifier.size(width = 75.dp, height = 18.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

/**
 * Shimmer skeleton loading state for Transactions screen.
 */
@Composable
fun TransactionsSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SkeletonBox(modifier = Modifier.size(width = 180.dp, height = 32.dp))
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(5) {
                CupertinoCard(elevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(modifier = Modifier.size(42.dp), shape = CircleShape)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                SkeletonBox(modifier = Modifier.size(width = 140.dp, height = 18.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                SkeletonBox(modifier = Modifier.size(width = 100.dp, height = 14.dp))
                            }
                        }
                        SkeletonBox(modifier = Modifier.size(width = 80.dp, height = 20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Shimmer skeleton loading state for Credits screen.
 */
@Composable
fun CreditsSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SkeletonBox(modifier = Modifier.size(width = 140.dp, height = 32.dp))
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonBox(modifier = Modifier.size(width = 200.dp, height = 16.dp))
        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(3) {
                CupertinoCard(elevation = 2.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    SkeletonBox(modifier = Modifier.size(width = 130.dp, height = 18.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 14.dp))
                            SkeletonBox(modifier = Modifier.size(width = 90.dp, height = 14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shimmer skeleton loading state for Analytics screen.
 */
@Composable
fun AnalyticsSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            SkeletonBox(modifier = Modifier.size(width = 160.dp, height = 32.dp))
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(modifier = Modifier.size(width = 220.dp, height = 16.dp))
        }

        // Donut Chart Card Skeleton
        item {
            CupertinoCard(elevation = 2.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SkeletonBox(modifier = Modifier.size(width = 180.dp, height = 20.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    SkeletonBox(modifier = Modifier.size(180.dp), shape = CircleShape)
                }
            }
        }

        // Income vs Expense Bars Skeleton
        item {
            CupertinoCard(elevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SkeletonBox(modifier = Modifier.size(width = 190.dp, height = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
