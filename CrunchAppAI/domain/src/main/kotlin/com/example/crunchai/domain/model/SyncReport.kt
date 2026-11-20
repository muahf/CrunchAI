package com.example.crunchai.domain.model

/** Итог отправки пачки звуков на сервер. duplicateIds: сервер уже получал их раньше. */
data class SyncReport(
    val accepted: Int,
    val duplicateIds: List<String> = emptyList(),
)