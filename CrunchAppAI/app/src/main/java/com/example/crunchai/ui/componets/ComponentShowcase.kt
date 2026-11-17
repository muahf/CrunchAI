package com.example.crunchai.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.crunchai.ui.theme.Space

/** Демо-экран: все компоненты дизайн-системы и переключатели темы. */
@Composable
fun ComponentsShowcase(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("Рекс") }
    var dog by remember { mutableStateOf(true) }
    var cat by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s5),
    ) {
        Text(
            text = "Дизайн-система",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        SwitchRow("Тёмная тема", darkTheme, onDarkThemeChange)
        SwitchRow(
            label = "Динамические цвета (Android 12+)",
            checked = dynamicColor,
            onCheckedChange = onDynamicColorChange,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )

        Section("Кнопки") {
            ButtonType.entries.forEach { type ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                    AppButton("Кнопка", {}, Modifier.weight(1f), type = type)
                    AppButton("Выключена", {}, Modifier.weight(1f), type = type, enabled = false)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                AppButton("Загрузка", {}, Modifier.weight(1f), loading = true)
                AppButton("Загрузка", {}, Modifier.weight(1f), type = ButtonType.Tonal, loading = true)
            }
        }

        Section("Поле ввода") {
            AppTextField(name, { name = it }, "Имя", Modifier.fillMaxWidth(), placeholder = "Введите имя")
            AppTextField(
                value = "",
                onValueChange = {},
                label = "Возраст",
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Введите возраст",
                errorText = "Введите число от 1 до 80",
            )
            AppTextField("Рекс", {}, "Выключено", Modifier.fillMaxWidth(), enabled = false)
        }

        Section("Карточки") {
            CardKind.entries.forEach { kind ->
                AppCard(
                    title = "Заголовок карточки",
                    body = "Текст карточки на две строки, чтобы было видно перенос.",
                    modifier = Modifier.fillMaxWidth(),
                    kind = kind,
                )
            }
        }

        Section("Чипы и теги") {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                AppChip("Собака", dog, { dog = !dog })
                AppChip("Кошка", cat, { cat = !cat })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                AppTag("Спокойно", kind = TagKind.Ok)
                AppTag("Быстро", kind = TagKind.Warning)
                AppTag("Ошибка", kind = TagKind.Error)
                AppTag("YAMNet", kind = TagKind.Info)
            }
        }

        Section("Баннеры") {
            AppBanner("Нет интернета. Данные сохраняются на телефоне.", kind = BannerKind.Offline)
            AppBanner(
                "Не удалось отправить события.",
                kind = BannerKind.Error,
                actionText = "Повторить",
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}