package com.gtc.app_finance.data.database

data class DatabaseConfig(
    val dbName: String = "finance_app.db",
    val dbVersion: Int = 5,
    val tursoUrl: String = "",
    val primaryAuthToken: String = "",
    val backupAuthToken: String = ""
)
