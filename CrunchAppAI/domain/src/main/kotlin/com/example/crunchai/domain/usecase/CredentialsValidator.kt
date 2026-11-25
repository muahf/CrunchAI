package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError

/** Проверка email и пароля. Правила такие же, как на сервере: пароль 8–72 символа. */
object CredentialsValidator {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 72

    fun validateEmail(raw: String): DomainError.Validation? {
        val email = raw.trim()
        return when {
            email.isEmpty() -> DomainError.Validation("email", "Введите email")
            email.length > 254 || !EMAIL.matches(email) -> DomainError.Validation("email", "Введите email вида name@mail.ru")
            else -> null
        }
    }

    fun validatePassword(password: String): DomainError.Validation? =
        when {
            password.isEmpty() -> DomainError.Validation("password", "Введите пароль")
            password.length < MIN_PASSWORD -> DomainError.Validation("password", "Пароль не короче $MIN_PASSWORD символов")
            password.length > MAX_PASSWORD -> DomainError.Validation("password", "Пароль не длиннее $MAX_PASSWORD символов")
            else -> null
        }
}