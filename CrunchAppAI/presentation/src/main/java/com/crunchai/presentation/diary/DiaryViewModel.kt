package com.crunchai.presentation.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crunchai.presentation.common.toUserMessage
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.usecase.GetDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel
    @Inject
    constructor(
        private val getDiary: GetDiaryUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow(DiaryUiState())
        val state: StateFlow<DiaryUiState> = _state.asStateFlow()

        init {
            load(DiaryPeriod.WEEK)
        }

        fun selectPeriod(period: DiaryPeriod) {
            if (_state.value.period == period && !_state.value.isLoading) return
            load(period)
        }

        fun retry() {
            load(_state.value.period)
        }

        fun toggleErrorDetails() {
            _state.update { it.copy(showErrorDetails = !it.showErrorDetails) }
        }

        private fun load(period: DiaryPeriod) {
            _state.update {
                it.copy(
                    period = period,
                    isLoading = true,
                    errorMessage = null,
                    errorDetails = null,
                    showErrorDetails = false,
                )
            }

            viewModelScope.launch {
                when (val result = getDiary(period)) {
                    is DomainResult.Success -> {
                        val diary = result.value
                        _state.update {
                            it.copy(
                                period = diary.period,
                                isLoading = false,
                                isOfflineBannerVisible = false,
                                errorMessage = null,
                                errorDetails = null,
                                meals = diary.meals,
                                anomalies = diary.anomalies,
                                days = diary.days,
                                averageCalmIndex = diary.averageCalmIndex,
                            )
                        }
                    }
                    is DomainResult.Failure -> {
                        when (val err = result.error) {
                            is DomainError.Offline -> {
                                val hasCached = _state.value.meals.isNotEmpty() || _state.value.anomalies.isNotEmpty()
                                if (hasCached) {
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            isOfflineBannerVisible = true,
                                            errorMessage = null,
                                        )
                                    }
                                } else {
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            isOfflineBannerVisible = true,
                                            errorMessage = "Нет интернета. Для загрузки дневника нужен доступ к сети.",
                                            errorDetails = err.toUserMessage(),
                                        )
                                    }
                                }
                            }
                            else -> {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        isOfflineBannerVisible = false,
                                        errorMessage = "Сервер временно недоступен. Попробуйте ещё раз через минуту.",
                                        errorDetails = err.toUserMessage(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
