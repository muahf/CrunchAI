package com.crunchai.data.repository

import com.crunchai.data.local.db.PetDao
import com.crunchai.data.mapper.toDomain
import com.crunchai.data.mapper.toEntity
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalPetRepositoryImpl
    @Inject
    constructor(
        private val dao: PetDao,
    ) : PetRepository {
        override fun observePet(): Flow<Pet?> = dao.observePet().map { it?.toDomain() }

        override suspend fun savePet(pet: Pet): DomainResult<Unit> {
            dao.upsertPet(pet.toEntity())
            return DomainResult.Success(Unit)
        }
    }
