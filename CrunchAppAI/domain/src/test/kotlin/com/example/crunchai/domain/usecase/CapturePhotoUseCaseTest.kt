package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.fakes.FakePhotoCapturer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CapturePhotoUseCaseTest {
    @Test
    fun `возвращает байты снимка от камеры`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val capture = CapturePhotoUseCase(FakePhotoCapturer(DomainResult.Success(bytes)))

        val result = (capture() as DomainResult.Success).value

        assertArrayEquals(bytes, result)
    }

    @Test
    fun `возвращает ошибку при сбое камеры`() = runTest {
        val capture = CapturePhotoUseCase(FakePhotoCapturer(DomainResult.Failure(DomainError.Offline)))

        assertEquals(DomainResult.Failure(DomainError.Offline), capture())
    }
}