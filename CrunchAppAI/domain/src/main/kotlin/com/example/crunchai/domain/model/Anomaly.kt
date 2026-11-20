package com.example.crunchai.domain.model

import java.time.Instant

enum class AnomalyType { COUGH, LONG_PAUSE }

/** Отклонение, о котором нужно сообщить Наблюдателю. */
data class Anomaly(
    val id: String,
    val type: AnomalyType,
    val startedAt: Instant,
    val durationSeconds: Int,
    val seen: Boolean = false,
)