package com.crunchai.presentation.pairing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crunchai.presentation.R
import com.crunchai.presentation.ui.components.AppBanner
import com.crunchai.presentation.ui.components.AppButton
import com.crunchai.presentation.ui.components.AppTag
import com.crunchai.presentation.ui.components.AppTextField
import com.crunchai.presentation.ui.components.BannerKind
import com.crunchai.presentation.ui.components.ButtonType
import com.crunchai.presentation.ui.components.ContentCard
import com.crunchai.presentation.ui.components.TagKind
import com.crunchai.presentation.ui.theme.Space
import com.example.crunchai.domain.model.DeviceRole
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun PairingRoute(
    onBack: () -> Unit,
    role: DeviceRole,
    onPairedDone: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(role) {
        viewModel.setRole(role)
    }

    // Разрешение на уведомления (для Станции, макет 01)
    var showNotifDialog by remember { mutableStateOf(false) }
    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            showNotifDialog = false
        }

    LaunchedEffect(state.role, state.pairKey) {
        if (state.role == DeviceRole.STATION && state.pairKey != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) {
                showNotifDialog = true
            }
        }
    }

    // Разрешение на камеру (для Наблюдателя, макет 02)
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showCameraDialog by remember { mutableStateOf(false) }
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
            showCameraDialog = false
        }

    LaunchedEffect(state.role) {
        if (state.role == DeviceRole.OBSERVER && !hasCameraPermission) {
            showCameraDialog = true
        }
    }

    if (showNotifDialog) {
        NotificationPermissionDialog(
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

    if (showCameraDialog && state.role == DeviceRole.OBSERVER && !hasCameraPermission) {
        ScannerCameraDialog(
            onAllow = { cameraLauncher.launch(Manifest.permission.CAMERA) },
            onDeny = { showCameraDialog = false },
        )
    }

    if (state.showManualInputDialog) {
        ManualKeyInputDialog(
            value = state.manualKeyInput,
            error = state.manualKeyError,
            onValueChange = viewModel::onManualKeyChange,
            onConfirm = { viewModel.submitKey(state.manualKeyInput) },
            onDismiss = viewModel::closeManualDialog,
        )
    }

    if (state.role == DeviceRole.STATION) {
        StationPairingScreen(
            state = state,
            onBack = onBack,
            onRetry = viewModel::createNewKey,
            onToggleDetails = viewModel::toggleStationDetails,
            onStartListening = onBack,
        )
    } else {
        ObserverPairingScreen(
            state = state,
            hasCameraPermission = hasCameraPermission,
            onBack = onBack,
            onCodeScanned = viewModel::submitKey,
            onOpenManualInput = viewModel::openManualDialog,
            onScanAgain = viewModel::resetScanner,
            onToggleDetails = viewModel::toggleObserverDetails,
            onPairedDone = onPairedDone,
        )
    }
}

// =====================================================================
// ЭКРАН 01 · ПРИВЯЗКА: СТАНЦИЯ (генерация QR-кода)
// =====================================================================
@Composable
private fun StationPairingScreen(
    state: PairingUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleDetails: () -> Unit,
    onStartListening: () -> Unit,
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(text = "Привязка", style = MaterialTheme.typography.headlineMedium)
            }

            when {
                state.isStationOffline -> {
                    AppBanner(
                        text = "Нет интернета. Для привязки нужны оба телефона онлайн.",
                        kind = BannerKind.Offline,
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                    painter = painterResource(R.drawable.ic_wifi_off),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text("Подключитесь к сети", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Как только появится интернет, мы создадим ключ.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            AppButton(
                                text = "Проверить снова",
                                onClick = onRetry,
                                type = ButtonType.Tonal,
                                leadingIcon = {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                        }
                    }
                }

                state.stationError != null -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Text("Не удалось создать ключ", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = state.stationError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            if (state.showStationDetails && state.stationErrorDetails != null) {
                                Text(
                                    text = state.stationErrorDetails,
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
                            AppButton(text = "Подробности", onClick = onToggleDetails, type = ButtonType.Text)
                        }
                    }
                }

                state.isKeyExpired -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                    painter = painterResource(R.drawable.ic_qr),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text("Ключ истёк", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Одноразовый ключ действует 10 минут.\nСоздайте новый, чтобы связать телефоны.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            AppButton(
                                text = "Создать новый ключ",
                                onClick = onRetry,
                                leadingIcon = {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                        }
                    }
                }

                state.isCreatingKey -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.s4),
                    ) {
                        Spacer(Modifier.height(Space.s2))
                        ContentCard(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(Space.s5),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Space.s3),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(210.dp)
                                            .clip(MaterialTheme.shapes.large)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        Text("Создаём ключ...", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Это займёт несколько секунд.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    val key = state.pairKey.orEmpty()
                    val qrBitmap = remember(key) { if (key.isNotBlank()) QrBitmapGenerator.generate(key) else null }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.s4),
                    ) {
                        Spacer(Modifier.height(Space.s2))
                        ContentCard(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(Space.s5),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Space.s2),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(210.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(Color.White)
                                            .padding(Space.s2),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "QR-код привязки $key",
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Text(
                                            text = key.ifBlank { "Нет ключа" },
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(Space.s1))
                                Text(
                                    text = "Ключ: $key",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    text = "Действует ${state.remainingMinutes} минут",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Text(
                            text = "Откройте приложение на втором телефоне,\nвыберите «Наблюдатель» и отсканируйте код.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        AppTag(
                            text = "Ожидаем сканирование...",
                            kind = TagKind.Info,
                            showDefaultIcon = false,
                        )
                    }

                    AppButton(
                        text = "Начать слушать",
                        onClick = onStartListening,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Space.s3),
                    )
                }
            }
        }
    }
}

