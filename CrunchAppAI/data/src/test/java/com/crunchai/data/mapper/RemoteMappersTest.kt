package com.crunchai.data.mapper

import com.crunchai.data.remote.dto.AnomalyDto
import com.crunchai.data.remote.dto.FoodAnalysisDto
import com.crunchai.data.remote.dto.MealDto
import com.crunchai.data.remote.dto.PairConfirmDto
import com.crunchai.data.remote.dto.PairingSessionDto
import com.crunchai.data.remote.dto.SyncReportDto
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.PairingStatus
import com.example.crunchai.domain.model.PhotoKind
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteMappersTest {
    @Test
    fun `meal dto maps duration and time`() {
        val meal = MealDto("m1", "2026-11-25T10:00:00Z", 300, 8).toDomain()
        assertEquals(Instant.parse("2026-11-25T10:00:00Z"), meal.startedAt)
        assertEquals(300, meal.durationSeconds)
        assertEquals(8, meal.calmIndex)
    }

    @Test
    fun `anomaly dto maps lowercase type`() {
        val a = AnomalyDto("a1", "cough", "2026-11-25T10:00:00Z", 4).toDomain()
        assertEquals(AnomalyType.COUGH, a.type)
        assertEquals(4, a.durationSeconds)
    }

    @Test
    fun `sync report maps duplicates`() {
        val report = SyncReportDto(accepted = 2, duplicates = listOf("x")).toDomain()
        assertEquals(2, report.accepted)
        assertEquals(listOf("x"), report.duplicateIds)
    }

    @Test
    fun `created pairing session is station waiting`() {
        val s = PairingSessionDto("AB12CD", "2026-11-25T10:10:00Z").toDomain()
        assertEquals(DeviceRole.STATION, s.role)
        assertEquals(PairingStatus.WAITING, s.status)
        assertEquals(Instant.parse("2026-11-25T10:10:00Z"), s.expiresAt)
    }

    @Test
    fun `confirmed pairing is observer paired and keeps entered key`() {
        val s = PairConfirmDto("c1", "Кухня").toDomain("AB12CD")
        assertEquals(DeviceRole.OBSERVER, s.role)
        assertEquals(PairingStatus.PAIRED, s.status)
        assertEquals("AB12CD", s.key)
        assertNull(s.expiresAt)
    }

    @Test
    fun `food analysis maps kind and comment`() {
        val f = FoodAnalysisDto("f1", "bowl_after", 70, "Съел почти всё", "2026-11-25T10:00:00Z").toDomain()
        assertEquals(PhotoKind.BOWL_AFTER, f.kind)
        assertEquals(70, f.eatenPercent)
        assertEquals("Съел почти всё", f.composition)
    }
}