package com.crunchai.presentation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
class ThemeController(
    val darkTheme: Boolean,
    val toggle: () -> Unit,
)

/** null = переключатель темы не подключён (например, в превью). */
val LocalThemeController = staticCompositionLocalOf<ThemeController?> { null }