package com.example.crunchai.domain.port

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Короткий кусок звука с микрофона (для YAMNet: 16 кГц, моно). */
class AudioChunk(
    val samples: FloatArray,
    val recordedAt: Instant,
)

/** Микрофон. Реализация на AudioRecord лежит в :data. */
interface AudioRecorder {
    fun chunks(): Flow<AudioChunk>
}
