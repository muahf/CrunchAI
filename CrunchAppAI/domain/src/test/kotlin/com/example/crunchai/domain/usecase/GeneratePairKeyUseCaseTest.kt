package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.fakes.FakePairingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GeneratePairKeyUseCaseTest {
    private val repository = FakePairingRepository()
    private val generate = GeneratePairKeyUseCase(repository)

    @Test
    fun `возвращает сессию с ключом от репозитория`() = runTest {
        val session = (generate() as DomainResult.Success).value

        assertEquals("ABC-123", session.key)
    }

    @Test
    fun `ошибка сервера возвращается как есть`() = runTest {
        repository.createResult = DomainResult.Failure(DomainError.Offline)

        assertEquals(DomainResult.Failure(DomainError.Offline), generate())
    }
}