package com.gtc.app_finance

import com.gtc.app_finance.data.database.DatabaseConfig
import com.gtc.app_finance.data.database.TursoConfigProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TursoConfigProviderTest {

    @Test
    fun testInitialTokenDefaultsToBackupWhenAvailable() {
        val config = DatabaseConfig(
            tursoUrl = "https://example.turso.io",
            primaryAuthToken = "primary-secret-token-long-1234567890",
            backupAuthToken = "backup-secret-token-long-1234567890"
        )
        val provider = TursoConfigProvider(config)

        assertEquals("backup-secret-token-long-1234567890", provider.activeToken)
        assertTrue(provider.isUsingBackupToken())
    }

    @Test
    fun testInitialTokenUsesPrimaryWhenBackupEmpty() {
        val config = DatabaseConfig(
            tursoUrl = "https://example.turso.io",
            primaryAuthToken = "primary-secret-token-long-1234567890",
            backupAuthToken = ""
        )
        val provider = TursoConfigProvider(config)

        assertEquals("primary-secret-token-long-1234567890", provider.activeToken)
        assertFalse(provider.isUsingBackupToken())
    }

    @Test
    fun testSwitchTokenTogglesCorrectly() {
        val config = DatabaseConfig(
            tursoUrl = "https://example.turso.io",
            primaryAuthToken = "primary-token-123456789012345",
            backupAuthToken = "backup-token-123456789012345"
        )
        val provider = TursoConfigProvider(config)

        // Starts on backup
        assertTrue(provider.isUsingBackupToken())

        // Switch to primary
        val switchedToPrimary = provider.switchToken()
        assertEquals("primary-token-123456789012345", switchedToPrimary)
        assertFalse(provider.isUsingBackupToken())

        // Switch back to backup
        val switchedToBackup = provider.switchToken()
        assertEquals("backup-token-123456789012345", switchedToBackup)
        assertTrue(provider.isUsingBackupToken())
    }

    @Test
    fun testGetMaskedToken() {
        val longToken = "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.abcdef"
        val config = DatabaseConfig(
            tursoUrl = "https://example.turso.io",
            primaryAuthToken = longToken,
            backupAuthToken = ""
        )
        val provider = TursoConfigProvider(config)

        val masked = provider.getMaskedToken()
        assertTrue(masked.contains("..."))
        assertEquals("${longToken.take(10)}...${longToken.takeLast(8)}", masked)

        val emptyProvider = TursoConfigProvider(DatabaseConfig(tursoUrl = "", primaryAuthToken = "", backupAuthToken = ""))
        assertEquals("No configurado", emptyProvider.getMaskedToken())
    }

    @Test
    fun testSwitchToBackupExplicit() {
        val config = DatabaseConfig(
            tursoUrl = "https://example.turso.io",
            primaryAuthToken = "primary-token-123456789012345",
            backupAuthToken = "backup-token-123456789012345"
        )
        val provider = TursoConfigProvider(config)
        provider.switchToken() // Now on primary
        assertFalse(provider.isUsingBackupToken())

        val switched = provider.switchToBackup()
        assertTrue(switched)
        assertTrue(provider.isUsingBackupToken())
    }
}
