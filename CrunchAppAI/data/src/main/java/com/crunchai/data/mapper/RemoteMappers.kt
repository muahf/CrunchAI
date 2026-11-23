package com.crunchai.data.mapper

import com.crunchai.data.remote.dto.FoodAnalysisDto
import com.crunchai.data.remote.dto.PairingSessionDto
import com.crunchai.data.remote.dto.SoundEventDto
import com.crunchai.data.remote.dto.SyncReportDto
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.FoodAnalysis
import com.example.crunchai.domain.model.PairingSession
import com.example.crunchai.domain.model.PairingStatus
import com.example.crunchai.domain.model.PhotoKind
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SyncReport
import java.time.Instant

fun SoundEvent.toDto(): SoundEventDto =
    SoundEventDto(
        id = id,
        type = type.name,
        confidence = confidence,
        startedAt = startedAt.toString(),
        durationSeconds = durationSeconds,
    )

fun SyncReportDto.toDomain(): SyncReport =
    SyncReport(
        accepted = accepted,
        duplicateIds = duplicateIds,
    )

fun PairingSessionDto.toDomain(): PairingSession =
    PairingSession(
        key = key,
        role = DeviceRole.entries.find { it.name.equals(role, ignoreCase = true) } ?: DeviceRole.OBSERVER,
        status = PairingStatus.entries.find { it.name.equals(status, ignoreCase = true) } ?: PairingStatus.WAITING,
        expiresAt = expiresAt?.let { Instant.parse(it) },
    )

fun FoodAnalysisDto.toDomain(): FoodAnalysis =
    FoodAnalysis(
        id = id,
        kind = PhotoKind.entries.find { it.name.equals(kind, ignoreCase = true) } ?: PhotoKind.BOWL_AFTER,
        eatenPercent = eatenPercent,
        composition = composition,
        risks = risks,
    )
