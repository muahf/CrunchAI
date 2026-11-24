package com.crunchai.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.crunchai.presentation.ui.theme.Space
import com.crunchai.presentation.ui.theme.extendedColors

/** Виды маленького ярлыка: «Спокойно», «Быстро», «Ошибка», «YAMNet». */
enum class TagKind { Ok, Warning, Error, Info }

private val TagHeight = 24.dp
private val TagIconSize = 14.dp

/**
 * @param icon своя иконка (например, Wi-Fi для «Нет связи»); если задана, стандартная не рисуется
 * @param showDefaultIcon false убирает стандартную галочку/треугольник (у «Спокойно» в макете её нет)
 */
@Composable
fun AppTag(
    text: String,
    modifier: Modifier = Modifier,
    kind: TagKind = TagKind.Ok,
    icon: Painter? = null,
    showDefaultIcon: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val container = when (kind) {
        TagKind.Ok -> extended.successContainer
        TagKind.Warning -> extended.warningContainer
        TagKind.Error -> colors.errorContainer
        TagKind.Info -> colors.primaryContainer
    }
    val content = when (kind) {
        TagKind.Ok -> extended.onSuccessContainer
        TagKind.Warning -> extended.onWarningContainer
        TagKind.Error -> colors.onErrorContainer
        TagKind.Info -> colors.onPrimaryContainer
    }
    val defaultIcon = if (showDefaultIcon) {
        when (kind) {
            TagKind.Ok -> Icons.Filled.Check
            TagKind.Error -> Icons.Filled.Warning
            else -> null
        }
    } else {
        null
    }

    Row(
        modifier = modifier
            .height(TagHeight)
            .background(container, MaterialTheme.shapes.small)
            .padding(horizontal = Space.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s1),
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, tint = content, modifier = Modifier.size(TagIconSize))
        } else if (defaultIcon != null) {
            Icon(imageVector = defaultIcon, contentDescription = null, tint = content, modifier = Modifier.size(TagIconSize))
        }
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = content)
    }
}