package com.crunchai.presentation.navigation

import kotlinx.serialization.Serializable

/** Режим экрана «Фото»: что снимаем. Это аргумент навигации. */
@Serializable
enum class FoodAnalysisMode { BOWL, LABEL }

/**
 * Все экраны приложения. Аргументы передаются как поля класса,
 * строки с маршрутами писать не нужно.
 */
sealed interface CrunchDestination {
    @Serializable
    data object Home : CrunchDestination
    @Serializable
    data object Auth : CrunchDestination
    @Serializable
    data object Diary : CrunchDestination

    @Serializable
    data object Station : CrunchDestination

    @Serializable
    data object Pairing : CrunchDestination

    @Serializable
    data object PetProfile : CrunchDestination

    @Serializable
    data class FoodAnalysis(
        val mode: FoodAnalysisMode = FoodAnalysisMode.BOWL,
    ) : CrunchDestination
}