package com.gtc.app_finance.cloud.domain.model

data class UploadFileRequest(
    val path: String,
    val bytes: ByteArray,
    val mimeType: String = "application/octet-stream",
    val customMetadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UploadFileRequest
        if (path != other.path) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        if (customMetadata != other.customMetadata) return false
        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + customMetadata.hashCode()
        return result
    }
}
