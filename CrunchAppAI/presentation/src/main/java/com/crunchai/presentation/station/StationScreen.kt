package com.crunchai.presentation.station

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crunchai.presentation.R
import com.crunchai.presentation.common.formatTime
import com.crunchai.presentation.ui.components.AppBanner
import com.crunchai.presentation.ui.components.AppButton
import com.crunchai.presentation.ui.components.AppTag
import com.crunchai.presentation.ui.components.BannerKind
import com.crunchai.presentation.ui.components.ButtonType
import com.crunchai.presentation.ui.components.ContentCard
import com.crunchai.presentation.ui.components.TagKind
import com.crunchai.presentation.ui.theme.LocalThemeController
import com.crunchai.presentation.ui.theme.Space
import java.time.ZoneId
import java.util.Locale

@Composable
fun StationRoute(
    onNavigateToPairing: () -> Unit,
    viewModel: StationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showPermissionDialog by remember { mutableStateOf(!hasMicPermission) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasMicPermission = granted
            showPermissionDialog = false
        }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog && !hasMicPermission) {
        MicPermissionDialog(
            onAllow = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onDeny = { showPermissionDialog = false },
        )
    }

    StationScreen(
        state = state,
        hasMicPermission = hasMicPermission,
        onToggleListening = {
            if (!hasMicPermission) {
                showPermissionDialog = true
            } else {
                viewModel.toggleListening()
            }
        },
        onRetrySync = viewModel::onRetrySync,
        onInfoClick = onNavigateToPairing,
    )
}

@Composable
fun StationScreen(
    state: StationUiState,
    hasMicPermission: Boolean,
    onToggleListening: () -> Unit,
    onRetrySync: () -> Unit,
    onInfoClick: () -> Unit,
) {
    val zone = ZoneId.systemDefault()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = Space.s4),
        ) {
            // Top bar: Станция + переключатель темы + кнопка (i)
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Станция",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                LocalThemeController.current?.let { theme ->
                    IconButton(onClick = theme.toggle) {
                        Icon(painterResource(R.drawable.ic_theme), contentDescription = "Сменить тему")
                    }
                }
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Outlined.Info, contentDescription = "Сопряжение и информация")
                }
            }

            Spacer(Modifier.height(Space.s2))

            if (state.isModelLoading) {
                // Состояние 2: Загрузка нейросети YAMNet
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = "Запускаем нейросеть...",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Загружаем модель распознавания звуков.\nОбычно это 2–3 секунды.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Space.s3),
                ) {
                    // Состояние 4: Ошибка отправки событий на сервер
                    state.syncError?.let {
                        item {
                            AppBanner(
                                text = "Не удалось отправить события на сервер. Ждут отправки: ${state.pendingEventsCount}.",
                                kind = BannerKind.Error,
                                actionText = "Повторить",
                                onAction = onRetrySync,
                            )
                        }
                    }

                    // Состояние 5: Нет интернета (офлайн-очередь)
                    if (state.isOffline) {
                        item {
                            AppBanner(
                                text =
                                    "Нет интернета. События сохраняются на телефоне (${state.pendingEventsCount}) и отправятся позже.",
                                kind = BannerKind.Offline,
                            )
                        }
                    }

                    // Карточка «Слушаю · От зарядки · 87%» + тег YAMNet + эквалайзер
                    item {
                        ListeningStatusCard(state = state, hasMicPermission = hasMicPermission)
                    }

                    // Карточка «Последние события · сегодня»
                    item {
                        RecentEventsCard(events = state.events, zone = zone)
                    }
                }

                Spacer(Modifier.height(Space.s3))

                AppButton(
                    text = if (state.isListening && hasMicPermission) "Остановить прослушивание" else "Начать прослушивание",
                    onClick = onToggleListening,
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.Outline,
                    leadingIcon = {
                        Box(
                            modifier =
                                Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    },
                )
                Spacer(Modifier.height(Space.s3))
            }
        }
    }
}

@Composable
private fun ListeningStatusCard(
    state: StationUiState,
    hasMicPermission: Boolean,
) {
    val active = state.isListening && hasMicPermission
    val statusTitle =
        when {
            !hasMicPermission -> "Нет доступа к микрофону"
            active -> "Слушаю"
            else -> "Пауза"
        }
    val powerText = if (state.isCharging) "От зарядки · ${state.batteryPercent}%" else "От батареи · ${state.batteryPercent}%"

    ContentCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(text = statusTitle, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = powerText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppTag(text = "YAMNet", kind = TagKind.Info, showDefaultIcon = false)
            }

            // Эквалайзер звукового потока
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                state.waveformBars.forEach { level ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height((36f * level).dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(
                                    if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentEventsCard(
    events: List<StationSoundEvent>,
    zone: ZoneId,
) {
    ContentCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Последние события", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "сегодня",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (events.isEmpty()) {
                // Состояние 3: Пустой список
                Text(
                    text = "Пока тихо. Звуки еды появятся здесь.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                events.forEachIndexed { index, event ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    StationEventRow(event = event, zone = zone)
                }
            }
        }
    }
}

@Composable
private fun StationEventRow(
    event: StationSoundEvent,
    zone: ZoneId,
) {
    val label =
        when (event.kind) {
            StationSoundKind.COUGH ->
                event.durationSeconds?.let { "Кашель ($it с)" } ?: "Кашель"
            StationSoundKind.SILENCE -> "Тишина"
            StationSoundKind.CHEW -> "Чавканье"
            StationSoundKind.CRUNCH -> "Хруст"
            StationSoundKind.LAP -> "Лакание"
        }
    val confidenceText = String.format(Locale.US, "%.2f", event.confidence)
    val tagKind = if (event.kind == StationSoundKind.COUGH) TagKind.Warning else TagKind.Ok

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.s1),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatTime(event.timestamp, zone),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        AppTag(text = confidenceText, kind = tagKind, showDefaultIcon = false)
    }
}

@Composable
private fun MicPermissionDialog(
    onAllow: () -> Unit,
    onDeny: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDeny,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        title = {
            Text(
                text = "Разрешить CrunchAI записывать аудио?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Нужно, чтобы распознавать звуки еды. Звук обрабатывается на телефоне и не уходит в интернет.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                AppButton(
                    text = "При использовании приложения",
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.Tonal,
                )
                AppButton(
                    text = "Только в этот раз",
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.Outline,
                )
                AppButton(
                    text = "Не разрешать",
                    onClick = onDeny,
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.Text,
                )
            }
        },
    )
}
