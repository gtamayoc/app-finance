package com.gtc.app_finance.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gtc.app_finance.R
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.Transaction
import com.gtc.app_finance.domain.model.TransactionType
import com.gtc.app_finance.ui.components.AddTransactionSheet
import com.gtc.app_finance.ui.components.CupertinoCard
import com.gtc.app_finance.ui.components.DatabaseDiagnosticSheet
import com.gtc.app_finance.ui.components.DatabaseDiagnosticActions
import com.gtc.app_finance.ui.theme.BorderColor
import com.gtc.app_finance.ui.theme.CupertinoBackground
import com.gtc.app_finance.ui.theme.CupertinoCardSurface
import com.gtc.app_finance.ui.theme.EmeraldGreen
import com.gtc.app_finance.ui.theme.IndigoBlue
import com.gtc.app_finance.ui.theme.SoftCoral
import com.gtc.app_finance.ui.theme.TextPrimary
import com.gtc.app_finance.ui.theme.TextSecondary
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.gtc.app_finance.ui.utils.TouchHapticType
import com.gtc.app_finance.ui.utils.bounceClickable
import com.gtc.app_finance.ui.utils.bouncePress
import com.gtc.app_finance.ui.utils.rememberTouchFeedback
import com.gtc.app_finance.ui.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCredits: () -> Unit
) {
    val summary by viewModel.summary.collectAsState()
    val transactions by viewModel.recentTransactions.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val dbStatus by viewModel.dbStatus.collectAsState()
    val diagnosticUiState by viewModel.diagnosticUiState.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var showDbSheet by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dbSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dashboard_clear_data_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dashboard_clear_data_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCoral)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_clear_data_confirm),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CupertinoCardSurface)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_clear_data_cancel),
                        color = TextPrimary
                    )
                }
            },
            containerColor = CupertinoCardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = CupertinoBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User Profile Card / Header with Sync Cloud Badge
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CupertinoCardSurface),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: User Avatar + Greeting & Title (Click to open reset dialog)
                        val feedback = rememberTouchFeedback()
                        val syncInteractionSource = remember { MutableInteractionSource() }
                        val addBtnInteractionSource = remember { MutableInteractionSource() }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .bounceClickable(
                                    minScale = 0.96f,
                                    hapticType = TouchHapticType.CLICK,
                                    onClick = { showClearDialog = true }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(IndigoBlue, IndigoBlue.copy(alpha = 0.65f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = stringResource(R.string.dashboard_user_greeting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_user_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }

                        // Right: Cloud Sync Badge & Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isConnected = dbStatus.remote.status == ConnectionStatus.CONNECTED
                            val isConnecting = dbStatus.remote.status == ConnectionStatus.CONNECTING
                            val cloudColor = when {
                                isConnected -> EmeraldGreen
                                isConnecting -> IndigoBlue
                                else -> SoftCoral
                            }

                            // Cloud Sync Badge Button: Green if synced, Red if not synced, tap to attempt sync
                            Surface(
                                onClick = {
                                    feedback.perform(TouchHapticType.CLICK)
                                    viewModel.syncData()
                                },
                                interactionSource = syncInteractionSource,
                                shape = CircleShape,
                                color = cloudColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, cloudColor.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .size(42.dp)
                                    .bouncePress(minScale = 0.90f, interactionSource = syncInteractionSource)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (isConnecting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = IndigoBlue
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = stringResource(R.string.dashboard_sync_action_desc),
                                            tint = cloudColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            // Add Transaction Button (+)
                            Button(
                                onClick = {
                                    feedback.perform(TouchHapticType.CLICK)
                                    showAddSheet = true
                                },
                                interactionSource = addBtnInteractionSource,
                                shape = CircleShape,
                                contentPadding = PaddingValues(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IndigoBlue,
                                    contentColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .size(42.dp)
                                    .bouncePress(minScale = 0.90f, interactionSource = addBtnInteractionSource)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.dashboard_btn_add_movement),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Wallet Balance Card
            item {
                CupertinoCard(
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IndigoBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = IndigoBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Balance Total Neto",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Balance display with ellipsis and bounded font size
                        Text(
                            text = CurrencyFormatter.formatPesos(summary.totalBalance),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                            color = if (summary.totalBalance >= 0) TextPrimary else SoftCoral,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Incomes & Expenses (50/50 weighted split to eliminate overflow)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Income Box
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CupertinoCardSurface.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Ingresos",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Ingresos",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatPesos(summary.totalIncome),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Expense Box
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CupertinoCardSurface.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(SoftCoral.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Gastos",
                                        tint = SoftCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Gastos",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatPesos(summary.totalExpense),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SoftCoral,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Credits Summary Card
            item {
                CupertinoCard(
                    onClick = onNavigateToCredits
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(SoftCoral.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Deudas y Créditos",
                                    tint = SoftCoral,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Deudas y Créditos Activos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_pending_credits_count, summary.activeCreditsCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = CurrencyFormatter.formatPesos(summary.totalDebt),
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftCoral,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_recent_movements),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = onNavigateToTransactions,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.dashboard_view_all))
                    }
                }
            }

            // Recent Transactions List
            val recentList = transactions.take(4)
            if (recentList.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.dashboard_no_movements),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(
                    items = recentList,
                    key = { it.id },
                    contentType = { "transaction" }
                ) { tx ->
                    DashboardTransactionItem(
                        transaction = tx,
                        onClick = onNavigateToTransactions
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp)) // Padding for floating bottom tab bar
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            sheetState = addSheetState,
            onDismissRequest = { showAddSheet = false },
            onSaveTransaction = { title, amount, type, category ->
                viewModel.addQuickTransaction(title, amount, type, category)
                showAddSheet = false
            }
        )
    }

    if (showDbSheet) {
        DatabaseDiagnosticSheet(
            sheetState = dbSheetState,
            uiState = diagnosticUiState,
            actions = DatabaseDiagnosticActions(
                onDismissRequest = { showDbSheet = false },
                onTestConnection = { viewModel.testDatabaseConnection() },
                onSyncSchema = { viewModel.syncRemoteSchema() },
                onSwitchToken = { viewModel.switchToken() }
            )
        )
    }
}

@Composable
fun DashboardTransactionItem(
    transaction: Transaction,
    onClick: (() -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) EmeraldGreen else SoftCoral

    CupertinoCard(
        elevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = transaction.title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${transaction.category} • ${transaction.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${if (isIncome) "+" else "-"} ${CurrencyFormatter.formatPesosPositive(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
