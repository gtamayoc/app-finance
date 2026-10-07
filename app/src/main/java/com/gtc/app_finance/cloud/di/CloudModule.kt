package com.gtc.app_finance.cloud.di

import com.google.gson.Gson
import com.gtc.app_finance.cloud.domain.ports.IAuthService
import com.gtc.app_finance.cloud.domain.ports.IDatabaseService
import com.gtc.app_finance.cloud.domain.ports.IFunctionsService
import com.gtc.app_finance.cloud.domain.ports.IJsonSerializer
import com.gtc.app_finance.cloud.domain.ports.IStorageService
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import com.gtc.app_finance.cloud.infrastructure.common.GsonJsonSerializer
import com.gtc.app_finance.cloud.infrastructure.factory.CloudServiceFactory
import com.gtc.app_finance.cloud.infrastructure.factory.DefaultCloudConfigProvider
import com.gtc.app_finance.cloud.infrastructure.factory.ICloudConfigProvider
import com.gtc.app_finance.cloud.infrastructure.gcp.LocalFileCredentialsProvider
import org.koin.dsl.module

val cloudModule = module {
    single { Gson() }
    single<IJsonSerializer> { GsonJsonSerializer(gson = get()) }

    single<ICloudConfigProvider> { DefaultCloudConfigProvider() }
    single<ICloudCredentialsProvider> { LocalFileCredentialsProvider() }

    single {
        CloudServiceFactory(
            configProvider = get(),
            credentialsProvider = get(),
            jsonSerializer = get()
        )
    }

    single<IAuthService> { get<CloudServiceFactory>().getAuthService() }
    single<IStorageService> { get<CloudServiceFactory>().getStorageService() }
    single<IDatabaseService> { get<CloudServiceFactory>().getDatabaseService() }
    single<IFunctionsService> { get<CloudServiceFactory>().getFunctionsService() }
}
