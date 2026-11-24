package com.crunchai.presentation.home

import com.crunchai.presentation.MainDispatcherRule
import com.example.crunchai.domain.common.DomainError
import com.example.crunchai.domain.common.DomainResult
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.model.Species
import com.example.crunchai.domain.repository.DiaryRepository
import com.example.crunchai.domain.repository.PetRepository
import com.example.crunchai.domain.usecase.GetDiaryUseCase
import com.example.crunchai.domain.usecase.ObservePetUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

// ---------- Фейки: настоящих репозиториев (data-слоя) тут нет ----------

private class FakeDiaryRepository : DiaryRepository {
    var mealsResult: DomainResult<List<Meal>> = DomainResult.Success(emptyList())
    var anomaliesResult: DomainResult<List<Anomaly>> = DomainResult.Success(emptyList())

    /** Если задан, getMeals «зависает», пока его не завершат (так проверяем Loading). */
    var gate: CompletableDeferred<Unit>? = null
    val requestedPeriods = mutableListOf<DiaryPeriod>()

    override suspend fun getMeals(period: DiaryPeriod): DomainResult<List<Meal>> {
        requestedPeriods += period
        gate?.await()
        return mealsResult
    }

    override suspend fun getAnomalies(period: DiaryPeriod): DomainResult<List<Anomaly>> = anomaliesResult
}

private class FakePetRepository : PetRepository {
    val pet = MutableStateFlow<Pet?>(null)

    override fun observePet(): Flow<Pet?> = pet

    override suspend fun savePet(pet: Pet): DomainResult<Unit> = DomainResult.Success(Unit)
}

// ---------- Тестовые данные ----------

private val now: Instant = Instant.parse("2026-11-24T09:00:00Z")

private fun meal(id: String, calm: Int = 9) =
    Meal(id = id, startedAt = now, durationSeconds = 240, calmIndex = calm)

private fun cough() =
    Anomaly(id = "a1", type = AnomalyType.COUGH, startedAt = now, durationSeconds = 6)

private val rex =
    Pet(id = "1", name = "Рекс", species = Species.DOG, ageMonths = 36, weightKg = 18.4, bodyScore = 5)

class HomeViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val diary = FakeDiaryRepository()
    private val pets = FakePetRepository()

    private fun createViewModel() =
        HomeViewModel(
            observePetUseCase = ObservePetUseCase(pets),
            getDiaryUseCase = GetDiaryUseCase(diary),
        )

    @Test
    fun `данные загружены - состояние Content со списками`() {
        diary.mealsResult = DomainResult.Success(listOf(meal("m1")))
        diary.anomaliesResult = DomainResult.Success(listOf(cough()))

        val state = createViewModel().uiState.value

        assertEquals(
            HomeContentState.Content(meals = listOf(meal("m1")), anomalies = listOf(cough())),
            state.contentState,
        )
    }

    @Test
    fun `пока данные грузятся - состояние Loading, потом Content`() {
        diary.mealsResult = DomainResult.Success(listOf(meal("m1")))
        val gate = CompletableDeferred<Unit>()
        diary.gate = gate

        val vm = createViewModel()
        assertEquals(HomeContentState.Loading, vm.uiState.value.contentState)

        gate.complete(Unit)
        assertTrue(vm.uiState.value.contentState is HomeContentState.Content)
    }

    @Test
    fun `нет ни приёмов пищи, ни аномалий - состояние Empty`() {
        val vm = createViewModel()

        assertEquals(HomeContentState.Empty, vm.uiState.value.contentState)
    }

    @Test
    fun `ошибка репозитория - состояние Error с понятным текстом`() {
        diary.mealsResult = DomainResult.Failure(DomainError.Offline)

        val state = createViewModel().uiState.value.contentState

        assertEquals(
            HomeContentState.Error("Нет подключения к интернету и локальный кэш пуст"),
            state,
        )
    }

    @Test
    fun `ошибки домена переводятся в сообщения для пользователя`() {
        val expected =
            mapOf<DomainError, String>(
                DomainError.Unauthorized to "Сессия истекла. Войдите заново",
                DomainError.RateLimited to "Слишком много запросов. Попробуйте позже",
                DomainError.AiUnavailable to "ИИ-сервис временно недоступен",
                DomainError.Server(null) to "Ошибка сервера",
                DomainError.Server("boom") to "boom",
            )

        expected.forEach { (error, text) ->
            diary.mealsResult = DomainResult.Failure(error)
            val state = createViewModel().uiState.value.contentState
            assertEquals("для $error", HomeContentState.Error(text), state)
        }
    }

    @Test
    fun `повторить после ошибки - данные загружаются заново`() {
        diary.mealsResult = DomainResult.Failure(DomainError.Offline)
        val vm = createViewModel()
        assertTrue(vm.uiState.value.contentState is HomeContentState.Error)

        diary.mealsResult = DomainResult.Success(listOf(meal("m1")))
        vm.onRetry()

        assertTrue(vm.uiState.value.contentState is HomeContentState.Content)
    }

    @Test
    fun `смена периода - запрос с новым периодом`() {
        val vm = createViewModel()
        assertEquals(listOf(DiaryPeriod.WEEK), diary.requestedPeriods)

        vm.onPeriodSelected(DiaryPeriod.MONTH)

        assertEquals(DiaryPeriod.MONTH, vm.uiState.value.selectedPeriod)
        assertEquals(listOf(DiaryPeriod.WEEK, DiaryPeriod.MONTH), diary.requestedPeriods)
    }

    @Test
    fun `выбор уже выбранного периода не делает лишний запрос`() {
        val vm = createViewModel()

        vm.onPeriodSelected(DiaryPeriod.WEEK)

        assertEquals(listOf(DiaryPeriod.WEEK), diary.requestedPeriods)
    }

    @Test
    fun `профиль питомца попадает в состояние и обновляется`() {
        val vm = createViewModel()
        assertNull(vm.uiState.value.pet)

        pets.pet.value = rex
        assertEquals(rex, vm.uiState.value.pet)

        pets.pet.value = null
        assertNull(vm.uiState.value.pet)
    }
}