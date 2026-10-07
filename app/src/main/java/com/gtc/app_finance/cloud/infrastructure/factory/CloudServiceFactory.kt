package com.gtc.app_finance.cloud.infrastructure.factory

import com.gtc.app_finance.cloud.domain.ports.IAuthService
import com.gtc.app_finance.cloud.domain.ports.IDatabaseService
import com.gtc.app_finance.cloud.domain.ports.IFunctionsService
import com.gtc.app_finance.cloud.domain.ports.IJsonSerializer
import com.gtc.app_finance.cloud.domain.ports.IStorageService
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseAuthAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseFunctionsAdapter
import com.gtc.app_finance.cloud.infrastructure.firebase.FirebaseStorageAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.GoogleCloudAuthAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.GoogleCloudDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.GoogleCloudFunctionsAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.GoogleCloudStorageAdapter
import com.gtc.app_finance.cloud.infrastructure.gcp.ServiceAccountTokenProvider
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryAuthAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryDatabaseAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryFunctionsAdapter
import com.gtc.app_finance.cloud.infrastructure.memory.InMemoryStorageAdapter
import java.util.concurrent.ConcurrentHashMap

data class CloudServiceBundle(
    val authService: IAuthService,
    val storageService: IStorageService,
    val databaseService: IDatabaseService,
    val functionsService: IFunctionsService
)

class CloudServiceFactory(
    private val configProvider: ICloudConfigProvider,
    private val credentialsProvider: ICloudCredentialsProvider,
    private val jsonSerializer: IJsonSerializer? = null
) {
    private val customRegistrations = ConcurrentHashMap<CloudProvider, () -> CloudServiceBundle>()

    private val inMemoryBundle by lazy {
        CloudServiceBundle(
            authService = InMemoryAuthAdapter(),
            storageService = InMemoryStorageAdapter(),
            databaseService = InMemoryDatabaseAdapter(),
            functionsService = InMemoryFunctionsAdapter()
        )
    }

    private val gcpBundle by lazy {
        val tokenProvider = if (jsonSerializer != null) {
            ServiceAccountTokenProvider(credentialsProvider, jsonSerializer)
        } else {
            null
        }
        CloudServiceBundle(
            authService = GoogleCloudAuthAdapter(credentialsProvider, tokenProvider),
            storageService = GoogleCloudStorageAdapter(credentialsProvider),
            databaseService = GoogleCloudDatabaseAdapter(credentialsProvider),
            functionsService = GoogleCloudFunctionsAdapter(credentialsProvider)
        )
    }

    private val firebaseBundle by lazy {
        CloudServiceBundle(
            authService = FirebaseAuthAdapter(),
            storageService = FirebaseStorageAdapter(),
            databaseService = FirebaseDatabaseAdapter(),
            functionsService = FirebaseFunctionsAdapter()
        )
    }

    fun registerProvider(provider: CloudProvider, factory: () -> CloudServiceBundle) {
        customRegistrations[provider] = factory
    }

    fun getServiceBundle(): CloudServiceBundle {
        val targetProvider = configProvider.getActiveProvider()

        val customFactory = customRegistrations[targetProvider]
        if (customFactory != null) {
            return customFactory()
        }

        return when (targetProvider) {
            CloudProvider.GCP -> gcpBundle
            CloudProvider.FIREBASE -> firebaseBundle
            CloudProvider.IN_MEMORY -> inMemoryBundle
            CloudProvider.AWS,
            CloudProvider.SUPABASE -> {
                if (configProvider.isFallbackAllowed()) {
                    inMemoryBundle
                } else {
                    throw UnsupportedOperationException(
                        "Provider $targetProvider adapter not implemented in Phase 2 and fallback is disabled."
                    )
                }
            }
        }
    }

    fun getAuthService(): IAuthService = getServiceBundle().authService
    fun getStorageService(): IStorageService = getServiceBundle().storageService
    fun getDatabaseService(): IDatabaseService = getServiceBundle().databaseService
    fun getFunctionsService(): IFunctionsService = getServiceBundle().functionsService
}
