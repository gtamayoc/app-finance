package com.gtc.app_finance.cloud.domain.ports

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.FunctionPayload

interface IFunctionsService {
    suspend fun invoke(functionName: String, payload: FunctionPayload): CloudResult<FunctionPayload>
}
