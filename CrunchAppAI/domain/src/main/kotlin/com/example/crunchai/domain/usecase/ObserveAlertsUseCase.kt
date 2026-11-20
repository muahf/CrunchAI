package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow

class ObserveAlertsUseCase(
    private val repository: AlertRepository,
) {
    operator fun invoke(): Flow<Anomaly> = repository.observeAlerts()
}