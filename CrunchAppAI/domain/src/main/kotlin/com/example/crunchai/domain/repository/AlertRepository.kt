package com.example.crunchai.domain.repository

import com.example.crunchai.domain.model.Anomaly
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    /** Поток уведомлений об аномалиях (на практике приходит через push FCM). */
    fun observeAlerts(): Flow<Anomaly>
}