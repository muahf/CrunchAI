package com.example.crunchai.domain.repository

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Pet
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    /** Текущий профиль питомца (null, пока не создан). */
    fun observePet(): Flow<Pet?>

    suspend fun savePet(pet: Pet): DomainResult<Unit>
}