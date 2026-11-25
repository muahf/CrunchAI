package com.crunchai.data.repository

import com.crunchai.data.mapper.toDomain
import com.crunchai.data.remote.api.CrunchApi
import com.crunchai.data.remote.network.safeApiCall
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.FoodAnalysis
import com.example.crunchai.domain.model.PhotoKind
import com.example.crunchai.domain.repository.FoodAnalysisRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class RemoteFoodAnalysisRepositoryImpl
@Inject
constructor(
    private val api: CrunchApi,
) : FoodAnalysisRepository {
    override suspend fun analyze(
        kind: PhotoKind,
        photo: ByteArray,
    ): DomainResult<FoodAnalysis> =
        safeApiCall {
            // сервер ждёт "bowl_after" / "feed_label" строчными
            val kindBody = kind.name.lowercase().toRequestBody("text/plain".toMediaType())
            val photoBody = photo.toRequestBody("image/jpeg".toMediaType())
            val photoPart = MultipartBody.Part.createFormData("photo", "photo.jpg", photoBody)

            api.analyzeFood(kind = kindBody, photo = photoPart).toDomain()
        }
}