package com.gtc.app_finance.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.gtc.app_finance.R
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.LocalDbDiagnostic

/**
 * Semantic severity states for database diagnostics.
 * Keeps presentation mappers independent of UI composition and theme instances.
 */
enum class StatusSeverity {
    SUCCESS,
    ERROR,
    INFO,
    MUTED,
    NEUTRAL
}

/**
 * Maps a [StatusSeverity] to a semantic [Color] using the provided [ColorScheme].
 */
fun StatusSeverity.toColor(colorScheme: ColorScheme): Color = when (this) {
    StatusSeverity.SUCCESS -> colorScheme.secondary
    StatusSeverity.ERROR -> colorScheme.error
    StatusSeverity.INFO -> colorScheme.primary
    StatusSeverity.MUTED -> colorScheme.onSurfaceVariant
    StatusSeverity.NEUTRAL -> colorScheme.onSurface
}

/**
 * Resolves the [Color] for this [StatusSeverity] dynamically within a @Composable scope.
 */
@Composable
@ReadOnlyComposable
fun StatusSeverity.asColor(): Color = toColor(MaterialTheme.colorScheme)

/**
 * Pure presentation logic for database diagnostics.
 * Decouples presentation rules and semantic statuses from Compose UI rendering,
 * enabling fast, isolated unit testing without requiring Android or Compose runtimes.
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

    // --- Semantic Status Mappers (Decoupled from ColorScheme) ---

    fun getStatusSeverity(status: ConnectionStatus): StatusSeverity = when (status) {
        ConnectionStatus.CONNECTED -> StatusSeverity.SUCCESS
        ConnectionStatus.ERROR -> StatusSeverity.ERROR
        ConnectionStatus.CONNECTING -> StatusSeverity.INFO
        ConnectionStatus.NOT_CHECKED -> StatusSeverity.MUTED
    }

    fun getActiveTokenSeverity(status: ConnectionStatus): StatusSeverity =
        if (status == ConnectionStatus.CONNECTED) StatusSeverity.SUCCESS else StatusSeverity.NEUTRAL

    fun getStatusMessageSeverity(status: ConnectionStatus): StatusSeverity =
        if (status == ConnectionStatus.CONNECTED) StatusSeverity.SUCCESS else StatusSeverity.ERROR

    fun getLatencySeverity(latencyMs: Long): StatusSeverity =
        if (isLatencyOptimal(latencyMs)) StatusSeverity.SUCCESS else StatusSeverity.NEUTRAL

    fun getHttpStatusSeverity(statusCode: Int): StatusSeverity =
        if (isHttpOk(statusCode)) StatusSeverity.SUCCESS else StatusSeverity.ERROR

    fun getLocalStatusSeverity(isHealthy: Boolean): StatusSeverity =
        if (isHealthy) StatusSeverity.SUCCESS else StatusSeverity.ERROR

    // --- Legacy ColorScheme Bridge (Deprecated in favor of StatusSeverity) ---

    @Deprecated(
        message = "Use getStatusSeverity() and resolve color via StatusSeverity.asColor() in Composable scope",
        replaceWith = ReplaceWith("getStatusSeverity(status).toColor(colorScheme)")
    )
    fun getStatusColor(status: ConnectionStatus, colorScheme: ColorScheme): Color =
        getStatusSeverity(status).toColor(colorScheme)

    @Deprecated(
        message = "Use getActiveTokenSeverity() and resolve color via StatusSeverity.asColor() in Composable scope",
        replaceWith = ReplaceWith("getActiveTokenSeverity(status).toColor(colorScheme)")
    )
    fun getActiveTokenColor(status: ConnectionStatus, colorScheme: ColorScheme): Color =
        getActiveTokenSeverity(status).toColor(colorScheme)

    @Deprecated(
        message = "Use getStatusMessageSeverity() and resolve color via StatusSeverity.asColor() in Composable scope",
        replaceWith = ReplaceWith("getStatusMessageSeverity(status).toColor(colorScheme)")
    )
    fun getStatusMessageColor(status: ConnectionStatus, colorScheme: ColorScheme): Color =
        getStatusMessageSeverity(status).toColor(colorScheme)

    @Deprecated(
        message = "Use getLatencySeverity() and resolve color via StatusSeverity.asColor() in Composable scope",
        replaceWith = ReplaceWith("getLatencySeverity(latencyMs).toColor(colorScheme)")
    )
    fun getLatencyColor(latencyMs: Long, colorScheme: ColorScheme): Color =
        getLatencySeverity(latencyMs).toColor(colorScheme)

    @Deprecated(
        message = "Use getHttpStatusSeverity() and resolve color via StatusSeverity.asColor() in Composable scope",
        replaceWith = ReplaceWith("getHttpStatusSeverity(statusCode).toColor(colorScheme)")
    )
    fun getHttpStatusColor(statusCode: Int, colorScheme: ColorScheme): Color =
        getHttpStatusSeverity(statusCode).toColor(colorScheme)

    @Deprecated(
        message = "Use getLocalStatusSeverity() and resolve color via StatusSeverity.asColor() in Composable scope",
        replaceWith = ReplaceWith("getLocalStatusSeverity(isHealthy).toColor(colorScheme)")
    )
    fun getLocalStatusColor(isHealthy: Boolean, colorScheme: ColorScheme): Color =
        getLocalStatusSeverity(isHealthy).toColor(colorScheme)

    // --- Resources and Formatting ---

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
