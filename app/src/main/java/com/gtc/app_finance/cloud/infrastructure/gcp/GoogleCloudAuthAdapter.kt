package com.gtc.app_finance.cloud.infrastructure.gcp

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.CloudUser
import com.gtc.app_finance.cloud.domain.ports.IAuthService
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleCloudAuthAdapter(
    private val credentialsProvider: ICloudCredentialsProvider,
    private val errorMapper: GcpErrorMapper = GcpErrorMapper()
) : IAuthService {

    private var activeUser: CloudUser? = null

    override suspend fun getCurrentUser(): CloudResult<CloudUser?> = withContext(Dispatchers.IO) {
        CloudResult.Success(activeUser)
    }

    override suspend fun signIn(credentials: Map<String, String>): CloudResult<CloudUser> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credResult = credentialsProvider.getCredentialsPath()
                if (credResult is CloudResult.Failure) {
                    return@withContext credResult
                }
            }

            val email = credentials["email"]
            if (email.isNullOrBlank()) {
                return@withContext CloudResult.Failure(
                    CloudError.AuthenticationError.InvalidCredentials("GCP service identity email cannot be empty")
                )
            }

            val user = CloudUser(
                id = "gcp_sa_${email.hashCode()}",
                email = email,
                displayName = "GCP Service Principal ($email)"
            )
            activeUser = user
            CloudResult.Success(user)
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun signOut(): CloudResult<Unit> = withContext(Dispatchers.IO) {
        activeUser = null
        CloudResult.Success(Unit)
    }

    override suspend fun refreshToken(): CloudResult<String> = withContext(Dispatchers.IO) {
        try {
            if (activeUser == null) {
                return@withContext CloudResult.Failure(
                    CloudError.AuthenticationError.SessionExpired("No active GCP session to refresh")
                )
            }
            CloudResult.Success("gcp_bearer_token_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }
}
