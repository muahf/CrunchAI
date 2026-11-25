package com.crunchai.presentation.common

import com.example.crunchai.domain.common.DomainError

fun DomainError.toUserMessage(): String =
    when (this) {
        is DomainError.Validation -> "$field: $reason"
        DomainError.Unauthorized -> "Сессия истекла. Войдите заново"
        DomainError.Forbidden -> "Недостаточно прав для этого действия"
        DomainError.NotFound -> "Данные не найдены"
        DomainError.Conflict -> "Конфликт данных"
        DomainError.PairingKeyExpired -> "Срок действия ключа сопряжения истёк"
        DomainError.TooLarge -> "Слишком большой объём данных"
        DomainError.RateLimited -> "Слишком много запросов. Попробуйте позже"
        DomainError.AiUnavailable -> "ИИ-сервис временно недоступен"
        DomainError.Offline -> "Нет подключения к интернету и локальный кэш пуст"
        is DomainError.Server -> message ?: "Ошибка сервера"
        is DomainError.Unknown -> cause?.localizedMessage ?: "Неизвестная ошибка"
    }