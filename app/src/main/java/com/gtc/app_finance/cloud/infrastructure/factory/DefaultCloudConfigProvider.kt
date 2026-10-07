package com.gtc.app_finance.cloud.infrastructure.factory

class DefaultCloudConfigProvider(
    private val explicitProvider: CloudProvider? = null
) : ICloudConfigProvider {
    override fun getActiveProvider(): CloudProvider {
        if (explicitProvider != null) return explicitProvider
        val envProvider = System.getenv("CLOUD_PROVIDER")
        return CloudProvider.fromString(envProvider)
    }

    override fun isFallbackAllowed(): Boolean = true
}
