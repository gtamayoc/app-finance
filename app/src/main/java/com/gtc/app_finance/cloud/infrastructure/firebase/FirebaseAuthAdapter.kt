package com.gtc.app_finance.cloud.infrastructure.firebase

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.CloudUser
import com.gtc.app_finance.cloud.domain.ports.IAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseAuthAdapter(
    private val errorMapper: FirebaseErrorMapper = FirebaseErrorMapper()
) : IAuthService {

    private var currentFirebaseUser: CloudUser? = null

    override suspend fun getCurrentUser(): CloudResult<CloudUser?> = withContext(Dispatchers.IO) {
        CloudResult.Success(currentFirebaseUser)
    }

    override suspend fun signIn(credentials: Map<String, String>): CloudResult<CloudUser> = withContext(Dispatchers.IO) {
        val email = credentials["email"]
        val password = credentials["password"]

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            return@withContext CloudResult.Failure(
                errorMapper.map(Exception("auth/invalid-credential: Email and password required"))
            )
        }

        if (!email.contains("@")) {
            return@withContext CloudResult.Failure(
                errorMapper.map(Exception("auth/invalid-email: Malformed email"))
            )
        }

        val user = CloudUser(
            id = "firebase_uid_${UUID.nameUUIDFromBytes(email.toByteArray())}",
            email = email,
            displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            customClaims = mapOf("provider" to "firebase", "auth_time" to System.currentTimeMillis())
        )
        currentFirebaseUser = user
        CloudResult.Success(user)
    }

    override suspend fun signOut(): CloudResult<Unit> = withContext(Dispatchers.IO) {
        currentFirebaseUser = null
        CloudResult.Success(Unit)
    }

    override suspend fun refreshToken(): CloudResult<String> = withContext(Dispatchers.IO) {
        if (currentFirebaseUser == null) {
            return@withContext CloudResult.Failure(
                errorMapper.map(Exception("auth/id-token-expired: No authenticated user session"))
            )
        }
        CloudResult.Success("firebase_id_token_${System.currentTimeMillis()}")
    }
}
