package com.gtc.app_finance

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.FullDatabaseStatus
import com.gtc.app_finance.domain.model.LocalDbDiagnostic
import com.gtc.app_finance.ui.components.DiagnosticUiMapper
import com.gtc.app_finance.ui.components.StatusSeverity
import com.gtc.app_finance.ui.components.toColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticUiMapperTest {

    private val testColorScheme = darkColorScheme(
        primary = Color(0xFF112233),
        secondary = Color(0xFF22AA44),
        error = Color(0xFFAA2222),
        onSurface = Color(0xFFEEEEEE),
        onSurfaceVariant = Color(0xFF888888)
    )

    @Test
    fun `isDataValid validates presence and correctness of FullDatabaseStatus`() {
        assertFalse(DiagnosticUiMapper.isDataValid(null))
        assertFalse(DiagnosticUiMapper.isDataValid(FullDatabaseStatus(local = LocalDbDiagnostic(databaseName = ""))))
        assertFalse(DiagnosticUiMapper.isDataValid(FullDatabaseStatus(local = LocalDbDiagnostic(version = 0))))
        assertFalse(DiagnosticUiMapper.isDataValid(FullDatabaseStatus(local = LocalDbDiagnostic(version = -1))))
        assertTrue(DiagnosticUiMapper.isDataValid(FullDatabaseStatus()))
    }

    @Test
    fun `isLocalHealthy verifies that database is healthy with valid name and version`() {
        assertTrue(DiagnosticUiMapper.isLocalHealthy(LocalDbDiagnostic(isHealthy = true, databaseName = "db.sqlite", version = 1)))
        assertFalse(DiagnosticUiMapper.isLocalHealthy(LocalDbDiagnostic(isHealthy = false, databaseName = "db.sqlite", version = 1)))
        assertFalse(DiagnosticUiMapper.isLocalHealthy(LocalDbDiagnostic(isHealthy = true, databaseName = "", version = 1)))
        assertFalse(DiagnosticUiMapper.isLocalHealthy(LocalDbDiagnostic(isHealthy = true, databaseName = "db.sqlite", version = 0)))
    }

    @Test
    fun `getStatusSeverity maps connection statuses to semantic severities`() {
        assertEquals(StatusSeverity.SUCCESS, DiagnosticUiMapper.getStatusSeverity(ConnectionStatus.CONNECTED))
        assertEquals(StatusSeverity.ERROR, DiagnosticUiMapper.getStatusSeverity(ConnectionStatus.ERROR))
        assertEquals(StatusSeverity.INFO, DiagnosticUiMapper.getStatusSeverity(ConnectionStatus.CONNECTING))
        assertEquals(StatusSeverity.MUTED, DiagnosticUiMapper.getStatusSeverity(ConnectionStatus.NOT_CHECKED))
    }

    @Test
    fun `getActiveTokenSeverity maps connection status properly`() {
        assertEquals(StatusSeverity.SUCCESS, DiagnosticUiMapper.getActiveTokenSeverity(ConnectionStatus.CONNECTED))
        assertEquals(StatusSeverity.NEUTRAL, DiagnosticUiMapper.getActiveTokenSeverity(ConnectionStatus.ERROR))
        assertEquals(StatusSeverity.NEUTRAL, DiagnosticUiMapper.getActiveTokenSeverity(ConnectionStatus.CONNECTING))
    }

    @Test
    fun `getStatusMessageSeverity maps connection status properly`() {
        assertEquals(StatusSeverity.SUCCESS, DiagnosticUiMapper.getStatusMessageSeverity(ConnectionStatus.CONNECTED))
        assertEquals(StatusSeverity.ERROR, DiagnosticUiMapper.getStatusMessageSeverity(ConnectionStatus.ERROR))
    }

    @Test
    fun `getLatencySeverity maps optimal and non-optimal latencies`() {
        assertEquals(StatusSeverity.SUCCESS, DiagnosticUiMapper.getLatencySeverity(200))
        assertEquals(StatusSeverity.NEUTRAL, DiagnosticUiMapper.getLatencySeverity(0))
        assertEquals(StatusSeverity.NEUTRAL, DiagnosticUiMapper.getLatencySeverity(500))
    }

    @Test
    fun `getHttpStatusSeverity maps 200 to SUCCESS and others to ERROR`() {
        assertEquals(StatusSeverity.SUCCESS, DiagnosticUiMapper.getHttpStatusSeverity(200))
        assertEquals(StatusSeverity.ERROR, DiagnosticUiMapper.getHttpStatusSeverity(401))
        assertEquals(StatusSeverity.ERROR, DiagnosticUiMapper.getHttpStatusSeverity(500))
    }

    @Test
    fun `getLocalStatusSeverity maps healthy flag to SUCCESS or ERROR`() {
        assertEquals(StatusSeverity.SUCCESS, DiagnosticUiMapper.getLocalStatusSeverity(true))
        assertEquals(StatusSeverity.ERROR, DiagnosticUiMapper.getLocalStatusSeverity(false))
    }

    @Test
    fun `StatusSeverity toColor maps to correct ColorScheme tokens`() {
        assertEquals(testColorScheme.secondary, StatusSeverity.SUCCESS.toColor(testColorScheme))
        assertEquals(testColorScheme.error, StatusSeverity.ERROR.toColor(testColorScheme))
        assertEquals(testColorScheme.primary, StatusSeverity.INFO.toColor(testColorScheme))
        assertEquals(testColorScheme.onSurfaceVariant, StatusSeverity.MUTED.toColor(testColorScheme))
        assertEquals(testColorScheme.onSurface, StatusSeverity.NEUTRAL.toColor(testColorScheme))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated getStatusColor delegates properly to theme color tokens`() {
        assertEquals(testColorScheme.secondary, DiagnosticUiMapper.getStatusColor(ConnectionStatus.CONNECTED, testColorScheme))
        assertEquals(testColorScheme.error, DiagnosticUiMapper.getStatusColor(ConnectionStatus.ERROR, testColorScheme))
        assertEquals(testColorScheme.primary, DiagnosticUiMapper.getStatusColor(ConnectionStatus.CONNECTING, testColorScheme))
        assertEquals(testColorScheme.onSurfaceVariant, DiagnosticUiMapper.getStatusColor(ConnectionStatus.NOT_CHECKED, testColorScheme))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated getActiveTokenColor returns secondary when connected and onSurface otherwise`() {
        assertEquals(testColorScheme.secondary, DiagnosticUiMapper.getActiveTokenColor(ConnectionStatus.CONNECTED, testColorScheme))
        assertEquals(testColorScheme.onSurface, DiagnosticUiMapper.getActiveTokenColor(ConnectionStatus.ERROR, testColorScheme))
        assertEquals(testColorScheme.onSurface, DiagnosticUiMapper.getActiveTokenColor(ConnectionStatus.CONNECTING, testColorScheme))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated getStatusMessageColor returns secondary when connected and error otherwise`() {
        assertEquals(testColorScheme.secondary, DiagnosticUiMapper.getStatusMessageColor(ConnectionStatus.CONNECTED, testColorScheme))
        assertEquals(testColorScheme.error, DiagnosticUiMapper.getStatusMessageColor(ConnectionStatus.ERROR, testColorScheme))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated getLatencyColor returns secondary for optimal latencies and onSurface otherwise`() {
        assertEquals(testColorScheme.secondary, DiagnosticUiMapper.getLatencyColor(200, testColorScheme))
        assertEquals(testColorScheme.onSurface, DiagnosticUiMapper.getLatencyColor(0, testColorScheme))
        assertEquals(testColorScheme.onSurface, DiagnosticUiMapper.getLatencyColor(500, testColorScheme))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated getHttpStatusColor returns secondary for 200 and error otherwise`() {
        assertEquals(testColorScheme.secondary, DiagnosticUiMapper.getHttpStatusColor(200, testColorScheme))
        assertEquals(testColorScheme.error, DiagnosticUiMapper.getHttpStatusColor(401, testColorScheme))
        assertEquals(testColorScheme.error, DiagnosticUiMapper.getHttpStatusColor(500, testColorScheme))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated getLocalStatusColor returns secondary when healthy and error when unhealthy`() {
        assertEquals(testColorScheme.secondary, DiagnosticUiMapper.getLocalStatusColor(true, testColorScheme))
        assertEquals(testColorScheme.error, DiagnosticUiMapper.getLocalStatusColor(false, testColorScheme))
    }

    @Test
    fun `getStatusBadgeTextRes maps each ConnectionStatus to its string resource`() {
        assertEquals(R.string.diagnostic_badge_connected, DiagnosticUiMapper.getStatusBadgeTextRes(ConnectionStatus.CONNECTED))
        assertEquals(R.string.diagnostic_badge_disconnected, DiagnosticUiMapper.getStatusBadgeTextRes(ConnectionStatus.ERROR))
        assertEquals(R.string.diagnostic_badge_connecting, DiagnosticUiMapper.getStatusBadgeTextRes(ConnectionStatus.CONNECTING))
        assertEquals(R.string.diagnostic_badge_not_checked, DiagnosticUiMapper.getStatusBadgeTextRes(ConnectionStatus.NOT_CHECKED))
    }

    @Test
    fun `getTokenTypeRes returns backup when true and primary when false`() {
        assertEquals(R.string.diagnostic_token_backup, DiagnosticUiMapper.getTokenTypeRes(isBackup = true))
        assertEquals(R.string.diagnostic_token_primary, DiagnosticUiMapper.getTokenTypeRes(isBackup = false))
    }

    @Test
    fun `isLatencyOptimal returns true only for latencies between 1 and 400 ms`() {
        assertTrue(DiagnosticUiMapper.isLatencyOptimal(1))
        assertTrue(DiagnosticUiMapper.isLatencyOptimal(150))
        assertTrue(DiagnosticUiMapper.isLatencyOptimal(400))

        assertFalse(DiagnosticUiMapper.isLatencyOptimal(0))
        assertFalse(DiagnosticUiMapper.isLatencyOptimal(-10))
        assertFalse(DiagnosticUiMapper.isLatencyOptimal(401))
        assertFalse(DiagnosticUiMapper.isLatencyOptimal(1200))
    }

    @Test
    fun `isHttpOk returns true only for HTTP status 200`() {
        assertTrue(DiagnosticUiMapper.isHttpOk(200))
        assertFalse(DiagnosticUiMapper.isHttpOk(201))
        assertFalse(DiagnosticUiMapper.isHttpOk(401))
        assertFalse(DiagnosticUiMapper.isHttpOk(500))
        assertFalse(DiagnosticUiMapper.isHttpOk(0))
    }

    @Test
    fun `formatLatency formats millisecond numbers cleanly`() {
        assertEquals("125 ms", DiagnosticUiMapper.formatLatency(125))
        assertEquals("-- ms", DiagnosticUiMapper.formatLatency(0))
        assertEquals("-- ms", DiagnosticUiMapper.formatLatency(-1))
    }

    @Test
    fun `formatHttpStatus formats codes properly`() {
        assertEquals("200", DiagnosticUiMapper.formatHttpStatus(200))
        assertEquals("401", DiagnosticUiMapper.formatHttpStatus(401))
        assertEquals("--", DiagnosticUiMapper.formatHttpStatus(0))
    }

    @Test
    fun `getEmptyTablesMessageRes returns correct message depending on connection`() {
        assertEquals(
            R.string.diagnostic_no_tables_created,
            DiagnosticUiMapper.getEmptyTablesMessageRes(ConnectionStatus.CONNECTED)
        )
        assertEquals(
            R.string.diagnostic_no_connection_tables,
            DiagnosticUiMapper.getEmptyTablesMessageRes(ConnectionStatus.ERROR)
        )
        assertEquals(
            R.string.diagnostic_no_connection_tables,
            DiagnosticUiMapper.getEmptyTablesMessageRes(ConnectionStatus.CONNECTING)
        )
    }
}
