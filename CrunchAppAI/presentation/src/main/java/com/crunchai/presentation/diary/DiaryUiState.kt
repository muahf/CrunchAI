package com.crunchai.presentation.diary

import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.DiaryDay
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.Meal

data class DiaryUiState(
    val period: DiaryPeriod = DiaryPeriod.WEEK,
    val isLoading: Boolean = true,
    val isOfflineBannerVisible: Boolean = false,
    val errorMessage: String? = null,
    val errorDetails: String? = null,
    val showErrorDetails: Boolean = false,
    val meals: List<Meal> = emptyList(),
    val anomalies: List<Anomaly> = emptyList(),
    val days: List<DiaryDay> = emptyList(),
    val averageCalmIndex: Double? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && meals.isEmpty() && anomalies.isEmpty()
}
