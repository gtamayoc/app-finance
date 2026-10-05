package com.gtc.app_finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gtc.app_finance.R
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.LocalDbDiagnostic
import com.gtc.app_finance.domain.model.RemoteDbDiagnostic
import com.gtc.app_finance.ui.theme.CupertinoSheetShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseDiagnosticSheet(
    sheetState: SheetState,
    dbStatus: FullDatabaseStatus,
    onDismissRequest: () -> Unit,
    onTestConnection: () -> Unit,
    onSyncSchema: () -> Unit,
    onSwitchToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = CupertinoSheetShape,
        containerColor = colorScheme.surface,
        scrimColor = colorScheme.scrim.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        if (!DiagnosticUiMapper.isDataValid(dbStatus)) {
            DiagnosticDataErrorContent(
                onDismissRequest = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        } else {
            val scrollState = rememberScrollState()
            val isConnecting = dbStatus.remote.status == ConnectionStatus.CONNECTING

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
            ) {
                DiagnosticHeader()

                Spacer(modifier = Modifier.height(20.dp))

                RemoteStatusCard(
                    remote = dbStatus.remote,
                    onSwitchToken = onSwitchToken
                )

                Spacer(modifier = Modifier.height(16.dp))

                LocalStatusCard(
                    local = dbStatus.local
                )

                Spacer(modifier = Modifier.height(20.dp))

                DiagnosticActionButtons(
                    isConnecting = isConnecting,
                    onSyncSchema = onSyncSchema,
                    onTestConnection = onTestConnection
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticHeader(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
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
                    .background(colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = stringResource(R.string.diagnostic_title),
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = stringResource(R.string.diagnostic_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.diagnostic_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: ConnectionStatus,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val isConnecting = status == ConnectionStatus.CONNECTING
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(statusColor.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = stringResource(DiagnosticUiMapper.getStatusBadgeTextRes(status)),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = statusColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemoteStatusCard(
    remote: RemoteDbDiagnostic,
    onSwitchToken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = DiagnosticUiMapper.getStatusColor(remote.status, colorScheme)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
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
                    Icon(
                        imageVector = DiagnosticUiMapper.getStatusIcon(remote.status),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.diagnostic_remote_db),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(
                    status = remote.status,
                    statusColor = statusColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Host URL
            Text(
                text = stringResource(R.string.diagnostic_endpoint_label),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = remote.endpointUrl.ifBlank { stringResource(R.string.diagnostic_not_configured) },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Token Info & Switch Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.diagnostic_active_token_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    val tokenTypeLabel = stringResource(DiagnosticUiMapper.getTokenTypeRes(remote.isBackupToken))
                    Text(
                        text = "${remote.activeTokenMasked} $tokenTypeLabel",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = DiagnosticUiMapper.getActiveTokenColor(remote.status, colorScheme),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onSwitchToken,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.diagnostic_btn_switch_token),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Latency and HTTP Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.diagnostic_latency_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DiagnosticUiMapper.formatLatency(remote.latencyMs),
                            style = MaterialTheme.typography.titleMedium,
                            color = DiagnosticUiMapper.getLatencyColor(remote.latencyMs, colorScheme)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.diagnostic_http_status_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DiagnosticUiMapper.formatHttpStatus(remote.httpStatusCode),
                            style = MaterialTheme.typography.titleMedium,
                            color = DiagnosticUiMapper.getHttpStatusColor(remote.httpStatusCode, colorScheme)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tables Verified in Cloud
            Text(
                text = stringResource(R.string.diagnostic_cloud_tables_label),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (remote.tablesVerified.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    remote.tablesVerified.forEach { table ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.secondary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(table, style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurface)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(DiagnosticUiMapper.getEmptyTablesMessageRes(remote.status)),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            if (remote.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = remote.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = DiagnosticUiMapper.getStatusMessageColor(remote.status, colorScheme)
                )
            }
        }
    }
}

@Composable
private fun LocalStatusCard(
    local: LocalDbDiagnostic,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isHealthy = DiagnosticUiMapper.isLocalHealthy(local)
    val localStatusColor = DiagnosticUiMapper.getLocalStatusColor(isHealthy, colorScheme)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
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
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = localStatusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.diagnostic_local_db),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(localStatusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isHealthy) {
                            stringResource(R.string.diagnostic_local_active, local.version)
                        } else {
                            stringResource(R.string.diagnostic_local_unhealthy, local.version)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = localStatusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.diagnostic_local_file, local.databaseName),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            if (!isHealthy) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.error.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.diagnostic_local_unhealthy_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Local metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocalMetricItem(
                    title = stringResource(R.string.diagnostic_stat_transactions),
                    count = local.transactionCount,
                    modifier = Modifier.weight(1f)
                )

                LocalMetricItem(
                    title = stringResource(R.string.diagnostic_stat_credits),
                    count = local.creditCount,
                    modifier = Modifier.weight(1f)
                )

                LocalMetricItem(
                    title = stringResource(R.string.diagnostic_stat_payments),
                    count = local.paymentCount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LocalMetricItem(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DiagnosticActionButtons(
    isConnecting: Boolean,
    onSyncSchema: () -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onSyncSchema,
            enabled = !isConnecting,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.diagnostic_btn_sync),
                maxLines = 1,
                color = colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Button(
            onClick = onTestConnection,
            enabled = !isConnecting,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.diagnostic_btn_testing),
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.diagnostic_btn_test),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun DiagnosticDataErrorContent(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colorScheme.error.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.diagnostic_data_invalid_title),
            style = MaterialTheme.typography.titleLarge,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.diagnostic_data_invalid_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismissRequest,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.diagnostic_btn_close),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
