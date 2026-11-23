package com.crunchai.presentation.home

import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.Pet
import java.time.Instant

data class HomeUiState(
    val selectedPeriod: DiaryPeriod = DiaryPeriod.WEEK,
    val pet: Pet? = null,
    val contentState: HomeContentState = HomeContentState.Loading,
    val offlineSince: Instant? = null,
)

sealed interface HomeContentState {
    data object Loading : HomeContentState

    data object Empty : HomeContentState

    data class Content(
        val meals: List<Meal>,
        val anomalies: List<Anomaly>,
    ) : HomeContentState

    data class Error(
        val message: String,
    ) : HomeContentState
}
