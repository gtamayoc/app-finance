package com.gtc.app_finance.cloud.infrastructure.memory

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.StorageItemMetadata
import com.gtc.app_finance.cloud.domain.model.UploadFileRequest
import com.gtc.app_finance.cloud.domain.ports.IStorageService
import java.util.concurrent.ConcurrentHashMap

class InMemoryStorageAdapter(
    private val maxSizeBytes: Long = 10 * 1024 * 1024 // 10 MB default limit
) : IStorageService {

    private val storageMap = ConcurrentHashMap<String, ByteArray>()
    private val metadataMap = ConcurrentHashMap<String, StorageItemMetadata>()

    override suspend fun upload(request: UploadFileRequest): CloudResult<StorageItemMetadata> {
        if (request.path.isBlank()) {
            return CloudResult.Failure(
                CloudError.StorageError.FileNotFound(
                    path = request.path,
                    message = "Target storage path cannot be empty"
                )
            )
        }

        if (request.bytes.size > maxSizeBytes) {
            return CloudResult.Failure(
                CloudError.StorageError.FileTooLarge(
                    sizeBytes = request.bytes.size.toLong(),
                    maxSizeBytes = maxSizeBytes
                )
            )
        }

        storageMap[request.path] = request.bytes
        val fileName = request.path.substringAfterLast("/")
        val metadata = StorageItemMetadata(
            path = request.path,
            name = fileName,
            sizeBytes = request.bytes.size.toLong(),
            mimeType = request.mimeType,
            downloadUrl = "memory://${request.path}",
            createdAtEpochMs = System.currentTimeMillis(),
            customMetadata = request.customMetadata
        )
        metadataMap[request.path] = metadata
        return CloudResult.Success(metadata)
    }

    override suspend fun download(path: String): CloudResult<ByteArray> {
        val bytes = storageMap[path]
        return if (bytes != null) {
            CloudResult.Success(bytes.copyOf())
        } else {
            CloudResult.Failure(CloudError.StorageError.FileNotFound(path))
        }
    }

    override suspend fun delete(path: String): CloudResult<Unit> {
        return if (storageMap.remove(path) != null) {
            metadataMap.remove(path)
            CloudResult.Success(Unit)
        } else {
            CloudResult.Failure(CloudError.StorageError.FileNotFound(path))
        }
    }

    override suspend fun getDownloadUrl(path: String): CloudResult<String> {
        val metadata = metadataMap[path]
        return if (metadata != null) {
            CloudResult.Success(metadata.downloadUrl ?: "memory://$path")
        } else {
            CloudResult.Failure(CloudError.StorageError.FileNotFound(path))
        }
    }

    fun clear() {
        storageMap.clear()
        metadataMap.clear()
    }
}
