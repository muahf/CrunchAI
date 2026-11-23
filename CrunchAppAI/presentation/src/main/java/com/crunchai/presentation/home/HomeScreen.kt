package com.crunchai.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crunchai.presentation.R
import com.example.crunchai.domain.model.Anomaly
import com.example.crunchai.domain.model.AnomalyType
import com.example.crunchai.domain.model.Meal
import com.example.crunchai.domain.model.Pet
import com.example.crunchai.domain.model.Species
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Цвета из Figma. Если такие есть в теме (практика 13), возьми оттуда.
private val SuccessContainer = Color(0xFFCDEFD2)
private val OnSuccessContainer = Color(0xFF0B3A15)
private val WarningContainer = Color(0xFFFFE8B8)
private val OnWarningContainer = Color(0xFF3A2600)

@Composable
fun HomeRoute(
    onNavigateToPairing: () -> Unit,
    onNavigateToFoodAnalysis: () -> Unit,
    onNavigateToPetProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = uiState,
        onRetry = viewModel::onRetry,
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
) {
    val zone = ZoneId.systemDefault()
    val content = state.contentState as? HomeContentState.Content
    val meals = content?.meals.orEmpty().sortedByDescending { it.startedAt }
    val lastAnomaly = content?.anomalies.orEmpty().maxByOrNull { it.startedAt }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { HomeBottomBar(onPet = onNavigateToPetProfile) },
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { /* оповещения: практика про push */ }) {
                    Icon(painterResource(R.drawable.bell), contentDescription = "Оповещения")
                }
            }

            when (val c = state.contentState) {
                HomeContentState.Loading ->
                    CenterBox(Modifier.weight(1f)) {
                        CircularProgressIndicator()
                    }

                is HomeContentState.Error ->
                    ErrorBlock(
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
                            item { OfflineBanner(since, zone) }
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
                            items(meals.take(3), key = { "m_${it.id}" }) { MealItem(it, zone) }
                        }
                    }
            }
            if (state.contentState !is HomeContentState.Error) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToFoodAnalysis,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                ) {
                    Icon(painterResource(R.drawable.camera), contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Сфотографировать миску", fontWeight = FontWeight.SemiBold)
                }
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

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { content() }
}

@Composable
private fun PetCard(
    pet: Pet?,
    onClick: () -> Unit,
    offline: Boolean,
) {
    AppCard(Modifier.fillMaxWidth()) {
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
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                if (offline) Tag("Нет связи", WarningContainer, OnWarningContainer)
                if (pet == null) {
                    Text("Питомец не добавлен", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = onClick) { Text("Заполнить профиль") }
                } else {
                    Text(pet.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = petSubtitle(pet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnomalyBanner(
    anomaly: Anomaly,
    zone: ZoneId,
) {
    val time = DateTimeFormatter.ofPattern("HH:mm").format(anomaly.startedAt.atZone(zone))
    val title =
        when (anomaly.type) {
            AnomalyType.COUGH -> "Обнаружен кашель"
            AnomalyType.LONG_PAUSE -> "Долгая пауза в еде"
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WarningContainer,
        contentColor = OnWarningContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    "в течение ${anomaly.durationSeconds} секунд · $time",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { /* откроем деталь позже */ }) {
                    Text("Подробнее", color = OnWarningContainer, fontWeight = FontWeight.SemiBold)
                }
            }
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
    AppCard(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MealItem(
    meal: Meal,
    zone: ZoneId,
) {
    val time = DateTimeFormatter.ofPattern("HH:mm").format(meal.startedAt.atZone(zone))
    val minutes = (meal.durationSeconds / 60).coerceAtLeast(1)
    val calm = meal.calmIndex >= 7

    AppCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.bowl),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("$time · $minutes мин", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Приём пищи",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (calm) {
                Tag("Спокойно", SuccessContainer, OnSuccessContainer)
            } else {
                Tag("Беспокойно", WarningContainer, OnWarningContainer)
            }
        }
    }
}

@Composable
private fun Tag(
    text: String,
    container: Color,
    content: Color,
) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeBottomBar(onPet: () -> Unit) {
    val colors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(painterResource(R.drawable.home), null) },
                label = { Text("Главная") },
                colors = colors,
            )
            NavigationBarItem(
                selected = false,
                onClick = { /* экран «Дневник» добавим позже */ },
                icon = { Icon(painterResource(R.drawable.diary), null) },
                label = { Text("Дневник") },
                colors = colors,
            )
            NavigationBarItem(
                selected = false,
                onClick = onPet,
                icon = { Icon(painterResource(R.drawable.paw), null) },
                label = { Text("Питомец") },
                colors = colors,
            )
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

@Composable
private fun OfflineBanner(
    since: Instant,
    zone: ZoneId,
) {
    val time = DateTimeFormatter.ofPattern("HH:mm").format(since.atZone(zone))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WarningContainer,
        contentColor = OnWarningContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_wifi_off), contentDescription = null)
            Text(
                "Нет интернета. Показаны данные на $time.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ErrorBlock(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Не удалось загрузить данные",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Повторить", fontWeight = FontWeight.SemiBold)
        }
    }
}
