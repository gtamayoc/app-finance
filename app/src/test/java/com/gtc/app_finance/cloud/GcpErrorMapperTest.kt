package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.infrastructure.gcp.GcpErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class GcpErrorMapperTest {

    private val mapper = GcpErrorMapper()

    @Test
    fun `maps network exceptions to NetworkError variants`() {
        val unknownHost = UnknownHostException("storage.googleapis.com")
        val mappedHost = mapper.map(unknownHost)
        assertTrue(mappedHost is CloudError.NetworkError.HostUnreachable)

        val connect = ConnectException("Connection refused")
        val mappedConnect = mapper.map(connect)
        assertTrue(mappedConnect is CloudError.NetworkError.ConnectionLost)

        val timeout = SocketTimeoutException("Read timed out")
        val mappedTimeout = mapper.map(timeout)
        assertTrue(mappedTimeout is CloudError.NetworkError.RequestTimeout)
    }

    @Test
    fun `maps gRPC and HTTP status codes correctly`() {
        // Status code tests
        assertTrue(mapper.mapStatusCode(400, "Bad query") is CloudError.DatabaseError.InvalidQuery)
        assertTrue(mapper.mapStatusCode(401, "Unauthorized") is CloudError.AuthenticationError.InvalidCredentials)
        assertTrue(mapper.mapStatusCode(403, "Forbidden") is CloudError.AuthenticationError.UnauthorizedAction)
        assertTrue(mapper.mapStatusCode(404, "Not found") is CloudError.StorageError.FileNotFound)
        assertTrue(mapper.mapStatusCode(409, "Conflict") is CloudError.DatabaseError.WriteConflict)
        assertTrue(mapper.mapStatusCode(429, "Rate limited") is CloudError.StorageError.QuotaExceeded)
        assertTrue(mapper.mapStatusCode(503, "Unavailable") is CloudError.NetworkError.HostUnreachable)
        assertTrue(mapper.mapStatusCode(504, "Gateway timeout") is CloudError.NetworkError.RequestTimeout)

        // String message patterns
        val unauthEx = IOException("Status: UNAUTHENTICATED: Invalid JWT")
        assertTrue(mapper.map(unauthEx) is CloudError.AuthenticationError.InvalidCredentials)

        val permEx = IOException("Status: PERMISSION_DENIED: User lacks roles/storage.objectViewer")
        assertTrue(mapper.map(permEx) is CloudError.AuthenticationError.UnauthorizedAction)

        val notFoundEx = IOException("Status: NOT_FOUND: path: invoices/inv_1.pdf")
        val mappedNotFound = mapper.map(notFoundEx)
        assertTrue(mappedNotFound is CloudError.StorageError.FileNotFound)
        assertEquals("invoices/inv_1.pdf", (mappedNotFound as CloudError.StorageError.FileNotFound).path)
    }
}
