package com.gtc.app_finance.cloud

import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import com.gtc.app_finance.cloud.infrastructure.factory.CloudProvider
import com.gtc.app_finance.cloud.infrastructure.factory.CloudServiceBundle
import com.gtc.app_finance.cloud.infrastructure.factory.CloudServiceFactory
import com.gtc.app_finance.cloud.infrastructure.factory.ICloudConfigProvider
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseAuthAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseFunctionsAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseStorageAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.GoogleCloudStorageAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.LocalFileCredentialsProvider
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryAuthAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryFunctionsAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryStorageAdapter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudServiceFactoryTest {

    private class TestConfigProvider(
        var active: CloudProvider = CloudProvider.IN_MEMORY,
        var allowFallback: Boolean = true
    ) : ICloudConfigProvider {
        override fun getActiveProvider(): CloudProvider = active
        override fun isFallbackAllowed(): Boolean = allowFallback
    }

    private val credentialsProvider: ICloudCredentialsProvider = LocalFileCredentialsProvider()

    @Test
    fun `factory provides in-memory adapters when configured with IN_MEMORY`() {
        val config = TestConfigProvider(active = CloudProvider.IN_MEMORY)
        val factory = CloudServiceFactory(config, credentialsProvider)

        val storage = factory.getStorageService()
        val database = factory.getDatabaseService()
        val auth = factory.getAuthService()
        val functions = factory.getFunctionsService()

        assertTrue(storage is InMemoryStorageAdapter)
        assertTrue(database is InMemoryDatabaseAdapter)
        assertTrue(auth is InMemoryAuthAdapter)
        assertTrue(functions is InMemoryFunctionsAdapter)
    }

    @Test
    fun `factory provides gcp adapters when configured with GCP`() {
        val config = TestConfigProvider(active = CloudProvider.GCP)
        val factory = CloudServiceFactory(config, credentialsProvider)

        val storage = factory.getStorageService()
        assertTrue(storage is GoogleCloudStorageAdapter)
    }

    @Test
    fun `factory provides firebase adapters when configured with FIREBASE`() {
        val config = TestConfigProvider(active = CloudProvider.FIREBASE)
        val factory = CloudServiceFactory(config, credentialsProvider)

        val storage = factory.getStorageService()
        val auth = factory.getAuthService()
        val database = factory.getDatabaseService()
        val functions = factory.getFunctionsService()

        assertTrue(storage is FirebaseStorageAdapter)
        assertTrue(auth is FirebaseAuthAdapter)
        assertTrue(database is FirebaseDatabaseAdapter)
        assertTrue(functions is FirebaseFunctionsAdapter)
    }

    @Test
    fun `factory falls back to in-memory for unmapped providers if fallback allowed`() {
        val config = TestConfigProvider(active = CloudProvider.AWS, allowFallback = true)
        val factory = CloudServiceFactory(config, credentialsProvider)

        val storage = factory.getStorageService()
        assertTrue(storage is InMemoryStorageAdapter)
    }

    @Test
    fun `factory supports custom registration of new providers (Open Closed Principle)`() {
        val config = TestConfigProvider(active = CloudProvider.SUPABASE)
        val factory = CloudServiceFactory(config, credentialsProvider)

        var customCalled = false
        factory.registerProvider(CloudProvider.SUPABASE) {
            customCalled = true
            CloudServiceBundle(
                authService = InMemoryAuthAdapter(),
                storageService = InMemoryStorageAdapter(),
                databaseService = InMemoryDatabaseAdapter(),
                functionsService = InMemoryFunctionsAdapter()
            )
        }

        val bundle = factory.getServiceBundle()
        assertNotNull(bundle)
        assertTrue(customCalled)
    }

    @Test
    fun `dynamic switching of active provider in runtime delivers appropriate adapters`() {
        val config = TestConfigProvider(active = CloudProvider.IN_MEMORY)
        val factory = CloudServiceFactory(config, credentialsProvider)

        // Initial check: IN_MEMORY
        assertTrue(factory.getStorageService() is InMemoryStorageAdapter)

        // Switch to GCP at runtime
        config.active = CloudProvider.GCP
        assertTrue(factory.getStorageService() is GoogleCloudStorageAdapter)

        // Switch to Firebase at runtime
        config.active = CloudProvider.FIREBASE
        assertTrue(factory.getStorageService() is FirebaseStorageAdapter)

        // Switch back to IN_MEMORY
        config.active = CloudProvider.IN_MEMORY
        assertTrue(factory.getStorageService() is InMemoryStorageAdapter)
    }
}
