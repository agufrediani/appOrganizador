package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.PuntoInteresEntity
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.MapaLibreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaLibreScreen(
    viewModel: MapaLibreViewModel,
    onVolver: () -> Unit
) {
    val odoState by viewModel.odoState.collectAsState()
    val puntos by viewModel.puntos.collectAsState()
    var mostrarModalAgregarPunto by remember { mutableStateOf(false) }
    var puntoCoordenadasSeleccionadas by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "EXPLORADOR & MAPA LIBRE",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = if (isDark) RallyCyanLight else FredianiNavy
                        )
                        Text(
                            text = "Reconocimiento de Terreno y Marcación de Zonas",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { ThemeManager.toggleTheme() }) {
                        Text(text = if (isDark) "☀️" else "🌙", fontSize = 18.sp)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.toggleSimulacion() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (odoState.modoSimulacion) FredianiGreen.copy(alpha = 0.25f) else (if (isDark) RallySurface else Color(0xFFF1F5F9))
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (odoState.modoSimulacion) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = "Simulador",
                            tint = if (odoState.modoSimulacion) FredianiGreen else textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (odoState.modoSimulacion) "SIMULANDO" else "SIMULAR GPS",
                            color = if (odoState.modoSimulacion) FredianiGreen else textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            puntoCoordenadasSeleccionadas = null
                            mostrarModalAgregarPunto = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RallyAccentYellow),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MARCAR ZONA / POI", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val isLandscape = maxWidth > 680.dp

            if (isLandscape) {
                // DISEÑO RESPONSIVO HORIZONTAL: 2 COLUMNAS (MAPA A TODA ALTURA)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Panel lateral izquierdo: GNSS + Zonas
                    Column(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CardPosicionGnss(odoState = odoState)

                        Text(
                            text = "ZONAS Y PUNTOS MARCADOS (${puntos.size})",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = RallyCyanLight
                        )

                        if (puntos.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = RallyCardBg.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tocá cualquier parte del mapa o pulsa 'MARCAR ZONA / POI' para registrar accesos, puestos médicos o zonas de público.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(puntos, key = { it.id }) { p ->
                                    PuntoInteresItemRow(
                                        punto = p,
                                        onEliminar = { viewModel.eliminarPunto(p) }
                                    )
                                }
                            }
                        }
                    }

                    // Panel derecho: ¡MAPA A PANTALLA COMPLETA A TODA LA ALTURA!
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1320))
                    ) {
                        com.example.roadbookorganizador.ui.components.RallyMapView(
                            latitud = odoState.latitud,
                            longitud = odoState.longitud,
                            rumbo = odoState.rumbo,
                            puntos = puntos,
                            modifier = Modifier.fillMaxSize(),
                            onMapClick = { lat, lng ->
                                puntoCoordenadasSeleccionadas = Pair(lat, lng)
                                mostrarModalAgregarPunto = true
                            }
                        )
                    }
                }
            } else {
                // DISEÑO RESPONSIVO VERTICAL (PORTRAIT)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CardPosicionGnss(odoState = odoState)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1320))
                    ) {
                        com.example.roadbookorganizador.ui.components.RallyMapView(
                            latitud = odoState.latitud,
                            longitud = odoState.longitud,
                            rumbo = odoState.rumbo,
                            puntos = puntos,
                            modifier = Modifier.fillMaxSize(),
                            onMapClick = { lat, lng ->
                                puntoCoordenadasSeleccionadas = Pair(lat, lng)
                                mostrarModalAgregarPunto = true
                            }
                        )
                    }

                    Text(
                        text = "ZONAS Y PUNTOS MARCADOS (${puntos.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(puntos, key = { it.id }) { p ->
                            PuntoInteresItemRow(
                                punto = p,
                                onEliminar = { viewModel.eliminarPunto(p) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarModalAgregarPunto) {
        val latDefecto = puntoCoordenadasSeleccionadas?.first ?: odoState.latitud
        val lngDefecto = puntoCoordenadasSeleccionadas?.second ?: odoState.longitud

        DialogoNuevoPOI(
            latitud = latDefecto,
            longitud = lngDefecto,
            origenClickEnMapa = puntoCoordenadasSeleccionadas != null,
            onDismiss = {
                mostrarModalAgregarPunto = false
                puntoCoordenadasSeleccionadas = null
            },
            onGuardar = { nombre, tipo, desc ->
                viewModel.agregarPuntoInteres(nombre, tipo, desc, latDefecto, lngDefecto)
                mostrarModalAgregarPunto = false
                puntoCoordenadasSeleccionadas = null
            }
        )
    }
}

@Composable
fun CardPosicionGnss(odoState: com.example.roadbookorganizador.service.OdometerState) {
    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

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
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "POSICIÓN ACTUAL (GNSS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textSecondary
                )
                Text(
                    text = if (odoState.latitud != 0.0) String.format("%.5f, %.5f", odoState.latitud, odoState.longitud) else "-32.95772, -60.63779",
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    color = textPrimary
                )
                Text(
                    text = String.format("Altitud: %.0f msnm • Precisión: %.1fm", odoState.altitud, odoState.precisionMetros),
                    fontSize = 11.sp,
                    color = if (isDark) RallyCyanLight else FredianiCyanText
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) RallySurface else Color(0xFFF1F5F9),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = if (isDark) RallyCyan else FredianiCyanText)
                    }
                }
                Text(
                    text = String.format("%.0f°", odoState.rumbo),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isDark) RallyCyan else FredianiCyanText
                )
            }
        }
    }
}

