package com.gtc.app_finance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.ui.theme.BorderColor
import com.gtc.app_finance.ui.theme.CupertinoCardSurface
import com.gtc.app_finance.ui.theme.CupertinoSheetShape
import com.gtc.app_finance.ui.theme.CupertinoSurface
import com.gtc.app_finance.ui.theme.EmeraldGreen
import com.gtc.app_finance.ui.theme.IndigoBlue
import com.gtc.app_finance.ui.theme.SoftCoral
import com.gtc.app_finance.ui.theme.TextPrimary
import com.gtc.app_finance.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatabaseDiagnosticSheet(
    sheetState: SheetState,
    dbStatus: FullDatabaseStatus,
    onDismissRequest: () -> Unit,
    onTestConnection: () -> Unit,
    onSyncSchema: () -> Unit,
    onSwitchToken: () -> Unit
) {
    val scrollState = rememberScrollState()
    val remote = dbStatus.remote
    val local = dbStatus.local
    val isConnecting = remote.status == ConnectionStatus.CONNECTING

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = CupertinoSheetShape,
        containerColor = CupertinoSurface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
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
                            .background(IndigoBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Base de datos",
                            tint = IndigoBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Estado de Base de Datos",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Diagnóstico y validación en tiempo real",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Remote Turso Cloud Status Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CupertinoCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
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
                                imageVector = when (remote.status) {
                                    ConnectionStatus.CONNECTED -> Icons.Default.CloudDone
                                    ConnectionStatus.ERROR -> Icons.Default.CloudOff
                                    ConnectionStatus.CONNECTING -> Icons.Default.CloudSync
                                    ConnectionStatus.NOT_CHECKED -> Icons.Default.Cloud
                                },
                                contentDescription = null,
                                tint = when (remote.status) {
                                    ConnectionStatus.CONNECTED -> EmeraldGreen
                                    ConnectionStatus.ERROR -> SoftCoral
                                    ConnectionStatus.CONNECTING -> IndigoBlue
                                    ConnectionStatus.NOT_CHECKED -> TextSecondary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Turso Cloud (libSQL)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (remote.status) {
                                        ConnectionStatus.CONNECTED -> EmeraldGreen.copy(alpha = 0.15f)
                                        ConnectionStatus.ERROR -> SoftCoral.copy(alpha = 0.15f)
                                        ConnectionStatus.CONNECTING -> IndigoBlue.copy(alpha = 0.15f)
                                        ConnectionStatus.NOT_CHECKED -> TextSecondary.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isConnecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 2.dp,
                                        color = IndigoBlue
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (remote.status) {
                                                    ConnectionStatus.CONNECTED -> EmeraldGreen
                                                    ConnectionStatus.ERROR -> SoftCoral
                                                    else -> TextSecondary
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = when (remote.status) {
                                        ConnectionStatus.CONNECTED -> "Conectado"
                                        ConnectionStatus.ERROR -> "Desconectado"
                                        ConnectionStatus.CONNECTING -> "Probando..."
                                        ConnectionStatus.NOT_CHECKED -> "Sin validar"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (remote.status) {
                                        ConnectionStatus.CONNECTED -> EmeraldGreen
                                        ConnectionStatus.ERROR -> SoftCoral
                                        ConnectionStatus.CONNECTING -> IndigoBlue
                                        ConnectionStatus.NOT_CHECKED -> TextSecondary
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Host URL
                    Text(
                        text = "Endpoint:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = remote.endpointUrl.ifBlank { "No configurado" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary,
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
                                text = "Token Activo:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${remote.activeTokenMasked} ${if (remote.isBackupToken) "(Resguardo)" else "(Primario)"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (remote.status == ConnectionStatus.CONNECTED) EmeraldGreen else TextPrimary,
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
                                tint = IndigoBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cambiar Token", fontSize = 11.sp, color = IndigoBlue)
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
                            color = CupertinoSurface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Latencia", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = if (remote.latencyMs > 0) "${remote.latencyMs} ms" else "-- ms",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (remote.latencyMs in 1..400) EmeraldGreen else TextPrimary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = CupertinoSurface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Código HTTP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = if (remote.httpStatusCode > 0) "${remote.httpStatusCode}" else "--",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (remote.httpStatusCode == 200) EmeraldGreen else SoftCoral
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tables Verified in Cloud
                    Text(
                        text = "Tablas en la Nube:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
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
                                    color = EmeraldGreen.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(table, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (remote.status == ConnectionStatus.CONNECTED) "No se detectaron tablas creadas aún." else "Sin conexión para consultar tablas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    if (remote.message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = remote.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remote.status == ConnectionStatus.CONNECTED) EmeraldGreen else SoftCoral
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Local SQLite Storage Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CupertinoCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
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
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SQLite Local (Offline)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Activo • v${local.version}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Archivo: ${local.databaseName} (Modo persistente local)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Local metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = CupertinoSurface
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Movimientos", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                                Text("${local.transactionCount}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = CupertinoSurface
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Créditos", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                                Text("${local.creditCount}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = CupertinoSurface
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Abonos", style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                                Text("${local.paymentCount}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        tint = IndigoBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sincronizar Tablas", maxLines = 1, color = IndigoBlue, fontSize = 12.sp)
                }

                Button(
                    onClick = onTestConnection,
                    enabled = !isConnecting,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndigoBlue,
                        contentColor = TextPrimary
                    )
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probando...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar Conexión", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
