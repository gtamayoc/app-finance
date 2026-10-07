package com.gtc.app_finance.cloud.infrastructure.factory

interface ICloudConfigProvider {
    fun getActiveProvider(): CloudProvider
    fun isFallbackAllowed(): Boolean = true
}
