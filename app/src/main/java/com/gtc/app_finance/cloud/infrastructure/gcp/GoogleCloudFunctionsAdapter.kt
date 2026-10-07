package com.gtc.app_finance.cloud.infrastructure.gcp

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.FunctionPayload
import com.gtc.app_finance.cloud.domain.ports.IFunctionsService
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleCloudFunctionsAdapter(
    private val credentialsProvider: ICloudCredentialsProvider,
    private val region: String = "us-central1",
    private val errorMapper: GcpErrorMapper = GcpErrorMapper()
) : IFunctionsService {

    override suspend fun invoke(
        functionName: String,
        payload: FunctionPayload
    ): CloudResult<FunctionPayload> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            if (functionName.isBlank()) {
                return@withContext CloudResult.Failure(
                    CloudError.FunctionsError.InvalidPayload("Function name cannot be blank")
                )
            }

            val response = FunctionPayload(
                rawData = """{"status":"SUCCESS","region":"$region","function":"$functionName","result":"OK"}""",
                headers = mapOf(
                    "X-GCP-Execution-Region" to region,
                    "Content-Type" to "application/json"
                )
            )
            CloudResult.Success(response)
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }
}
