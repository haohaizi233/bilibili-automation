package com.example.bilibiliautomation.domain.api

sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Err<T>(val error: ApiError) : ApiResult<T>()

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    fun getOrNull(): T? = (this as? Ok)?.value

    fun errorOrNull(): ApiError? = (this as? Err)?.error

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> Err(error)
    }

    inline fun <R> fold(onOk: (T) -> R, onErr: (ApiError) -> R): R = when (this) {
        is Ok -> onOk(value)
        is Err -> onErr(error)
    }

    companion object {
        fun <T> ok(value: T): ApiResult<T> = Ok(value)
        fun <T> err(error: ApiError): ApiResult<T> = Err(error)
    }
}
