package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.FunctionPayload
import com.gtc.app_finance.cloud.domain.model.UploadFileRequest
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryAuthAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryFunctionsAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryStorageAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryAdaptersTest {

    @Test
    fun `storage adapter handles upload, download, getDownloadUrl and delete cleanly`() = runBlocking {
        val storage = InMemoryStorageAdapter()
        val content = "Finance report binary content".toByteArray()
        val request = UploadFileRequest(
            path = "reports/2026/march.pdf",
            bytes = content,
            mimeType = "application/pdf"
        )

        // Upload
        val uploadResult = storage.upload(request)
        assertTrue(uploadResult.isSuccess)
        val metadata = uploadResult.getOrNull()
        assertNotNull(metadata)
        assertEquals("march.pdf", metadata?.name)
        assertEquals(content.size.toLong(), metadata?.sizeBytes)

        // Download
        val downloadResult = storage.download("reports/2026/march.pdf")
        assertTrue(downloadResult.isSuccess)
        assertArrayEquals(content, downloadResult.getOrNull())

        // Download URL
        val urlResult = storage.getDownloadUrl("reports/2026/march.pdf")
        assertTrue(urlResult.isSuccess)
        assertEquals("memory://reports/2026/march.pdf", urlResult.getOrNull())

        // Delete
        val deleteResult = storage.delete("reports/2026/march.pdf")
        assertTrue(deleteResult.isSuccess)

        // Download after delete yields FileNotFound
        val downloadAfterDelete = storage.download("reports/2026/march.pdf")
        assertTrue(downloadAfterDelete.isFailure)
        assertTrue(downloadAfterDelete.errorOrNull() is CloudError.StorageError.FileNotFound)
    }

    @Test
    fun `storage adapter rejects file exceeding max size limit`() = runBlocking {
        val storage = InMemoryStorageAdapter(maxSizeBytes = 100)
        val bigData = ByteArray(150)
        val request = UploadFileRequest(path = "big.bin", bytes = bigData)

        val result = storage.upload(request)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is CloudError.StorageError.FileTooLarge)
    }

    @Test
    fun `database adapter supports document CRUD and query filters`() = runBlocking {
        val database = InMemoryDatabaseAdapter()

        // Save
        val docData = mapOf("amount" to 2500.0, "category" to "Food", "currency" to "USD")
        val saveResult = database.saveDocument("transactions", "tx_1", docData)
        assertTrue(saveResult.isSuccess)

        // Get
        val getResult = database.getDocument("transactions", "tx_1")
        assertTrue(getResult.isSuccess)
        assertEquals(2500.0, getResult.getOrNull()?.data?.get("amount"))

        // Query with matching filter
        val queryResult = database.queryDocuments("transactions", mapOf("category" to "Food"))
        assertTrue(queryResult.isSuccess)
        assertEquals(1, queryResult.getOrNull()?.size)

        // Query with non-matching filter
        val nonMatching = database.queryDocuments("transactions", mapOf("category" to "Travel"))
        assertTrue(nonMatching.isSuccess)
        assertEquals(0, nonMatching.getOrNull()?.size)

        // Delete
        val deleteResult = database.deleteDocument("transactions", "tx_1")
        assertTrue(deleteResult.isSuccess)

        // Get after delete yields DocumentNotFound
        val getAfterDelete = database.getDocument("transactions", "tx_1")
        assertTrue(getAfterDelete.isFailure)
        assertTrue(getAfterDelete.errorOrNull() is CloudError.DatabaseError.DocumentNotFound)
    }

    @Test
    fun `auth adapter handles sign in, token refresh and sign out`() = runBlocking {
        val auth = InMemoryAuthAdapter()

        // Initial user is null
        assertNull(auth.getCurrentUser().getOrNull())

        // Invalid credentials
        val invalidLogin = auth.signIn(mapOf("email" to "test@example.com", "password" to "wrongPass"))
        assertTrue(invalidLogin.isFailure)
        assertTrue(invalidLogin.errorOrNull() is CloudError.AuthenticationError.InvalidCredentials)

        // Valid login
        val loginResult = auth.signIn(mapOf("email" to "test@example.com", "password" to "password123"))
        assertTrue(loginResult.isSuccess)
        val user = loginResult.getOrNull()
        assertNotNull(user)
        assertEquals("test@example.com", user?.email)

        // Refresh token
        val tokenResult = auth.refreshToken()
        assertTrue(tokenResult.isSuccess)

        // Sign out
        val logoutResult = auth.signOut()
        assertTrue(logoutResult.isSuccess)
        assertNull(auth.getCurrentUser().getOrNull())

        // Refresh token after sign out fails
        val tokenAfterLogout = auth.refreshToken()
        assertTrue(tokenAfterLogout.isFailure)
        assertTrue(tokenAfterLogout.errorOrNull() is CloudError.AuthenticationError.SessionExpired)
    }

    @Test
    fun `functions adapter supports default invocation and custom mock handlers`() = runBlocking {
        val functions = InMemoryFunctionsAdapter()

        // Default invocation
        val defaultResult = functions.invoke("calculateTax", FunctionPayload("""{"amount": 100}"""))
        assertTrue(defaultResult.isSuccess)
        assertTrue(defaultResult.getOrNull()?.rawData?.contains("calculateTax") == true)

        // Custom registered handler
        functions.registerMockHandler("customFunction") {
            CloudResult.Success(FunctionPayload("""{"processed": true}"""))
        }

        val customResult = functions.invoke("customFunction", FunctionPayload("{}"))
        assertTrue(customResult.isSuccess)
        assertEquals("""{"processed": true}""", customResult.getOrNull()?.rawData)
    }
}
