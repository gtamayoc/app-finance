package com.gtc.app_finance.cloud.domain.error

sealed interface CloudError {
    val message: String
    val cause: Throwable?

    sealed interface AuthenticationError : CloudError {
        data class InvalidCredentials(
            override val message: String = "Invalid user credentials",
            override val cause: Throwable? = null
        ) : AuthenticationError

        data class SessionExpired(
            override val message: String = "Session has expired",
            override val cause: Throwable? = null
        ) : AuthenticationError

        data class UserNotFound(
            override val message: String = "User not found",
            override val cause: Throwable? = null
        ) : AuthenticationError

        data class UnauthorizedAction(
            override val message: String = "Action unauthorized or permission denied",
            override val cause: Throwable? = null
        ) : AuthenticationError
    }

    sealed interface StorageError : CloudError {
        data class FileNotFound(
            val path: String,
            override val message: String = "File not found at: $path",
            override val cause: Throwable? = null
        ) : StorageError

        data class QuotaExceeded(
            override val message: String = "Storage quota exceeded",
            override val cause: Throwable? = null
        ) : StorageError

        data class FileTooLarge(
            val sizeBytes: Long,
            val maxSizeBytes: Long,
            override val message: String = "File size $sizeBytes exceeds maximum $maxSizeBytes",
            override val cause: Throwable? = null
        ) : StorageError

        data class InvalidMimeType(
            val mimeType: String,
            override val message: String = "Unsupported MIME type: $mimeType",
            override val cause: Throwable? = null
        ) : StorageError
    }

    sealed interface DatabaseError : CloudError {
        data class DocumentNotFound(
            val collection: String,
            val id: String,
            override val message: String = "Document $id not found in collection $collection",
            override val cause: Throwable? = null
        ) : DatabaseError

        data class WriteConflict(
            override val message: String = "Database write conflict or concurrent modification",
            override val cause: Throwable? = null
        ) : DatabaseError

        data class InvalidQuery(
            override val message: String = "Invalid query or malformed filter criteria",
            override val cause: Throwable? = null
        ) : DatabaseError

        data class PermissionDenied(
            override val message: String = "Database access denied",
            override val cause: Throwable? = null
        ) : DatabaseError
    }

    sealed interface FunctionsError : CloudError {
        data class ExecutionFailed(
            val functionName: String,
            override val message: String = "Execution of function $functionName failed",
            override val cause: Throwable? = null
        ) : FunctionsError

        data class Timeout(
            val functionName: String,
            override val message: String = "Execution of function $functionName timed out",
            override val cause: Throwable? = null
        ) : FunctionsError

        data class InvalidPayload(
            val details: String,
            override val message: String = "Invalid payload: $details",
            override val cause: Throwable? = null
        ) : FunctionsError
    }

    sealed interface NetworkError : CloudError {
        data class ConnectionLost(
            override val message: String = "Network connection lost or unavailable",
            override val cause: Throwable? = null
        ) : NetworkError

        data class HostUnreachable(
            val host: String? = null,
            override val message: String = "Host unreachable: ${host ?: "unknown"}",
            override val cause: Throwable? = null
        ) : NetworkError

        data class RequestTimeout(
            override val message: String = "Network request timed out",
            override val cause: Throwable? = null
        ) : NetworkError
    }

    sealed interface SecurityError : CloudError {
        data class MissingCredentials(
            override val message: String = "Required cloud credentials are missing",
            override val cause: Throwable? = null
        ) : SecurityError

        data class InvalidServiceAccountKey(
            override val message: String = "Invalid service account key file or payload",
            override val cause: Throwable? = null
        ) : SecurityError

        data class KeystoreAccessFailed(
            override val message: String = "Keystore access failed",
            override val cause: Throwable? = null
        ) : SecurityError
    }

    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : CloudError
}
