package com.crunchai.presentation.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crunchai.domain.usecase.ObservePetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MonitorViewModel
    @Inject
    constructor(
        private val observePetUseCase: ObservePetUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MonitorUiState())
        val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

        private var connectJob: Job? = null
        private var timerJob: Job? = null
        private var waveJob: Job? = null

        init {
            observePet()
            startStream()
        }

        private fun observePet() {
            viewModelScope.launch {
                observePetUseCase().collect { pet ->
                    pet?.name?.takeIf { it.isNotBlank() }?.let { name ->
                        _uiState.update { it.copy(petName = name) }
                    }
                }
            }
        }

        fun startStream() {
            cancelStreamJobs()
            _uiState.update {
                it.copy(
                    connectionState = MonitorConnectionState.CONNECTING,
                    elapsedSeconds = 0L,
                    errorMessage = null,
                    errorDetails = null,
                    showErrorDetails = false,
                )
            }

            connectJob =
                viewModelScope.launch {
                    delay(900)
                    if (!_uiState.value.isStationOnline) {
                        _uiState.update {
                            it.copy(connectionState = MonitorConnectionState.STATION_OFFLINE)
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(connectionState = MonitorConnectionState.LIVE)
                    }
                    startTimer()
                    startWaveform()
                }
        }

        fun stopStream() {
            cancelStreamJobs()
            _uiState.update {
                it.copy(
                    connectionState = MonitorConnectionState.STOPPED,
                    waveformBars = List(24) { 0.12f },
                )
            }
        }

        fun toggleMute() {
            _uiState.update { state ->
                val nextMuted = !state.isMuted
                state.copy(
                    isMuted = nextMuted,
                    waveformBars = if (nextMuted) List(24) { 0.12f } else defaultMonitorWaveform(),
                )
            }
        }

        fun toggleErrorDetails() {
            _uiState.update { it.copy(showErrorDetails = !it.showErrorDetails) }
        }

        fun simulateStationOffline() {
            cancelStreamJobs()
            _uiState.update {
                it.copy(
                    isStationOnline = false,
                    connectionState = MonitorConnectionState.STATION_OFFLINE,
                    waveformBars = List(24) { 0.12f },
                )
            }
        }

        fun simulateNoInternet() {
            cancelStreamJobs()
            _uiState.update {
                it.copy(
                    connectionState = MonitorConnectionState.NO_INTERNET,
                    waveformBars = List(24) { 0.12f },
                )
            }
        }

        fun simulateServerError(details: String = "WebRTC / audio relay timeout (503)") {
            cancelStreamJobs()
            _uiState.update {
                it.copy(
                    connectionState = MonitorConnectionState.SERVER_ERROR,
                    errorMessage = "Сервер временно недоступен. Попробуйте ещё раз через минуту.",
                    errorDetails = details,
                    waveformBars = List(24) { 0.12f },
                )
            }
        }

        private fun startTimer() {
            timerJob?.cancel()
            timerJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1_000L)
                        if (_uiState.value.connectionState == MonitorConnectionState.LIVE) {
                            _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1L) }
                        }
                    }
                }
        }

        private fun startWaveform() {
            waveJob?.cancel()
            waveJob =
                viewModelScope.launch {
                    while (true) {
                        delay(300L)
                        val current = _uiState.value
                        if (current.connectionState == MonitorConnectionState.LIVE && !current.isMuted) {
                            _uiState.update {
                                it.copy(
                                    waveformBars = List(24) { Random.nextFloat().coerceIn(0.16f, 0.96f) },
                                )
                            }
                        }
                    }
                }
        }

        private fun cancelStreamJobs() {
            connectJob?.cancel()
            timerJob?.cancel()
            waveJob?.cancel()
        }

        private fun defaultMonitorWaveform(): List<Float> =
            listOf(
                0.22f, 0.38f, 0.65f, 0.45f, 0.80f, 0.55f, 0.30f, 0.72f,
                0.90f, 0.48f, 0.35f, 0.68f, 0.84f, 0.52f, 0.28f, 0.60f,
                0.76f, 0.42f, 0.25f, 0.58f, 0.70f, 0.40f, 0.32f, 0.20f,
            )
    }
