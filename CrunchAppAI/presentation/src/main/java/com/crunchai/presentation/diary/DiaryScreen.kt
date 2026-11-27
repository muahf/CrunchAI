package com.crunchai.presentation.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.crunchai.presentation.ui.components.ContentCard
import com.crunchai.presentation.ui.components.TagKind
import com.crunchai.presentation.ui.theme.Space
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.DiaryDay
import com.example.crunchai.domain.model.DiaryPeriod
import com.example.crunchai.domain.model.Meal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryRoute(
    onNavigateToHome: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DiaryScreen(
        state = state,
        onSelectPeriod = viewModel::selectPeriod,
        onRetry = viewModel::retry,
        onToggleErrorDetails = viewModel::toggleErrorDetails,
        onOpenHome = onNavigateToHome,
    )
}

@Composable
fun DiaryScreen(
    state: DiaryUiState,
    onSelectPeriod: (DiaryPeriod) -> Unit,
    onRetry: () -> Unit,
    onToggleErrorDetails: () -> Unit,
    onOpenHome: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = Space.s4),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Дневник", style = MaterialTheme.typography.headlineMedium)
                Icon(
                    painter = painterResource(R.drawable.pdf),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.isOfflineBannerVisible) {
                AppBanner(
                    text = "Нет интернета. Показаны сохранённые данные.",
                    kind = BannerKind.Offline,
                )
                Spacer(Modifier.height(Space.s2))
            }

            when {
                state.isLoading -> DiaryLoadingContent()
                state.errorMessage != null ->
                    DiaryErrorContent(
                        message = state.errorMessage,
                        details = state.errorDetails,
                        showDetails = state.showErrorDetails,
                        onRetry = onRetry,
                        onToggleDetails = onToggleErrorDetails,
                    )
                state.isEmpty -> DiaryEmptyContent(onOpenHome = onOpenHome)
                else ->
                    DiaryDataContent(
                        state = state,
                        onSelectPeriod = onSelectPeriod,
                    )
            }
        }
    }
}

@Composable
private fun DiaryDataContent(
    state: DiaryUiState,
    onSelectPeriod: (DiaryPeriod) -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    val todayMeals =
        remember(state.meals, today, zoneId) {
            state.meals.filter { it.startedAt.atZone(zoneId).toLocalDate() == today }
        }
    val recentMeals =
        remember(state.meals, todayMeals) {
            todayMeals.ifEmpty { state.meals.take(5) }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Space.s5),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        item {
            PeriodSegmentedControl(
                selected = state.period,
                onSelect = onSelectPeriod,
            )
        }

        item {
            MealsChartCard(
                days = state.days,
                period = state.period,
            )
        }

        if (state.anomalies.isNotEmpty()) {
            item {
                AnomaliesCard(
                    anomalies = state.anomalies,
                    period = state.period,
                    zoneId = zoneId,
                )
            }
        }

        if (recentMeals.isNotEmpty()) {
            item {
                Text(
                    text = if (todayMeals.isNotEmpty()) "Сегодня" else "Недавние приёмы пищи",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Space.s1),
                )
            }

            items(recentMeals, key = { it.id }) { meal ->
                MealItemCard(meal = meal, zoneId = zoneId)
            }
        }
    }
}

@Composable
private fun PeriodSegmentedControl(
    selected: DiaryPeriod,
    onSelect: (DiaryPeriod) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(2.dp),
    ) {
        DiaryPeriod.entries.forEach { period ->
            val active = period == selected
            val label = if (period == DiaryPeriod.WEEK) "Неделя" else "Месяц"
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .clickable { onSelect(period) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Composable
private fun MealsChartCard(
    days: List<DiaryDay>,
    period: DiaryPeriod,
) {
    val locale = remember { Locale.forLanguageTag("ru") }
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE", locale) }
    val shortDateFormatter = remember { DateTimeFormatter.ofPattern("d.MM", locale) }
    val visibleDays =
        remember(days, period) {
            if (period == DiaryPeriod.WEEK) days.takeLast(7) else days.takeLast(14)
        }
    val maxCount = (visibleDays.maxOfOrNull { it.mealsCount } ?: 1).coerceAtLeast(1)

    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Text(text = "Приёмы пищи", style = MaterialTheme.typography.titleMedium)

            if (visibleDays.isEmpty()) {
                Text(
                    text = "Нет данных для графика за выбранный период.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(Space.s2),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    visibleDays.forEach { day ->
                        val ratio = (day.mealsCount.toFloat() / maxCount).coerceIn(0.08f, 1f)
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(0.75f)
                                        .fillMaxHeight(ratio)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (day.mealsCount > 0) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        ),
                            )
                            Spacer(Modifier.height(4.dp))
                            val label =
                                if (period == DiaryPeriod.WEEK) {
                                    day.date.format(dayFormatter).lowercase(locale)
                                } else {
                                    day.date.format(shortDateFormatter)
                                }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnomaliesCard(
    anomalies: List<Anomaly>,
    period: DiaryPeriod,
    zoneId: ZoneId,
) {
    val locale = remember { Locale.forLanguageTag("ru") }
    val dayShort = remember { DateTimeFormatter.ofPattern("EEE", locale) }
    val title = if (period == DiaryPeriod.WEEK) "Аномалии за неделю" else "Аномалии за месяц"

    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                AppTag(
                    text = anomalies.size.toString(),
                    kind = TagKind.Warning,
                    showDefaultIcon = false,
                )
            }

            anomalies.take(4).forEach { anomaly ->
                val dayText = anomaly.startedAt.atZone(zoneId).toLocalDate().format(dayShort).lowercase(locale)
                val typeText =
                    when (anomaly.type) {
                        AnomalyType.COUGH -> "кашель"
                        AnomalyType.LONG_PAUSE -> "долго не подходил к миске"
                        else -> "необычный звук"
                    }
                Text(
                    text = "$dayText · $typeText, ${anomaly.durationSeconds} с",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MealItemCard(
    meal: Meal,
    zoneId: ZoneId,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeText =
        remember(meal.startedAt, zoneId) {
            meal.startedAt.atZone(zoneId).toLocalTime().format(timeFormatter)
        }
    val minutes = ((meal.durationSeconds + 59) / 60).coerceAtLeast(1)
    val calmLabel = if (meal.calmIndex >= 70) "Спокойно" else "Быстро"
    val calmKind = if (meal.calmIndex >= 70) TagKind.Info else TagKind.Warning

    ContentCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Space.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.bowl),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$timeText · $minutes мин",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Приём пищи",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppTag(
                text = calmLabel,
                kind = calmKind,
                showDefaultIcon = false,
            )
        }
    }
}

@Composable
private fun DiaryLoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = Space.s2),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        repeat(2) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun DiaryEmptyContent(onOpenHome: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.pdf),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text("Записей пока нет", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Когда питомец поест, здесь появятся\nприёмы пищи, графики и аномалии.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            AppButton(
                text = "Открыть главную",
                onClick = onOpenHome,
                type = ButtonType.Tonal,
            )
        }
    }
}

@Composable
private fun DiaryErrorContent(
    message: String,
    details: String?,
    showDetails: Boolean,
    onRetry: () -> Unit,
    onToggleDetails: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text("Не удалось загрузить дневник", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (showDetails && details != null) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            AppButton(
                text = "Повторить",
                onClick = onRetry,
                leadingIcon = {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            AppButton(
                text = "Подробности",
                onClick = onToggleDetails,
                type = ButtonType.Text,
            )
        }
    }
}
