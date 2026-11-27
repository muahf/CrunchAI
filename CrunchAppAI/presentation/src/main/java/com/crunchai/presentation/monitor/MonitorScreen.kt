package com.crunchai.presentation.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.crunchai.presentation.ui.components.AppBanner
import com.crunchai.presentation.ui.components.AppButton
import com.crunchai.presentation.ui.components.AppTag
import com.crunchai.presentation.ui.components.BannerKind
import com.crunchai.presentation.ui.components.ButtonType
import com.crunchai.presentation.ui.components.ContentCard
import com.crunchai.presentation.ui.components.TagKind
import com.crunchai.presentation.ui.theme.Space
import java.util.Locale

@Composable
fun MonitorRoute(
    onBack: () -> Unit,
    viewModel: MonitorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNotifDialog by remember { mutableStateOf(false) }
    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            showNotifDialog = false
        }

    LaunchedEffect(state.connectionState) {
        if (state.connectionState == MonitorConnectionState.LIVE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) {
                showNotifDialog = true
            }
        }
    }

    if (showNotifDialog) {
        MonitorNotificationDialog(
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    showNotifDialog = false
                }
            },
            onDeny = { showNotifDialog = false },
        )
    }

    MonitorScreen(
        state = state,
        onBack = onBack,
        onStartStream = viewModel::startStream,
        onStopStream = viewModel::stopStream,
        onToggleErrorDetails = viewModel::toggleErrorDetails,
    )
}

@Composable
fun MonitorScreen(
    state: MonitorUiState,
    onBack: () -> Unit,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onToggleErrorDetails: () -> Unit,
) {
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
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(text = "Радионяня", style = MaterialTheme.typography.headlineMedium)
            }

            when (state.connectionState) {
                MonitorConnectionState.LIVE -> {
                    LiveStreamContent(
                        state = state,
                        onStopStream = onStopStream,
                    )
                }

                MonitorConnectionState.CONNECTING -> {
                    CenterStatusBlock(
                        icon = {
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
                        },
                        title = "Подключаемся к Станции...",
                        subtitle = "Устанавливаем защищённое соединение.",
                    )
                }

                MonitorConnectionState.STATION_OFFLINE -> {
                    CenterStatusBlock(
                        icon = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.headphones),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        title = "Станция не в сети",
                        subtitle = "Проверьте, что второй телефон включён, подключён к зарядке и к интернету.",
                        action = {
                            AppButton(
                                text = "Проверить снова",
                                onClick = onStartStream,
                                type = ButtonType.Tonal,
                                leadingIcon = {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                        },
                    )
                }

                MonitorConnectionState.SERVER_ERROR -> {
                    CenterStatusBlock(
                        icon = {
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
                        },
                        title = "Не удалось подключиться",
                        subtitle =
                            state.errorMessage
                                ?: "Сервер временно недоступен. Попробуйте ещё раз через минуту.",
                        details = if (state.showErrorDetails) state.errorDetails else null,
                        action = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Space.s2),
                            ) {
                                AppButton(
                                    text = "Повторить",
                                    onClick = onStartStream,
                                    leadingIcon = {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                )
                                AppButton(
                                    text = "Подробности",
                                    onClick = onToggleErrorDetails,
                                    type = ButtonType.Text,
                                )
                            }
                        },
                    )
                }

                MonitorConnectionState.NO_INTERNET -> {
                    AppBanner(
                        text = "Нет интернета. Для прослушивания нужна сеть на обоих телефонах.",
                        kind = BannerKind.Offline,
                    )
                    CenterStatusBlock(
                        icon = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_wifi_off),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        title = "Нет подключения",
                        subtitle = "Подключитесь к сети и нажмите «Слушать» снова.",
                        action = {
                            AppButton(
                                text = "Повторить",
                                onClick = onStartStream,
                                type = ButtonType.Tonal,
                                leadingIcon = {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                        },
                    )
                }

                MonitorConnectionState.STOPPED -> {
                    CenterStatusBlock(
                        icon = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.headphones),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        title = "Прослушивание остановлено",
                        subtitle = "Нажмите «Слушать», чтобы снова подключиться к Станции.",
                        action = {
                            AppButton(
                                text = "Слушать",
                                onClick = onStartStream,
                                leadingIcon = {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStreamContent(
    state: MonitorUiState,
    onStopStream: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(Modifier.height(Space.s3))

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
                            painter = painterResource(R.drawable.headphones),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Слушаем ${state.petName}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "Станция в сети",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AppTag(
                        text = "Live",
                        kind = TagKind.Ok,
                        showDefaultIcon = false,
                    )
                }

                // Волновая шкала звука
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.waveformBars.forEach { level ->
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height((40f * level).coerceAtLeast(6f).dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }

                // Таймер и задержка
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatElapsed(state.elapsedSeconds),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "задержка ≈ ${String.format(Locale("ru"), "%.1f", state.latencySeconds)} с",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.s3))

        Text(
            text = "Звук идёт со Станции в реальном времени.\nЗапись не сохраняется.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStopStream,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(horizontal = Space.s4),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onError),
                )
                Text(
                    text = "Остановить",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(Modifier.height(Space.s4))
    }
}

@Composable
private fun CenterStatusBlock(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    details: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Space.s4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            icon()
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (details != null) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            action?.invoke()
        }
    }
}

@Composable
private fun MonitorNotificationDialog(
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
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        title = {
            Text(
                text = "Разрешить CrunchAI отправлять уведомления?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Мы сообщим, если питомец кашляет или давно не подходил к миске.",
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

private fun formatElapsed(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val minutes = safe / 60L
    val seconds = safe % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
