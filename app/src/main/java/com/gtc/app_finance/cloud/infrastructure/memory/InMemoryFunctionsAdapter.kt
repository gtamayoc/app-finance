package com.gtc.app_finance.cloud.infrastructure.memory

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.FunctionPayload
import com.gtc.app_finance.cloud.domain.ports.IFunctionsService
import java.util.concurrent.ConcurrentHashMap

class InMemoryFunctionsAdapter : IFunctionsService {

    private val handlers = ConcurrentHashMap<String, suspend (FunctionPayload) -> CloudResult<FunctionPayload>>()

    override suspend fun invoke(
        functionName: String,
        payload: FunctionPayload
    ): CloudResult<FunctionPayload> {
        val handler = handlers[functionName]
        return if (handler != null) {
            handler(payload)
        } else {
            CloudResult.Success(
                FunctionPayload(
                    rawData = """{"echo":true,"function":"$functionName","payload":${payload.rawData.ifBlank { "{}" }}}""",
                    headers = mapOf("X-Mock-Execution" to "true")
                )
            )
        }
    }

    fun registerMockHandler(
        functionName: String,
        handler: suspend (FunctionPayload) -> CloudResult<FunctionPayload>
    ) {
        handlers[functionName] = handler
    }

    fun clear() {
        handlers.clear()
    }
}
