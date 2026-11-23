package com.crunchai.data.mapper

import com.crunchai.data.local.db.AnomalyEntity
import com.crunchai.data.local.db.MealEntity
import com.crunchai.data.local.db.PetEntity
import com.crunchai.data.local.db.SoundEventEntity
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SoundType
import com.example.crunchai.domain.model.Species
import com.example.crunchai.domain.model.SyncStatus
import java.time.Instant

fun SoundEvent.toEntity(): SoundEventEntity =
    SoundEventEntity(
        id = id,
        type = type.name,
        confidence = confidence,
        startedAtEpochMillis = startedAt.toEpochMilli(),
        durationSeconds = durationSeconds,
        syncStatus = syncStatus.name,
    )

fun SoundEventEntity.toDomain(): SoundEvent =
    SoundEvent(
        id = id,
        type = SoundType.valueOf(type),
        confidence = confidence,
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
        durationSeconds = durationSeconds,
        syncStatus = SyncStatus.valueOf(syncStatus),
    )

fun Pet.toEntity(): PetEntity =
    PetEntity(
        id = id,
        name = name,
        species = species.name,
        ageMonths = ageMonths,
        weightKg = weightKg,
        bodyScore = bodyScore,
    )

fun PetEntity.toDomain(): Pet =
    Pet(
        id = id,
        name = name,
        species = Species.valueOf(species),
        ageMonths = ageMonths,
        weightKg = weightKg,
        bodyScore = bodyScore,
    )

fun Meal.toEntity(): MealEntity =
    MealEntity(
        id = id,
        startedAtEpochMillis = startedAt.toEpochMilli(),
        durationSeconds = durationSeconds,
        calmIndex = calmIndex,
    )

fun MealEntity.toDomain(): Meal =
    Meal(
        id = id,
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
        durationSeconds = durationSeconds,
        calmIndex = calmIndex,
    )

fun Anomaly.toEntity(): AnomalyEntity =
    AnomalyEntity(
        id = id,
        type = type.name,
        startedAtEpochMillis = startedAt.toEpochMilli(),
        durationSeconds = durationSeconds,
        seen = seen,
    )

fun AnomalyEntity.toDomain(): Anomaly =
    Anomaly(
        id = id,
        type = AnomalyType.valueOf(type),
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis),
        durationSeconds = durationSeconds,
        seen = seen,
    )
