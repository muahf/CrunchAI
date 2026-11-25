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
import androidx.navigation.toRoute
import com.crunchai.presentation.auth.AuthRoute
import com.crunchai.presentation.home.HomeRoute
import com.crunchai.presentation.station.StationRoute
import com.example.crunchai.domain.model.DeviceRole

@Composable
fun CrunchNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = CrunchDestination.Auth,
        modifier = modifier,
    ) {
        composable<CrunchDestination.Home> {
            HomeRoute(
                onNavigateToDiary = { navController.navigate(CrunchDestination.Diary) },
                onNavigateToPairing = { navController.navigate(CrunchDestination.Pairing) },
                onNavigateToFoodAnalysis = {
                    navController.navigate(CrunchDestination.FoodAnalysis(FoodAnalysisMode.BOWL))
                },
                onNavigateToPetProfile = { navController.navigate(CrunchDestination.PetProfile) },
            )
        }

        composable<CrunchDestination.Station> {
            StationRoute(
                onNavigateToPairing = { navController.navigate(CrunchDestination.Pairing) },
            )
        }

        composable<CrunchDestination.Diary> {
            PlaceholderScreen(title = "Дневник")
        }

        composable<CrunchDestination.Pairing> {
            PlaceholderScreen(title = "Сопряжение устройств")
        }

        composable<CrunchDestination.FoodAnalysis> { entry ->
            val args = entry.toRoute<CrunchDestination.FoodAnalysis>()
            PlaceholderScreen(title = "Анализ корма: ${args.mode}")
        }

        composable<CrunchDestination.PetProfile> {
            PlaceholderScreen(title = "Профиль питомца")
        }

        composable<CrunchDestination.Auth> {
            AuthRoute(
                onAuthenticated = { role ->
                    val destination: CrunchDestination =
                        if (role == DeviceRole.STATION) {
                            CrunchDestination.Station
                        } else {
                            CrunchDestination.Home
                        }
                    navController.navigate(destination) {
                        popUpTo(CrunchDestination.Auth) { inclusive = true }
                    }
                },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}