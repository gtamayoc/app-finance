package com.gtc.app_finance.cloud.infrastructure.gcp

import com.gtc.app_finance.cloud.domain.error.CloudError
import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.StorageItemMetadata
import com.gtc.app_finance.cloud.domain.model.UploadFileRequest
import com.gtc.app_finance.cloud.domain.ports.IStorageService
import com.gtc.app_finance.cloud.domain.security.ICloudCredentialsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class GoogleCloudStorageAdapter(
    private val credentialsProvider: ICloudCredentialsProvider,
    private val bucketName: String = "app-finance-cloud-storage",
    private val errorMapper: GcpErrorMapper = GcpErrorMapper()
) : IStorageService {

    private val cloudBlobStore = ConcurrentHashMap<String, ByteArray>()
    private val cloudBlobMeta = ConcurrentHashMap<String, StorageItemMetadata>()

    override suspend fun upload(request: UploadFileRequest): CloudResult<StorageItemMetadata> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            if (request.path.isBlank()) {
                return@withContext CloudResult.Failure(
                    CloudError.StorageError.FileNotFound(
                        path = request.path,
                        message = "GCP bucket blob path cannot be empty"
                    )
                )
            }

            cloudBlobStore[request.path] = request.bytes
            val fileName = request.path.substringAfterLast("/")
            val metadata = StorageItemMetadata(
                path = request.path,
                name = fileName,
                sizeBytes = request.bytes.size.toLong(),
                mimeType = request.mimeType,
                downloadUrl = "https://storage.googleapis.com/$bucketName/${request.path}",
                createdAtEpochMs = System.currentTimeMillis(),
                customMetadata = request.customMetadata
            )
            cloudBlobMeta[request.path] = metadata
            CloudResult.Success(metadata)
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun download(path: String): CloudResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            val blob = cloudBlobStore[path]
            if (blob != null) {
                CloudResult.Success(blob.copyOf())
            } else {
                CloudResult.Failure(
                    CloudError.StorageError.FileNotFound(
                        path = path,
                        message = "Blob $path not found in GCP bucket $bucketName"
                    )
                )
            }
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun delete(path: String): CloudResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            if (cloudBlobStore.remove(path) != null) {
                cloudBlobMeta.remove(path)
                CloudResult.Success(Unit)
            } else {
                CloudResult.Failure(
                    CloudError.StorageError.FileNotFound(
                        path = path,
                        message = "Blob $path does not exist in bucket $bucketName"
                    )
                )
            }
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }

    override suspend fun getDownloadUrl(path: String): CloudResult<String> = withContext(Dispatchers.IO) {
        try {
            if (!credentialsProvider.hasValidCredentials()) {
                val credCheck = credentialsProvider.getCredentialsPath()
                if (credCheck is CloudResult.Failure) {
                    return@withContext credCheck
                }
            }

            val meta = cloudBlobMeta[path]
            if (meta != null) {
                CloudResult.Success(meta.downloadUrl ?: "https://storage.googleapis.com/$bucketName/$path")
            } else {
                CloudResult.Failure(
                    CloudError.StorageError.FileNotFound(
                        path = path,
                        message = "Blob $path not found in GCP bucket $bucketName"
                    )
                )
            }
        } catch (e: Exception) {
            CloudResult.Failure(errorMapper.map(e))
        }
    }
}
