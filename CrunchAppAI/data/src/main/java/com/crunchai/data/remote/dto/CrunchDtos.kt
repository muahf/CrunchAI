package com.crunchai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SoundEventDto(
    val id: String,
    val type: String,
    val confidence: Float,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
)

@Serializable
data class SyncBatchRequestDto(
    val events: List<SoundEventDto>,
)

@Serializable
data class SyncReportDto(
    val accepted: Int,
    @SerialName("duplicate_ids") val duplicateIds: List<String> = emptyList(),
)

@Serializable
data class PairingSessionDto(
    val key: String,
    val role: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class PairRequestDto(
    val key: String,
)

@Serializable
data class FoodAnalysisDto(
    val id: String,
    val kind: String,
    @SerialName("eaten_percent") val eatenPercent: Int? = null,
    val composition: String? = null,
    val risks: List<String> = emptyList(),
)

@Serializable
data class MealDto(
    val id: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("calm_index") val calmIndex: Int,
)

@Serializable
data class AnomalyDto(
    val id: String,
    val type: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val seen: Boolean = false,
)
