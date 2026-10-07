package com.gtc.app_finance.cloud.domain.ports

import com.gtc.app_finance.cloud.domain.error.CloudResult
import com.gtc.app_finance.cloud.domain.model.StorageItemMetadata
import com.gtc.app_finance.cloud.domain.model.UploadFileRequest

interface IStorageService {
    suspend fun upload(request: UploadFileRequest): CloudResult<StorageItemMetadata>
    suspend fun download(path: String): CloudResult<ByteArray>
    suspend fun delete(path: String): CloudResult<Unit>
    suspend fun getDownloadUrl(path: String): CloudResult<String>
}
