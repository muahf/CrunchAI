package com.crunchai.data.remote.dto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- Общие ----------

/** Страница списка: {"items":[...],"next_cursor":"...","has_more":true} */
@Serializable
data class PageDto<T>(
    val items: List<T>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ErrorBodyDto(
    val code: String = "",
    val message: String = "",
    @SerialName("request_id") val requestId: String? = null,
)

// ---------- Звуки ----------

@Serializable
data class SoundEventDto(
    val id: String,                       // UUID, его создаёт телефон
    val type: String,                     // "cough" | "chew" | "lap" | "bark" | "other" (строчными!)
    val confidence: Double,               // 0.0..1.0
    @SerialName("started_at") val startedAt: String,  // 2026-11-25T10:00:00Z
    val duration: Int,                    // секунды, 0..600 (раньше было duration_seconds)
)

@Serializable
data class SyncBatchRequestDto(val events: List<SoundEventDto>)

@Serializable
data class SyncReportDto(
    val accepted: Int,
    val duplicates: List<String> = emptyList(),   // раньше было duplicate_ids
)

// ---------- Дневник ----------

@Serializable
data class MealDto(
    val id: String,
    @SerialName("started_at") val startedAt: String,
    val duration: Int,
    @SerialName("calm_index") val calmIndex: Int,
)

@Serializable
data class AnomalyDto(
    val id: String,
    val type: String,
    @SerialName("started_at") val startedAt: String,
    val duration: Int,
)

// ---------- Привязка ----------

/** Станция: POST /pairing/sessions → ключ и время жизни */
@Serializable
data class PairingSessionDto(
    val key: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class PairRequestDto(val key: String)

@Serializable
data class PairConfirmDto(
    @SerialName("connection_id") val connectionId: String,
    @SerialName("station_name") val stationName: String,
)

// ---------- Анализ фото ----------

@Serializable
data class FoodAnalysisDto(
    val id: String,
    val kind: String,                                   // "bowl_after" | "feed_label"
    @SerialName("eaten_percent") val eatenPercent: Int? = null,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String,
)

// ---------- Авторизация ----------

@Serializable
data class AuthRequestDto(
    val email: String,
    val password: String,
    val role: String,                                   // "station" | "observer"
    @SerialName("device_name") val deviceName: String,
)
@Serializable
data class AuthResponseDto(
    val user: AuthUserDto,
    val device: AuthDeviceDto,
    val token: String,
    @SerialName("expires_in") val expiresIn: Long = 0,
)

@Serializable
data class AuthUserDto(val id: String, val email: String)

@Serializable
data class AuthDeviceDto(val id: String, val role: String, val name: String)

// ---------- Профиль питомца ----------

@Serializable
data class PetDto(
    val name: String,
    val species: String,                                // "cat" | "dog" | "other"
    @SerialName("age_month") val ageMonth: Int? = null, // именно age_month, без «s»
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("body_score") val bodyScore: Int? = null,
)