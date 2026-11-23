package com.crunchai.data.repository

import com.crunchai.data.mapper.toDomain
import com.crunchai.data.mapper.toDto
import com.crunchai.data.remote.api.CrunchApi
import com.crunchai.data.remote.dto.SyncBatchRequestDto
import com.crunchai.data.remote.network.safeApiCall
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SyncReport
import com.example.crunchai.domain.repository.EventSyncRepository
import javax.inject.Inject

class RemoteEventSyncRepositoryImpl
    @Inject
    constructor(
        private val api: CrunchApi,
    ) : EventSyncRepository {
        override suspend fun send(events: List<SoundEvent>): DomainResult<SyncReport> =
            safeApiCall {
                val request = SyncBatchRequestDto(events = events.map { it.toDto() })
                api.sendEvents(request).toDomain()
            }
    }
