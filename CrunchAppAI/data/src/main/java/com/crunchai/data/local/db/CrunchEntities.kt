package com.crunchai.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_events")
data class SoundEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val confidence: Float,
    val startedAtEpochMillis: Long,
    val durationSeconds: Int,
    val syncStatus: String,
)

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val species: String,
    val ageMonths: Int?,
    val weightKg: Double?,
    val bodyScore: Int?,
    val notes: String? = null,
)

@Entity(tableName = "cached_meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val startedAtEpochMillis: Long,
    val durationSeconds: Int,
    val calmIndex: Int,
)

@Entity(tableName = "cached_anomalies")
data class AnomalyEntity(
    @PrimaryKey val id: String,
    val type: String,
    val startedAtEpochMillis: Long,
    val durationSeconds: Int,
    val seen: Boolean,
)
