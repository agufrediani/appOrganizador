
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

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.gps.racebox.RaceBoxBleManager
import com.example.roadbookorganizador.gps.racebox.RaceBoxConnectionStatus
import kotlinx.coroutines.delay

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

    /** Los permisos se piden una sola vez por apertura; si se rechazan, no se insiste en bucle. */
    private var permisosYaSolicitados = false

    override fun onStart() {
        super.onStart()
        verificarPermisosEIniciar()
    }

    override fun onStop() {
        super.onStop()
        // App en segundo plano sin tramo en trazado: apagar GPS y servicio para no gastar batería.
        // Con un tramo abierto el servicio sigue (trazado con pantalla apagada o en otra app).
        if (!isChangingConfigurations && LocationTrackingService.tramoActivoId == null) {
            LocationTrackingService.stopService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Mantener la pantalla encendida en el habitáculo del auto durante el trazado
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            RoadbookOrganizadorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        RoadbookNavGraph()
                        RaceBoxConnectionFloatingBanner(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                        )
                    }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        val ubicacionOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

        if (missing.isNotEmpty() && !permisosYaSolicitados) {
            permisosYaSolicitados = true
            requestPermissionsLauncher.launch(missing.toTypedArray())
        } else if (ubicacionOk) {
            iniciarServicioRastreo()
        }
    }

    private fun iniciarServicioRastreo() {
        LocationTrackingService.startService(this)
    }
}

/**
 * Cartel flotante temporal de notificación que aparece cuando RaceBox se enlaza con éxito.
 * Flota unos segundos con animación suave y se oculta automáticamente.
 */
@Composable
fun RaceBoxConnectionFloatingBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val raceBoxManager = remember { RaceBoxBleManager.getInstance(context) }
    val status by raceBoxManager.connectionStatus.collectAsState()
    val deviceName by raceBoxManager.connectedDeviceName.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == RaceBoxConnectionStatus.CONNECTED) {
            visible = true
            delay(4500L) // Flotar durante 4.5 segundos
            visible = false
        } else {
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(0.92f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF065F46),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GPS EXTERNO CONECTADO",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = Color(0xFF34D399)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF047857)
                        ) {
                            Text(
                                text = "25 Hz GNSS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Enlace activo • Telemetría satelital de alta precisión en vivo",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                IconButton(
                    onClick = { visible = false },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}