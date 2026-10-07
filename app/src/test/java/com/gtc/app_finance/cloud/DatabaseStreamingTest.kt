package com.gtc.app_finance.cloud

import app.cash.turbine.test
import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.GoogleCloudDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryDatabaseAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseStreamingTest {

    private class ValidMockCredentialsProvider : ICloudCredentialsProvider {
        override suspend fun getCredentialsPath(): CloudResult<String> = CloudResult.Success("/valid/gcp/key.json")
        override suspend fun getRawCredentials(): CloudResult<String> = CloudResult.Success("{}")
        override suspend fun hasValidCredentials(): Boolean = true
    }

    @Test
    fun `InMemoryDatabaseAdapter streams document updates in real time`() = runBlocking {
        val db = InMemoryDatabaseAdapter()

        db.streamDocument("budgets", "b_1").test {
            // Initial emission: not found
            val initial = awaitItem()
            assertTrue(initial.isFailure)
            assertTrue(initial.errorOrNull() is CloudError.DatabaseError.DocumentNotFound)

            // Save document -> triggers reactive emission
            val saveResult = db.saveDocument("budgets", "b_1", mapOf("limit" to 1000.0))
            assertTrue(saveResult.isSuccess)
            val updated = awaitItem()
            assertTrue(updated.isSuccess)
            assertEquals(1000.0, (updated as CloudResult.Success).data.data["limit"])

            // Delete document -> triggers reactive emission
            val deleteResult = db.deleteDocument("budgets", "b_1")
            assertTrue(deleteResult.isSuccess)
            val afterDelete = awaitItem()
            assertTrue(afterDelete.isFailure)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `InMemoryDatabaseAdapter streams collection queries reactively`() = runBlocking {
        val db = InMemoryDatabaseAdapter()

        db.streamDocuments("wallets").test {
            // Initial empty emission
            val initial = awaitItem()
            assertTrue(initial.isSuccess)
            assertEquals(0, (initial as CloudResult.Success).data.size)

            // Insert wallet 1
            db.saveDocument("wallets", "w_1", mapOf("name" to "Main Wallet"))
            val firstAdd = awaitItem()
            assertTrue(firstAdd.isSuccess)
            assertEquals(1, (firstAdd as CloudResult.Success).data.size)

            // Insert wallet 2
            db.saveDocument("wallets", "w_2", mapOf("name" to "Savings"))
            val secondAdd = awaitItem()
            assertTrue(secondAdd.isSuccess)
            assertEquals(2, (secondAdd as CloudResult.Success).data.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FirebaseDatabaseAdapter streams real-time updates`() = runBlocking {
        val db = FirebaseDatabaseAdapter()

        db.streamDocument("accounts", "acc_100").test {
            // Initial not found
            val initial = awaitItem()
            assertTrue(initial.isFailure)

            // Save document
            val saveResult = db.saveDocument("accounts", "acc_100", mapOf("balance" to 5000.0))
            assertTrue(saveResult.isSuccess)
            val updated = awaitItem()
            assertTrue(updated.isSuccess)
            assertEquals(5000.0, (updated as CloudResult.Success).data.data["balance"])

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GoogleCloudDatabaseAdapter streams real-time updates with valid credentials`() = runBlocking {
        val creds: ICloudCredentialsProvider = ValidMockCredentialsProvider()
        val db = GoogleCloudDatabaseAdapter(credentialsProvider = creds)

        db.streamDocument("rates", "usd_cop").test {
            val initial = awaitItem()
            assertTrue(initial.isFailure)

            val saveResult = db.saveDocument("rates", "usd_cop", mapOf("rate" to 4200.0))
            assertTrue(saveResult.isSuccess)
            val updated = awaitItem()
            assertTrue(updated.isSuccess)
            assertEquals(4200.0, (updated as CloudResult.Success).data.data["rate"])

            cancelAndIgnoreRemainingEvents()
        }
    }
}
