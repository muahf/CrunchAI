package com.example.crunchai.domain.model

import java.time.Instant

enum class SoundType { COUGH, CHEW, LAP, BARK, OTHER }

/** LOCAL: хранится только на телефоне. SENT: отправлен на сервер. */
enum class SyncStatus { LOCAL, SENT }

/** Один распознанный звук. confidence: уверенность от 0 до 1. */
data class SoundEvent(
    val id: String,
    val type: SoundType,
    val confidence: Float,
    val startedAt: Instant,
    val durationSeconds: Int,
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
)