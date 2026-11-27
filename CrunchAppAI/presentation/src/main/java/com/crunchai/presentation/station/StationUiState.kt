package com.crunchai.presentation.station

import java.time.Instant

enum class StationSoundKind {
    COUGH,
    SILENCE,
    CHEW,
    CRUNCH,
    LAP,
}

data class StationSoundEvent(
    val id: String,
    val timestamp: Instant,
    val kind: StationSoundKind,
    val durationSeconds: Int? = null,
    val confidence: Float,
)

data class StationUiState(
    val isModelLoading: Boolean = false,
    val isListening: Boolean = true,
    val isCharging: Boolean = true,
    val batteryPercent: Int = 87,
    val isOffline: Boolean = false,
    val pendingEventsCount: Int = 0,
    val syncError: String? = null,
    val waveformBars: List<Float> = defaultWaveform(),
    val events: List<StationSoundEvent> = emptyList(),
)

private fun defaultWaveform(): List<Float> =
    listOf(
        0.18f, 0.28f, 0.22f, 0.45f, 0.30f, 0.55f, 0.78f, 0.52f, 0.35f, 0.62f,
        0.48f, 0.26f, 0.40f, 0.70f, 0.85f, 0.58f, 0.32f, 0.44f, 0.25f, 0.38f,
        0.52f, 0.30f, 0.20f, 0.26f,
    )
