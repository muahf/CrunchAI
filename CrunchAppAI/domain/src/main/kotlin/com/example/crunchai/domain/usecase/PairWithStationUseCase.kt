package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.PairingSession
import com.example.crunchai.domain.repository.PairingRepository
import javax.inject.Inject

/** Наблюдатель сканирует QR-код и связывает телефоны. Ключ имеет вид ABC-123. */
class PairWithStationUseCase @Inject
constructor(private val repository: PairingRepository) {

    suspend operator fun invoke(rawKey: String): DomainResult<PairingSession> {
        val key = rawKey.trim().uppercase()
        if (!KEY_FORMAT.matches(key)) {
            return DomainResult.Failure(DomainError.Validation("key", "Ключ должен быть вида ABC-123"))
        }
        return repository.pair(key)
    }

    private companion object {
        val KEY_FORMAT = Regex("^[A-Z0-9]{3}-[A-Z0-9]{3}$")
    }
}