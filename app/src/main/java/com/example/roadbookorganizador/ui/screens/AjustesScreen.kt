package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.roadbookorganizador.gps.racebox.DiscoveredRaceBox
import com.example.roadbookorganizador.gps.racebox.RaceBoxBleManager
import com.example.roadbookorganizador.gps.racebox.RaceBoxConnectionStatus
import com.example.roadbookorganizador.ui.viewmodel.CalibracionViewModel
import com.example.roadbookorganizador.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    calibracionViewModel: CalibracionViewModel,
    onVolver: () -> Unit,
    onNavigateToCalibrador: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.roadbookorganizador.data.local.SessionManager(context) }
    val serverUrl by sessionManager.serverUrl.collectAsState()
    var showServerDialog by remember { mutableStateOf(false) }
    var tempUrl by remember { mutableStateOf("") }

    val mapboxToken by sessionManager.mapboxToken.collectAsState()
    val snapToRoad by sessionManager.snapToRoad.collectAsState()
    var showMapboxDialog by remember { mutableStateOf(false) }
    var tempMapboxToken by remember { mutableStateOf("") }

    val calibracionActiva by calibracionViewModel.calibracionActiva.collectAsState()

    // Gestor RaceBox BLE 25 Hz
    val raceBoxManager = remember { RaceBoxBleManager.getInstance(context) }
    val rbStatus by raceBoxManager.connectionStatus.collectAsState()
    val rbConnectedName by raceBoxManager.connectedDeviceName.collectAsState()
    val rbTelemetry by raceBoxManager.latestTelemetry.collectAsState()
    val rbDiscovered by raceBoxManager.discoveredDevices.collectAsState()
    val isSimulating by raceBoxManager.isSimulating.collectAsState()

    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = bluetoothManager?.adapter

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val isEnabled = bm?.adapter?.isEnabled == true || result.resultCode == android.app.Activity.RESULT_OK
        if (isEnabled) {
            Toast.makeText(context, "Bluetooth activado. Buscando GPS Externo...", Toast.LENGTH_SHORT).show()
            raceBoxManager.startScan()
        } else {
            Toast.makeText(context, "Bluetooth no activado. Se requiere Bluetooth para conectar el GPS Externo.", Toast.LENGTH_LONG).show()
        }
    }

    val blePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (allGranted) {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter
            if (adapter != null && !adapter.isEnabled) {
                try {
                    @Suppress("DEPRECATION")
                    adapter.enable()
                } catch (_: Exception) {}

                if (adapter.isEnabled) {
                    Toast.makeText(context, "Bluetooth activado. Buscando GPS Externo...", Toast.LENGTH_SHORT).show()
                    raceBoxManager.startScan()
                } else {
                    try {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        enableBluetoothLauncher.launch(enableBtIntent)
                    } catch (e: Exception) {
                        raceBoxManager.startScan()
                    }
                }
            } else {
                raceBoxManager.startScan()
            }
        } else {
            Toast.makeText(context, "Se necesitan permisos de Bluetooth para detectar el GPS Externo.", Toast.LENGTH_LONG).show()
        }
    }

    fun startRaceBoxScanWithPermissions() {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter
        if (adapter == null) {
            Toast.makeText(context, "Este dispositivo no cuenta con módulo Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        // 1. Permisos en tiempo de ejecución (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val connectGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (!scanGranted || !connectGranted) {
                blePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
                return
            }
        }

        // 2. Encendido de Bluetooth forzado/asistido si está apagado
        if (!adapter.isEnabled) {
            var turnedOn = false
            try {
                @Suppress("DEPRECATION")
                turnedOn = adapter.enable()
            } catch (_: Exception) {}

            if (turnedOn || adapter.isEnabled) {
                Toast.makeText(context, "Activando Bluetooth...", Toast.LENGTH_SHORT).show()
                raceBoxManager.startScan()
            } else {
                try {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                } catch (e: Exception) {
                    try {
                        val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        context.startActivity(settingsIntent)
                        Toast.makeText(context, "Por favor active Bluetooth para conectar el GPS Externo", Toast.LENGTH_LONG).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Active Bluetooth para buscar el GPS Externo", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Buscando GPS Externo...", Toast.LENGTH_SHORT).show()
            raceBoxManager.startScan()
        }
    }

    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AJUSTES Y CONFIGURACIÓN",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (isDark) RallyCyanLight else FredianiNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { ThemeManager.toggleTheme() }) {
                        Text(text = if (isDark) "☀️" else "🌙", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECCIÓN CALIBRADOR DE ODÓMETRO (AQUÍ ESTÁ GUARDADO)
            Text(
                text = "CALIBRACIÓN DE INSTRUMENTOS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) RallyCyan else FredianiCyanText,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCalibrador() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RallyAccentYellow.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = RallyAccentYellow, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Calibrador de Odómetro (1.000m)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "Factor activo: k = ${String.format("%.4f", calibracionActiva?.factorCorreccion ?: 1.0)}",
                                fontSize = 12.sp,
                                color = if (isDark) RallyAccentYellow else FredianiAmberText
                            )
                            Text(
                                text = calibracionActiva?.vehiculoNombre ?: "Vehículo no configurado",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textSecondary)
                }
            }

            // SECCIÓN HARDWARE GNSS & SENSORES
            Text(
                text = "CONEXIONES Y HARDWARE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) RallyCyan else FredianiCyanText,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // GPS Tablet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GPS Tablet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                            Text(
                                if (rbStatus == RaceBoxConnectionStatus.CONNECTED) "Modo Respaldo automático" else "GPS integrado en uso",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (rbStatus == RaceBoxConnectionStatus.CONNECTED) (if (isDark) Color.Gray.copy(alpha = 0.2f) else Color(0xFFF1F5F9)) else RallyGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (rbStatus == RaceBoxConnectionStatus.CONNECTED) "STANDBY" else "ACTIVO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (rbStatus == RaceBoxConnectionStatus.CONNECTED) (if (isDark) Color.LightGray else Color.Gray) else RallyGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = cardBorder)

                    // GPS Externo 25Hz
                    when (rbStatus) {
                        RaceBoxConnectionStatus.CONNECTED -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "GPS Externo (25 Hz)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isDark) RallyCyanLight else FredianiCyanText
                                        )
                                        Text(
                                            text = "Transmisión de alta precisión a 25 Hz activa",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = RallyGreen.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = if (isSimulating) "DEMO 25 Hz" else "25 Hz ACTIVO",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = RallyGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                if (isSimulating) {
                                                    raceBoxManager.stopSimulation()
                                                } else {
                                                    raceBoxManager.disconnect()
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = RallyRed)
                                        ) {
                                            Text("Desconectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                rbTelemetry?.let { t ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) RallySurface.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("🛰️ Satélites: ${t.satellitesCount} SVs", fontSize = 12.sp, color = textPrimary)
                                                Text("🎯 Precisión: ${String.format("%.2f", t.horizontalAccuracyMeters)}m", fontSize = 12.sp, color = if (isDark) RallyCyan else FredianiCyanText)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    if (t.isMicroVoltage) "⚡ Voltaje: ${String.format("%.1fV", t.inputVoltageVolts)}"
                                                    else "🔋 Batería: ${t.batteryPercent}% ${if (t.isCharging) "(Cargando)" else ""}",
                                                    fontSize = 12.sp,
                                                    color = textPrimary
                                                )
                                                Text("⚡ Vel: ${String.format("%.1f", t.speedKmh)} km/h", fontSize = 12.sp, color = textPrimary)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(
                                                    "🏎️ G: X=${String.format("%.2f", t.gForceX)} | Y=${String.format("%.2f", t.gForceY)} | Z=${String.format("%.2f", t.gForceZ)}",
                                                    fontSize = 11.sp,
                                                    color = textSecondary
                                                )
                                                Text("🧭 Rumbo: ${String.format("%.0f°", t.headingDegrees)}", fontSize = 11.sp, color = textSecondary)
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { raceBoxManager.disconnect() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RallyRed),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Desconectar GPS Externo", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        RaceBoxConnectionStatus.CONNECTING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = RallyCyan)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Conectando a GPS Externo...", color = textPrimary, fontSize = 13.sp)
                                }
                                TextButton(onClick = { raceBoxManager.disconnect() }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                            }
                        }

                        RaceBoxConnectionStatus.SCANNING -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = RallyCyan)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Buscando GPS Externo...", color = textPrimary, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { raceBoxManager.stopScan() }) {
                                        Text("Detener", color = RallyRed)
                                    }
                                }

                                if (rbDiscovered.isEmpty()) {
                                    Text(
                                        text = "Asegúrate de que el GPS Externo esté encendido cerca de la tablet.",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                } else {
                                    rbDiscovered.forEach { dev ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isDark) RallySurface else Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                                .clickable { raceBoxManager.connect(dev.address) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = RallyCyan)
                                                Column {
                                                    Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                                                    Text("${dev.address} | Señal: ${dev.rssi} dBm", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                            Button(
                                                onClick = { raceBoxManager.connect(dev.address) },
                                                colors = ButtonDefaults.buttonColors(containerColor = RallyCyan, contentColor = Color.Black),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Conectar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        RaceBoxConnectionStatus.DISCONNECTED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("GPS Externo (25 Hz)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                                    Text("Conexión inalámbrica de alta precisión", fontSize = 12.sp, color = textSecondary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { raceBoxManager.startSimulation() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) RallyCyanLight else FredianiCyanText),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Simular 25Hz", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { startRaceBoxScanWithPermissions() },
                                        colors = ButtonDefaults.buttonColors(containerColor = RallyCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Vincular", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN CARTOGRAFÍA SATELITAL & MAPBOX (SNAP-TO-ROAD)
            Text(
                text = "CARTOGRAFÍA SATELITAL & MAPBOX",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) RallyCyan else FredianiCyanText,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Token Mapbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mapbox Public Access Token", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                            Text(
                                if (mapboxToken.isNotBlank()) "${mapboxToken.take(16)}... (Activo)" else "No configurado (Respaldo Google Hybrid HD activo)",
                                fontSize = 12.sp,
                                color = if (mapboxToken.isNotBlank()) RallyGreen else (if (isDark) RallyAccentYellow else FredianiAmberText),
                                fontWeight = FontWeight.Medium
                            )
                            Text("Activa Mapbox Satellite Streets HD por defecto y Map Matching", fontSize = 11.sp, color = textSecondary)
                        }
                        Button(
                            onClick = {
                                tempMapboxToken = mapboxToken
                                showMapboxDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Token", fontSize = 12.sp, color = textPrimary)
                        }
                    }

                    HorizontalDivider(color = cardBorder)

                    // Snap-to-Road por defecto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Snap-to-Road Inteligente", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                            Text(
                                if (snapToRoad) "Ajusta automáticamente las marcas y trazas a la red de caminos" else "Modo Fuera de Pista (Off-road directo)",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                        Switch(
                            checked = snapToRoad,
                            onCheckedChange = { sessionManager.guardarSnapToRoad(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = RallyCyan
                            )
                        )
                    }
                }
            }

            // SECCIÓN SERVIDOR WEB Y SINCRONIZACIÓN
            Text(
                text = "SERVIDOR WEB Y SINCRONIZACIÓN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) RallyCyan else FredianiCyanText,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("URL Servidor Plataforma", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                            Text(serverUrl, fontSize = 12.sp, color = if (isDark) RallyCyanLight else FredianiCyanText, fontWeight = FontWeight.Medium)
                            Text("Red Wi-Fi Local (PC de Control)", fontSize = 11.sp, color = textSecondary)
                        }
                        Button(
                            onClick = { 
                                tempUrl = serverUrl
                                showServerDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar", fontSize = 12.sp, color = textPrimary)
                        }
                    }
                }
            }

            // SECCIÓN INFORMACIÓN Y LICENCIA
            Text(
                text = "LICENCIA Y VERSIÓN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) RallyCyan else FredianiCyanText,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Frediani Roadbook Digital - Organizador v1.0", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                    Text("Licencia Oficial Profesional de Relevamiento", color = if (isDark) RallyCyanLight else FredianiCyanText, fontSize = 12.sp)
                    Text("Desarrollado para Frediani Competición y Clubes Fiscalizadores.", color = textSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
                title = {
                    Text("Configurar Servidor Web", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Ingresa la dirección IP y puerto de la PC donde corre 'rally_web' (ambos conectados al mismo Wi-Fi):",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            label = { Text("Ej: http://192.168.1.10:8000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = RallyCyan,
                                unfocusedBorderColor = cardBorder
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempUrl.isNotBlank()) {
                                sessionManager.guardarServerUrl(tempUrl)
                            }
                            showServerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RallyCyan)
                    ) {
                        Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showServerDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = cardBg
            )
        }

        if (showMapboxDialog) {
            AlertDialog(
                onDismissRequest = { showMapboxDialog = false },
                title = {
                    Text("Configurar Mapbox Token", fontWeight = FontWeight.Bold, color = textPrimary)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Ingresa tu Public Access Token de Mapbox (comienza con 'pk.eyJ...'). Puedes obtener uno gratis con hasta 50.000 cargas al mes en mapbox.com:",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                        OutlinedTextField(
                            value = tempMapboxToken,
                            onValueChange = { tempMapboxToken = it },
                            label = { Text("Mapbox Public Token (pk.eyJ...)") },
                            placeholder = { Text("pk.eyJ1Ijo...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = RallyCyan,
                                unfocusedBorderColor = cardBorder
                            )
                        )
                        if (tempMapboxToken.isNotBlank()) {
                            TextButton(
                                onClick = { tempMapboxToken = "" },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Borrar token (usar respaldo Google Hybrid)", fontSize = 11.sp, color = RallyRed)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sessionManager.guardarMapboxToken(tempMapboxToken)
                            showMapboxDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RallyCyan)
                    ) {
                        Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMapboxDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = cardBg
            )
        }
    }
}

