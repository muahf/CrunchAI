package com.crunchai.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crunchai.presentation.common.toUserMessage
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DeviceRole
import com.example.crunchai.domain.usecase.GeneratePairKeyUseCase
import com.example.crunchai.domain.usecase.PairWithStationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class PairingViewModel
    @Inject
    constructor(
        private val generatePairKey: GeneratePairKeyUseCase,
        private val pairWithStation: PairWithStationUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow(PairingUiState())
        val state: StateFlow<PairingUiState> = _state.asStateFlow()

        private var timerJob: Job? = null

        fun setRole(role: DeviceRole) {
            _state.update { it.copy(role = role) }
            if (role == DeviceRole.STATION && _state.value.pairKey == null && !_state.value.isCreatingKey) {
                createNewKey()
            }
        }

        fun createNewKey() {
            if (_state.value.isCreatingKey) return
            timerJob?.cancel()
            _state.update {
                it.copy(
                    isCreatingKey = true,
                    isKeyExpired = false,
                    stationError = null,
                    stationErrorDetails = null,
                    showStationDetails = false,
                    isStationOffline = false,
                )
            }

            viewModelScope.launch {
                when (val result = generatePairKey()) {
                    is DomainResult.Success -> {
                        val session = result.value
                        val expires = session.expiresAt ?: Instant.now().plusSeconds(600)
                        _state.update {
                            it.copy(
                                isCreatingKey = false,
                                pairKey = session.key,
                                expiresAt = expires,
                                remainingMinutes = 10,
                                isKeyExpired = false,
                            )
                        }
                        startExpiryCountdown(expires)
                    }
                    is DomainResult.Failure -> {
                        when (val err = result.error) {
                            is DomainError.Offline ->
                                _state.update {
                                    it.copy(
                                        isCreatingKey = false,
                                        isStationOffline = true,
                                    )
                                }
                            else ->
                                _state.update {
                                    it.copy(
                                        isCreatingKey = false,
                                        stationError = "Сервер временно недоступен. Попробуйте ещё раз через минуту.",
                                        stationErrorDetails = err.toUserMessage(),
                                    )
                                }
                        }
                    }
                }
            }
        }

        private fun startExpiryCountdown(expiresAt: Instant) {
            timerJob =
                viewModelScope.launch {
                    while (true) {
                        val leftSeconds = Duration.between(Instant.now(), expiresAt).seconds
                        if (leftSeconds <= 0) {
                            _state.update { it.copy(isKeyExpired = true, remainingMinutes = 0) }
                            break
                        }
                        val mins = ((leftSeconds + 59) / 60).toInt().coerceAtLeast(1)
                        _state.update { it.copy(remainingMinutes = mins) }
                        delay(5_000L)
                    }
                }
        }

        fun toggleStationDetails() {
            _state.update { it.copy(showStationDetails = !it.showStationDetails) }
        }

        // --- Методы Наблюдателя ---

        fun openManualDialog() {
            _state.update { it.copy(showManualInputDialog = true, manualKeyError = null) }
        }

        fun closeManualDialog() {
            _state.update { it.copy(showManualInputDialog = false, manualKeyError = null) }
        }

        fun onManualKeyChange(raw: String) {
            val cleaned = raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
            val formatted =
                if (cleaned.length > 3) {
                    "${cleaned.substring(0, 3)}-${cleaned.substring(3)}"
                } else {
                    cleaned
                }
            _state.update { it.copy(manualKeyInput = formatted, manualKeyError = null) }
        }

        fun submitKey(rawKey: String) {
            if (_state.value.observerState == ObserverScanState.CHECKING) return
            _state.update {
                it.copy(
                    observerState = ObserverScanState.CHECKING,
                    showManualInputDialog = false,
                    manualKeyError = null,
                    observerErrorDetails = null,
                    showObserverDetails = false,
                )
            }

            viewModelScope.launch {
                when (val result = pairWithStation(rawKey)) {
                    is DomainResult.Success -> {
                        _state.update {
                            it.copy(
                                observerState = ObserverScanState.PAIRED,
                                pairedSuccess = true,
                            )
                        }
                    }
                    is DomainResult.Failure -> {
                        when (val err = result.error) {
                            is DomainError.Validation ->
                                _state.update {
                                    it.copy(
                                        observerState = ObserverScanState.SCANNING,
                                        showManualInputDialog = true,
                                        manualKeyError = err.reason,
                                    )
                                }
                            is DomainError.NotFound ->
                                _state.update {
                                    it.copy(observerState = ObserverScanState.NOT_FOUND)
                                }
                            is DomainError.Offline ->
                                _state.update {
                                    it.copy(observerState = ObserverScanState.OFFLINE)
                                }
                            else ->
                                _state.update {
                                    it.copy(
                                        observerState = ObserverScanState.SERVER_ERROR,
                                        observerErrorDetails = err.toUserMessage(),
                                    )
                                }
                        }
                    }
                }
            }
        }

        fun resetScanner() {
            _state.update {
                it.copy(
                    observerState = ObserverScanState.SCANNING,
                    observerErrorDetails = null,
                    showObserverDetails = false,
                )
            }
        }

        fun toggleObserverDetails() {
            _state.update { it.copy(showObserverDetails = !it.showObserverDetails) }
        }
    }
