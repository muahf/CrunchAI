package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SoundType
import java.time.Duration
import java.time.Instant
import java.util.UUID

class DetectAnomalyUseCase {

    operator fun invoke(
        recentEvents: List<SoundEvent>,
        now: Instant,
    ): Anomaly? {
        if (recentEvents.isEmpty()) return null
        val sorted = recentEvents.sortedBy { it.startedAt }

        // Правило 1: серия кашля (минимум 3 эпизода за последние 60 секунд)
        val windowStart = now.minusSeconds(COUGH_WINDOW_SECONDS)
        val recentCoughs = sorted.filter {
            it.type == SoundType.COUGH && !it.startedAt.isBefore(windowStart)
        }
        if (recentCoughs.size >= MIN_COUGH_COUNT) {
            val firstCough = recentCoughs.first()
            val duration = Duration.between(firstCough.startedAt, now).seconds
                .toInt()
                .coerceAtLeast(firstCough.durationSeconds)
            return Anomaly(
                id = UUID.randomUUID().toString(),
                type = AnomalyType.COUGH,
                startedAt = firstCough.startedAt,
                durationSeconds = duration,
            )
        }

        // Правило 2: долгая пауза после последнего звука еды (жевание/лакание)
        val lastEatingEvent = sorted.lastOrNull {
            it.type == SoundType.CHEW || it.type == SoundType.LAP
        } ?: return null

        val eatingEnd = lastEatingEvent.startedAt.plusSeconds(lastEatingEvent.durationSeconds.toLong())
        val pauseSeconds = Duration.between(eatingEnd, now).seconds.toInt()
        if (pauseSeconds >= LONG_PAUSE_SECONDS) {
            return Anomaly(
                id = UUID.randomUUID().toString(),
                type = AnomalyType.LONG_PAUSE,
                startedAt = eatingEnd,
                durationSeconds = pauseSeconds,
            )
        }

        return null
    }

    companion object {
        const val COUGH_WINDOW_SECONDS = 60L
        const val MIN_COUGH_COUNT = 3
        const val LONG_PAUSE_SECONDS = 120
    }
}