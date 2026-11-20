package com.example.crunchai.domain.common

/** Что может пойти не так. Экран по типу ошибки решает, что показать пользователю. */
sealed interface DomainError {
    /** Неверное значение поля (например, вес питомца 500 кг). */
    data class Validation(val field: String, val reason: String) : DomainError

    /** Токен недействителен: нужно войти заново. */
    data object Unauthorized : DomainError

    /** Действие не для этой роли (Станция / Наблюдатель). */
    data object Forbidden : DomainError

    data object NotFound : DomainError

    /** Уже существует (например, email занят, ключ уже использован). */
    data object Conflict : DomainError

    data object PairingKeyExpired : DomainError

    /** Слишком большая пачка звуков или фото. */
    data object TooLarge : DomainError

    data object RateLimited : DomainError

    /** ИИ-сервис (Gemini) не ответил. */
    data object AiUnavailable : DomainError

    /** Нет интернета. Станция в этом случае продолжает работать с локальным кэшем. */
    data object Offline : DomainError

    data class Server(val message: String? = null) : DomainError

    data class Unknown(val cause: Throwable? = null) : DomainError
}