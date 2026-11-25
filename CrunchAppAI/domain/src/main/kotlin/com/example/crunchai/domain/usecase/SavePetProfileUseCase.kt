package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.repository.PetRepository
import javax.inject.Inject

/** Проверяет профиль питомца и сохраняет. Границы такие же, как на сервере. */
class SavePetProfileUseCase @Inject constructor(private val repository: PetRepository) {

    suspend operator fun invoke(pet: Pet): DomainResult<Unit> {
        val error = validate(pet)
        if (error != null) return DomainResult.Failure(error)
        return repository.savePet(pet.copy(name = pet.name.trim()))
    }

    private fun validate(pet: Pet): DomainError.Validation? {
        val age = pet.ageMonths
        val weight = pet.weightKg
        val score = pet.bodyScore
        return when {
            pet.name.isBlank() -> DomainError.Validation("name", "Введите имя")
            pet.name.trim().length > MAX_NAME -> DomainError.Validation("name", "Не больше $MAX_NAME символов")
            age != null && age !in 0..MAX_AGE_MONTHS ->
                DomainError.Validation("ageMonths", "Возраст от 0 до $MAX_AGE_MONTHS месяцев")
            weight != null && weight !in MIN_WEIGHT..MAX_WEIGHT ->
                DomainError.Validation("weightKg", "Вес от $MIN_WEIGHT до $MAX_WEIGHT кг")
            score != null && score !in 1..9 -> DomainError.Validation("bodyScore", "Оценка от 1 до 9")
            else -> null
        }
    }

    private companion object {
        const val MAX_NAME = 40
        const val MAX_AGE_MONTHS = 480
        const val MIN_WEIGHT = 0.1
        const val MAX_WEIGHT = 120.0
    }
}