package com.gtc.app_finance.cloud.infrastructure.common

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.ports.IJsonSerializer
import java.lang.reflect.Type

class GsonJsonSerializer(
    private val gson: Gson = Gson()
) : IJsonSerializer {

    override fun <T> toJson(data: T): String {
        return gson.toJson(data)
    }

    override fun <T> fromJson(json: String, clazz: Class<T>): CloudResult<T> {
        return try {
            val result = gson.fromJson(json, clazz)
            if (result != null) {
                CloudResult.Success(result)
            } else {
                CloudResult.Failure(CloudError.UnknownError("Parsed JSON yielded null for class ${clazz.name}"))
            }
        } catch (e: JsonSyntaxException) {
            CloudResult.Failure(
                CloudError.FunctionsError.InvalidPayload(
                    details = "JSON syntax error: ${e.message}",
                    cause = e
                )
            )
        } catch (e: Exception) {
            CloudResult.Failure(CloudError.UnknownError("Failed to parse JSON: ${e.message}", e))
        }
    }

    override fun <T> fromJson(json: String, typeOfT: Type): CloudResult<T> {
        return try {
            val result: T? = gson.fromJson(json, typeOfT)
            if (result != null) {
                CloudResult.Success(result)
            } else {
                CloudResult.Failure(CloudError.UnknownError("Parsed JSON yielded null for type $typeOfT"))
            }
        } catch (e: JsonSyntaxException) {
            CloudResult.Failure(
                CloudError.FunctionsError.InvalidPayload(
                    details = "JSON syntax error: ${e.message}",
                    cause = e
                )
            )
        } catch (e: Exception) {
            CloudResult.Failure(CloudError.UnknownError("Failed to parse JSON: ${e.message}", e))
        }
    }
}
