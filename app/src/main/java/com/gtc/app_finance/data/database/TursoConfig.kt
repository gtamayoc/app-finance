package com.gtc.app_finance.data.database

object TursoConfig {
    const val PRIMARY_AUTH_TOKEN = "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJSS3lrSmNEZEVmR01jbGFpak94YnFBIiwib3JnX2lkIjoxMDAwMjY0MTI4fQ.bJg7BGpj4btysurO-L--vSWwMlv73iaipkD4h5MeJ0O2E-e5TWg0J-Lo_tM6KoHNrOZ3cwaz8ssBZSkDNFZEBQ"
    const val BACKUP_AUTH_TOKEN = "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.eyJhIjoicnciLCJnaWQiOiIzNWQ5MDBiYy0xMjgwLTQ2NzItYmEyMS1lNGY3OGJmZmEyNGQiLCJpYXQiOjE3OTEyMTkyOTgsImtpZCI6Ik1xT2xpRi02Y2x4OXhRcWg0MHQxZldESHE1c1FIMzN4U0tFVTMwMWV5eEkiLCJyaWQiOiI5NWFlNzJlYi1iZjUxLTQwNDctYjFiNS03ZmFkYzFkNzgyMTkifQ.78Ysi0FsskjHW9wT653eqxTQcSxf9POp-In0Cz_noYxAFRgIkPy_djYmjZR4kQZufk81_OFujT1PPzrRqPpLBg"

    // Default to the valid, tested backup auth token
    var TURSO_AUTH_TOKEN: String = BACKUP_AUTH_TOKEN

    var TURSO_DB_URL: String = "https://demo-gtamayoc.aws-us-east-1.turso.io"

    const val DATABASE_NAME = "finance_app.db"
    const val DATABASE_VERSION = 1

    fun isUsingBackupToken(): Boolean = TURSO_AUTH_TOKEN == BACKUP_AUTH_TOKEN

    fun switchToken(): String {
        TURSO_AUTH_TOKEN = if (isUsingBackupToken()) PRIMARY_AUTH_TOKEN else BACKUP_AUTH_TOKEN
        return TURSO_AUTH_TOKEN
    }

    fun getMaskedToken(token: String = TURSO_AUTH_TOKEN): String {
        return if (token.length > 20) {
            "${token.take(10)}...${token.takeLast(8)}"
        } else {
            token
        }
    }
}
