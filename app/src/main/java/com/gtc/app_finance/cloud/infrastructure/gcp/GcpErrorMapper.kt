package com.gtc.app_finance.cloud.infrastructure.gcp

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.infrastructure.common.ErrorMapper
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class GcpErrorMapper : ErrorMapper {

    override fun map(throwable: Throwable): CloudError {
        val msg = throwable.message ?: throwable.javaClass.simpleName

        when (throwable) {
            is UnknownHostException -> return CloudError.NetworkError.HostUnreachable(
                host = throwable.message,
                cause = throwable
            )
            is ConnectException -> return CloudError.NetworkError.ConnectionLost(
                message = "Failed to establish connection: $msg",
                cause = throwable
            )
            is SocketTimeoutException -> return CloudError.NetworkError.RequestTimeout(
                message = "Socket timed out waiting for response: $msg",
                cause = throwable
            )
            is IOException -> {
                if (msg.contains("timeout", ignoreCase = true)) {
                    return CloudError.NetworkError.RequestTimeout(message = msg, cause = throwable)
                }
            }
        }

        return when {
            msg.contains("UNAUTHENTICATED", ignoreCase = true) || msg.contains("401") -> {
                CloudError.AuthenticationError.InvalidCredentials(
                    message = "GCP authentication failed: $msg",
                    cause = throwable
                )
            }
            msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("403") -> {
                CloudError.AuthenticationError.UnauthorizedAction(
                    message = "GCP permission denied: $msg",
                    cause = throwable
                )
            }
            msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("404") -> {
                CloudError.StorageError.FileNotFound(
                    path = extractPath(msg),
                    message = "GCP resource not found: $msg",
                    cause = throwable
                )
            }
            msg.contains("ALREADY_EXISTS", ignoreCase = true) || msg.contains("409") -> {
                CloudError.DatabaseError.WriteConflict(
                    message = "GCP conflict: $msg",
                    cause = throwable
                )
            }
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("429") -> {
                CloudError.StorageError.QuotaExceeded(
                    message = "GCP quota or rate limit exceeded: $msg",
                    cause = throwable
                )
            }
            msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) || msg.contains("504") -> {
                CloudError.FunctionsError.Timeout(
                    functionName = "gcp_operation",
                    message = "GCP deadline exceeded: $msg",
                    cause = throwable
                )
            }
            msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("503") -> {
                CloudError.NetworkError.HostUnreachable(
                    host = "gcp-service",
                    message = "GCP service temporarily unavailable: $msg",
                    cause = throwable
                )
            }
            msg.contains("INVALID_ARGUMENT", ignoreCase = true) || msg.contains("400") -> {
                CloudError.DatabaseError.InvalidQuery(
                    message = "Invalid argument or query syntax: $msg",
                    cause = throwable
                )
            }
            else -> {
                CloudError.UnknownError(
                    message = "Unhandled GCP error: $msg",
                    cause = throwable
                )
            }
        }
    }

    fun mapStatusCode(statusCode: Int, message: String): CloudError {
        return when (statusCode) {
            400 -> CloudError.DatabaseError.InvalidQuery(message)
            401 -> CloudError.AuthenticationError.InvalidCredentials(message)
            403 -> CloudError.AuthenticationError.UnauthorizedAction(message)
            404 -> CloudError.StorageError.FileNotFound(extractPath(message), message)
            409 -> CloudError.DatabaseError.WriteConflict(message)
            429 -> CloudError.StorageError.QuotaExceeded(message)
            503 -> CloudError.NetworkError.HostUnreachable("gcp-service", message)
            504 -> CloudError.NetworkError.RequestTimeout(message)
            else -> CloudError.UnknownError("GCP error with status code $statusCode: $message")
        }
    }

    private fun extractPath(message: String): String {
        return if (message.contains("path: ")) {
            message.substringAfter("path: ").trim()
        } else if (message.contains("/")) {
            message.substringAfter("/").trim()
        } else {
            "unknown"
        }
    }
}
