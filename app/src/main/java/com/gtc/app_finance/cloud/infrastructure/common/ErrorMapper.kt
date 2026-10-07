package com.gtc.app_finance.cloud.infrastructure.common

import com.gtc.app_finance.cloud.domain.error.CloudError

interface ErrorMapper {
    fun map(throwable: Throwable): CloudError
}
