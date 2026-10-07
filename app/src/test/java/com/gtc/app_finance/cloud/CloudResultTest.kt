package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudResultTest {

    @Test
    fun `success result holds data and has correct flags`() {
        val result = CloudResult.Success("test_data")

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("test_data", result.getOrNull())
        assertNull(result.errorOrNull())
        assertEquals("test_data", result.getOrElse("fallback"))
    }

    @Test
    fun `failure result holds error and has correct flags`() {
        val error = CloudError.UnknownError("test_error")
        val result: CloudResult<String> = CloudResult.Failure(error)

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(error, result.errorOrNull())
        assertEquals("fallback", result.getOrElse("fallback"))
    }

    @Test
    fun `map transforms success value and preserves failure`() {
        val success: CloudResult<Int> = CloudResult.Success(10)
        val mappedSuccess = success.map { it * 2 }
        assertEquals(20, mappedSuccess.getOrNull())

        val failure: CloudResult<Int> = CloudResult.Failure(CloudError.UnknownError("fail"))
        val mappedFailure = failure.map { it * 2 }
        assertTrue(mappedFailure.isFailure)
    }

    @Test
    fun `flatMap chains successful computations and handles failure in chain`() {
        val initial: CloudResult<String> = CloudResult.Success("123")
        val chainedSuccess = initial.flatMap { str ->
            CloudResult.Success(str.toInt())
        }
        assertEquals(123, chainedSuccess.getOrNull())

        val chainedFailure = initial.flatMap {
            CloudResult.Failure(CloudError.UnknownError("failed in flatMap"))
        }
        assertTrue(chainedFailure.isFailure)
    }

    @Test
    fun `fold executes appropriate branch`() {
        val success: CloudResult<String> = CloudResult.Success("hello")
        val successFolded = success.fold(
            onSuccess = { "success: $it" },
            onFailure = { "error" }
        )
        assertEquals("success: hello", successFolded)

        val failure: CloudResult<String> = CloudResult.Failure(CloudError.UnknownError("bad"))
        val failureFolded = failure.fold(
            onSuccess = { "success: $it" },
            onFailure = { "error: ${it.message}" }
        )
        assertEquals("error: bad", failureFolded)
    }

    @Test
    fun `onSuccess and onFailure callbacks trigger correctly`() {
        var successCalled = false
        var failureCalled = false

        CloudResult.Success(42).onSuccess { successCalled = true }
        assertTrue(successCalled)

        CloudResult.Failure(CloudError.UnknownError("fail")).onFailure { failureCalled = true }
        assertTrue(failureCalled)
    }
}
