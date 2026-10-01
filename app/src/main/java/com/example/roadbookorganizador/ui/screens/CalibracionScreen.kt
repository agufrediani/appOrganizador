package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.CalibracionPaso
import com.example.roadbookorganizador.ui.viewmodel.CalibracionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibracionScreen(
    viewModel: CalibracionViewModel,
    onVolver: () -> Unit
) {
    val paso by viewModel.paso.collectAsState()
    val odoState by viewModel.odoState.collectAsState()
    val distanciaMedida by viewModel.distanciaMedidaMetros.collectAsState()
    val factorCalculado by viewModel.factorCalculado.collectAsState()
    val calibracionActiva by viewModel.calibracionActiva.collectAsState()

    var vehiculoNombre by remember { mutableStateOf("Camioneta Trazador 000") }
    var distanciaOficialTexto by remember { mutableStateOf("1000") }

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
                        text = "CALIBRACIÓN DE ODÓMETRO",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (isDark) RallyCyanLight else FredianiNavy
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textPrimary)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Estado Actual
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Factor de Corrección Activo", fontSize = 12.sp, color = textSecondary)
                        Text(
                            text = String.format("k = %.4f", calibracionActiva?.factorCorreccion ?: 1.0),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) RallyAccentYellow else FredianiAmberText
                        )
                        Text(
                            text = calibracionActiva?.vehiculoNombre ?: "Sin calibración previa (1.0000)",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (isDark) RallyCyan else FredianiCyanText,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Explicación guiada
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PROCEDIMIENTO OFICIAL (TRAMO TESTIGO):", fontWeight = FontWeight.Bold, color = if (isDark) RallyCyan else FredianiCyanText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("1. Ubique el vehículo sobre el mojón/jalón oficial de 0 metros.", color = textPrimary, fontSize = 13.sp)
                    Text("2. Pulse 'INICIAR RECORRIDO' y avance hasta el mojón final (1.000 m).", color = textPrimary, fontSize = 13.sp)
                    Text("3. Al detenerse con el eje delantero en la marca, pulse 'FINALIZAR'.", color = textPrimary, fontSize = 13.sp)
                }
            }

            // PANTALLA DINÁMICA SEGÚN EL PASO
            when (paso) {
                CalibracionPaso.LISTO_PARA_INICIAR -> {
                    OutlinedTextField(
                        value = vehiculoNombre,
                        onValueChange = { vehiculoNombre = it },
                        label = { Text("Nombre del Vehículo / Equipo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = RallyCyan,
                            unfocusedBorderColor = cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = distanciaOficialTexto,
                        onValueChange = { distanciaOficialTexto = it },
                        label = { Text("Distancia Oficial del Tramo Testigo (Metros)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = RallyCyan,
                            unfocusedBorderColor = cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { viewModel.iniciarRecorridoCalibracion() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RallyGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INICIAR RECORRIDO DE CALIBRACIÓN", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }

                CalibracionPaso.EN_MARCHA -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("DISTANCIA SATELITAL MEDIDA", color = textSecondary, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format("%.1f m", odoState.odometroTotalKm * 1000.0),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) RallyAccentYellow else FredianiAmberText
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = String.format("Velocidad: %.0f km/h | Rumbo: %.0f°", odoState.velocidadKmh, odoState.rumbo),
                                color = textPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val oficial = distanciaOficialTexto.toDoubleOrNull() ?: 1000.0
                            viewModel.finalizarRecorridoCalibracion(oficial, vehiculoNombre)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RallyAccentAmber),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FINALIZAR EN JALÓN 1.000M", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }

                CalibracionPaso.FINALIZADO -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RallyGreen, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("¡CALIBRACIÓN GUARDADA!", fontWeight = FontWeight.Black, color = textPrimary, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Distancia Oficial: ${distanciaOficialTexto} m", color = textPrimary, fontSize = 14.sp)
                            Text("Distancia GPS Medida: ${String.format("%.1f m", distanciaMedida)}", color = textPrimary, fontSize = 14.sp)

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Nuevo Factor de Calibración:", color = textSecondary, fontSize = 12.sp)
                            Text(
                                text = String.format("k = %.4f", factorCalculado),
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                color = if (isDark) RallyAccentYellow else FredianiAmberText
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.reiniciarCalibrador() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) RallySurface else Color(0xFFE2E8F0))
                        ) {
                            Text("Repetir", color = textPrimary)
                        }
                        Button(
                            onClick = onVolver,
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RallyCyan)
                        ) {
                            Text("Aceptar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
