package com.example.crunchai.domain.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainResultTest {

    @Test
    fun `map меняет значение у успеха`() {
        val result = DomainResult.Success(2).map { it * 10 }

        assertEquals(DomainResult.Success(20), result)
    }

    @Test
    fun `map не трогает ошибку`() {
        val failure: DomainResult<Int> = DomainResult.Failure(DomainError.NotFound)

        assertEquals(failure, failure.map { it * 10 })
    }

    @Test
    fun `getOrNull отдаёт значение у успеха и null у ошибки`() {
        assertEquals(5, DomainResult.Success(5).getOrNull())
        assertNull(DomainResult.Failure(DomainError.Offline).getOrNull())
    }

    @Test
    fun `onSuccess вызывается только при успехе`() {
        var called = false

        DomainResult.Failure(DomainError.Unauthorized).onSuccess { called = true }

        assertEquals(false, called)
    }

    @Test
    fun `onFailure получает ошибку`() {
        var received: DomainError? = null

        DomainResult.Failure(DomainError.RateLimited).onFailure { received = it }

        assertEquals(DomainError.RateLimited, received)
    }
}