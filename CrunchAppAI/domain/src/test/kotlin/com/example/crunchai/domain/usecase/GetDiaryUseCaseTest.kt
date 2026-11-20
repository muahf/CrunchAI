package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.fakes.FakeDiaryRepository
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.DiaryDay
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.Meal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GetDiaryUseCaseTest {
    @Test
    fun `группирует приёмы пищи и аномалии по дням и считает средний индекс спокойствия`() = runTest {
        val day1 = Instant.parse("2026-11-17T08:00:00Z")
        val day2 = Instant.parse("2026-11-18T09:00:00Z")
        val repository = FakeDiaryRepository(
            mealsResult = DomainResult.Success(
                listOf(
                    Meal("m1", day1, 300, calmIndex = 8),
                    Meal("m2", day2, 250, calmIndex = 10),
                ),
            ),
            anomaliesResult = DomainResult.Success(
                listOf(Anomaly("a1", AnomalyType.COUGH, day1, 15)),
            ),
        )
        val getDiary = GetDiaryUseCase(repository)

        val diary = (getDiary(DiaryPeriod.WEEK) as DomainResult.Success).value

        assertEquals(9.0, diary.averageCalmIndex!!, 0.01)
        assertEquals(2, diary.days.size)
        assertEquals(DiaryDay(LocalDate.of(2026, 11, 17), mealsCount = 1, anomaliesCount = 1), diary.days[0])
        assertEquals(DiaryDay(LocalDate.of(2026, 11, 18), mealsCount = 1, anomaliesCount = 0), diary.days[1])
    }

    @Test
    fun `при пустой истории средний индекс равен null`() = runTest {
        val getDiary = GetDiaryUseCase(FakeDiaryRepository())

        val diary = (getDiary(DiaryPeriod.MONTH) as DomainResult.Success).value

        assertNull(diary.averageCalmIndex)
    }

    @Test
    fun `ошибка загрузки приёмов пищи возвращается сразу`() = runTest {
        val repository = FakeDiaryRepository(mealsResult = DomainResult.Failure(DomainError.Offline))
        val getDiary = GetDiaryUseCase(repository)

        assertEquals(DomainResult.Failure(DomainError.Offline), getDiary(DiaryPeriod.WEEK))
    }
}