package com.gtc.app_finance.cloud.infrastructure.firebase

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.infrastructure.common.ErrorMapper
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class FirebaseErrorMapper : ErrorMapper {

    override fun map(throwable: Throwable): CloudError {
        val msg = throwable.message ?: throwable.javaClass.simpleName

        when (throwable) {
            is UnknownHostException -> return CloudError.NetworkError.HostUnreachable(
                host = throwable.message,
                cause = throwable
            )
            is ConnectException -> return CloudError.NetworkError.ConnectionLost(
                message = "Failed to connect to Firebase: $msg",
                cause = throwable
            )
            is SocketTimeoutException -> return CloudError.NetworkError.RequestTimeout(
                message = "Firebase network timeout: $msg",
                cause = throwable
            )
            is IOException -> {
                if (msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true)) {
                    return CloudError.NetworkError.ConnectionLost(message = msg, cause = throwable)
                }
            }
        }

        return when {
            msg.contains("auth/invalid-credential", ignoreCase = true) ||
            msg.contains("auth/wrong-password", ignoreCase = true) ||
            msg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) -> {
                CloudError.AuthenticationError.InvalidCredentials(
                    message = "Invalid password or credentials: $msg",
                    cause = throwable
                )
            }
            msg.contains("auth/user-not-found", ignoreCase = true) ||
            msg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) -> {
                CloudError.AuthenticationError.UserNotFound(
                    message = "No Firebase user registered: $msg",
                    cause = throwable
                )
            }
            msg.contains("auth/id-token-expired", ignoreCase = true) ||
            msg.contains("auth/user-token-expired", ignoreCase = true) -> {
                CloudError.AuthenticationError.SessionExpired(
                    message = "Firebase session expired: $msg",
                    cause = throwable
                )
            }
            msg.contains("auth/user-disabled", ignoreCase = true) ||
            msg.contains("permission-denied", ignoreCase = true) ||
            msg.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                CloudError.AuthenticationError.UnauthorizedAction(
                    message = "Firebase permission denied or account disabled: $msg",
                    cause = throwable
                )
            }
            msg.contains("object-not-found", ignoreCase = true) ||
            msg.contains("not-found", ignoreCase = true) -> {
                CloudError.StorageError.FileNotFound(
                    path = extractPath(msg),
                    message = "Firebase resource not found: $msg",
                    cause = throwable
                )
            }
            msg.contains("quota-exceeded", ignoreCase = true) ||
            msg.contains("resource-exhausted", ignoreCase = true) -> {
                CloudError.StorageError.QuotaExceeded(
                    message = "Firebase quota exceeded: $msg",
                    cause = throwable
                )
            }
            msg.contains("already-exists", ignoreCase = true) -> {
                CloudError.DatabaseError.WriteConflict(
                    message = "Firebase document already exists: $msg",
                    cause = throwable
                )
            }
            msg.contains("invalid-argument", ignoreCase = true) -> {
                CloudError.DatabaseError.InvalidQuery(
                    message = "Invalid Firestore query argument: $msg",
                    cause = throwable
                )
            }
            msg.contains("deadline-exceeded", ignoreCase = true) -> {
                CloudError.FunctionsError.Timeout(
                    functionName = "firebase_operation",
                    message = "Firebase operation deadline exceeded: $msg",
                    cause = throwable
                )
            }
            else -> {
                CloudError.UnknownError(
                    message = "Unhandled Firebase error: $msg",
                    cause = throwable
                )
            }
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
