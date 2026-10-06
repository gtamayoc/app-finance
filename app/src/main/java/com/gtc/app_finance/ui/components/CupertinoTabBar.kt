package com.gtc.app_finance.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtc.app_finance.R
import com.gtc.app_finance.ui.utils.TouchHapticType
import com.gtc.app_finance.ui.utils.bouncePress
import com.gtc.app_finance.ui.utils.rememberTouchFeedback
sealed class NavTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val title: String
) {
    object Dashboard : NavTab("dashboard", R.string.tab_dashboard, Icons.Default.AccountBalanceWallet, "Inicio")
    object Transactions : NavTab("transactions", R.string.tab_transactions, Icons.AutoMirrored.Filled.ReceiptLong, "Movimientos")
    object Credits : NavTab("credits", R.string.tab_credits, Icons.Default.CreditCard, "Créditos")
    object Analytics : NavTab("analytics", R.string.tab_analytics, Icons.Default.PieChart, "Analítica")
}

val navTabs = listOf(
    NavTab.Dashboard,
    NavTab.Transactions,
    NavTab.Credits,
    NavTab.Analytics
)

@Composable
fun CupertinoTabBar(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val feedback = rememberTouchFeedback()

    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    var activeRoute by rememberSaveable { mutableStateOf(currentRoute) }
    LaunchedEffect(currentRoute) {
        activeRoute = currentRoute
    }
    val selectedIndex = navTabs.indexOfFirst { it.route == activeRoute }.coerceAtLeast(0)

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 2800f
        ),
        label = "tabIndicatorIndex"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .border(1.dp, outlineColor, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = surfaceColor,
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Physical sliding pill indicator with spring physics (Draw phase GPU translation)
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f / navTabs.size)
                    .height(52.dp)
                    .graphicsLayer {
                        translationX = animatedIndex * size.width
                    }
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(primaryColor)
            )

            // Touch-interactive tab item buttons
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navTabs.forEachIndexed { index, tab ->
                    val isSelected = selectedIndex == index
                    val tabTitle = stringResource(tab.labelRes)
                    val interactionSource = remember { MutableInteractionSource() }

                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        unselectedColor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .bouncePress(minScale = 0.94f, interactionSource = interactionSource)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    if (activeRoute != tab.route) {
                                        activeRoute = tab.route
                                        feedback.perform(TouchHapticType.TICK)
                                        onTabSelected(tab)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tabTitle,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tabTitle,
                                color = contentColor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
