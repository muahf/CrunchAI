package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.FoodAnalysis
import com.example.crunchai.domain.model.PhotoKind
import com.example.crunchai.domain.repository.FoodAnalysisRepository

class AnalyzeBowlPhotoUseCase(
    private val repository: FoodAnalysisRepository,
) {
    suspend operator fun invoke(photo: ByteArray): DomainResult<FoodAnalysis> {
        if (photo.isEmpty()) {
            return DomainResult.Failure(DomainError.Validation("photo", "Фото пустое"))
        }
        return repository.analyze(PhotoKind.BOWL_AFTER, photo)
    }
}