package com.crunchai.presentation.ui.componets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crunchai.presentation.ui.components.AppBanner
import com.crunchai.presentation.ui.components.AppButton
import com.crunchai.presentation.ui.components.AppCard
import com.crunchai.presentation.ui.components.AppChip
import com.crunchai.presentation.ui.components.AppTag
import com.crunchai.presentation.ui.components.AppTextField
import com.crunchai.presentation.ui.components.BannerKind
import com.crunchai.presentation.ui.components.ButtonType
import com.crunchai.presentation.ui.components.CardKind
import com.crunchai.presentation.ui.components.TagKind
import com.crunchai.presentation.ui.theme.CrunchAiTheme
import com.crunchai.presentation.ui.theme.Space

/** Одна аннотация вместо двух: превью сразу в светлой и в тёмной теме. */
@Preview(name = "Светлая тема")
@Preview(name = "Тёмная тема", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

@Composable
private fun PreviewBox(content: @Composable () -> Unit) {
    CrunchAiTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(Space.s4)) {
            content()
        }
    }
}

@ThemePreviews
@Composable
private fun ButtonsPreview() =
    PreviewBox {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            ButtonType.entries.forEach { type ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                    AppButton("Кнопка", {}, type = type)
                    AppButton("Кнопка", {}, type = type, enabled = false)
                    AppButton("Кнопка", {}, type = type, loading = true)
                }
            }
        }
    }

@ThemePreviews
@Composable
private fun TextFieldsPreview() =
    PreviewBox {
        Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            AppTextField("Рекс", {}, "Имя")
            AppTextField("", {}, "Имя", placeholder = "Введите имя")
            AppTextField(
                "",
                {},
                "Имя",
                placeholder = "Введите имя",
                errorText = "Введите число от 1 до 80",
            )
            AppTextField("Рекс", {}, "Имя", enabled = false)
        }
    }

@ThemePreviews
@Composable
private fun CardsPreview() =
    PreviewBox {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
            CardKind.entries.forEach { kind ->
                AppCard(
                    title = "Заголовок карточки",
                    body = "Текст карточки на две строки, чтобы было видно перенос.",
                    modifier = Modifier.width(300.dp),
                    kind = kind,
                )
            }
        }
    }

@ThemePreviews
@Composable
private fun ChipsPreview() =
    PreviewBox {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            AppChip("Собака", selected = false, onClick = {})
            AppChip("Собака", selected = true, onClick = {})
        }
    }

@ThemePreviews
@Composable
private fun TagsPreview() =
    PreviewBox {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            AppTag("Спокойно", kind = TagKind.Ok)
            AppTag("Быстро", kind = TagKind.Warning)
            AppTag("Ошибка", kind = TagKind.Error)
            AppTag("YAMNet", kind = TagKind.Info)
        }
    }

@ThemePreviews
@Composable
private fun BannersPreview() =
    PreviewBox {
        Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(Space.s3)) {
            AppBanner("Нет интернета. Данные сохраняются на телефоне.", kind = BannerKind.Offline)
            AppBanner(
                "Не удалось отправить события.",
                kind = BannerKind.Error,
                actionText = "Повторить",
            )
        }
    }
