package com.gtc.app_finance.cloud.domain.error

sealed class CloudResult<out T> {
    data class Success<out T>(val data: T) : CloudResult<T>()
    data class Failure(val error: CloudError) : CloudResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun errorOrNull(): CloudError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    inline fun <R> map(transform: (T) -> R): CloudResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun <R> flatMap(transform: (T) -> CloudResult<R>): CloudResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }

    inline fun <R> fold(onSuccess: (T) -> R, onFailure: (CloudError) -> R): R = when (this) {
        is Success -> onSuccess(data)
        is Failure -> onFailure(error)
    }

    inline fun onSuccess(action: (T) -> Unit): CloudResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (CloudError) -> Unit): CloudResult<T> {
        if (this is Failure) action(error)
        return this
    }

    fun getOrElse(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> defaultValue
    }
}
