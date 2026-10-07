package com.gtc.app_finance.cloud.infrastructure.gcp

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import java.io.File

class LocalFileCredentialsProvider(
    private val configuredPath: String? = null
) : ICloudCredentialsProvider {

    override suspend fun getCredentialsPath(): CloudResult<String> {
        val path = configuredPath
            ?: System.getenv("GCP_CREDENTIALS_PATH")
            ?: System.getenv("GOOGLE_APPLICATION_CREDENTIALS")

        return if (!path.isNullOrBlank() && File(path).exists()) {
            CloudResult.Success(path)
        } else {
            CloudResult.Failure(
                CloudError.SecurityError.MissingCredentials(
                    message = "GCP Service Account credentials not found at: ${path ?: "unspecified path"}"
                )
            )
        }
    }

    override suspend fun getRawCredentials(): CloudResult<String> {
        return when (val pathResult = getCredentialsPath()) {
            is CloudResult.Success -> {
                try {
                    val content = File(pathResult.data).readText()
                    CloudResult.Success(content)
                } catch (e: Exception) {
                    CloudResult.Failure(
                        CloudError.SecurityError.InvalidServiceAccountKey(
                            message = "Failed to read credentials file: ${e.message}",
                            cause = e
                        )
                    )
                }
            }
            is CloudResult.Failure -> pathResult
        }
    }

    override suspend fun hasValidCredentials(): Boolean {
        val path = configuredPath
            ?: System.getenv("GCP_CREDENTIALS_PATH")
            ?: System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        return !path.isNullOrBlank() && File(path).exists()
    }
}
