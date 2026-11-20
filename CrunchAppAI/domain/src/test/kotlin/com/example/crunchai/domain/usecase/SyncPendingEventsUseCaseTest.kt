package com.example.crunchai.domain.usecase

import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.fakes.FakeEventSyncRepository
import com.example.crunchai.domain.fakes.FakeSoundEventRepository
import com.example.crunchai.domain.model.SoundEvent
import com.example.crunchai.domain.model.SoundType
import com.example.crunchai.domain.model.SyncReport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SyncPendingEventsUseCaseTest {
    @Test
    fun `если неотправленных звуков нет, возвращается accepted 0 без запроса на сервер`() = runTest {
        val localRepo = FakeSoundEventRepository()
        val syncRepo = FakeEventSyncRepository()
        val sync = SyncPendingEventsUseCase(localRepo, syncRepo)

        val result = sync()

        assertEquals(DomainResult.Success(SyncReport(accepted = 0)), result)
        assertTrue(syncRepo.lastSentBatch.isEmpty())
    }

    @Test
    fun `отправляет локальные звуки и помечает их как SENT`() = runTest {
        val localRepo = FakeSoundEventRepository()
        localRepo.save(SoundEvent("1", SoundType.CHEW, 0.9f, Instant.now(), 1))
        localRepo.save(SoundEvent("2", SoundType.LAP, 0.8f, Instant.now(), 1))

        val syncRepo = FakeEventSyncRepository(DomainResult.Success(SyncReport(accepted = 2)))
        val sync = SyncPendingEventsUseCase(localRepo, syncRepo)

        val result = sync()

        assertTrue(result is DomainResult.Success)
        assertEquals(listOf("1", "2"), localRepo.sentIds)
        assertTrue(localRepo.getUnsent(10).isEmpty())
    }

    @Test
    fun `при ошибке сети звуки остаются неотправленными`() = runTest {
        val localRepo = FakeSoundEventRepository()
        localRepo.save(SoundEvent("1", SoundType.CHEW, 0.9f, Instant.now(), 1))

        val syncRepo = FakeEventSyncRepository(DomainResult.Failure(DomainError.Offline))
        val sync = SyncPendingEventsUseCase(localRepo, syncRepo)

        val result = sync()

        assertEquals(DomainResult.Failure(DomainError.Offline), result)
        assertTrue(localRepo.sentIds.isEmpty())
    }
}