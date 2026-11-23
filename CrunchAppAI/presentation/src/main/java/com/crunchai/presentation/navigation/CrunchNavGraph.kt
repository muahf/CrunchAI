package com.crunchai.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crunchai.presentation.home.HomeRoute

@Composable
fun CrunchNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = CrunchDestination.Home.route,
        modifier = modifier,
    ) {
        composable(route = CrunchDestination.Home.route) {
            HomeRoute(
                onNavigateToPairing = { navController.navigate(CrunchDestination.Pairing.route) },
                onNavigateToFoodAnalysis = { navController.navigate(CrunchDestination.FoodAnalysis.route) },
                onNavigateToPetProfile = { navController.navigate(CrunchDestination.PetProfile.route) },
            )
        }

        composable(route = CrunchDestination.Pairing.route) {
            PlaceholderScreen(title = "Сопряжение устройств")
        }

        composable(route = CrunchDestination.FoodAnalysis.route) {
            PlaceholderScreen(title = "Анализ корма по фото")
        }

        composable(route = CrunchDestination.PetProfile.route) {
            PlaceholderScreen(title = "Профиль питомца")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
