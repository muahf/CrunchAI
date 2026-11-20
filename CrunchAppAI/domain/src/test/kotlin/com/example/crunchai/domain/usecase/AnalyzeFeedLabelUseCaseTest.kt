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

class AnalyzeFeedLabelUseCaseTest {
    private val repository = FakeFoodAnalysisRepository(
        DomainResult.Success(FoodAnalysis("f2", PhotoKind.FEED_LABEL, composition = "Мясо 40%")),
    )
    private val analyze = AnalyzeFeedLabelUseCase(repository)

    @Test
    fun `пустое фото этикетки отклоняется с ошибкой валидации`() = runTest {
        val result = analyze(byteArrayOf())

        assertEquals(DomainResult.Failure(DomainError.Validation("photo", "Фото этикетки пустое")), result)
        assertNull(repository.lastKind)
    }

    @Test
    fun `корректное фото отправляется с типом FEED_LABEL`() = runTest {
        val result = analyze(byteArrayOf(10, 20))

        assertEquals("Мясо 40%", (result as DomainResult.Success).value.composition)
        assertEquals(PhotoKind.FEED_LABEL, repository.lastKind)
    }
}