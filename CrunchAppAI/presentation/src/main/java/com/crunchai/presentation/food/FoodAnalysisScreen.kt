package com.crunchai.presentation.food

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crunchai.presentation.R
import com.crunchai.presentation.common.formatTime
import com.crunchai.presentation.navigation.FoodAnalysisMode
import com.crunchai.presentation.ui.components.AppBanner
import com.crunchai.presentation.ui.components.AppButton
import com.crunchai.presentation.ui.components.AppTag
import com.crunchai.presentation.ui.components.BannerKind
import com.crunchai.presentation.ui.components.ButtonType
import com.crunchai.presentation.ui.components.ContentCard
import com.crunchai.presentation.ui.components.TagKind
import com.crunchai.presentation.ui.theme.Space
import com.example.crunchai.domain.model.FoodAnalysis
import java.io.ByteArrayOutputStream
import java.time.ZoneId

@Composable
fun FoodAnalysisRoute(
    initialMode: FoodAnalysisMode,
    onBack: () -> Unit,
    viewModel: FoodAnalysisViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialMode) {
        viewModel.setInitialMode(initialMode)
    }

    var pendingCaptureMode by remember { mutableStateOf(initialMode) }
    var showCameraDialog by remember { mutableStateOf(false) }

    val cameraPreviewLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                viewModel.onPhotoCaptured(pendingCaptureMode, bitmap.toJpegBytes())
            }
        }

    val galleryPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val bytes = context.readUriBytes(uri)
                if (bytes != null) {
                    viewModel.onPhotoCaptured(pendingCaptureMode, bytes)
                }
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            showCameraDialog = false
            if (granted) {
                cameraPreviewLauncher.launch(null)
            } else {
                galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

    fun startCapture(mode: FoodAnalysisMode) {
        pendingCaptureMode = mode
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            cameraPreviewLauncher.launch(null)
        } else {
            showCameraDialog = true
        }
    }

    if (showCameraDialog) {
        CameraPermissionDialog(
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onDeny = {
                showCameraDialog = false
                galleryPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }

    FoodAnalysisScreen(
        state = state,
        onBack = onBack,
        onModeChange = viewModel::onModeChange,
        onCaptureBowl = { startCapture(FoodAnalysisMode.BOWL) },
        onCaptureFeed = { startCapture(FoodAnalysisMode.LABEL) },
        onRetry = viewModel::retryLastAnalysis,
        onToggleDetails = viewModel::toggleErrorDetails,
    )
}

@Composable
fun FoodAnalysisScreen(
    state: FoodAnalysisUiState,
    onBack: () -> Unit,
    onModeChange: (FoodAnalysisMode) -> Unit,
    onCaptureBowl: () -> Unit,
    onCaptureFeed: () -> Unit,
    onRetry: () -> Unit,
    onToggleDetails: () -> Unit,
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
            // Top bar: ← Фото
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    text = "Фото",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            when {
                // Состояние 2: Загрузка («ИИ анализирует фото...»)
                state.isAnalyzing -> {
                    AnalyzingContent(
                        photo =
                            if (state.mode == FoodAnalysisMode.BOWL) {
                                state.bowlAfterPhoto ?: state.bowlBeforePhoto
                            } else {
                                state.feedLabelPhoto
                            },
                    )
                }

                // Состояние 4: Ошибка сервера («Не удалось проанализировать фото»)
                state.serverError != null -> {
                    ServerErrorContent(
                        message = state.serverError,
                        details = if (state.showErrorDetails) state.serverErrorDetails else null,
                        onRetry = onRetry,
                        onToggleDetails = onToggleDetails,
                    )
                }

                // Состояние 3: Пустой экран («Фото ещё нет»)
                !state.hasAnyPhotoForCurrentMode && state.bowlBeforePhoto == null && state.feedLabelPhoto == null -> {
                    EmptyPhotosContent(
                        onCaptureBowl = onCaptureBowl,
                        onCaptureFeed = onCaptureFeed,
                    )
                }

                // Состояние 1 и 5: Данные или Офлайн («Ожидает отправки»)
                else -> {
                    ResultAndPhotosContent(
                        state = state,
                        zone = zone,
                        onModeChange = onModeChange,
                        onCaptureMore = {
                            if (state.mode == FoodAnalysisMode.BOWL) onCaptureBowl() else onCaptureFeed()
                        },
                        onDone = onBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzingContent(photo: PhotoSlot?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Space.s3))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            photo?.bytes?.toBitmapOrNull()?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
            }
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Space.s5))

        Text(
            text = "ИИ анализирует фото...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.s2))
        Text(
            text = "Оцениваем, сколько съедено, и читаем состав корма.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.s4))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun EmptyPhotosContent(
    onCaptureBowl: () -> Unit,
    onCaptureFeed: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                    painter = painterResource(R.drawable.camera),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = "Фото ещё нет",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Сфотографируйте миску до и после еды\nили упаковку корма.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.s2))
            AppButton(
                text = "Сфотографировать миску",
                onClick = onCaptureBowl,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.camera),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            AppButton(
                text = "Сфотографировать корм",
                onClick = onCaptureFeed,
                modifier = Modifier.fillMaxWidth(),
                type = ButtonType.Tonal,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.camera),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ServerErrorContent(
    message: String,
    details: String?,
    onRetry: () -> Unit,
    onToggleDetails: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                    Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(
                text = "Не удалось проанализировать фото",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            details?.let {
                Text(
                    text = it,
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

@Composable
private fun ResultAndPhotosContent(
    state: FoodAnalysisUiState,
    zone: ZoneId,
    onModeChange: (FoodAnalysisMode) -> Unit,
    onCaptureMore: () -> Unit,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            if (state.isOfflinePending) {
                // Состояние 5: Нет интернета
                AppBanner(
                    text = "Нет интернета. Фото сохранено, анализ начнётся при появлении сети.",
                    kind = BannerKind.Offline,
                )
            } else {
                // Переключатель «Миска / Корм»
                ModeSegmentedSwitch(
                    selected = state.mode,
                    onSelect = onModeChange,
                )
            }

            // Превью фотографий
            if (state.mode == FoodAnalysisMode.BOWL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s3),
                ) {
                    PhotoPreviewTile(
                        slot = state.bowlBeforePhoto,
                        caption =
                            state.bowlBeforePhoto?.let { "До · ${formatTime(it.capturedAt, zone)}" }
                                ?: "До · не снято",
                        modifier = Modifier.weight(1f),
                    )
                    PhotoPreviewTile(
                        slot = state.bowlAfterPhoto,
                        caption =
                            state.bowlAfterPhoto?.let { "После · ${formatTime(it.capturedAt, zone)}" }
                                ?: "После · не снято",
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                PhotoPreviewTile(
                    slot = state.feedLabelPhoto,
                    caption =
                        state.feedLabelPhoto?.let { "Упаковка · ${formatTime(it.capturedAt, zone)}" }
                            ?: "Упаковка корма",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.isOfflinePending) {
                ContentCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Space.s4),
                        verticalArrangement = Arrangement.spacedBy(Space.s1),
                    ) {
                        Text("Ожидает отправки", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Мы покажем результат, как только появится сеть.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Карточка «Съедено ≈ X%»
                val eaten = state.bowlResult?.eatenPercent
                if (eaten != null) {
                    EatenPercentCard(percent = eaten)
                } else if (state.mode == FoodAnalysisMode.BOWL && state.bowlBeforePhoto != null && state.bowlAfterPhoto == null) {
                    ContentCard(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(Space.s4),
                            verticalArrangement = Arrangement.spacedBy(Space.s1),
                        ) {
                            Text("Фото «До» сохранено", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Когда питомец поест, нажмите «Снять ещё», чтобы сфотографировать миску после еды.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Карточка «Состав корма»
                val feed = state.feedResult ?: state.currentResult
                if (feed != null && (!feed.composition.isNullOrBlank() || feed.risks.isNotEmpty())) {
                    FeedCompositionCard(analysis = feed)
                }
            }
        }

        // Нижняя пара кнопок: «Снять ещё» + «Готово»
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Space.s3),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            AppButton(
                text = "Снять ещё",
                onClick = onCaptureMore,
                type = ButtonType.Tonal,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.camera),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            AppButton(
                text = "Готово",
                onClick = onDone,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeSegmentedSwitch(
    selected: FoodAnalysisMode,
    onSelect: (FoodAnalysisMode) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, colors.outlineVariant),
        color = colors.surface,
    ) {
        Row(Modifier.fillMaxSize()) {
            SegmentButton(
                text = "Миска",
                selected = selected == FoodAnalysisMode.BOWL,
                onClick = { onSelect(FoodAnalysisMode.BOWL) },
                modifier = Modifier.weight(1f),
            )
            SegmentButton(
                text = "Корм",
                selected = selected == FoodAnalysisMode.LABEL,
                onClick = { onSelect(FoodAnalysisMode.LABEL) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        color = if (selected) colors.primaryContainer else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhotoPreviewTile(
    slot: PhotoSlot?,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s1),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = remember(slot?.bytes) { slot?.bytes?.toBitmapOrNull() }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.camera),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EatenPercentCard(percent: Int) {
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
                Text(text = "Съедено", style = MaterialTheme.typography.labelLarge)
                Text(text = "≈ $percent%", style = MaterialTheme.typography.headlineSmall)
            }
            LinearProgressIndicator(
                progress = { (percent.coerceIn(0, 100)) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedCompositionCard(analysis: FoodAnalysis) {
    ContentCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            Text(text = "Состав корма", style = MaterialTheme.typography.labelLarge)
            analysis.composition?.takeIf { it.isNotBlank() }?.let { comp ->
                Text(
                    text = comp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (analysis.risks.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s2),
                    verticalArrangement = Arrangement.spacedBy(Space.s2),
                ) {
                    analysis.risks.forEach { risk ->
                        val isWarning =
                            risk.contains("консервант", ignoreCase = true) ||
                                risk.contains("сахар", ignoreCase = true) ||
                                risk.contains("красител", ignoreCase = true) ||
                                risk.contains("E3", ignoreCase = true) ||
                                risk.contains("E2", ignoreCase = true)
                        AppTag(
                            text = risk,
                            kind = if (isWarning) TagKind.Warning else TagKind.Ok,
                            showDefaultIcon = isWarning,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionDialog(
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
                    painter = painterResource(R.drawable.camera),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        title = {
            Text(
                text = "Разрешить CrunchAI снимать фото?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Камера нужна, чтобы сканировать QR-код и фотографировать миску и корм.",
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

private fun Bitmap.toJpegBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 85, stream)
    return stream.toByteArray()
}

private fun ByteArray.toBitmapOrNull(): Bitmap? = runCatching { BitmapFactory.decodeByteArray(this, 0, size) }.getOrNull()

private fun Context.readUriBytes(uri: Uri): ByteArray? =
    runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
