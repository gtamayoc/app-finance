package com.gtc.app_finance.cloud.infrastructure.memory

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.CloudUser
import com.gtc.app_finance.cloud.domain.ports.IAuthService
import java.util.UUID

class InMemoryAuthAdapter(
    initialUser: CloudUser? = null
) : IAuthService {

    private var currentUser: CloudUser? = initialUser
    private val validUsers = mutableMapOf<String, String>(
        "test@example.com" to "password123",
        "admin@finance.app" to "adminPass!"
    )

    override suspend fun getCurrentUser(): CloudResult<CloudUser?> {
        return CloudResult.Success(currentUser)
    }

    override suspend fun signIn(credentials: Map<String, String>): CloudResult<CloudUser> {
        val email = credentials["email"]
        val password = credentials["password"]

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            return CloudResult.Failure(
                CloudError.AuthenticationError.InvalidCredentials("Email and password must not be empty")
            )
        }

        val expectedPass = validUsers[email]
        if (expectedPass == null) {
            return CloudResult.Failure(
                CloudError.AuthenticationError.UserNotFound("User with email $email not registered")
            )
        }

        if (expectedPass != password) {
            return CloudResult.Failure(
                CloudError.AuthenticationError.InvalidCredentials("Invalid password")
            )
        }

        val user = CloudUser(
            id = UUID.nameUUIDFromBytes(email.toByteArray()).toString(),
            email = email,
            displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        )
        currentUser = user
        return CloudResult.Success(user)
    }

    override suspend fun signOut(): CloudResult<Unit> {
        currentUser = null
        return CloudResult.Success(Unit)
    }

    override suspend fun refreshToken(): CloudResult<String> {
        return if (currentUser != null) {
            CloudResult.Success("in_memory_token_${System.currentTimeMillis()}")
        } else {
            CloudResult.Failure(
                CloudError.AuthenticationError.SessionExpired("No authenticated user to refresh token for")
            )
        }
    }

    fun registerMockUser(email: String, password: String) {
        validUsers[email] = password
    }
}
