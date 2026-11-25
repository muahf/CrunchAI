package com.crunchai.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crunchai.presentation.R
import com.crunchai.presentation.ui.components.AppBanner
import com.crunchai.presentation.ui.components.AppButton
import com.crunchai.presentation.ui.components.AppTag
import com.crunchai.presentation.ui.components.BannerKind
import com.crunchai.presentation.ui.components.ButtonType
import com.crunchai.presentation.ui.components.TagKind
import com.crunchai.presentation.ui.theme.LocalThemeController
import com.crunchai.presentation.ui.theme.extendedColors
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.model.Species
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.crunchai.presentation.common.formatTime
import com.crunchai.presentation.ui.components.AppBottomBar
import com.crunchai.presentation.ui.components.ContentCard
import com.crunchai.presentation.ui.components.ErrorState
import com.crunchai.presentation.ui.components.MainTab
import com.crunchai.presentation.ui.components.MealRow

@Composable
fun HomeRoute(
    onNavigateToDiary: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToFoodAnalysis: () -> Unit,
    onNavigateToPetProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = uiState,
        onRetry = viewModel::onRetry,
        onNavigateToDiary = onNavigateToDiary,
        onNavigateToPairing = onNavigateToPairing,
        onNavigateToFoodAnalysis = onNavigateToFoodAnalysis,
        onNavigateToPetProfile = onNavigateToPetProfile,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToFoodAnalysis: () -> Unit,
    onNavigateToPetProfile: () -> Unit,
    onNavigateToDiary: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val content = state.contentState as? HomeContentState.Content
    val meals = content?.meals.orEmpty().sortedByDescending { it.startedAt }
    val lastAnomaly = content?.anomalies.orEmpty().maxByOrNull { it.startedAt }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AppBottomBar(selected = MainTab.Home,
            onHome = {}, onDiary = onNavigateToDiary, onPet = onNavigateToPetProfile) },
    ) { inner ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp),
        ) {
            // App bar
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Главная",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                LocalThemeController.current?.let { theme ->
                    IconButton(onClick = theme.toggle) {
                        Icon(painterResource(R.drawable.ic_theme), contentDescription = "Сменить тему")
                    }
                }
                IconButton(onClick = { /* оповещения: практика про push */ }) {
                    Icon(painterResource(R.drawable.bell), contentDescription = "Оповещения")
                }
            }
            Spacer(Modifier.height(8.dp)) // в макете у контента отступ сверху 8

            when (val c = state.contentState) {
                HomeContentState.Loading ->
                    CenterBox(Modifier.weight(1f)) {
                        CircularProgressIndicator()
                    }

                is HomeContentState.Error ->
                    ErrorState(
                        message = c.message,
                        onRetry = onRetry,
                        modifier = Modifier.weight(1f),
                    )

                else ->
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.offlineSince?.let { since ->
                            item {
                                AppBanner(
                                    text = "Нет интернета. Показаны данные на ${formatTime(since, zone)}.",
                                    kind = BannerKind.Offline,
                                )
                            }
                        }
                        item {
                            PetCard(
                                state.pet,
                                offline = state.offlineSince != null,
                                onClick = onNavigateToPetProfile,
                            )
                        }

                        if (lastAnomaly != null) {
                            item { AnomalyBanner(lastAnomaly, zone) }
                        }

                        item { StatsRow(meals, zone) }

                        item {
                            Text(
                                "Последние приёмы пищи",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        if (meals.isEmpty()) {
                            item {
                                Text(
                                    "Приёмов пищи пока нет",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(meals.take(3), key = { "m_${it.id}" }) { MealRow(it, zone) }
                        }
                    }
            }
            if (state.contentState !is HomeContentState.Error) {
                Spacer(Modifier.height(12.dp))
                AppButton(
                    text = "Сфотографировать миску",
                    onClick = onNavigateToFoodAnalysis,
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.Tonal,
                    leadingIcon = {
                        Icon(painterResource(R.drawable.camera), contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CenterBox(
    modifier: Modifier,
    content: @Composable () -> Unit,
) = Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }

/** Простая карточка-контейнер: белая, с рамкой, скругление 16. */


@Composable
private fun PetCard(
    pet: Pet?,
    onClick: () -> Unit,
    offline: Boolean,
) {
    ContentCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.paw),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                if (pet == null) {
                    Text("Питомец не добавлен", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = onClick) { Text("Заполнить профиль") }
                } else {
                    Text(pet.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = petSubtitle(pet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (offline) {
                AppTag(
                    text = "Нет связи",
                    kind = TagKind.Warning,
                    icon = painterResource(R.drawable.ic_wifi_off),
                )
            }
        }
    }
}

@Composable
private fun AnomalyBanner(
    anomaly: Anomaly,
    zone: ZoneId,
) {
    val title =
        when (anomaly.type) {
            AnomalyType.COUGH -> "Обнаружен кашель"
            AnomalyType.LONG_PAUSE -> "Долгая пауза в еде"
        }
    val extended = MaterialTheme.extendedColors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(extended.warningContainer, MaterialTheme.shapes.large)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = extended.onWarningContainer,
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = extended.onWarningContainer)
                Text(
                    "в течение ${anomaly.durationSeconds} секунд · ${formatTime(anomaly.startedAt, zone)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.onWarningContainer,
                )
            }
        }
        // «Подробнее»: кнопка 48 dp по центру, как в макете. Действие добавим позже.
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(48.dp)
                    .clickable { /* откроем деталь позже */ }
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Подробнее", style = MaterialTheme.typography.labelLarge, color = extended.onWarningContainer)
        }
    }
}

@Composable
private fun StatsRow(
    meals: List<Meal>,
    zone: ZoneId,
) {
    val today = LocalDate.now(zone)
    val todayCount = meals.count { it.startedAt.atZone(zone).toLocalDate() == today }
    val calm = if (meals.isEmpty()) "—" else "${meals.map { it.calmIndex }.average().toInt()} из 10"

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Индекс спокойствия", calm, Modifier.weight(1f))
        StatCard("Приёмов сегодня", todayCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier,
) {
    ContentCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}



private fun petSubtitle(pet: Pet): String {
    val species =
        when (pet.species) {
            Species.DOG -> "Собака"
            Species.CAT -> "Кошка"
            else -> "Питомец"
        }
    val parts =
        buildList {
            add(species)
            pet.ageMonths?.let { add(ageText(it)) }
            pet.weightKg?.let { add("$it кг".replace('.', ',')) }
        }
    return parts.joinToString(" · ")
}

private fun ageText(months: Int): String {
    if (months < 12) return "$months мес."
    val y = months / 12
    val word =
        when {
            y % 100 in 11..14 -> "лет"
            y % 10 == 1 -> "год"
            y % 10 in 2..4 -> "года"
            else -> "лет"
        }
    return "$y $word"
}