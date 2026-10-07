package com.gtc.app_finance.cloud.domain.ports

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.CloudUser

interface IAuthService {
    suspend fun getCurrentUser(): CloudResult<CloudUser?>
    suspend fun signIn(credentials: Map<String, String>): CloudResult<CloudUser>
    suspend fun signOut(): CloudResult<Unit>
    suspend fun refreshToken(): CloudResult<String>
}
