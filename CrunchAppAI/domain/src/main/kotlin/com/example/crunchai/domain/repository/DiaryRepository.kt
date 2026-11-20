package com.example.crunchai.domain.repository

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.Meal

interface DiaryRepository {
    suspend fun getMeals(period: DiaryPeriod): DomainResult<List<Meal>>

    suspend fun getAnomalies(period: DiaryPeriod): DomainResult<List<Anomaly>>
}