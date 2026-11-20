package com.example.crunchai.domain.port

import com.example.crunchai.domain.model.SoundType

data class Classification(
    val type: SoundType,
    val confidence: Float,
)

/** Распознаватель звука. Реальный: YAMNet. Для тестов: FakeSoundClassifier. */
interface SoundClassifier {
    suspend fun classify(chunk: AudioChunk): Classification
}