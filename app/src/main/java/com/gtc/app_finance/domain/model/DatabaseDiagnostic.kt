package com.gtc.app_finance.domain.model

enum class ConnectionStatus {
    NOT_CHECKED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class RemoteDbDiagnostic(
    val status: ConnectionStatus = ConnectionStatus.NOT_CHECKED,
    val endpointUrl: String = "",
    val latencyMs: Long = 0,
    val httpStatusCode: Int = 0,
    val activeTokenMasked: String = "",
    val isBackupToken: Boolean = false,
    val message: String = "",
    val tablesVerified: List<String> = emptyList(),
    val lastCheckedTime: String = ""
)

data class LocalDbDiagnostic(
    val databaseName: String = "finance_app.db",
    val version: Int = 1,
    val transactionCount: Int = 0,
    val creditCount: Int = 0,
    val paymentCount: Int = 0,
    val isHealthy: Boolean = true
)

data class FullDatabaseStatus(
    val local: LocalDbDiagnostic = LocalDbDiagnostic(),
    val remote: RemoteDbDiagnostic = RemoteDbDiagnostic()
)
