package com.crunchai.presentation.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.crunchai.presentation.ui.theme.CrunchAiTheme
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.model.Species
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Размер как в макете Figma (360×800), сразу светлая и тёмная тема. */
@Preview(name = "Светлая", widthDp = 360, heightDp = 800)
@Preview(
    name = "Тёмная",
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private annotation class HomePreviews

// Тестовые данные для превью. Если у тебя id не String (например, Long), поправь тут.
private val previewPet =
    Pet(id = "1", name = "Рекс", species = Species.DOG, ageMonths = 36, weightKg = 18.4, bodyScore = 5)

private fun minutesAgo(m: Long): Instant = Instant.now().minus(m, ChronoUnit.MINUTES)

private val previewMeals =
    listOf(
        Meal(id = "m1", startedAt = minutesAgo(30), durationSeconds = 240, calmIndex = 9),
        Meal(id = "m2", startedAt = minutesAgo(300), durationSeconds = 360, calmIndex = 5),
        Meal(id = "m3", startedAt = minutesAgo(600), durationSeconds = 300, calmIndex = 10),
    )

private val previewAnomalies =
    listOf(
        Anomaly(id = "a1", type = AnomalyType.COUGH, startedAt = minutesAgo(35), durationSeconds = 6),
    )

private val dataState =
    HomeUiState(
        pet = previewPet,
        contentState = HomeContentState.Content(previewMeals, previewAnomalies),
    )

@Composable
private fun PreviewHome(state: HomeUiState) =
    CrunchAiTheme {
        HomeScreen(
            state = state,
            onRetry = {},
            onNavigateToDiary = {},
            onNavigateToPairing = {},
            onNavigateToFoodAnalysis = {},
            onNavigateToPetProfile = {},
        )
    }

@HomePreviews
@Composable
private fun HomeDataPreview() = PreviewHome(dataState)

@HomePreviews
@Composable
private fun HomeOfflinePreview() = PreviewHome(dataState.copy(offlineSince = minutesAgo(35)))

@HomePreviews
@Composable
private fun HomeErrorPreview() =
    PreviewHome(
        HomeUiState(
            contentState = HomeContentState.Error("Сервер временно недоступен. Попробуйте ещё раз через минуту."),
        ),
    )

@HomePreviews
@Composable
private fun HomeLoadingPreview() = PreviewHome(HomeUiState(pet = previewPet))

@HomePreviews
@Composable
private fun HomeEmptyPreview() = PreviewHome(HomeUiState(pet = previewPet, contentState = HomeContentState.Empty))

@HomePreviews
@Composable
private fun HomeNoPetPreview() =
    PreviewHome(
        HomeUiState(contentState = HomeContentState.Content(emptyList(), emptyList())),
    )
