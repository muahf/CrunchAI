package com.crunchai.data.remote.network

import com.crunchai.data.remote.interceptor.NoConnectivityException
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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
        DomainResult.Failure(DomainError.Server("Ошибка формата ответа сервера"))
    }
}

/** Ошибка сервера: {"error":{"code":"pairing_key_expired","message":"...","request_id":"..."}} */
@Serializable
data class ErrorEnvelopeDto(val error: ErrorBodyDto)

@Serializable
data class ErrorBodyDto(
    val code: String = "",
    val message: String = "",
    @SerialName("request_id") val requestId: String? = null,
)

private val errorJson = Json { ignoreUnknownKeys = true }

/** Достаёт «code» и «message» из тела ответа; если тело не наше — null. */
internal fun HttpException.serverError(): ErrorBodyDto? =
    try {
        response()?.errorBody()?.string()?.let { errorJson.decodeFromString<ErrorEnvelopeDto>(it).error }
    } catch (e: Exception) {
        null
    }

fun mapHttpException(e: HttpException): DomainError {
    val server = e.serverError()
    val text = server?.message?.takeIf { it.isNotBlank() }
    return when {
        server?.code == "pairing_key_expired" || e.code() == 410 -> DomainError.PairingKeyExpired
        e.code() == 400 || e.code() == 422 -> DomainError.Validation("request", text ?: "Некорректные данные запроса")
        e.code() == 401 -> DomainError.Unauthorized
        e.code() == 403 -> DomainError.Forbidden
        e.code() == 404 -> DomainError.NotFound
        e.code() == 409 -> DomainError.Conflict
        e.code() == 413 -> DomainError.TooLarge
        e.code() == 429 -> DomainError.RateLimited
        e.code() == 502 -> DomainError.AiUnavailable
        e.code() in 500..599 -> DomainError.Server(text)
        else -> DomainError.Server(text ?: "Ошибка сервера: ${e.code()}")
    }
}