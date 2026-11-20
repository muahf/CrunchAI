package com.example.crunchai.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateCalmIndexUseCaseTest {
    private val calculate = CalculateCalmIndexUseCase()

    @Test
    fun `без кашля индекс 10`() = assertEquals(10, calculate(0))

    @Test
    fun `каждый кашель снижает индекс на 1`() = assertEquals(7, calculate(3))

    @Test
    fun `индекс не бывает меньше 1`() {
        assertEquals(1, calculate(9))
        assertEquals(1, calculate(50))
    }

    @Test
    fun `отрицательное число кашля не даёт индекс выше 10`() = assertEquals(10, calculate(-2))
}