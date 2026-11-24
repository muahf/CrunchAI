package com.crunchai.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Сетка отступов с шагом 4 dp: «5 · Дизайн-система» → Spacing.
object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp   // поля экрана
    val s5 = 24.dp
    val s6 = 32.dp
    val s7 = 48.dp
}

// Скругления из макета.
object Radius {
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 28.dp
}

val AppShapes = Shapes(
    small = RoundedCornerShape(Radius.S),
    medium = RoundedCornerShape(Radius.M),
    large = RoundedCornerShape(Radius.L),
    extraLarge = RoundedCornerShape(Radius.XL),
)