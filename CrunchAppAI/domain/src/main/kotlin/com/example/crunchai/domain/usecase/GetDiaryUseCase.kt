package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Diary
import com.example.crunchai.domain.model.DiaryDay
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.repository.DiaryRepository
import java.time.ZoneId
import javax.inject.Inject

class GetDiaryUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(period: DiaryPeriod): DomainResult<Diary> {
        val mealsResult = repository.getMeals(period)
        if (mealsResult is DomainResult.Failure) return mealsResult

        val anomaliesResult = repository.getAnomalies(period)
        if (anomaliesResult is DomainResult.Failure) return anomaliesResult

        val meals = (mealsResult as DomainResult.Success).value
        val anomalies = (anomaliesResult as DomainResult.Success).value

        val zoneId = ZoneId.systemDefault()
        val mealsByDate = meals.groupBy { it.startedAt.atZone(zoneId).toLocalDate() }
        val anomaliesByDate = anomalies.groupBy { it.startedAt.atZone(zoneId).toLocalDate() }

        val allDates = (mealsByDate.keys + anomaliesByDate.keys).sorted()
        val days = allDates.map { date ->
            DiaryDay(
                date = date,
                mealsCount = mealsByDate[date]?.size ?: 0,
                anomaliesCount = anomaliesByDate[date]?.size ?: 0,
            )
        }

        val averageCalmIndex = if (meals.isEmpty()) {
            null
        } else {
            meals.map { it.calmIndex }.average()
        }

        return DomainResult.Success(
            Diary(
                period = period,
                meals = meals,
                anomalies = anomalies,
                days = days,
                averageCalmIndex = averageCalmIndex,
            ),
        )
    }
}