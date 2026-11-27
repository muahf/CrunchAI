package com.crunchai.presentation.auth

import com.example.crunchai.domain.model.DeviceRole

enum class AuthMode { LOG_IN, SIGN_UP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOG_IN,
    val role: DeviceRole = DeviceRole.OBSERVER,
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    /** Ошибки под полями: ключ — имя поля ("email", "password", "repeatPassword"). */
    val fieldErrors: Map<String, String> = emptyMap(),
    /** Общая ошибка (сервер, нет сети, неверный пароль). */
    val formError: String? = null,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
)
