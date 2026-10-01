package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.DialogoMarcadoState

/**
 * Modal rápido al congelar kilometraje con el botón verde de cabina (+ MARCAR)
 */
@Composable
fun DialogoMarcadoRapido(
    state: DialogoMarcadoState,
    onDiagramaSeleccionada: (String) -> Unit,
    onNotaCambio: (String) -> Unit,
    onPeligroCambio: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val maniobras = listOf(
        "RECTA" to "⬆ Recta",
        "DER_90" to "➡ Der 90°",
        "IZQ_90" to "⬅ Izq 90°",
        "DER_45" to "↗ Der 45°",
        "IZQ_45" to "↖ Izq 45°",
        "HORQUILLA_DER" to "↩ Horq D",
        "HORQUILLA_IZQ" to "↪ Horq I",
        "PELIGRO" to "⚠ Peligro"
    )
    val peligros = listOf("" to "Sin peligro", "!" to "!", "!!" to "!!", "!!!" to "!!!")

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = if (isDark) RallyCardBg else Color.White,
        titleContentColor = if (isDark) Color.White else Color(0xFF0F172A),
        textContentColor = if (isDark) Color.White else Color(0xFF0F172A),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${state.numero}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
                Column {
                    Text(
                        text = "NUEVA INDICACIÓN",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "KM ${String.format("%.3f", state.distanciaTotalCongelada).replace('.', ',')}  |  PARC: ${String.format("%.3f", state.distanciaParcialCongelada).replace('.', ',')}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = FredianiCyanText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DIBUJO RÁPIDO DE MANIOBRA
                Text(
                    text = "DIBUJO DE MANIOBRA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) OdometerLabel else Color(0xFF64748B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    maniobras.take(4).forEach { (tipo, label) ->
                        val sel = state.tulipaSeleccionada == tipo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) FredianiCyanText else (if (isDark) RallySurface else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDiagramaSeleccionada(tipo) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else (if (isDark) Color.White else Color(0xFF0F172A)),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    maniobras.drop(4).forEach { (tipo, label) ->
                        val sel = state.tulipaSeleccionada == tipo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) FredianiCyanText else (if (isDark) RallySurface else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDiagramaSeleccionada(tipo) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else (if (isDark) Color.White else Color(0xFF0F172A)),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // GRADO DE PELIGRO
                Text(
                    text = "NIVEL DE PELIGRO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) OdometerLabel else Color(0xFF64748B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    peligros.forEach { (pel, label) ->
                        val sel = state.peligro == pel
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (sel) {
                                if (pel.isEmpty()) FredianiCyanText else RallyRed
                            } else {
                                if (isDark) RallySurface else Color(0xFFF1F5F9)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPeligroCambio(pel) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (sel) Color.White else (if (isDark) Color.White else Color(0xFF0F172A)),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                // ANOTACIÓN COLOQUIAL RÁPIDA
                OutlinedTextField(
                    value = state.nota,
                    onValueChange = onNotaCambio,
                    label = { Text("Nota rápida (ej: Huella por derecha)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) RallySurface else Color.White,
                        unfocusedContainerColor = if (isDark) RallySurface else Color.White,
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = FredianiCyanText,
                        unfocusedBorderColor = if (isDark) RallyCardBg else Color(0xFFCBD5E1)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGuardar,
                colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("GUARDAR INDICACIÓN", fontWeight = FontWeight.Black, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Descartar", color = if (isDark) Color.LightGray else Color(0xFF64748B))
            }
        }
    )
}
