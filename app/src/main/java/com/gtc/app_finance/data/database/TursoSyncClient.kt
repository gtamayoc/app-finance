package com.gtc.app_finance.data.database

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gtc.app_finance.domain.model.ConnectionStatus
import com.gtc.app_finance.domain.model.RemoteDbDiagnostic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TursoSyncClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private fun getEndpoint(): String {
        val rawUrl = TursoConfig.TURSO_DB_URL.trim()
        val formattedUrl = rawUrl.replace("^libsql://".toRegex(), "https://").removeSuffix("/")
        return if (formattedUrl.endsWith("/v2/pipeline")) formattedUrl else "$formattedUrl/v2/pipeline"
    }

    suspend fun testConnection(): RemoteDbDiagnostic = withContext(Dispatchers.IO) {
        val endpoint = getEndpoint()
        val startTime = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = dateFormat.format(Date())

        if (TursoConfig.TURSO_DB_URL.isBlank() || TursoConfig.TURSO_DB_URL.contains("YOUR_TURSO_DB")) {
            return@withContext RemoteDbDiagnostic(
                status = ConnectionStatus.ERROR,
                endpointUrl = TursoConfig.TURSO_DB_URL,
                latencyMs = 0,
                httpStatusCode = 0,
                activeTokenMasked = TursoConfig.getMaskedToken(),
                isBackupToken = TursoConfig.isUsingBackupToken(),
                message = "URL de Turso no configurada",
                tablesVerified = emptyList(),
                lastCheckedTime = timeNow
            )
        }

        fun executePing(token: String): Pair<Int, String?> {
            return try {
                val stmtObj = JsonObject().apply {
                    addProperty("sql", "SELECT name FROM sqlite_master WHERE type='table';")
                }
                val reqObj = JsonObject().apply {
                    addProperty("type", "execute")
                    add("stmt", stmtObj)
                }
                val closeObj = JsonObject().apply {
                    addProperty("type", "close")
                }
                val payload = JsonObject().apply {
                    add("requests", JsonArray().apply {
                        add(reqObj)
                        add(closeObj)
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $token")
                    .post(payload.toString().toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    Pair(response.code, response.body?.string())
                }
            } catch (e: Exception) {
                Pair(-1, e.message)
            }
        }

        var activeToken = TursoConfig.TURSO_AUTH_TOKEN
        var (code, body) = executePing(activeToken)

        // Automatic token recovery if primary 401 unauthorized
        if (code == 401 && activeToken != TursoConfig.BACKUP_AUTH_TOKEN) {
            Log.w("TursoSyncClient", "Token primario falló con 401, probando token de resguardo...")
            val (backupCode, backupBody) = executePing(TursoConfig.BACKUP_AUTH_TOKEN)
            if (backupCode == 200) {
                TursoConfig.TURSO_AUTH_TOKEN = TursoConfig.BACKUP_AUTH_TOKEN
                activeToken = TursoConfig.BACKUP_AUTH_TOKEN
                code = backupCode
                body = backupBody
            }
        }

        val latencyMs = System.currentTimeMillis() - startTime

        if (code == 200 && body != null) {
            val tables = mutableListOf<String>()
            try {
                val root = JsonParser.parseString(body).asJsonObject
                val results = root.getAsJsonArray("results")
                if (results != null && results.size() > 0) {
                    val respObj = results[0].asJsonObject.getAsJsonObject("response")
                    val resultObj = respObj?.getAsJsonObject("result")
                    val rows = resultObj?.getAsJsonArray("rows")
                    if (rows != null) {
                        for (rowElem in rows) {
                            val rowArr = rowElem.asJsonArray
                            if (rowArr.size() > 0) {
                                val item = rowArr[0]
                                val tableName = if (item.isJsonObject) {
                                    val obj = item.asJsonObject
                                    obj.get("value")?.asString ?: obj.get("text")?.asString ?: ""
                                } else {
                                    item.asString
                                }
                                if (tableName.isNotBlank() && !tableName.startsWith("_litestream")) {
                                    tables.add(tableName)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TursoSyncClient", "Error parseando tablas: ${e.message}")
            }

            RemoteDbDiagnostic(
                status = ConnectionStatus.CONNECTED,
                endpointUrl = endpoint.removeSuffix("/v2/pipeline"),
                latencyMs = latencyMs,
                httpStatusCode = 200,
                activeTokenMasked = TursoConfig.getMaskedToken(activeToken),
                isBackupToken = TursoConfig.isUsingBackupToken(),
                message = "Conexión activa y verificada con Turso Cloud",
                tablesVerified = tables,
                lastCheckedTime = timeNow
            )
        } else {
            val friendlyMsg = when (code) {
                401 -> "Error 401: Token JWT no autorizado o revocado"
                404 -> "Error 404: Endpoint de Turso no encontrado"
                -1 -> "Error de red / Timeout: ${body ?: "Compruebe su conexión a internet"}"
                else -> "Error HTTP $code: ${body?.take(120) ?: "Respuesta desconocida"}"
            }

            RemoteDbDiagnostic(
                status = ConnectionStatus.ERROR,
                endpointUrl = endpoint.removeSuffix("/v2/pipeline"),
                latencyMs = latencyMs,
                httpStatusCode = code,
                activeTokenMasked = TursoConfig.getMaskedToken(activeToken),
                isBackupToken = TursoConfig.isUsingBackupToken(),
                message = friendlyMsg,
                tablesVerified = emptyList(),
                lastCheckedTime = timeNow
            )
        }
    }

    suspend fun initializeRemoteDatabase(): Boolean = withContext(Dispatchers.IO) {
        val createTransactionsSql = """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                date TEXT NOT NULL
            );
        """.trimIndent()

        val createCreditsSql = """
            CREATE TABLE IF NOT EXISTS credits (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                total_amount REAL NOT NULL,
                remaining_amount REAL NOT NULL,
                due_date TEXT NOT NULL
            );
        """.trimIndent()

        val createPaymentsSql = """
            CREATE TABLE IF NOT EXISTS payments (
                id TEXT PRIMARY KEY,
                credit_id TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL
            );
        """.trimIndent()

        val ok1 = executeQuery(createTransactionsSql)
        val ok2 = executeQuery(createCreditsSql)
        val ok3 = executeQuery(createPaymentsSql)
        return@withContext ok1 && ok2 && ok3
    }

    suspend fun executeQuery(sql: String): Boolean = withContext(Dispatchers.IO) {
        if (TursoConfig.TURSO_DB_URL.isBlank() || TursoConfig.TURSO_DB_URL.contains("YOUR_TURSO_DB")) {
            Log.w("TursoSyncClient", "Turso DB URL not configured. Skipping remote sync.")
            return@withContext false
        }

        try {
            val endpoint = getEndpoint()

            fun doCall(token: String): okhttp3.Response {
                val stmtObj = JsonObject().apply {
                    addProperty("sql", sql)
                }
                val reqObj = JsonObject().apply {
                    addProperty("type", "execute")
                    add("stmt", stmtObj)
                }
                val closeObj = JsonObject().apply {
                    addProperty("type", "close")
                }
                val payload = JsonObject().apply {
                    add("requests", JsonArray().apply {
                        add(reqObj)
                        add(closeObj)
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $token")
                    .post(payload.toString().toRequestBody(mediaType))
                    .build()

                return client.newCall(request).execute()
            }

            var resp = doCall(TursoConfig.TURSO_AUTH_TOKEN)
            if (resp.code == 401 && TursoConfig.TURSO_AUTH_TOKEN != TursoConfig.BACKUP_AUTH_TOKEN) {
                resp.close()
                Log.w("TursoSyncClient", "Retrying query with backup token...")
                resp = doCall(TursoConfig.BACKUP_AUTH_TOKEN)
                if (resp.isSuccessful) {
                    TursoConfig.TURSO_AUTH_TOKEN = TursoConfig.BACKUP_AUTH_TOKEN
                }
            }

            resp.use { response ->
                if (response.isSuccessful) {
                    Log.d("TursoSyncClient", "Successfully executed SQL on Turso: $sql")
                    true
                } else {
                    Log.e("TursoSyncClient", "Turso HTTP Error ${response.code}: ${response.body?.string()}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("TursoSyncClient", "Failed to sync with Turso cloud", e)
            false
        }
    }
}
