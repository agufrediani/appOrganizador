package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.roadbookorganizador.service.OdometerState
import com.example.roadbookorganizador.ui.theme.*

@Composable
fun DialogoTelemetriaOdometro(
    odoState: OdometerState,
    onResetParcial: () -> Unit,
    onResetTotal: () -> Unit,
    onAjustarMetros: (Double) -> Unit,
    onToggleModoReverso: () -> Unit = {},
    onToggleAutoPcs: () -> Unit = {},
    onNavigateToCalibrador: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // CABECERA MODAL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = FredianiCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = FredianiCyan, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TELEMETRÍA & ODÓMETRO",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "Instrumentación de Precisión Frediani Competición",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                HorizontalDivider(color = cardBorder)

                // 1. HARDWARE GNSS & RACEBOX 25 Hz
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) RallySurface else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (odoState.raceBoxConectado) Color(0xFF34D399) else FredianiGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (odoState.raceBoxConectado) "GPS EXTERNO (25 Hz)" else "GPS TABLET",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (odoState.raceBoxConectado) (if (isDark) RallyCyan else FredianiCyanText) else FredianiGreenText
                                )
                            }

                            if (odoState.raceBoxBateriaPct != null) {
                                Text(
                                    text = "🔋 ${odoState.raceBoxBateriaPct}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "🛰️ Satélites: ${odoState.raceBoxSatelites ?: 28} SVs",
                                fontSize = 12.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "🎯 Precisión: ${String.format("%.2f", odoState.precisionMetros)} m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) RallyCyan else FredianiCyanText
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "⚡ Velocidad: ${String.format("%.1f", odoState.velocidadKmh)} km/h",
                                fontSize = 12.sp,
                                color = textPrimary
                            )
                            Text(
                                text = "🧭 Rumbo: ${String.format("%.0f°", odoState.rumbo)}",
                                fontSize = 12.sp,
                                color = textPrimary
                            )
                        }

                        Text(
                            text = "📍 Coordenadas: ${String.format("%.5f, %.5f", odoState.latitud, odoState.longitud)} • Altitud: ${String.format("%.0f", odoState.altitud)}m",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = textSecondary
                        )
                    }
                }

                // 2. CONTADORES DE ODÓMETRO Y PUESTA A CERO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Odómetro Total
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ODÓMETRO TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Text(
                                text = String.format("%.3f", odoState.odometroTotalKm),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) RallyAccentYellow else FredianiAmberText
                            )
                            Text("KILÓMETROS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        }
                    }

                    // Odómetro Parcial
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ODÓMETRO PARCIAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Text(
                                text = String.format("%.3f", odoState.odometroParcialKm),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) RallyCyan else FredianiCyanText
                            )
                            Text("KILÓMETROS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        }
                    }
                }

                // BOTONES DE RESET
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onResetParcial,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) RallyCyan else FredianiCyanText)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET PARCIAL (0.00)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onResetTotal,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RallyRed)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET TOTAL (0.00)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // MODO REVERSO & FILTRO ANTI-JITTER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (odoState.modoReverso) Color(0xFF7F1D1D) else (if (isDark) RallySurface else Color(0xFFF1F5F9)),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (odoState.modoReverso) "◀ ODÓMETRO REVERSO (RESTANDO)" else "▶ SENTIDO NORMAL (SUMANDO)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = if (odoState.modoReverso) Color(0xFFFECACA) else textPrimary
                        )
                        Text(
                            text = if (odoState.modoReverso) "La distancia recorrida se resta para retroceder" else "Filtro anti-jitter: < 2.0 km/h estático",
                            fontSize = 10.sp,
                            color = if (odoState.modoReverso) Color(0xFFFCA5A5) else textSecondary
                        )
                    }
                    Button(
                        onClick = onToggleModoReverso,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (odoState.modoReverso) RallyRed else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (odoState.modoReverso) "DESACTIVAR" else "ACTIVAR REVERSO",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }

                // AUTO-WAYPOINTS (AUTO-WPS)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) RallySurface else Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎯 Auto-WPs (WayPoints)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Genera WP a ~100m y luego periódicos cada ~350m (±15%)",
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                    Switch(
                        checked = odoState.autoPcsHabilitado,
                        onCheckedChange = { onToggleAutoPcs() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFA855F7),
                            checkedTrackColor = Color(0xFF581C87)
                        )
                    )
                }

                // 3. AJUSTE FINO DE METROS AL PASO (+/-10m, +/-50m)
                Text(
                    text = "CALIBRACIÓN FINA AL PASO DE REFERENCIAS:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textSecondary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { onAjustarMetros(-0.050) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("-50m", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = { onAjustarMetros(-0.010) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("-10m", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = { onAjustarMetros(0.010) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+10m", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    FilledTonalButton(
                        onClick = { onAjustarMetros(0.050) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+50m", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                // 4. ACCESO AL CALIBRADOR DE TRAMO TESTIGO
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToCalibrador()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9))
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calibrador Oficial de Tramo Testigo (1.000m)", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
