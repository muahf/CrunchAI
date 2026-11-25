package com.crunchai.data.remote.api

import com.crunchai.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/**
 * Все пути без ведущего «/» — они добавляются к baseUrl, который должен
 * заканчиваться на ".../api/v1/" (см. шаг 2).
 */
interface CrunchApi {

    // --- Звуки (Станция) ---
    @POST("events/batch")
    suspend fun sendEvents(@Body body: SyncBatchRequestDto): SyncReportDto

    // --- Дневник ---
    @GET("meals")
    suspend fun getMeals(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): PageDto<MealDto>

    @GET("anomalies")
    suspend fun getAnomalies(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): PageDto<AnomalyDto>

    // --- Привязка ---
    @POST("pairing/sessions")                 // Станция создаёт ключ (тела нет)
    suspend fun createPairKey(): PairingSessionDto

    @POST("pairing/confirm")                  // Наблюдатель вводит ключ
    suspend fun pairWithStation(@Body body: PairRequestDto): PairConfirmDto

    // --- Анализ фото (multipart: поле kind + файл photo) ---
    @Multipart
    @POST("food-analyses")
    suspend fun analyzeFood(
        @Part("kind") kind: RequestBody,
        @Part photo: MultipartBody.Part,
    ): FoodAnalysisDto
    @POST("auth/signup")
    suspend fun signUp(@Body body: AuthRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun logIn(@Body body: AuthRequestDto): AuthResponseDto
}