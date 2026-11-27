package com.crunchai.presentation.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crunchai.presentation.common.toUserMessage
import com.crunchai.presentation.navigation.FoodAnalysisMode
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.usecase.AnalyzeBowlPhotoUseCase
import com.example.crunchai.domain.usecase.AnalyzeFeedLabelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class FoodAnalysisViewModel
    @Inject
    constructor(
        private val analyzeBowlPhoto: AnalyzeBowlPhotoUseCase,
        private val analyzeFeedLabel: AnalyzeFeedLabelUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow(FoodAnalysisUiState())
        val state: StateFlow<FoodAnalysisUiState> = _state.asStateFlow()

        private var pendingMode: FoodAnalysisMode? = null
        private var pendingBytes: ByteArray? = null

        fun setInitialMode(mode: FoodAnalysisMode) {
            _state.update { it.copy(mode = mode) }
        }

        fun onModeChange(mode: FoodAnalysisMode) {
            _state.update {
                it.copy(
                    mode = mode,
                    serverError = null,
                    serverErrorDetails = null,
                    showErrorDetails = false,
                    isOfflinePending = false,
                )
            }
        }

        fun onPhotoCaptured(
            targetMode: FoodAnalysisMode,
            bytes: ByteArray,
        ) {
            if (bytes.isEmpty()) return
            val now = Instant.now()
            pendingMode = targetMode
            pendingBytes = bytes

            _state.update { s ->
                when (targetMode) {
                    FoodAnalysisMode.BOWL -> {
                        if (s.bowlBeforePhoto == null) {
                            s.copy(
                                mode = FoodAnalysisMode.BOWL,
                                bowlBeforePhoto = PhotoSlot(bytes, now),
                                serverError = null,
                                isOfflinePending = false,
                            )
                        } else {
                            s.copy(
                                mode = FoodAnalysisMode.BOWL,
                                bowlAfterPhoto = PhotoSlot(bytes, now),
                                serverError = null,
                                isOfflinePending = false,
                            )
                        }
                    }
                    FoodAnalysisMode.LABEL -> {
                        s.copy(
                            mode = FoodAnalysisMode.LABEL,
                            feedLabelPhoto = PhotoSlot(bytes, now),
                            serverError = null,
                            isOfflinePending = false,
                        )
                    }
                }
            }

            val current = _state.value
            if (targetMode == FoodAnalysisMode.LABEL || current.bowlAfterPhoto != null) {
                runAnalysis(targetMode, bytes)
            }
        }

        fun retryLastAnalysis() {
            val mode = pendingMode ?: return
            val bytes = pendingBytes ?: return
            runAnalysis(mode, bytes)
        }

        fun toggleErrorDetails() {
            _state.update { it.copy(showErrorDetails = !it.showErrorDetails) }
        }

        private fun runAnalysis(
            mode: FoodAnalysisMode,
            bytes: ByteArray,
        ) {
            if (_state.value.isAnalyzing) return
            _state.update {
                it.copy(
                    isAnalyzing = true,
                    isOfflinePending = false,
                    serverError = null,
                    serverErrorDetails = null,
                    showErrorDetails = false,
                )
            }

            viewModelScope.launch {
                val result =
                    if (mode == FoodAnalysisMode.BOWL) {
                        analyzeBowlPhoto(bytes)
                    } else {
                        analyzeFeedLabel(bytes)
                    }

                when (result) {
                    is DomainResult.Success -> {
                        _state.update { s ->
                            s.copy(
                                isAnalyzing = false,
                                isOfflinePending = false,
                                serverError = null,
                                bowlResult = if (mode == FoodAnalysisMode.BOWL) result.value else s.bowlResult,
                                feedResult = if (mode == FoodAnalysisMode.LABEL) result.value else s.feedResult,
                            )
                        }
                    }
                    is DomainResult.Failure -> {
                        val err = result.error
                        if (err is DomainError.Offline) {
                            _state.update {
                                it.copy(
                                    isAnalyzing = false,
                                    isOfflinePending = true,
                                    serverError = null,
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(
                                    isAnalyzing = false,
                                    isOfflinePending = false,
                                    serverError = "Сервер временно недоступен. Фото сохранено, попробуйте ещё раз.",
                                    serverErrorDetails = err.toUserMessage(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
