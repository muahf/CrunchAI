package com.crunchai.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.crunchai.presentation.ui.theme.Space

private val FieldHeight = 56.dp
private val NormalBorder = 1.5.dp
private val ActiveBorder = 2.dp

/** Поле ввода из макета: подпись сверху, 4 состояния (обычное, фокус, ошибка, выключено). */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    errorText: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = errorText != null

    val borderColor = when {
        !enabled -> colors.outlineVariant
        isError -> colors.error
        focused -> colors.primary
        else -> colors.outline
    }
    val labelColor = when {
        isError -> colors.error
        focused -> colors.primary
        else -> colors.onSurfaceVariant
    }
    val textColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f)
    val fillColor = if (enabled) colors.surface else colors.onSurface.copy(alpha = 0.04f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Space.s1)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = labelColor)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            cursorBrush = SolidColor(colors.primary),
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FieldHeight)
                        .background(fillColor, MaterialTheme.shapes.medium)
                        .border(
                            if (isError || focused) ActiveBorder else NormalBorder,
                            borderColor,
                            MaterialTheme.shapes.medium,
                        )
                        .padding(horizontal = Space.s4),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (errorText != null) {
            Text(text = errorText, style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
    }
}