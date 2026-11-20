package com.example.crunchai.domain.model

import java.time.Instant

/** Один приём пищи. calmIndex: индекс спокойствия от 1 до 10. */
data class Meal(
    val id: String,
    val startedAt: Instant,
    val durationSeconds: Int,
    val calmIndex: Int,
)