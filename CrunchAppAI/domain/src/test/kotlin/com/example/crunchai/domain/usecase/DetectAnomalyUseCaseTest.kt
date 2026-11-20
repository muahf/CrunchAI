package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SoundType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class DetectAnomalyUseCaseTest {
    private val detect = DetectAnomalyUseCase()
    private val base = Instant.parse("2026-11-18T10:00:00Z")

    @Test
    fun `пустой список событий не даёт аномалии`() {
        assertNull(detect(emptyList(), now = base))
    }

    @Test
    fun `три кашля за 60 секунд определяются как аномалия COUGH`() {
        val events = listOf(
            SoundEvent("1", SoundType.COUGH, 0.9f, base, 1),
            SoundEvent("2", SoundType.COUGH, 0.85f, base.plusSeconds(15), 1),
            SoundEvent("3", SoundType.COUGH, 0.92f, base.plusSeconds(30), 1),
        )

        val anomaly = detect(events, now = base.plusSeconds(35))

        assertNotNull(anomaly)
        assertEquals(AnomalyType.COUGH, anomaly?.type)
        assertEquals(35, anomaly?.durationSeconds)
    }

    @Test
    fun `тишина более 120 секунд после еды определяется как LONG_PAUSE`() {
        val events = listOf(
            SoundEvent("1", SoundType.CHEW, 0.9f, base, durationSeconds = 5),
        )

        val anomaly = detect(events, now = base.plusSeconds(130))

        assertNotNull(anomaly)
        assertEquals(AnomalyType.LONG_PAUSE, anomaly?.type)
        assertEquals(125, anomaly?.durationSeconds)
    }

    @Test
    fun `обычный приём пищи с редким кашлем проходит без аномалий`() {
        val events = listOf(
            SoundEvent("1", SoundType.CHEW, 0.9f, base, 2),
            SoundEvent("2", SoundType.COUGH, 0.8f, base.plusSeconds(10), 1),
        )

        assertNull(detect(events, now = base.plusSeconds(20)))
    }
}