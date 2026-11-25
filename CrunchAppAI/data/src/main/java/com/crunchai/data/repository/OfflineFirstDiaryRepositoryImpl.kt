package com.crunchai.data.repository

import com.crunchai.data.local.db.DiaryCacheDao
import com.crunchai.data.mapper.toDomain
import com.crunchai.data.mapper.toEntity
import com.crunchai.data.remote.api.CrunchApi
import com.crunchai.data.remote.dto.PageDto
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
        val since = periodStart(period)
        val remote =
            safeApiCall {
                loadAll { cursor -> api.getMeals(PAGE_SIZE, cursor) }
                    .map { it.toDomain() }
                    .filter { it.startedAt >= since }
            }
        return when (remote) {
            is DomainResult.Success -> {
                cacheDao.insertMeals(remote.value.map { it.toEntity() })
                remote
            }
            is DomainResult.Failure -> {
                val cached = cacheDao.getMealsSince(since.toEpochMilli()).map { it.toDomain() }
                if (cached.isNotEmpty()) DomainResult.Success(cached) else remote
            }
        }
    }

    override suspend fun getAnomalies(period: DiaryPeriod): DomainResult<List<Anomaly>> {
        val since = periodStart(period)
        val remote =
            safeApiCall {
                loadAll { cursor -> api.getAnomalies(PAGE_SIZE, cursor) }
                    .map { it.toDomain() }
                    .filter { it.startedAt >= since }
            }
        return when (remote) {
            is DomainResult.Success -> {
                cacheDao.insertAnomalies(remote.value.map { it.toEntity() })
                remote
            }
            is DomainResult.Failure -> {
                val cached = cacheDao.getAnomaliesSince(since.toEpochMilli()).map { it.toDomain() }
                if (cached.isNotEmpty()) DomainResult.Success(cached) else remote
            }
        }
    }

    /** Сервер отдаёт списки страницами, периода не знает: качаем страницы по очереди (не больше MAX_PAGES). */
    private suspend fun <T> loadAll(fetch: suspend (cursor: String?) -> PageDto<T>): List<T> {
        val all = mutableListOf<T>()
        var cursor: String? = null
        repeat(MAX_PAGES) {
            val page = fetch(cursor)
            all += page.items
            cursor = page.nextCursor
            if (!page.hasMore || cursor == null) return all
        }
        return all
    }

    private fun periodStart(period: DiaryPeriod): Instant {
        val days = if (period == DiaryPeriod.WEEK) 7L else 30L
        return Instant.now().minus(days, ChronoUnit.DAYS)
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 10
    }
}