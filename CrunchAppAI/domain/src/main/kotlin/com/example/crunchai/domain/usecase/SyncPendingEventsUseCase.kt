package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.SyncReport
import com.example.crunchai.domain.repository.EventSyncRepository
import com.example.crunchai.domain.repository.SoundEventRepository

class SyncPendingEventsUseCase(
    private val localRepository: SoundEventRepository,
    private val syncRepository: EventSyncRepository,
) {
    suspend operator fun invoke(): DomainResult<SyncReport> {
        val unsent = localRepository.getUnsent(BATCH_LIMIT)
        if (unsent.isEmpty()) {
            return DomainResult.Success(SyncReport(accepted = 0))
        }

        val result = syncRepository.send(unsent)
        if (result is DomainResult.Success) {
            localRepository.markSent(unsent.map { it.id })
        }
        return result
    }

    companion object {
        const val BATCH_LIMIT = 100
    }
}