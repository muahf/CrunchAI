package com.crunchai.presentation.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.crunchai.presentation.R
import com.crunchai.presentation.ui.theme.Space
import com.crunchai.presentation.ui.theme.extendedColors

/** Offline: «Нет интернета» (жёлтый, иконка пульсирует). Error: ошибка сервера с кнопкой «Повторить» (красный). */
enum class BannerKind { Offline, Error }

private val BannerIconSize = 24.dp
private const val PULSE_DURATION_MS = 900
private const val PULSE_MIN_ALPHA = 0.35f

@Composable
fun AppBanner(
    text: String,
    modifier: Modifier = Modifier,
    kind: BannerKind = BannerKind.Offline,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val container = if (kind == BannerKind.Offline) extended.warningContainer else colors.errorContainer
    val content = if (kind == BannerKind.Offline) extended.onWarningContainer else colors.onErrorContainer

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(container, MaterialTheme.shapes.medium)
                .padding(horizontal = Space.s4, vertical = Space.s3),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        val iconModifier = Modifier.size(BannerIconSize)
        if (kind == BannerKind.Offline) {
            // Анимация: прозрачность плавно меняется 1 → 0.35 → 1 и так по кругу.
            val transition = rememberInfiniteTransition(label = "offlinePulse")
            val alpha by transition.animateFloat(
                initialValue = 1f,
                targetValue = PULSE_MIN_ALPHA,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = PULSE_DURATION_MS),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "offlineAlpha",
            )
            Icon(
                painter = painterResource(R.drawable.ic_wifi_off),
                contentDescription = null,
                tint = content,
                modifier = iconModifier.alpha(alpha),
            )
        } else {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = content, modifier = iconModifier)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = content)
            if (actionText != null) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    modifier = Modifier.clickable(onClick = onAction),
                )
            }
        }
    }
}
