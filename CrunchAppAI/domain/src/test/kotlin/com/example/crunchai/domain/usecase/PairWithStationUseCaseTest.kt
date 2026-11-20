package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.fakes.FakePairingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairWithStationUseCaseTest {
    private val repository = FakePairingRepository()
    private val pair = PairWithStationUseCase(repository)

    @Test
    fun `ключ приводится к верхнему регистру и без пробелов`() = runTest {
        pair("  abc-123 ")

        assertEquals("ABC-123", repository.lastPairedKey)
    }

    @Test
    fun `ключ неверного вида это ошибка и сервер не вызывается`() = runTest {
        val result = pair("12345")

        assertEquals(
            DomainResult.Failure(DomainError.Validation("key", "Ключ должен быть вида ABC-123")),
            result,
        )
        assertNull(repository.lastPairedKey)
    }

    @Test
    fun `истёкший ключ возвращается как ошибка`() = runTest {
        repository.pairResult = DomainResult.Failure(DomainError.PairingKeyExpired)

        assertEquals(DomainResult.Failure(DomainError.PairingKeyExpired), pair("ABC-123"))
    }
}