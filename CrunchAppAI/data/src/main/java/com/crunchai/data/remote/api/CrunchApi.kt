package com.crunchai.data.remote.api

import com.crunchai.data.remote.dto.AnomalyDto
import com.crunchai.data.remote.dto.FoodAnalysisDto
import com.crunchai.data.remote.dto.MealDto
import com.crunchai.data.remote.dto.PairRequestDto
import com.crunchai.data.remote.dto.PairingSessionDto
import com.crunchai.data.remote.dto.SyncBatchRequestDto
import com.crunchai.data.remote.dto.SyncReportDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface CrunchApi {
    @POST("api/v1/events/batch")
    suspend fun sendEvents(
        @Body request: SyncBatchRequestDto,
    ): SyncReportDto

    @POST("api/v1/pairing/key")
    suspend fun createPairKey(): PairingSessionDto

    @POST("api/v1/pairing/pair")
    suspend fun pairWithStation(
        @Body request: PairRequestDto,
    ): PairingSessionDto

    @Multipart
    @POST("api/v1/food/analyze")
    suspend fun analyzeFood(
        @Part("kind") kind: RequestBody,
        @Part photo: MultipartBody.Part,
    ): FoodAnalysisDto

    @GET("api/v1/diary/meals")
    suspend fun getMeals(
        @Query("period") period: String,
    ): List<MealDto>

    @GET("api/v1/diary/anomalies")
    suspend fun getAnomalies(
        @Query("period") period: String,
    ): List<AnomalyDto>
}
