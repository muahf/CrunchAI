package com.crunchai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.crunchai.presentation.auth.AuthRoute
import com.crunchai.presentation.diary.DiaryRoute
import com.crunchai.presentation.food.FoodAnalysisRoute
import com.crunchai.presentation.home.HomeRoute
import com.crunchai.presentation.monitor.MonitorRoute
import com.crunchai.presentation.pairing.PairingRoute
import com.crunchai.presentation.pet.PetProfileRoute
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
                onNavigateToPairing = {
                    navController.navigate(CrunchDestination.Pairing(DeviceRole.OBSERVER))
                },
                onNavigateToFoodAnalysis = {
                    navController.navigate(CrunchDestination.FoodAnalysis(FoodAnalysisMode.BOWL))
                },
                onNavigateToPetProfile = { navController.navigate(CrunchDestination.PetProfile) },
                onNavigateToMonitor = { navController.navigate(CrunchDestination.Monitor) },
            )
        }

        composable<CrunchDestination.Station> {
            StationRoute(
                onNavigateToPairing = {
                    navController.navigate(CrunchDestination.Pairing(DeviceRole.STATION))
                },
            )
        }

        composable<CrunchDestination.Diary> {
            DiaryRoute(
                onNavigateToHome = {
                    navController.navigate(CrunchDestination.Home) {
                        popUpTo(CrunchDestination.Home) { inclusive = true }
                    }
                },
            )
        }

        composable<CrunchDestination.Pairing> { entry ->
            val args = entry.toRoute<CrunchDestination.Pairing>()
            PairingRoute(
                role = args.role,
                onBack = { navController.popBackStack() },
                onPairedDone = {
                    navController.navigate(CrunchDestination.Home) {
                        popUpTo(CrunchDestination.Home) { inclusive = true }
                    }
                },
            )
        }

        composable<CrunchDestination.FoodAnalysis> { entry ->
            val args = entry.toRoute<CrunchDestination.FoodAnalysis>()
            FoodAnalysisRoute(
                initialMode = args.mode,
                onBack = { navController.popBackStack() },
            )
        }

        composable<CrunchDestination.PetProfile> {
            PetProfileRoute(
                onNavigateToHome = {
                    navController.navigate(CrunchDestination.Home) {
                        popUpTo(CrunchDestination.Home) { inclusive = true }
                    }
                },
                onNavigateToDiary = {
                    navController.navigate(CrunchDestination.Diary) {
                        popUpTo(CrunchDestination.Home)
                    }
                },
            )
        }

        composable<CrunchDestination.Monitor> {
            MonitorRoute(
                onBack = { navController.popBackStack() },
            )
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
