package com.crunchai.data.mapper

import com.crunchai.data.remote.dto.AnomalyDto
import com.crunchai.data.remote.dto.FoodAnalysisDto
import com.crunchai.data.remote.dto.MealDto
import com.crunchai.data.remote.dto.PairConfirmDto
import com.crunchai.data.remote.dto.PairingSessionDto
import com.crunchai.data.remote.dto.SoundEventDto
import com.crunchai.data.remote.dto.SyncReportDto
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.model.FoodAnalysis
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.PairingSession
import com.example.crunchai.domain.model.PairingStatus
import com.example.crunchai.domain.model.PhotoKind
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SyncReport
import java.time.Instant

/** Сервер принимает тип звука строчными буквами: "cough", "chew"... */
fun SoundEvent.toDto(): SoundEventDto =
    SoundEventDto(
        id = id,
        type = type.name.lowercase(),
        confidence = confidence.toDouble(),
        startedAt = startedAt.toString(),
        duration = durationSeconds,
    )

fun SyncReportDto.toDomain(): SyncReport =
    SyncReport(
        accepted = accepted,
        duplicateIds = duplicates,
    )

/** Станция создала ключ: ждём, пока Наблюдатель его введёт. */
fun PairingSessionDto.toDomain(): PairingSession =
    PairingSession(
        key = key,
        role = DeviceRole.STATION,
        status = PairingStatus.WAITING,
        expiresAt = Instant.parse(expiresAt),
    )

/** Наблюдатель ввёл ключ: сервер ключ не возвращает, поэтому берём тот, что вводили. */
fun PairConfirmDto.toDomain(key: String): PairingSession =
    PairingSession(
        key = key,
        role = DeviceRole.OBSERVER,
        status = PairingStatus.PAIRED,
        expiresAt = null,
    )

fun FoodAnalysisDto.toDomain(): FoodAnalysis {
    // На сервере один текст «comment»
    return FoodAnalysis(
        id = id,
        kind = PhotoKind.entries.find { it.name.equals(kind, ignoreCase = true) } ?: PhotoKind.BOWL_AFTER,
        eatenPercent = eatenPercent,
        composition = comment,
    )
}

fun MealDto.toDomain(): Meal = Meal(id, Instant.parse(startedAt), duration, calmIndex)

fun AnomalyDto.toDomain(): Anomaly {
    // Сервер статус «просмотрено» в списке не отдаёт
    return Anomaly(
        id = id,
        type = AnomalyType.entries.find { it.name.equals(type, ignoreCase = true) } ?: AnomalyType.COUGH,
        startedAt = Instant.parse(startedAt),
        durationSeconds = duration,
        seen = false,
    )
}
