package com.gtc.app_finance.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtc.app_finance.R
import com.gtc.app_finance.ui.utils.TouchHapticType
import com.gtc.app_finance.ui.utils.bounceClickable

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
    var activeRoute by remember(currentRoute) { mutableStateOf(currentRoute) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp))
            .border(1.dp, outlineColor, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = surfaceColor,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navTabs.forEach { tab ->
                val isSelected = activeRoute == tab.route
                val tabTitle = stringResource(tab.labelRes)

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) primaryColor.copy(alpha = 0.18f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                    label = "tabBg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) primaryColor else unselectedColor,
                    animationSpec = tween(durationMillis = 140),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(backgroundColor)
                        .bounceClickable(
                            minScale = 0.94f,
                            hapticType = if (isSelected) null else TouchHapticType.TICK,
                            onClick = {
                                if (activeRoute != tab.route) {
                                    activeRoute = tab.route
                                    onTabSelected(tab)
                                }
                            }
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tabTitle,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(animationSpec = tween(140)) + expandHorizontally(animationSpec = tween(140)),
                            exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(animationSpec = tween(100))
                        ) {
                            Text(
                                text = tabTitle,
                                color = primaryColor,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
