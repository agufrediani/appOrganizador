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
import com.example.roadbookorganizador.ui.components.MarcoRally
import com.example.roadbookorganizador.ui.components.PestanaRally
import com.example.roadbookorganizador.ui.viewmodel.*

@Composable
fun RoadbookNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val sesion by authViewModel.sesion.collectAsState()

    // Si la sesión se cierra (token vencido o el servidor contestó 401), se vuelve al login
    androidx.compose.runtime.LaunchedEffect(sesion.isLoggedIn) {
        if (!sesion.isLoggedIn) {
            val ruta = navController.currentDestination?.route
            if (ruta != null && ruta != "login" && ruta != "splash") {
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            }
        }
    }

    /** Cambiar de pestaña adentro del rally sin apilar pantallas (atrás vuelve al Panel y después a Mis Rallies). */
    fun irAPestana(p: PestanaRally) {
        when (p) {
            PestanaRally.PANEL -> navController.popBackStack("menu_principal", inclusive = false)
            PestanaRally.TRAMOS, PestanaRally.ZONAS -> {
                val ruta = if (p == PestanaRally.TRAMOS) "tramos" else "mapa_libre"
                navController.navigate(ruta) {
                    popUpTo("menu_principal")
                    launchSingleTop = true
                }
            }
        }
    }

    /** La cabecera y las pestañas de la plataforma, iguales en las tres pestañas del rally. */
    @Composable
    fun Marco(menuViewModel: MenuPrincipalViewModel, pestana: PestanaRally, contenido: @Composable () -> Unit) {
        val rallyActivo by menuViewModel.activeRally.collectAsState()
        val rallies by menuViewModel.rallies.collectAsState()
        MarcoRally(
            sesion = sesion,
            rallyActivo = rallyActivo,
            rallies = rallies,
            pestana = pestana,
            onPestana = { irAPestana(it) },
            onMisRallies = { navController.popBackStack("seleccion_rally", inclusive = false) },
            onElegirRally = { r -> menuViewModel.seleccionarRallyActivo(r.id) },
            onPreferencias = { navController.navigate("ajustes") },
            onLogout = {
                authViewModel.logout {
                    navController.navigate("login") { popUpTo("seleccion_rally") { inclusive = true } }
                }
            },
            contenido = contenido
        )
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Pantalla de Splash / Carga Oficial (GIF animado de Frediani Roadbook)
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    val dest = if (sesion.isLoggedIn) "seleccion_rally" else "login"
                    navController.navigate(dest) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Login / Autenticación
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginExitoso = {
                    navController.navigate("seleccion_rally") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Pantalla 1: Selección y Estado del Rally Asignado (Puerta de Entrada)
        composable("seleccion_rally") {
            val menuViewModel: MenuPrincipalViewModel = viewModel()
            SeleccionRallyScreen(
                viewModel = menuViewModel,
                sesion = sesion,
                onIngresarAlRally = { navController.navigate("menu_principal") },
                onNavigateToAjustes = { navController.navigate("ajustes") },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate("login") {
                            popUpTo("seleccion_rally") { inclusive = true }
                        }
                    }
                }
            )
        }

        // ADENTRO DE UN RALLY: las tres pestañas de la plataforma (Panel General, Tramos y Zonas
        // y puntos de interés), siempre con la misma cabecera (MarcoRally).
        composable("menu_principal") {
            val menuViewModel: MenuPrincipalViewModel = viewModel()
            Marco(menuViewModel, PestanaRally.PANEL) {
                PanelGeneralRally(viewModel = menuViewModel, onVerZonas = { irAPestana(PestanaRally.ZONAS) })
            }
        }

        // Pantalla Lista de Rallies del Organizador
        composable("rallies") {
            val menuViewModel: MenuPrincipalViewModel = viewModel()
            RalliesListScreen(
                viewModel = menuViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        composable("tramos") {
            val menuViewModel: MenuPrincipalViewModel = viewModel()
            val tramosViewModel: TramosViewModel = viewModel()
            Marco(menuViewModel, PestanaRally.TRAMOS) {
                TramosScreen(
                    viewModel = tramosViewModel,
                    onNavigateToCockpit = { tramoId -> navController.navigate("cockpit/$tramoId") },
                    onVolver = { navController.popBackStack() },
                    conCabeceraPropia = false
                )
            }
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
                },
                onNavigateToCalibrar = {
                    navController.navigate("calibracion")
                }
            )
        }

        // Pantalla Explorador y Mapa Libre con POIs
        composable("mapa_libre") {
            val menuViewModel: MenuPrincipalViewModel = viewModel()
            val mapaViewModel: MapaLibreViewModel = viewModel()
            Marco(menuViewModel, PestanaRally.ZONAS) {
                MapaLibreScreen(
                    viewModel = mapaViewModel,
                    onVolver = { navController.popBackStack() },
                    conCabeceraPropia = false
                )
            }
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
