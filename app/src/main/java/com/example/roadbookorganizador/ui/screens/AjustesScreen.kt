package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.CalibracionViewModel

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

    val calibracionActiva by calibracionViewModel.calibracionActiva.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AJUSTES Y CONFIGURACIÓN",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = RallyCyanLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RallyDarkBg)
            )
        },
        containerColor = RallyDarkBg
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
                color = RallyCyan,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCalibrador() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg)
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
                                color = Color.White
                            )
                            Text(
                                text = "Factor activo: k = ${String.format("%.4f", calibracionActiva?.factorCorreccion ?: 1.0)}",
                                fontSize = 12.sp,
                                color = RallyAccentYellow
                            )
                            Text(
                                text = calibracionActiva?.vehiculoNombre ?: "Vehículo no configurado",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            // SECCIÓN HARDWARE GNSS & SENSORES
            Text(
                text = "CONEXIONES Y HARDWARE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RallyCyan,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Receptor GNSS / GPS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text("Antena Interna del Dispositivo (1 Hz)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RallyGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = RallyGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = RallySurface)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Antena Externa Bluetooth (10 Hz)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text("Garmin GLO 2 / Dual XGPS160", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        TextButton(onClick = {}) {
                            Text("Vincular", color = RallyCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECCIÓN SERVIDOR WEB Y SINCRONIZACIÓN
            Text(
                text = "SERVIDOR WEB Y SINCRONIZACIÓN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RallyCyan,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("URL Servidor Plataforma", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text(serverUrl, fontSize = 12.sp, color = RallyCyanLight, fontWeight = FontWeight.Medium)
                            Text("Red Wi-Fi Local (PC de Control)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Button(
                            onClick = { 
                                tempUrl = serverUrl
                                showServerDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RallySurface),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // SECCIÓN INFORMACIÓN Y LICENCIA
            Text(
                text = "LICENCIA Y VERSIÓN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RallyCyan,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RallyCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Frediani Roadbook Digital - Organizador v1.0", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Licencia Oficial Profesional de Relevamiento", color = RallyCyanLight, fontSize = 12.sp)
                    Text("Desarrollado para Frediani Competición y Clubes Fiscalizadores.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showServerDialog) {
            AlertDialog(
                onDismissRequest = { showServerDialog = false },
                title = {
                    Text("Configurar Servidor Web", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Ingresa la dirección IP y puerto de la PC donde corre 'rally_web' (ambos conectados al mismo Wi-Fi):",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            label = { Text("Ej: http://192.168.1.10:8000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = RallyCyan,
                                unfocusedBorderColor = RallySurface
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
                containerColor = RallyCardBg
            )
        }
    }
}
