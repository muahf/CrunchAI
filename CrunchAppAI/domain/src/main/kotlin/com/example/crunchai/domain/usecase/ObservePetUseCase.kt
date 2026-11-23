package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePetUseCase @Inject constructor(
    private val petRepository: PetRepository,
) {
    operator fun invoke(): Flow<Pet?> = petRepository.observePet()
}