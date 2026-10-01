package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.DialogoPuntoManualState

data class OpcionTipoPunto(
    val tipo: String,
    val label: String,
    val icono: ImageVector,
    val color: Color
)

val TIPOS_PUNTO_RALLY = listOf(
    OpcionTipoPunto("INICIO", "INICIO / LARGADA", Icons.Default.SportsScore, Color(0xFF10B981)),
    OpcionTipoPunto("WAYPOINT", "WAYPOINT", Icons.Default.Navigation, Color(0xFF00B4D8)),
    OpcionTipoPunto("FINAL", "FINAL / LLEGADA", Icons.Default.Flag, Color(0xFFE11D48)),
    OpcionTipoPunto("ZONA DE ESPECTADORES", "ZONA ESPECTADORES", Icons.Default.Groups, Color(0xFFFFB703)),
    OpcionTipoPunto("PARQUE DE ASISTENCIA", "PARQUE ASISTENCIA", Icons.Default.Build, Color(0xFF8B5CF6)),
    OpcionTipoPunto("AMBULANCIA", "AMBULANCIA / MÉDICO", Icons.Default.LocalHospital, Color(0xFFEF4444)),
    OpcionTipoPunto("HIDRATACIÓN", "HIDRATACIÓN", Icons.Default.WaterDrop, Color(0xFF0284C7)),
    OpcionTipoPunto("BOMBEROS", "BOMBEROS / RESCATE", Icons.Default.FireTruck, Color(0xFFF97316)),
    OpcionTipoPunto("CONTROL HORARIO (CH)", "CONTROL HORARIO (CH)", Icons.Default.Timer, Color(0xFFEAB308)),
    OpcionTipoPunto("RESCATE 4X4", "RESCATE 4X4", Icons.Default.Sos, Color(0xFFD946EF)),
    OpcionTipoPunto("HELIPUERTO", "HELIPUERTO EVAC.", Icons.Default.Flight, Color(0xFF06B6D4)),
    OpcionTipoPunto("PELIGRO (!)", "PELIGRO / CUIDADO", Icons.Default.Warning, Color(0xFFDC2626))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoNuevoPuntoManual(
    state: DialogoPuntoManualState,
    onTipoSeleccionado: (String) -> Unit,
    onNotaCambio: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    if (!state.visible) return

    val isDark = ThemeManager.isDarkTheme
    val dialogBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Dialog(onDismissRequest = onCancelar) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = dialogBg,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CABECERA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RallyCyan.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "#${state.numero}",
                                    color = RallyCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "AÑADIR PUNTO DE PASO",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "Trackeo Manual en Mapa con Geometría Inteligente",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary)
                    }
                }

                // TIRA DE COORDENADAS & SNAP TO ROAD
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) RallySurface else Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COORDENADAS SELECCIONADAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                            Text(
                                text = "${String.format("%.5f", state.latitud)}, ${String.format("%.5f", state.longitud)}",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            if (state.nombreCamino.isNotBlank()) {
                                Text(
                                    text = "🛣️ ${state.nombreCamino}",
                                    fontSize = 11.sp,
                                    color = RallyGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // BADGE ODÓMETRO CALCULADO
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "KM TOTAL / PARCIAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                            Text(
                                text = "${String.format("%.3f", state.distanciaTotal)} km (+${String.format("%.3f", state.distanciaParcial)})",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) RallyAccentYellow else FredianiAmberText
                            )
                        }
                    }
                }

                // SELECTOR DE TIPO DE PUNTO
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TIPO DE PUNTO (PRESELECCIÓN INTELIGENTE):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isDark) RallyCyan else FredianiCyanText,
                        letterSpacing = 0.5.sp
                    )

                    // CHIPS EN WRAP / GRID HORIZONTAL
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(TIPOS_PUNTO_RALLY) { op ->
                            val isSelected = op.tipo == state.tipoPunto
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) op.color.copy(alpha = 0.25f) else (if (isDark) RallySurface else Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) op.color else cardBorder
                                ),
                                modifier = Modifier.clickable { onTipoSeleccionado(op.tipo) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Icon(
                                        imageVector = op.icono,
                                        contentDescription = null,
                                        tint = if (isSelected) op.color else textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = op.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) (if (isDark) Color.White else Color(0xFF0F172A)) else textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // CAMPO NOTAS / INFORMACIÓN
                OutlinedTextField(
                    value = state.nota,
                    onValueChange = onNotaCambio,
                    label = { Text("NOTAS / DESCRIPCIÓN DEL PUNTO") },
                    placeholder = { Text("Ej: Curva veloz a derecha, puente angosto, asfalto roto...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = RallyCyan,
                        unfocusedBorderColor = cardBorder,
                        focusedLabelColor = RallyCyan
                    )
                )

                // BOTONES DE ACCIÓN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancelar,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onConfirmar,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RallyCyan,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AÑADIR AL ROADBOOK",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
