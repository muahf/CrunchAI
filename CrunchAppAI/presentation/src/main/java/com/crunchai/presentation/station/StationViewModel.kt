package com.crunchai.presentation.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SoundType
import com.example.crunchai.domain.usecase.MonitorSoundsUseCase
import com.example.crunchai.domain.usecase.SyncPendingEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class StationViewModel
    @Inject
    constructor(
        private val monitorSoundsUseCase: MonitorSoundsUseCase,
        private val syncPendingEventsUseCase: SyncPendingEventsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StationUiState(isModelLoading = true))
        val uiState: StateFlow<StationUiState> = _uiState.asStateFlow()

        private var monitorJob: Job? = null
        private var syncJob: Job? = null
        private var waveformJob: Job? = null

        init {
            startYamnet()
        }

        private fun startYamnet() {
            viewModelScope.launch {
                _uiState.update { it.copy(isModelLoading = true) }
                delay(1200)
                _uiState.update {
                    it.copy(
                        isModelLoading = false,
                        isListening = true,
                    )
                }
                startWaveformAnimation()
                startSoundMonitoring()
                syncPendingEvents()
            }
        }

        private fun startSoundMonitoring() {
            monitorJob?.cancel()
            monitorJob =
                viewModelScope.launch {
                    monitorSoundsUseCase()
                        .catch { cause ->
                            _uiState.update {
                                it.copy(
                                    isListening = false,
                                    syncError = cause.localizedMessage ?: "Ошибка записи звука",
                                )
                            }
                        }
                        .collect { domainEvent ->
                            val uiEvent = domainEvent.toStationUiEvent()
                            _uiState.update { state ->
                                state.copy(
                                    events = (listOf(uiEvent) + state.events).take(MAX_VISIBLE_EVENTS),
                                    pendingEventsCount = state.pendingEventsCount + 1,
                                )
                            }
                            syncPendingEvents()
                        }
                }
        }

        private fun syncPendingEvents() {
            if (syncJob?.isActive == true) return
            syncJob =
                viewModelScope.launch {
                    when (val result = syncPendingEventsUseCase()) {
                        is DomainResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isOffline = false,
                                    syncError = null,
                                    pendingEventsCount = 0,
                                )
                            }
                        }

                        is DomainResult.Failure -> {
                            when (result.error) {
                                DomainError.Offline ->
                                    _uiState.update {
                                        it.copy(
                                            isOffline = true,
                                            syncError = null,
                                        )
                                    }

                                else ->
                                    _uiState.update { state ->
                                        state.copy(
                                            isOffline = false,
                                            syncError =
                                                "Не удалось отправить события на сервер. Ждут отправки: ${state.pendingEventsCount}.",
                                        )
                                    }
                            }
                        }
                    }
                }
        }

        private fun startWaveformAnimation() {
            if (waveformJob?.isActive == true) return
            waveformJob =
                viewModelScope.launch {
                    while (true) {
                        delay(450)
                        if (_uiState.value.isListening) {
                            _uiState.update { state ->
                                state.copy(
                                    waveformBars = List(24) { Random.nextFloat().coerceIn(0.14f, 0.95f) },
                                )
                            }
                        }
                    }
                }
        }

        fun toggleListening() {
            val nextListening = !_uiState.value.isListening
            _uiState.update { state ->
                state.copy(
                    isListening = nextListening,
                    waveformBars = if (nextListening) state.waveformBars else List(24) { 0.12f },
                )
            }
            if (nextListening) {
                startSoundMonitoring()
                syncPendingEvents()
            } else {
                monitorJob?.cancel()
            }
        }

        fun onRetrySync() {
            syncPendingEvents()
        }

        private fun SoundEvent.toStationUiEvent(): StationSoundEvent {
            val kind =
                when (type) {
                    SoundType.COUGH -> StationSoundKind.COUGH
                    SoundType.CHEW -> StationSoundKind.CHEW
                    SoundType.LAP -> StationSoundKind.LAP
                    SoundType.BARK,
                    SoundType.OTHER,
                    -> StationSoundKind.SILENCE
                }
            return StationSoundEvent(
                id = id,
                timestamp = startedAt,
                kind = kind,
                durationSeconds = durationSeconds,
                confidence = confidence,
            )
        }

        private companion object {
            const val MAX_VISIBLE_EVENTS = 30
        }
    }
