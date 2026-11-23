package com.crunchai.data.remote.network

import com.crunchai.data.remote.interceptor.NoConnectivityException
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): DomainResult<T> {
    return try {
        DomainResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: NoConnectivityException) {
        DomainResult.Failure(DomainError.Offline)
    } catch (e: SocketTimeoutException) {
        DomainResult.Failure(DomainError.Offline)
    } catch (e: IOException) {
        DomainResult.Failure(DomainError.Offline)
    } catch (e: HttpException) {
        DomainResult.Failure(mapHttpException(e))
    } catch (e: SerializationException) {
        DomainResult.Failure(DomainError.Validation("server", "Ошибка формата ответа сервера"))
    }
}

fun mapHttpException(e: HttpException): DomainError =
    when (e.code()) {
        400, 422 -> DomainError.Validation("request", "Некорректные данные запроса (${e.code()})")
        410 -> DomainError.PairingKeyExpired
        in 500..599 -> DomainError.Offline
        else -> DomainError.Validation("http", "Ошибка сервера: ${e.code()}")
    }
