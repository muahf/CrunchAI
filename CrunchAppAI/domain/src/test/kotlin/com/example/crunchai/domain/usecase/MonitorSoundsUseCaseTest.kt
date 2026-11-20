package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.fakes.FakeAudioRecorder
import com.example.crunchai.domain.fakes.FakeSoundClassifier
import com.example.crunchai.domain.fakes.FakeSoundEventRepository
import com.example.crunchai.domain.model.SoundType
import com.example.crunchai.domain.port.AudioChunk
import com.example.crunchai.domain.port.Classification
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class MonitorSoundsUseCaseTest {
    @Test
    fun `отбрасывает шум OTHER и низкую уверенность, сохраняет значимые звуки`() = runTest {
        val t = Instant.parse("2026-11-18T10:00:00Z")
        val c1 = AudioChunk(floatArrayOf(0.1f), t)
        val c2 = AudioChunk(floatArrayOf(0.2f), t.plusSeconds(1))
        val c3 = AudioChunk(floatArrayOf(0.3f), t.plusSeconds(2))

        val classifier = FakeSoundClassifier { chunk ->
            when (chunk) {
                c1 -> Classification(SoundType.CHEW, 0.85f)
                c2 -> Classification(SoundType.COUGH, 0.40f)
                else -> Classification(SoundType.OTHER, 0.99f)
            }
        }
        val repository = FakeSoundEventRepository()
        val monitor = MonitorSoundsUseCase(FakeAudioRecorder(listOf(c1, c2, c3)), classifier, repository)

        val emitted = monitor().toList()

        assertEquals(1, emitted.size)
        assertEquals(SoundType.CHEW, emitted.first().type)
        assertEquals(1, repository.saved.size)
    }
}