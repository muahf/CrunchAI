package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SoundType
import com.example.crunchai.domain.port.AudioRecorder
import com.example.crunchai.domain.port.SoundClassifier
import com.example.crunchai.domain.repository.SoundEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.util.UUID

class MonitorSoundsUseCase(
    private val recorder: AudioRecorder,
    private val classifier: SoundClassifier,
    private val repository: SoundEventRepository,
) {
    operator fun invoke(minConfidence: Float = DEFAULT_MIN_CONFIDENCE): Flow<SoundEvent> =
        recorder.chunks().mapNotNull { chunk ->
            val result = classifier.classify(chunk)
            if (result.confidence < minConfidence || result.type == SoundType.OTHER) {
                return@mapNotNull null
            }
            val event = SoundEvent(
                id = UUID.randomUUID().toString(),
                type = result.type,
                confidence = result.confidence,
                startedAt = chunk.recordedAt,
                durationSeconds = CHUNK_DURATION_SECONDS,
            )
            repository.save(event)
            event
        }

    private companion object {
        const val DEFAULT_MIN_CONFIDENCE = 0.6f
        const val CHUNK_DURATION_SECONDS = 1
    }
}