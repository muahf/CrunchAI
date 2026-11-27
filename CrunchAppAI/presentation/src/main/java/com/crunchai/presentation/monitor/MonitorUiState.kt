package com.crunchai.presentation.monitor

enum class MonitorConnectionState {
    CONNECTING,
    LIVE,
    STATION_OFFLINE,
    NO_INTERNET,
    SERVER_ERROR,
    STOPPED,
}

data class MonitorUiState(
    val petName: String = "Рекс",
    val isStationOnline: Boolean = true,
    val connectionState: MonitorConnectionState = MonitorConnectionState.CONNECTING,
    val elapsedSeconds: Long = 0L,
    val latencySeconds: Float = 1.2f,
    val isMuted: Boolean = false,
    val waveformBars: List<Float> = defaultMonitorWaveform(),
    val errorMessage: String? = null,
    val errorDetails: String? = null,
    val showErrorDetails: Boolean = false,
)

private fun defaultMonitorWaveform(): List<Float> =
    listOf(
        0.22f, 0.38f, 0.65f, 0.45f, 0.80f, 0.55f, 0.30f, 0.72f,
        0.90f, 0.48f, 0.35f, 0.68f, 0.84f, 0.52f, 0.28f, 0.60f,
        0.76f, 0.42f, 0.25f, 0.58f, 0.70f, 0.40f, 0.32f, 0.20f,
    )
