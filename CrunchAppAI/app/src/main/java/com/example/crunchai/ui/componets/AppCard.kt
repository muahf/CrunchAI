package com.example.crunchai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.crunchai.ui.theme.Space
import com.example.crunchai.ui.theme.extendedColors

/** Виды карточки из макета. */
enum class CardKind { Default, Info, Warning, Error, Success }

@Composable
fun AppCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    kind: CardKind = CardKind.Default,
) {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val container = when (kind) {
        CardKind.Default -> colors.surface
        CardKind.Info -> colors.primaryContainer
        CardKind.Warning -> extended.warningContainer
        CardKind.Error -> colors.errorContainer
        CardKind.Success -> extended.successContainer
    }
    val content = when (kind) {
        CardKind.Default -> colors.onSurface
        CardKind.Info -> colors.onPrimaryContainer
        CardKind.Warning -> extended.onWarningContainer
        CardKind.Error -> colors.onErrorContainer
        CardKind.Success -> extended.onSuccessContainer
    }
    val shape = MaterialTheme.shapes.large

    Column(
        modifier = modifier
            .background(container, shape)
            .then(if (kind == CardKind.Default) Modifier.border(1.dp, colors.outlineVariant, shape) else Modifier)
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = content)
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = content)
    }
}