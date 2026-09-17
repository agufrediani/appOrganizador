package com.example.roadbookorganizador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.data.model.TulipaCatalog
import com.example.roadbookorganizador.data.model.TulipaItem
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.CockpitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreen(
    tramoId: Long,
    viewModel: CockpitViewModel,
    onVolver: () -> Unit,
    onNavigateToExportar: (Long) -> Unit
) {
    LaunchedEffect(tramoId) {
        viewModel.cargarTramo(tramoId)
    }

    val tramo by viewModel.tramoActivo.collectAsState()
    val odoState by viewModel.odoState.collectAsState()
    val vinetas by viewModel.vinetas.collectAsState()
    val dialogoMarcado by viewModel.dialogoMarcado.collectAsState()

    var tabSeleccionada by remember { mutableStateOf(0) } // 0: Mapa Táctico, 1: Hoja de Trazado FIA
    var vinetaParaEditar by remember { mutableStateOf<VinetaEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (tramo?.tipo == "PE") RallyRed else RallyCyan,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = tramo?.tipo ?: "TRAMO",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${tramo?.identificador ?: "TRAZADO"} • ${tramo?.nombre ?: ""}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Dist. Est: ${String.format("%.2f km", tramo?.distanciaTotalEstimada ?: 0.0)} • T. Otorgado: ${tramo?.tiempoOtorgado ?: "25'"} • 1° Auto: ${tramo?.horaPrimerAuto ?: "09:00"}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.toggleSimulacion() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (odoState.modoSimulacion) FredianiGreen.copy(alpha = 0.25f) else RallySurface
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (odoState.modoSimulacion) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = null,
                            tint = if (odoState.modoSimulacion) FredianiGreen else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (odoState.modoSimulacion) "SIMULANDO" else "SIMULAR GPS",
                            color = if (odoState.modoSimulacion) FredianiGreen else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(RallySurface, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (odoState.satelitesConectados) FredianiGreen else RallyRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (odoState.satelitesConectados) String.format("±%.1fm", odoState.precisionMetros) else "Buscando GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onNavigateToExportar(tramoId) }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar", tint = RallyCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RallyDarkBg)
            )
        },
        containerColor = RallyDarkBg
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val isLandscape = maxWidth > 720.dp

            if (isLandscape) {
                // DISEÑO RESPONSIVO HORIZONTAL PARA TABLET (2 COLUMNAS)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // COLUMNA IZQUIERDA: PANEL ODÓMETRO GIGANTE + BOTÓN DE ACCIÓN OFICIAL
                    Column(
                        modifier = Modifier
                            .width(440.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CardOdometroTablero(
                            odoState = odoState,
                            onResetParcial = { viewModel.resetParcialManual() },
                            onAjustarMetros = { viewModel.ajustarMetros(it) }
                        )

                        // BOTÓN OFICIAL GIGANTE: + AÑADIR INDICACIÓN (IDÉNTICO A LA WEB)
                        Button(
                            onClick = { viewModel.presionarMarcarVineta() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                            shape = RoundedCornerShape(18.dp),
                            elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AddLocation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "PULSAR EN CRUCE / PELIGRO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "+ AÑADIR INDICACIÓN",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Botones de acción inferior
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.finalizarTramo() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FredianiGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = FredianiGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("FINALIZAR TRAMO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onNavigateToExportar(tramoId) },
                                colors = ButtonDefaults.buttonColors(containerColor = RallySurface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = RallyCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("EXPORTAR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    // COLUMNA DERECHA: SELECTOR DE PESTAÑA (MAPA TÁCTICO vs HOJA DE TRAZADO FIA)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // BARRA DE PESTAÑAS DERECHA
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RallySurface, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TabBoton(
                                texto = "🗺️ MAPA TÁCTICO EN VIVO",
                                seleccionado = tabSeleccionada == 0,
                                onClick = { tabSeleccionada = 0 },
                                modifier = Modifier.weight(1f)
                            )
                            TabBoton(
                                texto = "📋 HOJA DE TRAZADO (${vinetas.size})",
                                seleccionado = tabSeleccionada == 1,
                                onClick = { tabSeleccionada = 1 },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (tabSeleccionada == 0) {
                            // MAPA TÁCTICO A PANTALLA COMPLETA A TODA LA ALTURA
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
                                    indicaciones = vinetas,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            // HOJA DE TRAZADO FIA OFICIAL DE 5 COLUMNAS
                            HojaTrazadoFia(
                                vinetas = vinetas,
                                distanciaTotalEstimada = tramo?.distanciaTotalEstimada ?: 10.0,
                                tripParcialActual = odoState.odometroParcialKm,
                                onEditar = { vinetaParaEditar = it },
                                onEliminar = { viewModel.eliminarVineta(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // DISEÑO RESPONSIVO VERTICAL (PORTRAIT)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CardOdometroTablero(
                        odoState = odoState,
                        onResetParcial = { viewModel.resetParcialManual() },
                        onAjustarMetros = { viewModel.ajustarMetros(it) }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RallySurface, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabBoton(
                            texto = "🗺️ MAPA",
                            seleccionado = tabSeleccionada == 0,
                            onClick = { tabSeleccionada = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        TabBoton(
                            texto = "📋 HOJA (${vinetas.size})",
                            seleccionado = tabSeleccionada == 1,
                            onClick = { tabSeleccionada = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (tabSeleccionada == 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1320))
                        ) {
                            com.example.roadbookorganizador.ui.components.RallyMapView(
                                latitud = odoState.latitud,
                                longitud = odoState.longitud,
                                rumbo = odoState.rumbo,
                                indicaciones = vinetas,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        HojaTrazadoFia(
                            vinetas = vinetas,
                            distanciaTotalEstimada = tramo?.distanciaTotalEstimada ?: 10.0,
                            tripParcialActual = odoState.odometroParcialKm,
                            onEditar = { vinetaParaEditar = it },
                            onEliminar = { viewModel.eliminarVineta(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { viewModel.presionarMarcarVineta() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+ AÑADIR INDICACIÓN", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // MODAL REGISTRO DE NUEVA INDICACIÓN
    if (dialogoMarcado.visible) {
        DialogoMarcadoRapido(
            state = dialogoMarcado,
            onTulipaSeleccionada = { viewModel.actualizarTulipaSeleccionada(it) },
            onNotaCambio = { viewModel.actualizarNota(it) },
            onPeligroCambio = { viewModel.actualizarPeligro(it) },
            onGuardar = { viewModel.guardarVineta() },
            onCancelar = { viewModel.cancelarMarcado() }
        )
    }

    // MODAL EDITAR INDICACIÓN YA REGISTRADA
    vinetaParaEditar?.let { v ->
        DialogoEditarIndicacion(
            vineta = v,
            onDismiss = { vinetaParaEditar = null },
            onGuardar = { vinetaActualizada ->
                viewModel.actualizarVineta(vinetaActualizada)
                vinetaParaEditar = null
            }
        )
    }
}

@Composable
fun TabBoton(texto: String, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (seleccionado) RallyCyan else Color.Transparent,
            contentColor = if (seleccionado) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier
    ) {
        Text(text = texto, fontWeight = if (seleccionado) FontWeight.Black else FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun CardOdometroTablero(
    odoState: com.example.roadbookorganizador.service.OdometerState,
    onResetParcial: () -> Unit,
    onAjustarMetros: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = RallyCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ODÓMETRO TOTAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OdometerLabel
            )

            Text(
                text = String.format("%.3f", odoState.odometroTotalKm),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = OdometerDigits
            )
            Text(
                text = "KILÓMETROS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = OdometerLabel
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PARCIAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OdometerLabel
                    )
                    Text(
                        text = String.format("%.3f", odoState.odometroParcialKm),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = RallyAccentYellow
                    )
                }

                Button(
                    onClick = onResetParcial,
                    colors = ButtonDefaults.buttonColors(containerColor = RallySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = RallyAccentYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("0.000", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%.0f km/h", odoState.velocidadKmh),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = RallyCyan, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.0f°", odoState.rumbo),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RallyCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones finos de calibración
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AjusteBoton("-50m") { onAjustarMetros(-50.0) }
                AjusteBoton("-10m") { onAjustarMetros(-10.0) }
                AjusteBoton("+10m") { onAjustarMetros(10.0) }
                AjusteBoton("+50m") { onAjustarMetros(50.0) }
            }
        }
    }
}

@Composable
fun HojaTrazadoFia(
    vinetas: List<VinetaEntity>,
    distanciaTotalEstimada: Double,
    tripParcialActual: Double,
    onEditar: (VinetaEntity) -> Unit,
    onEliminar: (VinetaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // CABECERA SUPERIOR
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOJA DE TRAZADO EN VIVO & INDICACIONES",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = FredianiTextMuted
                )
                Text(
                    text = "TRIP PARCIAL: ${String.format("%.3f Km", tripParcialActual)}",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = FredianiCyanText
                )
            }

            // ENCABEZADO 5 COLUMNAS FIA (NEGRO CON TEXTO BLANCO)
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "DIST. TOTAL", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.18f))
                    Text(text = "PARCIAL", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.16f))
                    Text(text = "DIRECCIÓN", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.18f))
                    Text(text = "INFORMACIÓN / NOTAS", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Start, modifier = Modifier.weight(0.32f).padding(start = 6.dp))
                    Text(text = "REGRESIVA", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(0.16f))
                }
            }

            if (vinetas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Avanzá con el vehículo y pulsá el botón verde para añadir la primera indicación.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(vinetas, key = { it.id }) { v ->
                        val distRegresiva = (distanciaTotalEstimada - v.distanciaTotal).coerceAtLeast(0.0)
                        FilaIndicacionFia(
                            vineta = v,
                            distanciaRegresiva = distRegresiva,
                            onEditar = { onEditar(v) },
                            onEliminar = { onEliminar(v) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilaIndicacionFia(
    vineta: VinetaEntity,
    distanciaRegresiva: Double,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditar() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Distancia Total
            Column(modifier = Modifier.weight(0.18f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", vineta.distanciaTotal),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    color = FredianiTextDark
                )
                Text(text = "KM TOTAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = FredianiTextMuted)
            }

            // 2. Parcial
            Column(modifier = Modifier.weight(0.16f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2f", vineta.distanciaParcial),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = FredianiTextDark
                )
                Text(text = "PARCIAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = FredianiTextMuted)
            }

            // 3. Dirección (Tulipa + Círculo Negro con Número)
            Box(modifier = Modifier.weight(0.18f), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            vineta.tulipTipo.contains("DER", ignoreCase = true) -> Icons.Default.TurnRight
                            vineta.tulipTipo.contains("IZQ", ignoreCase = true) -> Icons.Default.TurnLeft
                            vineta.tulipTipo.contains("HORQ", ignoreCase = true) -> Icons.Default.TurnSharpLeft
                            else -> Icons.Default.Straight
                        },
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.Black,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "${vineta.numero}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }

            // 4. Info / Notas
            Column(modifier = Modifier.weight(0.32f).padding(horizontal = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (vineta.peligro) {
                            "! ATENCIÓN" -> FredianiAmberBg
                            "!! PELIGRO", "!!!" -> FredianiRedBg
                            "LARGADA" -> FredianiGreenBg
                            else -> Color(0xFFF1F5F9)
                        }
                    ) {
                        Text(
                            text = if (vineta.peligro.isNotBlank()) vineta.peligro else "NORMAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = when (vineta.peligro) {
                                "! ATENCIÓN" -> FredianiAmberText
                                "!! PELIGRO", "!!!" -> FredianiRedText
                                "LARGADA" -> FredianiGreenText
                                else -> Color(0xFF475569)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("CAP: %.0f°", vineta.rumbo),
                        fontSize = 9.sp,
                        color = FredianiTextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = if (vineta.informacion.isNotBlank()) vineta.informacion else vineta.tulipTipo,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = FredianiTextDark,
                    maxLines = 1
                )

                Text(
                    text = "GPS: ${String.format("%.4f, %.4f", vineta.latitud, vineta.longitud)}",
                    fontSize = 8.sp,
                    color = FredianiTextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 5. Regresiva y Botonera (Editar y Borrar)
            Row(modifier = Modifier.weight(0.16f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f", distanciaRegresiva),
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = FredianiTextDark
                    )
                    Text(text = "REGRESIVA", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = FredianiTextMuted)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onEditar, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = FredianiCyanText, modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onEliminar, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = RallyRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AjusteBoton(texto: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(RallySurface)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VinetaItemRow(
    vineta: VinetaEntity,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RallyCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Número Viñeta
            Surface(
                shape = CircleShape,
                color = RallyCyan,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${vineta.numero}",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Distancias Total y Parcial
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%.3f km", vineta.distanciaTotal),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format("(+%.3f km)", vineta.distanciaParcial),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = RallyAccentYellow
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vineta.tulipTipo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = RallyCyanLight
                    )
                    if (vineta.peligro.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = vineta.peligro,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = RallyRed
                        )
                    }
                    if (vineta.informacion.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "- ${vineta.informacion}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }

            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoMarcadoRapido(
    state: com.example.roadbookorganizador.ui.viewmodel.DialogoMarcadoState,
    onTulipaSeleccionada: (String) -> Unit,
    onNotaCambio: (String) -> Unit,
    onPeligroCambio: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = RallyCardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Indicación #${state.numero}",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 20.sp
                )
                Text(
                    text = String.format("%.3f km", state.distanciaTotalCongelada),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = RallyAccentYellow,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info congelada
                Text(
                    text = String.format("Parcial: %.3f km | Rumbo: %.0f° | Lat/Lon: %.5f, %.5f",
                        state.distanciaParcialCongelada, state.rumbo, state.latitud, state.longitud),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                // SELECTOR DE TULIPAS RÁPIDAS
                Text("Seleccionar Maniobra / Tulipa:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RallyCyan)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTulipas = listOf(
                        "RECTA" to "Recta",
                        "DERECHA_2" to "Der 2",
                        "DERECHA_3" to "Der 90°",
                        "RETOME_DER" to "Horq Der",
                        "IZQUIERDA_2" to "Izq 2",
                        "IZQUIERDA_3" to "Izq 90°",
                        "RETOME_IZQ" to "Horq Izq",
                        "CRUCE_RECTO" to "Cruce",
                        "SALTO" to "Salto",
                        "VADO" to "Vado",
                        "PUENTE" to "Puente",
                        "TRANQUERA" to "Tranquera"
                    )

                    for ((codigo, label) in quickTulipas) {
                        val seleccionada = state.tulipaSeleccionada == codigo
                        FilterChip(
                            selected = seleccionada,
                            onClick = { onTulipaSeleccionada(codigo) },
                            label = { Text(label, fontWeight = if (seleccionada) FontWeight.Black else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RallyCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = RallySurface,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // PELIGROS
                Text("Nivel de Peligro:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RallyCyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val peligros = listOf("" to "Ninguno", "!" to "!", "!!" to "!!", "!!!" to "!!!")
                    for ((peligroVal, label) in peligros) {
                        val sel = state.peligro == peligroVal
                        FilterChip(
                            selected = sel,
                            onClick = { onPeligroCambio(peligroVal) },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (peligroVal.isNotEmpty()) RallyRed else RallySurface,
                                selectedLabelColor = Color.White,
                                containerColor = RallySurface,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // NOTAS RÁPIDAS
                OutlinedTextField(
                    value = state.nota,
                    onValueChange = onNotaCambio,
                    label = { Text("Notas de ruta (ej: Huella profunda, rocas)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RallyCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGuardar,
                colors = ButtonDefaults.buttonColors(containerColor = RallyAccentYellow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("GUARDAR Y RESETEAR PARCIAL", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoEditarIndicacion(
    vineta: VinetaEntity,
    onDismiss: () -> Unit,
    onGuardar: (VinetaEntity) -> Unit
) {
    var tulipa by remember { mutableStateOf(vineta.tulipTipo) }
    var peligro by remember { mutableStateOf(vineta.peligro) }
    var nota by remember { mutableStateOf(vineta.informacion) }
    var distTotal by remember { mutableStateOf(String.format("%.3f", vineta.distanciaTotal)) }
    var distParcial by remember { mutableStateOf(String.format("%.3f", vineta.distanciaParcial)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RallyCardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editar Indicación #${vineta.numero}",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RallySurface
                ) {
                    Text(
                        text = "GPS: ${String.format("%.4f, %.4f", vineta.latitud, vineta.longitud)}",
                        fontSize = 10.sp,
                        color = RallyCyanLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Maniobra / Tulipa:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RallyCyan)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTulipas = listOf(
                        "RECTA" to "Recta",
                        "DERECHA_2" to "Der 2",
                        "DERECHA_3" to "Der 90°",
                        "RETOME_DER" to "Horq Der",
                        "IZQUIERDA_2" to "Izq 2",
                        "IZQUIERDA_3" to "Izq 90°",
                        "RETOME_IZQ" to "Horq Izq",
                        "CRUCE_RECTO" to "Cruce",
                        "SALTO" to "Salto",
                        "VADO" to "Vado",
                        "PUENTE" to "Puente",
                        "TRANQUERA" to "Tranquera"
                    )

                    for ((codigo, label) in quickTulipas) {
                        val seleccionada = tulipa == codigo
                        FilterChip(
                            selected = seleccionada,
                            onClick = { tulipa = codigo },
                            label = { Text(label, fontWeight = if (seleccionada) FontWeight.Black else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RallyCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = RallySurface,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Text("Grado de Peligro:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RallyCyan)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val peligros = listOf("NORMAL" to "NORMAL", "! ATENCIÓN" to "! ATENCIÓN", "!! PELIGRO" to "!! PELIGRO", "LARGADA" to "LARGADA")
                    for ((peligroVal, label) in peligros) {
                        val sel = peligro == peligroVal || (peligroVal == "NORMAL" && peligro.isBlank())
                        FilterChip(
                            selected = sel,
                            onClick = { peligro = if (peligroVal == "NORMAL") "" else peligroVal },
                            label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (peligroVal) {
                                    "! ATENCIÓN" -> RallyAccentAmber
                                    "!! PELIGRO" -> RallyRed
                                    "LARGADA" -> FredianiGreen
                                    else -> RallyCyan
                                },
                                selectedLabelColor = Color.Black,
                                containerColor = RallySurface,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Texto / Referencia de la Indicación") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RallyCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = distTotal,
                        onValueChange = { distTotal = it },
                        label = { Text("Km Total") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        )
                    )
                    OutlinedTextField(
                        value = distParcial,
                        onValueChange = { distParcial = it },
                        label = { Text("Km Parcial") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RallyCyan
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nuevoTotal = distTotal.replace(",", ".").toDoubleOrNull() ?: vineta.distanciaTotal
                    val nuevoParcial = distParcial.replace(",", ".").toDoubleOrNull() ?: vineta.distanciaParcial
                    onGuardar(
                        vineta.copy(
                            tulipTipo = tulipa,
                            peligro = peligro,
                            informacion = nota,
                            distanciaTotal = nuevoTotal,
                            distanciaParcial = nuevoParcial
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RallyCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("GUARDAR CAMBIOS", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
