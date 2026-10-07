package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FirebaseErrorMapperTest {

    private val mapper = FirebaseErrorMapper()

    @Test
    fun `maps Firebase Auth error codes to specific CloudError types`() {
        val wrongPass = Exception("com.google.firebase.auth.FirebaseAuthInvalidCredentialsException: auth/wrong-password")
        val mappedPass = mapper.map(wrongPass)
        assertTrue(mappedPass is CloudError.AuthenticationError.InvalidCredentials)

        val userNotFound = Exception("auth/user-not-found: User not found in Firebase")
        val mappedNotFound = mapper.map(userNotFound)
        assertTrue(mappedNotFound is CloudError.AuthenticationError.UserNotFound)

        val tokenExpired = Exception("auth/id-token-expired")
        val mappedToken = mapper.map(tokenExpired)
        assertTrue(mappedToken is CloudError.AuthenticationError.SessionExpired)

        val denied = Exception("permission-denied: User lacks role")
        val mappedDenied = mapper.map(denied)
        assertTrue(mappedDenied is CloudError.AuthenticationError.UnauthorizedAction)
    }

    @Test
    fun `maps Firebase Storage and Firestore error codes`() {
        val storageNotFound = Exception("object-not-found: path: avatars/user_1.png")
        val mappedStorage = mapper.map(storageNotFound)
        assertTrue(mappedStorage is CloudError.StorageError.FileNotFound)
        assertEquals("avatars/user_1.png", (mappedStorage as CloudError.StorageError.FileNotFound).path)

        val quotaExceeded = Exception("quota-exceeded: Storage limit reached")
        assertTrue(mapper.map(quotaExceeded) is CloudError.StorageError.QuotaExceeded)

        val alreadyExists = Exception("already-exists: Document tx_100 already exists")
        assertTrue(mapper.map(alreadyExists) is CloudError.DatabaseError.WriteConflict)

        val invalidArg = Exception("invalid-argument: Malformed query filter")
        assertTrue(mapper.map(invalidArg) is CloudError.DatabaseError.InvalidQuery)
    }

    @Test
    fun `maps network exceptions to NetworkError variants`() {
        assertTrue(mapper.map(UnknownHostException("firebase.googleapis.com")) is CloudError.NetworkError.HostUnreachable)
        assertTrue(mapper.map(ConnectException("Connection refused")) is CloudError.NetworkError.ConnectionLost)
        assertTrue(mapper.map(SocketTimeoutException("Read timeout")) is CloudError.NetworkError.RequestTimeout)
    }
}
