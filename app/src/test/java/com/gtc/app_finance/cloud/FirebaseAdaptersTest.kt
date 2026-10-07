package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.model.FunctionPayload
import com.gtc.app_finance.cloud.domain.model.UploadFileRequest
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseAuthAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseFunctionsAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseStorageAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAdaptersTest {

    @Test
    fun `FirebaseAuthAdapter handles login, refresh and logout`() = runBlocking {
        val auth = FirebaseAuthAdapter()

        // Null initially
        assertNull(auth.getCurrentUser().getOrNull())

        // Login with empty credentials
        val emptyLogin = auth.signIn(emptyMap())
        assertTrue(emptyLogin.isFailure)

        // Login with valid credentials
        val validLogin = auth.signIn(mapOf("email" to "tester@firebase.com", "password" to "secret123"))
        assertTrue(validLogin.isSuccess)
        val user = validLogin.getOrNull()
        assertNotNull(user)
        assertEquals("tester@firebase.com", user?.email)
        assertEquals("firebase", user?.customClaims?.get("provider"))

        // Refresh token
        val tokenResult = auth.refreshToken()
        assertTrue(tokenResult.isSuccess)
        assertTrue(tokenResult.getOrNull()?.startsWith("firebase_id_token_") == true)

        // Sign out
        val logoutResult = auth.signOut()
        assertTrue(logoutResult.isSuccess)
        assertNull(auth.getCurrentUser().getOrNull())
    }

    @Test
    fun `FirebaseStorageAdapter handles upload, download and delete`() = runBlocking {
        val storage = FirebaseStorageAdapter()
        val data = "Firebase binary storage test".toByteArray()
        val request = UploadFileRequest(path = "receipts/rec_01.jpg", bytes = data, mimeType = "image/jpeg")

        // Upload
        val uploadResult = storage.upload(request)
        assertTrue(uploadResult.isSuccess)
        val meta = uploadResult.getOrNull()
        assertEquals("rec_01.jpg", meta?.name)
        assertTrue(meta?.downloadUrl?.contains("firebasestorage.googleapis.com") == true)

        // Download
        val downloadResult = storage.download("receipts/rec_01.jpg")
        assertTrue(downloadResult.isSuccess)
        assertArrayEquals(data, downloadResult.getOrNull())

        // Delete
        val deleteResult = storage.delete("receipts/rec_01.jpg")
        assertTrue(deleteResult.isSuccess)

        // Download after delete
        val downloadAfterDelete = storage.download("receipts/rec_01.jpg")
        assertTrue(downloadAfterDelete.isFailure)
    }

    @Test
    fun `FirebaseFunctionsAdapter executes functions payload`() = runBlocking {
        val functions = FirebaseFunctionsAdapter()
        val payload = FunctionPayload("""{"process": true}""")

        val result = functions.invoke("generateMonthlyPdf", payload)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.rawData?.contains("firebase") == true)
    }
}
