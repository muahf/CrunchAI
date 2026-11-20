package com.example.crunchai.domain.repository

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.FoodAnalysis
import com.example.crunchai.domain.model.PhotoKind

interface FoodAnalysisRepository {
    /** Отправляет фото на сервер, а сервер обращается к ИИ (Gemini). */
    suspend fun analyze(kind: PhotoKind, photo: ByteArray): DomainResult<FoodAnalysis>
}