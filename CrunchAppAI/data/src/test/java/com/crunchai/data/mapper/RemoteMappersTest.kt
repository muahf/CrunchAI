package com.crunchai.data.mapper

import com.crunchai.data.remote.dto.FoodAnalysisDto
import com.crunchai.data.remote.dto.PairingSessionDto
import com.crunchai.data.remote.dto.SyncReportDto
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.PairingStatus
import com.example.crunchai.domain.model.PhotoKind
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SoundType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RemoteMappersTest {
    @Test
    fun `SoundEvent корректно мапится в SoundEventDto`() {
        val now = Instant.parse("2026-11-20T09:00:00Z")
        val domain = SoundEvent("e1", SoundType.COUGH, 0.92f, now, 2)

        val dto = domain.toDto()

        assertEquals("e1", dto.id)
        assertEquals("COUGH", dto.type)
        assertEquals(0.92f, dto.confidence, 0.001f)
        assertEquals("2026-11-20T09:00:00Z", dto.startedAt)
        assertEquals(2, dto.durationSeconds)
    }

    @Test
    fun `PairingSessionDto корректно мапится в PairingSession`() {
        val dto =
            PairingSessionDto(
                key = "ABC-123",
                role = "STATION",
                status = "PAIRED",
                expiresAt = "2026-11-20T10:00:00Z",
            )

        val domain = dto.toDomain()

        assertEquals("ABC-123", domain.key)
        assertEquals(DeviceRole.STATION, domain.role)
        assertEquals(PairingStatus.PAIRED, domain.status)
        assertEquals(Instant.parse("2026-11-20T10:00:00Z"), domain.expiresAt)
    }

    @Test
    fun `SyncReportDto и FoodAnalysisDto мапятся в доменные модели`() {
        val report = SyncReportDto(accepted = 5, duplicateIds = listOf("d1")).toDomain()
        assertEquals(5, report.accepted)
        assertEquals(listOf("d1"), report.duplicateIds)

        val analysis =
            FoodAnalysisDto(
                id = "f1",
                kind = "BOWL_AFTER",
                eatenPercent = 75,
                risks = listOf("Нет рисков"),
            ).toDomain()
        assertEquals(PhotoKind.BOWL_AFTER, analysis.kind)
        assertEquals(75, analysis.eatenPercent)
    }
}
