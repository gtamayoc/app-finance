package com.gtc.app_finance.cloud.infrastructure.firebase

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.FunctionPayload
import com.gtc.app_finance.cloud.domain.ports.IFunctionsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseFunctionsAdapter(
    private val region: String = "us-central1",
    private val errorMapper: FirebaseErrorMapper = FirebaseErrorMapper()
) : IFunctionsService {

    override suspend fun invoke(
        functionName: String,
        payload: FunctionPayload
    ): CloudResult<FunctionPayload> = withContext(Dispatchers.IO) {
        if (functionName.isBlank()) {
            return@withContext CloudResult.Failure(
                errorMapper.map(Exception("invalid-argument: Function name cannot be blank"))
            )
        }

        val resultPayload = FunctionPayload(
            rawData = """{"status":"OK","provider":"firebase","function":"$functionName","region":"$region"}""",
            headers = mapOf(
                "X-Firebase-Function" to functionName,
                "Content-Type" to "application/json"
            )
        )
        CloudResult.Success(resultPayload)
    }
}
