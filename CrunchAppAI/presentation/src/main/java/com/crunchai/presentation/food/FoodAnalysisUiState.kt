package com.crunchai.presentation.food

import com.crunchai.presentation.navigation.FoodAnalysisMode
import com.example.crunchai.domain.model.FoodAnalysis
import java.time.Instant

data class PhotoSlot(
    val bytes: ByteArray,
    val capturedAt: Instant,
)

data class FoodAnalysisUiState(
    val mode: FoodAnalysisMode = FoodAnalysisMode.BOWL,
    val bowlBeforePhoto: PhotoSlot? = null,
    val bowlAfterPhoto: PhotoSlot? = null,
    val feedLabelPhoto: PhotoSlot? = null,
    val isAnalyzing: Boolean = false,
    val isOfflinePending: Boolean = false,
    val serverError: String? = null,
    val serverErrorDetails: String? = null,
    val showErrorDetails: Boolean = false,
    val bowlResult: FoodAnalysis? = null,
    val feedResult: FoodAnalysis? = null,
) {
    val hasAnyPhotoForCurrentMode: Boolean
        get() =
            when (mode) {
                FoodAnalysisMode.BOWL -> bowlBeforePhoto != null || bowlAfterPhoto != null
                FoodAnalysisMode.LABEL -> feedLabelPhoto != null
            }

    val currentResult: FoodAnalysis?
        get() =
            when (mode) {
                FoodAnalysisMode.BOWL -> bowlResult
                FoodAnalysisMode.LABEL -> feedResult
            }
}
