package com.crunchai.data.repository

import com.crunchai.data.local.db.DiaryCacheDao
import com.crunchai.data.mapper.toDomain
import com.crunchai.data.mapper.toEntity
import com.crunchai.data.remote.api.CrunchApi
import com.crunchai.data.remote.network.safeApiCall
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.repository.DiaryRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class OfflineFirstDiaryRepositoryImpl
    @Inject
    constructor(
        private val api: CrunchApi,
        private val cacheDao: DiaryCacheDao,
    ) : DiaryRepository {
        override suspend fun getMeals(period: DiaryPeriod): DomainResult<List<Meal>> {
            val remote = safeApiCall { api.getMeals(period.name).map { it.toDomain() } }
            return when (remote) {
                is DomainResult.Success -> {
                    cacheDao.insertMeals(remote.value.map { it.toEntity() })
                    remote
                }
                is DomainResult.Failure -> {
                    val cached = cacheDao.getMealsSince(periodStartMillis(period)).map { it.toDomain() }
                    if (cached.isNotEmpty()) DomainResult.Success(cached) else remote
                }
            }
        }

        override suspend fun getAnomalies(period: DiaryPeriod): DomainResult<List<Anomaly>> {
            val remote = safeApiCall { api.getAnomalies(period.name).map { it.toDomain() } }
            return when (remote) {
                is DomainResult.Success -> {
                    cacheDao.insertAnomalies(remote.value.map { it.toEntity() })
                    remote
                }
                is DomainResult.Failure -> {
                    val cached = cacheDao.getAnomaliesSince(periodStartMillis(period)).map { it.toDomain() }
                    if (cached.isNotEmpty()) DomainResult.Success(cached) else remote
                }
            }
        }

        private fun periodStartMillis(period: DiaryPeriod): Long {
            val days = if (period == DiaryPeriod.WEEK) 7L else 30L
            return Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli()
        }
    }
