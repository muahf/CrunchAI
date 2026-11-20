package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.fakes.FakePetRepository
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.model.Species
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavePetProfileUseCaseTest {
    private val repository = FakePetRepository()
    private val save = SavePetProfileUseCase(repository)

    private fun pet(
        name: String = "Рекс",
        weightKg: Double? = 18.4,
        bodyScore: Int? = 5,
    ) = Pet("1", name, Species.DOG, ageMonths = 36, weightKg = weightKg, bodyScore = bodyScore)

    @Test
    fun `корректный профиль сохраняется, имя обрезается по краям`() = runTest {
        val result = save(pet(name = "  Рекс "))

        assertEquals(DomainResult.Success(Unit), result)
        assertEquals("Рекс", repository.saved?.name)
    }

    @Test
    fun `пустое имя это ошибка и в репозиторий ничего не уходит`() = runTest {
        val result = save(pet(name = "   "))

        assertEquals(DomainResult.Failure(DomainError.Validation("name", "Введите имя")), result)
        assertNull(repository.saved)
    }

    @Test
    fun `вес 500 кг это ошибка поля weightKg`() = runTest {
        val result = save(pet(weightKg = 500.0)) as DomainResult.Failure

        assertEquals("weightKg", (result.error as DomainError.Validation).field)
        assertNull(repository.saved)
    }

    @Test
    fun `оценка кондиции 10 это ошибка поля bodyScore`() = runTest {
        val result = save(pet(bodyScore = 10)) as DomainResult.Failure

        assertEquals("bodyScore", (result.error as DomainError.Validation).field)
    }

    @Test
    fun `необязательные поля можно не заполнять`() = runTest {
        val result = save(pet(weightKg = null, bodyScore = null))

        assertEquals(DomainResult.Success(Unit), result)
    }

    @Test
    fun `ошибка репозитория возвращается как есть`() = runTest {
        repository.saveResult = DomainResult.Failure(DomainError.Offline)

        assertEquals(DomainResult.Failure(DomainError.Offline), save(pet()))
    }
}