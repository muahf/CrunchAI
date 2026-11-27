package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.PairingSession
import com.example.crunchai.domain.repository.PairingRepository
import javax.inject.Inject

/** Станция создаёт одноразовый ключ для QR-кода. */
class GeneratePairKeyUseCase
@Inject
constructor(private val repository: PairingRepository) {
    suspend operator fun invoke(): DomainResult<PairingSession> = repository.createKey()
}