package com.gtc.app_finance.cloud.infrastructure.firebase

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.StorageItemMetadata
import com.gtc.app_finance.cloud.domain.model.UploadFileRequest
import com.gtc.app_finance.cloud.domain.ports.IStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class FirebaseStorageAdapter(
    private val storageBucket: String = "app-finance.appspot.com",
    private val errorMapper: FirebaseErrorMapper = FirebaseErrorMapper()
) : IStorageService {

    private val files = ConcurrentHashMap<String, ByteArray>()
    private val metadataStore = ConcurrentHashMap<String, StorageItemMetadata>()

    override suspend fun upload(request: UploadFileRequest): CloudResult<StorageItemMetadata> = withContext(Dispatchers.IO) {
        if (request.path.isBlank()) {
            return@withContext CloudResult.Failure(
                errorMapper.map(Exception("invalid-argument: Storage path cannot be empty"))
            )
        }

        files[request.path] = request.bytes
        val fileName = request.path.substringAfterLast("/")
        val meta = StorageItemMetadata(
            path = request.path,
            name = fileName,
            sizeBytes = request.bytes.size.toLong(),
            mimeType = request.mimeType,
            downloadUrl = "https://firebasestorage.googleapis.com/v0/b/$storageBucket/o/${request.path}?alt=media",
            createdAtEpochMs = System.currentTimeMillis(),
            customMetadata = request.customMetadata
        )
        metadataStore[request.path] = meta
        CloudResult.Success(meta)
    }

    override suspend fun download(path: String): CloudResult<ByteArray> = withContext(Dispatchers.IO) {
        val data = files[path]
        if (data != null) {
            CloudResult.Success(data.copyOf())
        } else {
            CloudResult.Failure(
                errorMapper.map(Exception("object-not-found: path: $path"))
            )
        }
    }

    override suspend fun delete(path: String): CloudResult<Unit> = withContext(Dispatchers.IO) {
        if (files.remove(path) != null) {
            metadataStore.remove(path)
            CloudResult.Success(Unit)
        } else {
            CloudResult.Failure(
                errorMapper.map(Exception("object-not-found: path: $path"))
            )
        }
    }

    override suspend fun getDownloadUrl(path: String): CloudResult<String> = withContext(Dispatchers.IO) {
        val meta = metadataStore[path]
        if (meta != null) {
            CloudResult.Success(meta.downloadUrl ?: "https://firebasestorage.googleapis.com/v0/b/$storageBucket/o/$path")
        } else {
            CloudResult.Failure(
                errorMapper.map(Exception("object-not-found: path: $path"))
            )
        }
    }
}
