package com.crunchai.presentation.pairing

import com.example.crunchai.domain.model.DeviceRole
import java.time.Instant

enum class ObserverScanState {
    SCANNING,
    CHECKING,
    NOT_FOUND,
    SERVER_ERROR,
    OFFLINE,
    PAIRED,
}

data class PairingUiState(
    val role: DeviceRole = DeviceRole.STATION,
    // --- Состояние Станции ---
    val isCreatingKey: Boolean = false,
    val pairKey: String? = null,
    val expiresAt: Instant? = null,
    val remainingMinutes: Int = 10,
    val isKeyExpired: Boolean = false,
    val stationError: String? = null,
    val stationErrorDetails: String? = null,
    val showStationDetails: Boolean = false,
    val isStationOffline: Boolean = false,
    // --- Состояние Наблюдателя ---
    val observerState: ObserverScanState = ObserverScanState.SCANNING,
    val manualKeyInput: String = "",
    val manualKeyError: String? = null,
    val showManualInputDialog: Boolean = false,
    val observerErrorDetails: String? = null,
    val showObserverDetails: Boolean = false,
    val pairedSuccess: Boolean = false,
)
