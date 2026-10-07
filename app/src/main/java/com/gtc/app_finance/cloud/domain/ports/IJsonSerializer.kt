package com.gtc.app_finance.cloud.domain.ports

import com.gtc.app_finance.cloud.domain.error.CloudResult
import java.lang.reflect.Type

interface IJsonSerializer {
    fun <T> toJson(data: T): String
    fun <T> fromJson(json: String, clazz: Class<T>): CloudResult<T>
    fun <T> fromJson(json: String, typeOfT: Type): CloudResult<T>
}
