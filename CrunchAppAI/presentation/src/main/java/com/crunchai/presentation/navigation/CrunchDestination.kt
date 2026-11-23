package com.crunchai.presentation.navigation

sealed interface CrunchDestination {
    val route: String

    data object Home : CrunchDestination {
        override val route: String = "home"
    }

    data object Pairing : CrunchDestination {
        override val route: String = "pairing"
    }

    data object FoodAnalysis : CrunchDestination {
        override val route: String = "food_analysis"
    }

    data object PetProfile : CrunchDestination {
        override val route: String = "pet_profile"
    }
}
