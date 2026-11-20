package com.example.crunchai.domain.repository

import com.example.crunchai.domain.model.SoundEvent

/** Хранение звуков на телефоне. Отправка на сервер вынесена в EventSyncRepository. */
interface SoundEventRepository {
    suspend fun save(event: SoundEvent)

    /** Звуки, которые ещё не отправлены на сервер (самые старые первыми). */
    suspend fun getUnsent(limit: Int): List<SoundEvent>

    suspend fun markSent(ids: List<String>)
}