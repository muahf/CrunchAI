package com.example.crunchai.domain.common

/** Результат use case: успех со значением или ошибка домена. */
sealed interface DomainResult<out T> {
    data class Success<out T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(value))
    is DomainResult.Failure -> this
}

fun <T> DomainResult<T>.getOrNull(): T? = (this as? DomainResult.Success)?.value

inline fun <T> DomainResult<T>.onSuccess(action: (T) -> Unit): DomainResult<T> {
    if (this is DomainResult.Success) action(value)
    return this
}

inline fun <T> DomainResult<T>.onFailure(action: (DomainError) -> Unit): DomainResult<T> {
    if (this is DomainResult.Failure) action(error)
    return this
}