package com.crunchai.presentation.pet

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crunchai.presentation.R
import com.crunchai.presentation.ui.components.AppBanner
import com.crunchai.presentation.ui.components.AppBottomBar
import com.crunchai.presentation.ui.components.AppButton
import com.crunchai.presentation.ui.components.AppChip
import com.crunchai.presentation.ui.components.AppTextField
import com.crunchai.presentation.ui.components.BannerKind
import com.crunchai.presentation.ui.components.ButtonType
import com.crunchai.presentation.ui.components.MainTab
import com.crunchai.presentation.ui.theme.Space
import com.example.crunchai.domain.model.Species

@Composable
fun PetProfileRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToDiary: () -> Unit,
    viewModel: PetProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCameraDialog by remember { mutableStateOf(false) }

    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.onPhotoSelected(uri.toString())
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            showCameraDialog = false
            if (granted) {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

    if (showCameraDialog) {
        CameraPermissionDialog(
            onAllow = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            onDeny = { showCameraDialog = false },
        )
    }

    PetProfileScreen(
        state = state,
        onAddPhotoClick = {
            val hasCamera =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            if (hasCamera) {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                showCameraDialog = true
            }
        },
        onNameChange = viewModel::onNameChange,
        onSpeciesChange = viewModel::onSpeciesChange,
        onAgeChange = viewModel::onAgeChange,
        onWeightChange = viewModel::onWeightChange,
        onBodyScoreChange = viewModel::onBodyScoreChange,
        onSave = viewModel::save,
        onNavigateToHome = onNavigateToHome,
        onNavigateToDiary = onNavigateToDiary,
    )
}

@Composable
fun PetProfileScreen(
    state: PetProfileUiState,
    onAddPhotoClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onSpeciesChange: (Species) -> Unit,
    onAgeChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onBodyScoreChange: (Int) -> Unit,
    onSave: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToDiary: () -> Unit,
) {
    val enabled = !state.isSaving

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                selected = MainTab.Pet,
                onHome = onNavigateToHome,
                onDiary = onNavigateToDiary,
                onPet = {},
            )
        },
    ) { inner ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = Space.s4),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.s4),
            ) {
                Spacer(Modifier.height(Space.s2))

                Text(
                    text = "Питомец",
                    style = MaterialTheme.typography.headlineMedium,
                )

                // Баннер ошибки сервера (состояние 4)
                state.serverError?.let { errorText ->
                    AppBanner(
                        text = errorText,
                        kind = BannerKind.Error,
                        actionText = "Повторить",
                        onAction = onSave,
                    )
                }

                // Баннер отсутствия интернета (состояние 5)
                if (state.isOffline) {
                    AppBanner(
                        text = "Нет интернета. Профиль сохранится на телефоне и отправится позже.",
                        kind = BannerKind.Offline,
                    )
                }

                // Аватар с лапкой + кнопка «Добавить фото»
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s4),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.paw),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    AppButton(
                        text = if (state.photoUri != null) "Изменить фото" else "Добавить фото",
                        onClick = onAddPhotoClick,
                        type = ButtonType.Outline,
                        enabled = enabled,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.camera),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }

                // Поле «Имя»
                AppTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "Имя",
                    placeholder = "Например, Рекс",
                    enabled = enabled,
                    errorText = state.fieldErrors["name"],
                    modifier = Modifier.fillMaxWidth(),
                )

                // Секция «Вид»
                Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    Text(
                        text = "Вид",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (state.fieldErrors["species"] != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                        AppChip(
                            text = "Кошка",
                            selected = state.species == Species.CAT,
                            onClick = { if (enabled) onSpeciesChange(Species.CAT) },
                        )
                        AppChip(
                            text = "Собака",
                            selected = state.species == Species.DOG,
                            onClick = { if (enabled) onSpeciesChange(Species.DOG) },
                        )
                        AppChip(
                            text = "Другое",
                            selected = state.species == Species.OTHER,
                            onClick = { if (enabled) onSpeciesChange(Species.OTHER) },
                        )
                    }
                    state.fieldErrors["species"]?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                // Два поля в ряд: «Возраст» и «Вес»
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s3),
                ) {
                    AppTextField(
                        value = state.ageYearsInput,
                        onValueChange = onAgeChange,
                        label = "Возраст",
                        placeholder = "Лет",
                        enabled = enabled,
                        errorText = state.fieldErrors["ageMonths"],
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = state.weightKgInput,
                        onValueChange = onWeightChange,
                        label = "Вес",
                        placeholder = "кг",
                        enabled = enabled,
                        errorText = state.fieldErrors["weightKg"],
                        modifier = Modifier.weight(1f),
                    )
                }

                // Секция «Упитанность (1–9)»
                Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                    Text(
                        text = "Упитанность (1–9)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.s1),
                    ) {
                        (1..9).forEach { score ->
                            BodyScoreButton(
                                score = score,
                                selected = state.bodyScore == score,
                                enabled = enabled,
                                onClick = { onBodyScoreChange(score) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    state.bodyScoreDescription?.let { hint ->
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (state.saveSuccess) {
                    Text(
                        text = "Профиль сохранён",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(Space.s2))
            }

            // Кнопка «Сохранить» / «Сохраняем...» над нижней навигацией
            AppButton(
                text = if (state.isSaving) "Сохраняем..." else "Сохранить",
                onClick = onSave,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(vertical = Space.s3),
            )
        }
    }
}

@Composable
private fun BodyScoreButton(
    score: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (selected) colors.primaryContainer else Color.Transparent
    val content =
        when {
            !enabled -> colors.onSurface.copy(alpha = 0.38f)
            selected -> colors.onPrimaryContainer
            else -> colors.onSurface
        }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(36.dp),
        shape = MaterialTheme.shapes.small,
        color = container,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = content,
            )
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
