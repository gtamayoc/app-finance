package com.gtc.app_finance

import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.ui.components.DiagnosticUiMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticUiMapperTest {

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
