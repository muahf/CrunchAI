package com.example.crunchai.domain.model

enum class PhotoKind { BOWL_AFTER, FEED_LABEL }

/**
 * Результат анализа фото.
 * BOWL_AFTER: заполняется eatenPercent (сколько съедено).
 * FEED_LABEL: заполняются composition (состав) и risks (риски).
 */
data class FoodAnalysis(
    val id: String,
    val kind: PhotoKind,
    val eatenPercent: Int? = null,
    val composition: String? = null,
    val risks: List<String> = emptyList(),
)