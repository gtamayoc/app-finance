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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dbSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Header Title with Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "Mi Billetera",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Resumen Financiero Mensual",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Database diagnostics button
                        Button(
                            onClick = { showDbSheet = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (dbStatus.remote.status) {
                                    ConnectionStatus.CONNECTED -> EmeraldGreen.copy(alpha = 0.18f)
                                    ConnectionStatus.ERROR -> SoftCoral.copy(alpha = 0.18f)
                                    else -> CupertinoCardSurface
                                },
                                contentColor = when (dbStatus.remote.status) {
                                    ConnectionStatus.CONNECTED -> EmeraldGreen
                                    ConnectionStatus.ERROR -> SoftCoral
                                    else -> IndigoBlue
                                }
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = when (dbStatus.remote.status) {
                                    ConnectionStatus.CONNECTED -> Icons.Default.CloudDone
                                    ConnectionStatus.ERROR -> Icons.Default.CloudOff
                                    else -> Icons.Default.CloudSync
                                },
                                contentDescription = "Validar Base de Datos",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Add transaction button
                        Button(
                            onClick = { showAddSheet = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoBlue,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar Movimiento",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Database Status Chip (Pill)
            item {
                Surface(
                    onClick = { showDbSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = when (dbStatus.remote.status) {
                        ConnectionStatus.CONNECTED -> EmeraldGreen.copy(alpha = 0.12f)
                        ConnectionStatus.CONNECTING -> IndigoBlue.copy(alpha = 0.12f)
                        ConnectionStatus.ERROR -> SoftCoral.copy(alpha = 0.12f)
                        ConnectionStatus.NOT_CHECKED -> CupertinoCardSurface
                    },
                    border = BorderStroke(
                        1.dp,
                        when (dbStatus.remote.status) {
                            ConnectionStatus.CONNECTED -> EmeraldGreen.copy(alpha = 0.35f)
                            ConnectionStatus.CONNECTING -> IndigoBlue.copy(alpha = 0.35f)
                            ConnectionStatus.ERROR -> SoftCoral.copy(alpha = 0.35f)
                            ConnectionStatus.NOT_CHECKED -> BorderColor
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (dbStatus.remote.status) {
                                            ConnectionStatus.CONNECTED -> EmeraldGreen
                                            ConnectionStatus.CONNECTING -> IndigoBlue
                                            ConnectionStatus.ERROR -> SoftCoral
                                            ConnectionStatus.NOT_CHECKED -> TextSecondary
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (dbStatus.remote.status) {
                                    ConnectionStatus.CONNECTED -> "Turso Cloud Conectado (${dbStatus.remote.latencyMs}ms)"
                                    ConnectionStatus.CONNECTING -> "Comprobando conexión Turso..."
                                    ConnectionStatus.ERROR -> "Turso Cloud Sin Conexión (Modo Offline)"
                                    ConnectionStatus.NOT_CHECKED -> "Validar Estado de Conexión BD"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = when (dbStatus.remote.status) {
                                    ConnectionStatus.CONNECTED -> EmeraldGreen
                                    ConnectionStatus.CONNECTING -> IndigoBlue
                                    ConnectionStatus.ERROR -> SoftCoral
                                    ConnectionStatus.NOT_CHECKED -> TextSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Detalles",
                            style = MaterialTheme.typography.labelSmall,
                            color = IndigoBlue
                        )
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
                                    text = "${summary.activeCreditsCount} créditos pendientes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
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
                        text = "Movimientos Recientes",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = onNavigateToTransactions,
                        colors = ButtonDefaults.textButtonColors(contentColor = IndigoBlue)
                    ) {
                        Text("Ver Todos")
                    }
                }
            }

            // Recent Transactions List
            val recentList = transactions.take(4)
            if (recentList.isEmpty()) {
                item {
                    Text(
                        text = "No hay movimientos registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(recentList, key = { it.id }) { tx ->
                    DashboardTransactionItem(transaction = tx)
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
fun DashboardTransactionItem(transaction: Transaction) {
    val isIncome = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) EmeraldGreen else SoftCoral

    CupertinoCard(elevation = 2.dp) {
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
