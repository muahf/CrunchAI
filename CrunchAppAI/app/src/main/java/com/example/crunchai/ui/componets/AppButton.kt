package com.example.crunchai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.crunchai.ui.theme.Space

/** Виды кнопок из макета: «Components / Light». */
enum class ButtonType { Primary, Tonal, Outline, Text, Danger }

private val ButtonHeight = 48.dp
private val ButtonPadding = 20.dp          // в макете 20 dp у всех, кроме Text (12 dp)
private val OutlineWidth = 1.5.dp

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val isFlat = type == ButtonType.Outline || type == ButtonType.Text

    val container = when (type) {
        ButtonType.Primary -> colors.primary
        ButtonType.Tonal -> colors.primaryContainer
        ButtonType.Danger -> colors.error
        else -> Color.Transparent
    }
    val content = when (type) {
        ButtonType.Primary -> colors.onPrimary
        ButtonType.Tonal -> colors.onPrimaryContainer
        ButtonType.Danger -> colors.onError
        else -> colors.primary
    }
    val disabledOverlay = colors.onSurface.copy(alpha = 0.12f)
    val disabledContent = colors.onSurface.copy(alpha = 0.38f)

    Button(
        // во время загрузки кнопка выглядит обычной, но повторно не нажимается
        onClick = { if (!loading) onClick() },
        modifier = modifier.heightIn(min = ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (isFlat) Color.Transparent else disabledOverlay,
            disabledContentColor = disabledContent,
        ),
        border = if (type == ButtonType.Outline) {
            BorderStroke(OutlineWidth, if (enabled) colors.outline else disabledOverlay)
        } else {
            null
        },
        contentPadding = PaddingValues(
            horizontal = if (type == ButtonType.Text) Space.s3 else ButtonPadding,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(Space.s2))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}