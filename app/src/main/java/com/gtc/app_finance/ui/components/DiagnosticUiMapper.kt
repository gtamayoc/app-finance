package com.gtc.app_finance.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.ui.graphics.vector.ImageVector
import com.gtc.app_finance.R
import com.gtc.app_finance.domain.model.ConnectionStatus

/**
 * Pure presentation logic for database diagnostics.
 * Decouples presentation rules and formatting from Compose UI rendering,
 * enabling fast, isolated unit testing.
 */
object DiagnosticUiMapper {

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
