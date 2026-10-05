package com.gtc.app_finance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtc.app_finance.ui.theme.BorderColor
import com.gtc.app_finance.ui.theme.CupertinoSurface
import com.gtc.app_finance.ui.theme.IndigoBlue
import com.gtc.app_finance.ui.theme.TextPrimary
import com.gtc.app_finance.ui.theme.TextSecondary

sealed class NavTab(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavTab("dashboard", "Inicio", Icons.Default.AccountBalanceWallet)
    object Transactions : NavTab("transactions", "Movimientos", Icons.AutoMirrored.Filled.ReceiptLong)
    object Credits : NavTab("credits", "Créditos", Icons.Default.CreditCard)
    object Analytics : NavTab("analytics", "Analítica", Icons.Default.PieChart)
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = CupertinoSurface,
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
                val isSelected = currentRoute == tab.route

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) IndigoBlue.copy(alpha = 0.2f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "tabBg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) IndigoBlue else TextSecondary,
                    animationSpec = tween(durationMillis = 300),
                    label = "tabContent"
                )

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Text(
                                text = tab.title,
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp
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
