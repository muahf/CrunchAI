package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.fakes.FakeAlertRepository
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ObserveAlertsUseCaseTest {
    @Test
    fun `передаёт поток уведомлений об аномалиях из репозитория`() = runTest {
        val alert = Anomaly("a1", AnomalyType.COUGH, Instant.now(), 20)
        val observe = ObserveAlertsUseCase(FakeAlertRepository(listOf(alert)))

        assertEquals(listOf(alert), observe().toList())
    }
}