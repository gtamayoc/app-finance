package com.gtc.app_finance.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.gtc.app_finance.R
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.LocalDbDiagnostic

/**
 * Pure presentation logic for database diagnostics.
 * Decouples presentation rules and formatting from Compose UI rendering,
 * enabling fast, isolated unit testing.
 */
object DiagnosticUiMapper {

    fun isDataValid(status: FullDatabaseStatus?): Boolean {
        if (status == null) return false
        if (status.local.databaseName.isBlank()) return false
        if (status.local.version <= 0) return false
        return true
    }

    fun isLocalHealthy(local: LocalDbDiagnostic): Boolean {
        return local.isHealthy && local.databaseName.isNotBlank() && local.version > 0
    }

    fun getStatusBadgeTextRes(status: ConnectionStatus): Int = when (status) {
        ConnectionStatus.CONNECTED -> R.string.diagnostic_badge_connected
        ConnectionStatus.ERROR -> R.string.diagnostic_badge_disconnected
        ConnectionStatus.CONNECTING -> R.string.diagnostic_badge_connecting
        ConnectionStatus.NOT_CHECKED -> R.string.diagnostic_badge_not_checked
    }

    fun getStatusIcon(status: ConnectionStatus): ImageVector = when (status) {
        ConnectionStatus.CONNECTED -> Icons.Default.CloudDone
        ConnectionStatus.ERROR -> Icons.Default.CloudOff
        ConnectionStatus.CONNECTING -> Icons.Default.CloudSync
        ConnectionStatus.NOT_CHECKED -> Icons.Default.Cloud
    }

    fun getStatusColor(status: ConnectionStatus, colorScheme: ColorScheme): Color = when (status) {
        ConnectionStatus.CONNECTED -> colorScheme.secondary
        ConnectionStatus.ERROR -> colorScheme.error
        ConnectionStatus.CONNECTING -> colorScheme.primary
        ConnectionStatus.NOT_CHECKED -> colorScheme.onSurfaceVariant
    }

    fun getActiveTokenColor(status: ConnectionStatus, colorScheme: ColorScheme): Color =
        if (status == ConnectionStatus.CONNECTED) colorScheme.secondary else colorScheme.onSurface

    fun getStatusMessageColor(status: ConnectionStatus, colorScheme: ColorScheme): Color =
        if (status == ConnectionStatus.CONNECTED) colorScheme.secondary else colorScheme.error

    fun getLatencyColor(latencyMs: Long, colorScheme: ColorScheme): Color =
        if (isLatencyOptimal(latencyMs)) colorScheme.secondary else colorScheme.onSurface

    fun getHttpStatusColor(statusCode: Int, colorScheme: ColorScheme): Color =
        if (isHttpOk(statusCode)) colorScheme.secondary else colorScheme.error

    fun getLocalStatusColor(isHealthy: Boolean, colorScheme: ColorScheme): Color =
        if (isHealthy) colorScheme.secondary else colorScheme.error

    fun getTokenTypeRes(isBackup: Boolean): Int =
        if (isBackup) R.string.diagnostic_token_backup else R.string.diagnostic_token_primary

    fun isLatencyOptimal(latencyMs: Long): Boolean = latencyMs in 1..400

    fun isHttpOk(statusCode: Int): Boolean = statusCode == 200

    fun formatLatency(latencyMs: Long): String =
        if (latencyMs > 0) "$latencyMs ms" else "-- ms"

    fun formatHttpStatus(code: Int): String =
        if (code > 0) "$code" else "--"

    fun getEmptyTablesMessageRes(status: ConnectionStatus): Int =
        if (status == ConnectionStatus.CONNECTED) {
            R.string.diagnostic_no_tables_created
        } else {
            R.string.diagnostic_no_connection_tables
        }
}
