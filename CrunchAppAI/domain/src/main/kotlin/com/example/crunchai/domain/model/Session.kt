package com.example.crunchai.domain.model

/** Результат входа: кто вошёл и токен для запросов. */
data class UserSession(
    val userId: String,
    val email: String,
    val deviceId: String,
    val role: DeviceRole,
    val token: String,
)