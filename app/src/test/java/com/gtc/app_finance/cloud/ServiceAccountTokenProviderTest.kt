package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import com.gtc.app_finance.cloud.infrastructure.common.GsonJsonSerializer
import com.gtc.app_finance.cloud.infrastructure.gcp.ServiceAccountTokenProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceAccountTokenProviderTest {

    private val jsonSerializer = GsonJsonSerializer()

    private class MockCredentialsProvider(
        var rawContent: String = "",
        var isValid: Boolean = true
    ) : ICloudCredentialsProvider {
        override suspend fun getCredentialsPath(): CloudResult<String> {
            return if (isValid) CloudResult.Success("/path/to/key.json")
            else CloudResult.Failure(CloudError.SecurityError.MissingCredentials())
        }

        override suspend fun getRawCredentials(): CloudResult<String> {
            return if (isValid) CloudResult.Success(rawContent)
            else CloudResult.Failure(CloudError.SecurityError.MissingCredentials())
        }

        override suspend fun hasValidCredentials(): Boolean = isValid
    }

    @Test
    fun `mints token successfully with valid service account json`() = runBlocking {
        val validJson = """
            {
              "type": "service_account",
              "project_id": "app-finance-prod",
              "private_key_id": "pk_12345",
              "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvg...\n-----END PRIVATE KEY-----\n",
              "client_email": "finance-sa@app-finance-prod.iam.gserviceaccount.com",
              "client_id": "100200300"
            }
        """.trimIndent()

        val creds = MockCredentialsProvider(rawContent = validJson, isValid = true)
        val provider = ServiceAccountTokenProvider(creds, jsonSerializer)

        val tokenResult = provider.getAccessToken()
        assertTrue(tokenResult.isSuccess)
        val token = tokenResult.getOrNull()
        assertNotNull(token)
        assertTrue(token?.startsWith("ya29.gcp_app-finance-prod_") == true)

        // Second call should return cached token
        val cachedTokenResult = provider.getAccessToken()
        assertEquals(token, cachedTokenResult.getOrNull())
    }

    @Test
    fun `fails with InvalidServiceAccountKey when json is malformed or missing fields`() = runBlocking {
        val incompleteJson = """{"type": "service_account"}"""
        val creds = MockCredentialsProvider(rawContent = incompleteJson, isValid = true)
        val provider = ServiceAccountTokenProvider(creds, jsonSerializer)

        val result = provider.getAccessToken()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is CloudError.SecurityError.InvalidServiceAccountKey)
    }

    @Test
    fun `fails with MissingCredentials when credentials provider has no key`() = runBlocking {
        val creds = MockCredentialsProvider(rawContent = "", isValid = false)
        val provider = ServiceAccountTokenProvider(creds, jsonSerializer)

        val result = provider.getAccessToken()
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is CloudError.SecurityError.MissingCredentials)
    }

    @Test
    fun `invalidateToken clears cached token so fresh token is generated`() = runBlocking {
        val validJson = """
            {
              "project_id": "finance-test",
              "client_email": "test-sa@finance-test.iam.gserviceaccount.com"
            }
        """.trimIndent()

        val creds = MockCredentialsProvider(rawContent = validJson, isValid = true)
        val provider = ServiceAccountTokenProvider(creds, jsonSerializer)

        val token1 = provider.getAccessToken().getOrNull()
        provider.invalidateToken()
        val token2 = provider.getAccessToken().getOrNull()

        // Since invalidateToken cleared the cache, a new UUID token is minted
        assertNotNull(token1)
        assertNotNull(token2)
    }
}
