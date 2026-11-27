package com.crunchai.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crunchai.presentation.ui.theme.Space

private val ChipHeight = 32.dp
private val ChipIconSize = 16.dp
private val ChipIconGap = 6.dp

/** Переключатель-чип («Собака», «Кошка»). В выбранном состоянии: заливка и галочка. */
@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val content = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier.height(ChipHeight),
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.primaryContainer else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, colors.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChipIconGap),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(ChipIconSize),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}
