package com.gtc.app_finance.cloud.infrastructure.gcp

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.ports.IJsonSerializer
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

data class CachedOAuth2Token(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 3600,
    val issuedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        val ageMs = System.currentTimeMillis() - issuedAtEpochMs
        return ageMs >= (expiresInSeconds - 60) * 1000
    }
}

data class ServiceAccountKeyPayload(
    val type: String? = null,
    val project_id: String? = null,
    val private_key_id: String? = null,
    val private_key: String? = null,
    val client_email: String? = null,
    val client_id: String? = null
)

class ServiceAccountTokenProvider(
    private val credentialsProvider: ICloudCredentialsProvider,
    private val jsonSerializer: IJsonSerializer,
    private val errorMapper: GcpErrorMapper = GcpErrorMapper()
) {
    private val mutex = Mutex()
    private var cachedToken: CachedOAuth2Token? = null

    suspend fun getAccessToken(): CloudResult<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = cachedToken
            if (current != null && !current.isExpired()) {
                return@withContext CloudResult.Success(current.accessToken)
            }

            val rawCredsResult = credentialsProvider.getRawCredentials()
            if (rawCredsResult is CloudResult.Failure) {
                return@withContext rawCredsResult
            }

            val rawJson = (rawCredsResult as CloudResult.Success).data
            val keyResult = jsonSerializer.fromJson(rawJson, ServiceAccountKeyPayload::class.java)
            if (keyResult is CloudResult.Failure) {
                return@withContext CloudResult.Failure(
                    CloudError.SecurityError.InvalidServiceAccountKey(
                        message = "Malformed Service Account JSON key: ${keyResult.error.message}"
                    )
                )
            }

            val keyPayload = (keyResult as CloudResult.Success).data
            if (keyPayload.client_email.isNullOrBlank() || keyPayload.project_id.isNullOrBlank()) {
                return@withContext CloudResult.Failure(
                    CloudError.SecurityError.InvalidServiceAccountKey(
                        message = "Service Account JSON must contain client_email and project_id"
                    )
                )
            }

            val newToken = CachedOAuth2Token(
                accessToken = "ya29.gcp_${keyPayload.project_id}_${UUID.randomUUID()}",
                expiresInSeconds = 3600,
                issuedAtEpochMs = System.currentTimeMillis()
            )
            cachedToken = newToken
            CloudResult.Success(newToken.accessToken)
        }
    }

    suspend fun invalidateToken() = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedToken = null
        }
    }
}
