package com.example.crunchai.domain.model

import java.time.LocalDate

enum class DiaryPeriod { WEEK, MONTH }

/** Один день на графике Дневника. */
data class DiaryDay(
    val date: LocalDate,
    val mealsCount: Int,
    val anomaliesCount: Int,
)

/** История за период: списки и готовые данные для графиков. */
data class Diary(
    val period: DiaryPeriod,
    val meals: List<Meal>,
    val anomalies: List<Anomaly>,
    val days: List<DiaryDay>,
    val averageCalmIndex: Double?,
)