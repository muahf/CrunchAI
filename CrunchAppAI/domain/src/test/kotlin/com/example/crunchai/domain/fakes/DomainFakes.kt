package com.example.crunchai.domain.fakes

import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.FoodAnalysis
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.PhotoKind
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SyncReport
import com.example.crunchai.domain.model.SyncStatus
import com.example.crunchai.domain.port.AudioChunk
import com.example.crunchai.domain.port.AudioRecorder
import com.example.crunchai.domain.port.Classification
import com.example.crunchai.domain.port.PhotoCapturer
import com.example.crunchai.domain.port.SoundClassifier
import com.example.crunchai.domain.repository.AlertRepository
import com.example.crunchai.domain.repository.DiaryRepository
import com.example.crunchai.domain.repository.EventSyncRepository
import com.example.crunchai.domain.repository.FoodAnalysisRepository
import com.example.crunchai.domain.repository.SoundEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAudioRecorder(private val chunksList: List<AudioChunk>) : AudioRecorder {
    override fun chunks(): Flow<AudioChunk> = flowOf(*chunksList.toTypedArray())
}

class FakeSoundClassifier(
    private val response: (AudioChunk) -> Classification,
) : SoundClassifier {
    override suspend fun classify(chunk: AudioChunk): Classification = response(chunk)
}

class FakeSoundEventRepository : SoundEventRepository {
    val saved = mutableListOf<SoundEvent>()
    val sentIds = mutableListOf<String>()

    override suspend fun save(event: SoundEvent) {
        saved.add(event)
    }

    override suspend fun getUnsent(limit: Int): List<SoundEvent> =
        saved.filter { it.syncStatus == SyncStatus.LOCAL }.take(limit)

    override suspend fun markSent(ids: List<String>) {
        sentIds.addAll(ids)
        for (i in saved.indices) {
            if (saved[i].id in ids) {
                saved[i] = saved[i].copy(syncStatus = SyncStatus.SENT)
            }
        }
    }
}

class FakeEventSyncRepository(
    var result: DomainResult<SyncReport> = DomainResult.Success(SyncReport(0)),
) : EventSyncRepository {
    var lastSentBatch: List<SoundEvent> = emptyList()

    override suspend fun send(events: List<SoundEvent>): DomainResult<SyncReport> {
        lastSentBatch = events
        return result
    }
}

class FakeAlertRepository(private val alerts: List<Anomaly>) : AlertRepository {
    override fun observeAlerts(): Flow<Anomaly> = flowOf(*alerts.toTypedArray())
}

class FakeDiaryRepository(
    var mealsResult: DomainResult<List<Meal>> = DomainResult.Success(emptyList()),
    var anomaliesResult: DomainResult<List<Anomaly>> = DomainResult.Success(emptyList()),
) : DiaryRepository {
    override suspend fun getMeals(period: DiaryPeriod): DomainResult<List<Meal>> = mealsResult
    override suspend fun getAnomalies(period: DiaryPeriod): DomainResult<List<Anomaly>> = anomaliesResult
}

class FakePhotoCapturer(var result: DomainResult<ByteArray>) : PhotoCapturer {
    override suspend fun capture(): DomainResult<ByteArray> = result
}

class FakeFoodAnalysisRepository(var result: DomainResult<FoodAnalysis>) : FoodAnalysisRepository {
    var lastKind: PhotoKind? = null

    override suspend fun analyze(kind: PhotoKind, photo: ByteArray): DomainResult<FoodAnalysis> {
        lastKind = kind
        return result
    }
}