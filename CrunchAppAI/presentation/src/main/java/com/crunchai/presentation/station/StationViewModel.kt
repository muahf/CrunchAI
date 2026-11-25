package com.crunchai.presentation.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class StationViewModel
@Inject
constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(StationUiState(isModelLoading = true))
    val uiState: StateFlow<StationUiState> = _uiState.asStateFlow()

    init {
        startYamnet()
    }

    private fun startYamnet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isModelLoading = true) }
            delay(1200)
            val now = Instant.now()
            _uiState.update {
                it.copy(
                    isModelLoading = false,
                    isListening = true,
                    events =
                        listOf(
                            StationSoundEvent(
                                id = UUID.randomUUID().toString(),
                                timestamp = now,
                                kind = StationSoundKind.COUGH,
                                durationSeconds = 6,
                                confidence = 0.91f,
                            ),
                            StationSoundEvent(
                                id = UUID.randomUUID().toString(),
                                timestamp = now.minus(2, ChronoUnit.MINUTES),
                                kind = StationSoundKind.SILENCE,
                                confidence = 0.88f,
                            ),
                            StationSoundEvent(
                                id = UUID.randomUUID().toString(),
                                timestamp = now.minus(3, ChronoUnit.MINUTES),
                                kind = StationSoundKind.CHEW,
                                confidence = 0.83f,
                            ),
                            StationSoundEvent(
                                id = UUID.randomUUID().toString(),
                                timestamp = now.minus(3, ChronoUnit.MINUTES),
                                kind = StationSoundKind.CRUNCH,
                                confidence = 0.95f,
                            ),
                        ),
                )
            }
            animateWaveform()
        }
    }

    private fun animateWaveform() {
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
        _uiState.update { state ->
            val nextListening = !state.isListening
            state.copy(
                isListening = nextListening,
                waveformBars = if (nextListening) state.waveformBars else List(24) { 0.12f },
            )
        }
    }

    fun onRetrySync() {
        _uiState.update { it.copy(syncError = null, pendingEventsCount = 0) }
    }
}