package com.gtc.app_finance.data.database

class TursoConfigProvider(
    val config: DatabaseConfig
) {
    var activeToken: String = when {
        config.backupAuthToken.isNotBlank() -> config.backupAuthToken
        config.primaryAuthToken.isNotBlank() -> config.primaryAuthToken
        else -> ""
    }
        private set

    val tursoUrl: String get() = config.tursoUrl
    val dbName: String get() = config.dbName
    val dbVersion: Int get() = config.dbVersion

    fun isUsingBackupToken(): Boolean =
        activeToken.isNotBlank() && activeToken == config.backupAuthToken

    fun switchToken(): String {
        activeToken = if (isUsingBackupToken() || config.backupAuthToken.isBlank()) {
            config.primaryAuthToken
        } else {
            config.backupAuthToken
        }
        return activeToken
    }

    fun switchToBackup(): Boolean {
        return if (config.backupAuthToken.isNotBlank() && activeToken != config.backupAuthToken) {
            activeToken = config.backupAuthToken
            true
        } else {
            false
        }
    }

    fun getMaskedToken(token: String = activeToken): String {
        return if (token.length > 20) {
            "${token.take(10)}...${token.takeLast(8)}"
        } else if (token.isNotBlank()) {
            "***"
        } else {
            "No configurado"
        }
    }
}
