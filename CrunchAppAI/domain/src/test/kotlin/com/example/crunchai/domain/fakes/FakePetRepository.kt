package com.example.crunchai.domain.fakes

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakePetRepository(
    var saveResult: DomainResult<Unit> = DomainResult.Success(Unit),
) : PetRepository {
    var saved: Pet? = null

    override fun observePet(): Flow<Pet?> = flowOf(saved)

    override suspend fun savePet(pet: Pet): DomainResult<Unit> {
        saved = pet
        return saveResult
    }
}