package com.example.crunchai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Палитра из макета Figma: «5 · Дизайн-система» → Palette / Light и Palette / Dark.

val LightColorScheme = lightColorScheme(
    background = Color(0xFFF6F7F4),
    onBackground = Color(0xFF1B1F1D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F1D),
    surfaceVariant = Color(0xFFE9EEEA),
    onSurfaceVariant = Color(0xFF444C48),
    primary = Color(0xFF1F6F5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFEDE3),
    onPrimaryContainer = Color(0xFF00382C),
    secondary = Color(0xFF9A4A12),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE0CC),
    onSecondaryContainer = Color(0xFF4A1D00),
    outline = Color(0xFF6F7973),
    outlineVariant = Color(0xFFC4CCC7),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

val DarkColorScheme = darkColorScheme(
    background = Color(0xFF0F1311),
    onBackground = Color(0xFFE3E8E5),
    surface = Color(0xFF171C19),
    onSurface = Color(0xFFE3E8E5),
    surfaceVariant = Color(0xFF232B27),
    onSurfaceVariant = Color(0xFFBBC5BF),
    primary = Color(0xFF86D4BE),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0F5243),
    onPrimaryContainer = Color(0xFFCFEDE3),
    secondary = Color(0xFFFFB68A),
    onSecondary = Color(0xFF4A1D00),
    secondaryContainer = Color(0xFF7A3300),
    onSecondaryContainer = Color(0xFFFFE0CC),
    outline = Color(0xFF8A948E),
    outlineVariant = Color(0xFF3B4540),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Цвета «Предупреждение» и «Успех»: в стандартной Material-теме их нет, поэтому они здесь. */
@Immutable
data class ExtendedColors(
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LightExtendedColors = ExtendedColors(
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFFE8B8),
    onWarningContainer = Color(0xFF3A2600),
    success = Color(0xFF1E6B2E),
    successContainer = Color(0xFFCDEFD2),
    onSuccessContainer = Color(0xFF0B3A15),
)

val DarkExtendedColors = ExtendedColors(
    warning = Color(0xFFF5C26B),
    warningContainer = Color(0xFF5C4000),
    onWarningContainer = Color(0xFFFFE8B8),
    success = Color(0xFF8FD99A),
    successContainer = Color(0xFF14522A),
    onSuccessContainer = Color(0xFFCDEFD2),
)