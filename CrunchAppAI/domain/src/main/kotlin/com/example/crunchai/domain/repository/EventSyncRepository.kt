package com.example.crunchai.domain.repository

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SyncReport

interface EventSyncRepository {
    /** POST /api/v1/events/batch: не больше 100 звуков за раз. */
    suspend fun send(events: List<SoundEvent>): DomainResult<SyncReport>
}