package com.example.crunchai.domain.repository

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.PairingSession

interface PairingRepository {
    /** Станция: создать одноразовый ключ для QR-кода. */
    suspend fun createKey(): DomainResult<PairingSession>

    /** Наблюдатель: связать телефоны по ключу из QR-кода. */
    suspend fun pair(key: String): DomainResult<PairingSession>
}