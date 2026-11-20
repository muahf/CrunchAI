package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.fakes.FakeFoodAnalysisRepository
import com.example.crunchai.domain.model.FoodAnalysis
import com.example.crunchai.domain.model.PhotoKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyzeBowlPhotoUseCaseTest {
    private val repository = FakeFoodAnalysisRepository(
        DomainResult.Success(FoodAnalysis("f1", PhotoKind.BOWL_AFTER, eatenPercent = 80)),
    )
    private val analyze = AnalyzeBowlPhotoUseCase(repository)

    @Test
    fun `пустое фото отклоняется с ошибкой валидации`() = runTest {
        val result = analyze(byteArrayOf())

        assertEquals(DomainResult.Failure(DomainError.Validation("photo", "Фото пустое")), result)
        assertNull(repository.lastKind)
    }

    @Test
    fun `корректное фото отправляется с типом BOWL_AFTER`() = runTest {
        val result = analyze(byteArrayOf(10, 20))

        assertEquals(80, (result as DomainResult.Success).value.eatenPercent)
        assertEquals(PhotoKind.BOWL_AFTER, repository.lastKind)
    }
}