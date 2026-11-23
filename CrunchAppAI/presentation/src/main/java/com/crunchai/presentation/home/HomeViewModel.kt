package com.crunchai.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.usecase.GetDiaryUseCase
import com.example.crunchai.domain.usecase.ObservePetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val observePetUseCase: ObservePetUseCase,
        private val getDiaryUseCase: GetDiaryUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            observePet()
            loadDiary(_uiState.value.selectedPeriod)
        }

        fun onPeriodSelected(period: DiaryPeriod) {
            if (_uiState.value.selectedPeriod == period) return
            _uiState.update { it.copy(selectedPeriod = period) }
            loadDiary(period)
        }

        fun onRetry() {
            loadDiary(_uiState.value.selectedPeriod)
        }

        private fun observePet() {
            viewModelScope.launch {
                observePetUseCase().collect { pet ->
                    _uiState.update { it.copy(pet = pet) }
                }
            }
        }

        private var diaryJob: Job? = null

        private fun loadDiary(period: DiaryPeriod) {
            diaryJob?.cancel()
            diaryJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(contentState = HomeContentState.Loading) }
                    val newState =
                        when (val result = getDiaryUseCase(period)) {
                            is DomainResult.Failure ->
                                HomeContentState.Error(result.error.toUserMessage())

                            is DomainResult.Success -> {
                                val diary = result.value
                                if (diary.meals.isEmpty() && diary.anomalies.isEmpty()) {
                                    HomeContentState.Empty
                                } else {
                                    HomeContentState.Content(
                                        meals = diary.meals,
                                        anomalies = diary.anomalies,
                                    )
                                }
                            }
                        }

                    _uiState.update { it.copy(contentState = newState) }
                }
        }

        private fun DomainError.toUserMessage(): String =
            when (this) {
                is DomainError.Validation -> "$field: $reason"
                DomainError.Unauthorized -> "Сессия истекла. Войдите заново"
                DomainError.Forbidden -> "Недостаточно прав для этого действия"
                DomainError.NotFound -> "Данные не найдены"
                DomainError.Conflict -> "Конфликт данных"
                DomainError.PairingKeyExpired -> "Срок действия ключа сопряжения истёк"
                DomainError.TooLarge -> "Слишком большой объём данных"
                DomainError.RateLimited -> "Слишком много запросов. Попробуйте позже"
                DomainError.AiUnavailable -> "ИИ-сервис временно недоступен"
                DomainError.Offline -> "Нет подключения к интернету и локальный кэш пуст"
                is DomainError.Server -> message ?: "Ошибка сервера"
                is DomainError.Unknown -> cause?.localizedMessage ?: "Неизвестная ошибка"
            }
    }
