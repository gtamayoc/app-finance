package com.gtc.app_finance.ui.components

/**
 * Action wrapper grouping all user interactions for the database diagnostic sheet.
 * Prevents parameter overinjection and simplifies Composable signatures.
 */
data class DatabaseDiagnosticActions(
    val onDismissRequest: () -> Unit = {},
    val onTestConnection: () -> Unit = {},
    val onSyncSchema: () -> Unit = {},
    val onSwitchToken: () -> Unit = {}
)
