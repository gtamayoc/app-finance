package com.gtc.app_finance.cloud.domain.security

import com.gtc.app_finance.cloud.domain.error.CloudResult

interface ICloudCredentialsProvider {
    suspend fun getCredentialsPath(): CloudResult<String>
    suspend fun getRawCredentials(): CloudResult<String>
    suspend fun hasValidCredentials(): Boolean
}
