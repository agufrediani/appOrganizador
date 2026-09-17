package com.example.roadbookorganizador

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.roadbookorganizador.service.LocationTrackingService
import com.example.roadbookorganizador.ui.navigation.RoadbookNavGraph
import com.example.roadbookorganizador.ui.theme.RallyDarkBg
import com.example.roadbookorganizador.ui.theme.RoadbookOrganizadorTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            iniciarServicioRastreo()
        }
    }

    override fun onResume() {
        super.onResume()
        verificarPermisosEIniciar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mantener la pantalla encendida en el habitáculo del auto durante el trazado
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        verificarPermisosEIniciar()

        setContent {
            RoadbookOrganizadorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = RallyDarkBg
                ) {
                    RoadbookNavGraph()
                }
            }
        }
    }

    private fun verificarPermisosEIniciar() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            iniciarServicioRastreo()
        } else {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    private fun iniciarServicioRastreo() {
        LocationTrackingService.startService(this)
    }
}