@Composable
fun PuntoInteresItemRow(
    punto: PuntoInteresEntity,
    onEliminar: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val (badgeBg, badgeFg) = when (punto.tipo) {
                "AMBULANCIA", "RESCATE" -> Pair(RallyRed.copy(alpha = 0.25f), RallyRed)
                "ZONA ESPECTADORES", "PUBLICO" -> Pair(RallyAccentYellow.copy(alpha = 0.25f), if (isDark) RallyAccentYellow else FredianiAmberText)
                "PARQUE DE ASISTENCIA" -> Pair(RallyCyan.copy(alpha = 0.25f), if (isDark) RallyCyan else FredianiCyanText)
                "HELIPUERTO" -> Pair(RallyAccentAmber.copy(alpha = 0.25f), RallyAccentAmber)
                else -> Pair(RallyGreen.copy(alpha = 0.25f), RallyGreen)
            }

            Surface(
                shape = CircleShape,
                color = badgeBg,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (punto.tipo) {
                            "AMBULANCIA" -> Icons.Default.MedicalServices
                            "RESCATE" -> Icons.Default.Emergency
                            "ZONA ESPECTADORES", "PUBLICO" -> Icons.Default.Groups
                            "HELIPUERTO" -> Icons.Default.LocalHospital
                            "PARQUE DE ASISTENCIA" -> Icons.Default.Build
                            "ACCESO" -> Icons.Default.AltRoute
                            else -> Icons.Default.Place
                        },
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = punto.nombre, fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 13.sp)
                Text(
                    text = "${punto.tipo} • ${String.format("%.5f, %.5f", punto.latitud, punto.longitud)}",
                    fontSize = 11.sp,
                    color = textSecondary
                )
                if (punto.descripcion.isNotEmpty()) {
                    Text(text = punto.descripcion, fontSize = 10.sp, color = if (isDark) RallyCyanLight else FredianiCyanText)
                }
            }

            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoNuevoPOI(
    latitud: Double,
    longitud: Double,
    origenClickEnMapa: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (String, String, String) -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("ZONA ESPECTADORES") }
    var descripcion by remember { mutableStateOf("") }

    val categoriasZonas = listOf(
        "ZONA ESPECTADORES",
        "ACCESO",
        "AMBULANCIA",
        "RESCATE",
        "PARQUE DE ASISTENCIA",
        "HELIPUERTO",
        "CONTROL HORARIO (CH)",
        "PELIGRO",
        "MOJÓN"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = RallyAccentYellow)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (origenClickEnMapa) "Registrar Zona en Punto Marcado" else "Registrar Zona en Posición Actual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) RallySurface else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Coordenadas: ${String.format("%.5f, %.5f", latitud, longitud)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) RallyCyanLight else FredianiCyanText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la Zona o Punto (Obligatorio)") },
                    placeholder = { Text("Ej: Zona Espectadores 1, Cruce Ruta 9...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = RallyCyan,
                        unfocusedBorderColor = cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "SELECCIONAR TIPO DE ZONA / PUNTO:",
                    fontSize = 11.sp,
                    color = if (isDark) RallyCyan else FredianiCyanText,
                    fontWeight = FontWeight.Black
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (cat in categoriasZonas) {
                        val sel = tipo == cat
                        FilterChip(
                            selected = sel,
                            onClick = { tipo = cat },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Black else FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (cat.contains("AMBULANCIA") || cat.contains("RESCATE")) RallyRed
                                else if (cat.contains("ESPECTADORES")) RallyAccentYellow
                                else RallyCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9),
                                labelColor = textPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción / Instrucciones (Opcional)") },
                    placeholder = { Text("Ej: Ingreso por tranquera blanca a 200m...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = RallyCyan,
                        unfocusedBorderColor = cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalNombre = if (nombre.isBlank()) tipo else nombre
                    onGuardar(finalNombre, tipo, descripcion)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RallyAccentYellow),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("GUARDAR ZONA", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
