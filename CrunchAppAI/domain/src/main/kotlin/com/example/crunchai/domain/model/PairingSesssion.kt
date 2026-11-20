package com.example.crunchai.domain.model

import java.time.Instant

enum class DeviceRole { STATION, OBSERVER }

enum class PairingStatus { WAITING, PAIRED, EXPIRED }

/** Связь двух телефонов: Станции (у миски) и Наблюдателя (у владельца). */
data class PairingSession(
    val key: String,
    val role: DeviceRole,
    val status: PairingStatus,
    val expiresAt: Instant? = null,
)