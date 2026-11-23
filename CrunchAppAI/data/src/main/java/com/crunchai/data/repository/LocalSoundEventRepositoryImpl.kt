package com.crunchai.data.repository

import com.crunchai.data.local.db.SoundEventDao
import com.crunchai.data.mapper.toDomain
import com.crunchai.data.mapper.toEntity
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.repository.SoundEventRepository
import javax.inject.Inject

class LocalSoundEventRepositoryImpl
    @Inject
    constructor(
        private val dao: SoundEventDao,
    ) : SoundEventRepository {
        override suspend fun save(event: SoundEvent) {
            dao.insert(event.toEntity())
        }

        override suspend fun getUnsent(limit: Int): List<SoundEvent> = dao.getUnsent(limit).map { it.toDomain() }

        override suspend fun markSent(ids: List<String>) {
            if (ids.isNotEmpty()) {
                dao.markSent(ids)
            }
        }
    }
