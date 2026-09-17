package com.example.roadbookorganizador.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.roadbookorganizador.ui.screens.*
import com.example.roadbookorganizador.ui.viewmodel.*

@Composable
fun RoadbookNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val sesion by authViewModel.sesion.collectAsState()

    val startDest = if (sesion.isLoggedIn) "menu_principal" else "login"

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        // Pantalla de Login / Autenticación
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginExitoso = {
                    navController.navigate("menu_principal") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Pantalla Menú Principal (Dashboard del Rally Activo & Módulos)
        composable("menu_principal") {
            val menuViewModel: MenuPrincipalViewModel = viewModel()
            MenuPrincipalScreen(
                viewModel = menuViewModel,
                sesion = sesion,
                onNavigateToTramos = { navController.navigate("tramos") },
                onNavigateToRallies = { navController.navigate("rallies") },
                onNavigateToMapaLibre = { navController.navigate("mapa_libre") },
                onNavigateToAjustes = { navController.navigate("ajustes") },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate("login") {
                            popUpTo("menu_principal") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Pantalla Lista de Rallies del Organizador
        composable("rallies") {
            val menuViewModel: MenuPrincipalViewModel = viewModel()
            RalliesListScreen(
                viewModel = menuViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        // Pantalla Tramos y Pruebas Especiales del Rally Activo
        composable("tramos") {
            val tramosViewModel: TramosViewModel = viewModel()
            TramosScreen(
                viewModel = tramosViewModel,
                onNavigateToCockpit = { tramoId ->
                    navController.navigate("cockpit/$tramoId")
                },
                onVolver = { navController.popBackStack() }
            )
        }

        // Pantalla Cockpit de Trazado
        composable(
            route = "cockpit/{tramoId}",
            arguments = listOf(navArgument("tramoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tramoId = backStackEntry.arguments?.getLong("tramoId") ?: 0L
            val cockpitViewModel: CockpitViewModel = viewModel()
            CockpitScreen(
                tramoId = tramoId,
                viewModel = cockpitViewModel,
                onVolver = { navController.popBackStack() },
                onNavigateToExportar = { id ->
                    navController.navigate("exportar/$id")
                }
            )
        }

        // Pantalla Explorador y Mapa Libre con POIs
        composable("mapa_libre") {
            val mapaViewModel: MapaLibreViewModel = viewModel()
            MapaLibreScreen(
                viewModel = mapaViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        // Pantalla de Ajustes (Engranaje)
        composable("ajustes") {
            val calibracionViewModel: CalibracionViewModel = viewModel()
            AjustesScreen(
                calibracionViewModel = calibracionViewModel,
                onVolver = { navController.popBackStack() },
                onNavigateToCalibrador = { navController.navigate("calibracion") }
            )
        }

        // Pantalla Calibración de Odómetro (1.000m)
        composable("calibracion") {
            val calibracionViewModel: CalibracionViewModel = viewModel()
            CalibracionScreen(
                viewModel = calibracionViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        // Pantalla Exportar GPX / JSON
        composable(
            route = "exportar/{tramoId}",
            arguments = listOf(navArgument("tramoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tramoId = backStackEntry.arguments?.getLong("tramoId") ?: 0L
            val cockpitViewModel: CockpitViewModel = viewModel()
            ExportarScreen(
                tramoId = tramoId,
                viewModel = cockpitViewModel,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
