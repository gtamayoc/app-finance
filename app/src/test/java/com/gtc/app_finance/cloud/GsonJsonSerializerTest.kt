package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.model.CloudUser
import com.gtc.app_finance.cloud.infrastructure.common.GsonJsonSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GsonJsonSerializerTest {

    private val serializer = GsonJsonSerializer()

    @Test
    fun `serialize and deserialize model successfully`() {
        val user = CloudUser(
            id = "user_123",
            email = "user@test.com",
            displayName = "Test User"
        )

        val json = serializer.toJson(user)
        assertTrue(json.contains("user_123"))
        assertTrue(json.contains("user@test.com"))

        val result = serializer.fromJson(json, CloudUser::class.java)
        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
    }

    @Test
    fun `deserialize invalid json returns Failure with FunctionsError InvalidPayload`() {
        val invalidJson = "{ invalid: json "

        val result = serializer.fromJson(invalidJson, CloudUser::class.java)
        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is CloudError.FunctionsError.InvalidPayload)
    }
}