// =====================================================================
// ЭКРАН 02 · ПРИВЯЗКА: НАБЛЮДАТЕЛЬ (сканирование QR / ввод кода)
// =====================================================================
@Composable
private fun ObserverPairingScreen(
    state: PairingUiState,
    hasCameraPermission: Boolean,
    onBack: () -> Unit,
    onCodeScanned: (String) -> Unit,
    onOpenManualInput: () -> Unit,
    onScanAgain: () -> Unit,
    onToggleDetails: () -> Unit,
    onPairedDone: () -> Unit,
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(text = "Сканирование", style = MaterialTheme.typography.headlineMedium)
            }

            when (state.observerState) {
                ObserverScanState.PAIRED -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = "Телефоны связаны",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Станция и Наблюдатель успешно подключены.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(Space.s2))
                            AppButton(
                                text = "Перейти на главную",
                                onClick = onPairedDone,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                ObserverScanState.OFFLINE -> {
                    AppBanner(
                        text = "Нет интернета. Для привязки нужен доступ к сети.",
                        kind = BannerKind.Offline,
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                    painter = painterResource(R.drawable.ic_wifi_off),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text("Нет подключения", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Подключитесь к Wi-Fi или мобильной сети и сканируйте снова.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            AppButton(
                                text = "Проверить снова",
                                onClick = onScanAgain,
                                type = ButtonType.Tonal,
                                leadingIcon = {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                        }
                    }
                }

                ObserverScanState.SERVER_ERROR -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Text("Не удалось связать телефоны", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text =
                                    state.observerErrorDetails
                                        ?: "Сервер не принял код. Он мог истечь.\nПопросите создать новый на Станции.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            AppButton(
                                text = "Сканировать снова",
                                onClick = onScanAgain,
                                leadingIcon = {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                            )
                            AppButton(text = "Подробности", onClick = onToggleDetails, type = ButtonType.Text)
                        }
                    }
                }

                else -> {
                    val overlayText =
                        when (state.observerState) {
                            ObserverScanState.CHECKING -> "Код распознан"
                            ObserverScanState.NOT_FOUND -> "QR-код не найден"
                            else -> "Наведите камеру на QR-код на экране Станции"
                        }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(Color(0xFF141816)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (hasCameraPermission && state.observerState == ObserverScanState.SCANNING) {
                            QrCameraPreview(onCodeDetected = onCodeScanned)
                        }
                        Box(
                            modifier =
                                Modifier
                                    .size(180.dp)
                                    .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(16.dp)),
                        )
                        Text(
                            text = overlayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(Space.s3),
                        )
                    }

                    Spacer(Modifier.height(Space.s4))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.s3),
                    ) {
                        when (state.observerState) {
                            ObserverScanState.CHECKING -> {
                                Text("Проверяем код...", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "Связываем телефоны.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                            ObserverScanState.NOT_FOUND -> {
                                Text("Код не найден", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "Подведите камеру ближе или введите код вручную.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                AppButton(
                                    text = "Ввести код вручную",
                                    onClick = onOpenManualInput,
                                    type = ButtonType.Tonal,
                                )
                            }
                            else -> {
                                Text(
                                    "Код распознаётся автоматически.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                AppButton(
                                    text = "Ввести код вручную",
                                    onClick = onOpenManualInput,
                                    modifier = Modifier.fillMaxWidth(),
                                    type = ButtonType.Outline,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrCameraPreview(onCodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var alreadyDetected by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView =
                PreviewView(ctx).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                val scanner = BarcodeScanning.getClient()
                val analysis =
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !alreadyDetected) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstOrNull()?.rawValue?.trim()?.uppercase()
                                if (!raw.isNullOrBlank() && !alreadyDetected) {
                                    alreadyDetected = true
                                    onCodeDetected(raw)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
    )
}

@Composable
private fun ManualKeyInputDialog(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Введите код Станции", style = MaterialTheme.typography.headlineSmall) },
        text = {
            AppTextField(
                value = value,
                onValueChange = onValueChange,
                label = "Ключ привязки",
                placeholder = "4F7-A9C",
                errorText = error,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            AppButton(text = "Связать", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        },
        dismissButton = {
            AppButton(text = "Отмена", onClick = onDismiss, modifier = Modifier.fillMaxWidth(), type = ButtonType.Text)
        },
    )
}

@Composable
private fun NotificationPermissionDialog(
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
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                AppButton(
                    text = "При использовании приложения",
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.Tonal,
                )
                AppButton(text = "Только в этот раз", onClick = onAllow, modifier = Modifier.fillMaxWidth(), type = ButtonType.Outline)
                AppButton(text = "Не разрешать", onClick = onDeny, modifier = Modifier.fillMaxWidth(), type = ButtonType.Text)
            }
        },
    )
}

@Composable
private fun ScannerCameraDialog(
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
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                AppButton(
                    text = "При использовании приложения",
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.Tonal,
                )
                AppButton(text = "Только в этот раз", onClick = onAllow, modifier = Modifier.fillMaxWidth(), type = ButtonType.Outline)
                AppButton(text = "Не разрешать", onClick = onDeny, modifier = Modifier.fillMaxWidth(), type = ButtonType.Text)
            }
        },
    )
}
