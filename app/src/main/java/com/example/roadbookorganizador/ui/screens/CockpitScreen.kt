package com.example.roadbookorganizador.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.service.OdometerState
import com.example.roadbookorganizador.ui.components.DialogoMarcadoRapido
import com.example.roadbookorganizador.ui.components.DialogoNuevoPuntoManual
import com.example.roadbookorganizador.ui.components.DialogoTelemetriaOdometro
import com.example.roadbookorganizador.ui.components.RallyMapView
import com.example.roadbookorganizador.ui.components.RoadbookMasterView
import com.example.roadbookorganizador.ui.theme.*
import com.example.roadbookorganizador.ui.viewmodel.CockpitViewModel
import com.example.roadbookorganizador.ui.viewmodel.DialogoMarcadoState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CockpitScreen(
    tramoId: Long,
    viewModel: CockpitViewModel,
    onVolver: () -> Unit,
    onNavigateToExportar: (Long) -> Unit,
    onNavigateToCalibrar: () -> Unit = {}
) {
    LaunchedEffect(tramoId) {
        viewModel.cargarTramo(tramoId)
    }

    val tramo by viewModel.tramoActivo.collectAsState()
    val odoState by viewModel.odoState.collectAsState()
    val vinetas by viewModel.vinetas.collectAsState()
    val dialogoMarcado by viewModel.dialogoMarcado.collectAsState()
    val dialogoPuntoManual by viewModel.dialogoPuntoManual.collectAsState()

    var vinetaSeleccionadaId by remember { mutableStateOf<Long?>(null) }
    var mostrarDialogoTelemetria by remember { mutableStateOf(false) }
    var mostrarDialogoPcs by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDark = ThemeManager.isDarkTheme

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.example.roadbookorganizador.data.local.SessionManager(context) }
    val mapboxToken by sessionManager.mapboxToken.collectAsState()
    val snapToRoad by sessionManager.snapToRoad.collectAsState()

    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    // Si no hay viñeta seleccionada, predeterminar la última
    LaunchedEffect(vinetas) {
        if (vinetaSeleccionadaId == null && vinetas.isNotEmpty()) {
            vinetaSeleccionadaId = vinetas.last().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // LOGO / TÍTULO
                        Column {
                            Text(
                                text = "FREDIANI ROADBOOK",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (isDark) RallyCyan else FredianiCyanText,
                                letterSpacing = 0.8.sp
                            )
                            tramo?.let { t ->
                                Text(
                                    text = "${t.identificador} • ${t.nombre}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // TIRA COMPACTA DE ODÓMETRO & GNSS EN BARRA SUPERIOR
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) RallySurface else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                // TOTAL
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TOT ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = String.format("%.3f", odoState.odometroTotalKm).replace('.', ','),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = " km",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                }

                                Text(text = "•", color = textSecondary, fontSize = 10.sp)

                                // PARCIAL
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "PAR ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = String.format("%.3f", odoState.odometroParcialKm).replace('.', ','),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) RallyAccentYellow else FredianiAmberText
                                    )
                                    Text(
                                        text = " km",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                }

                                Text(text = "•", color = textSecondary, fontSize = 10.sp)

                                // COORDENADAS
                                Text(
                                    text = "${String.format("%.4f", odoState.latitud)}, ${String.format("%.4f", odoState.longitud)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary
                                )

                                Text(text = "•", color = textSecondary, fontSize = 10.sp)

                                // BADGE GNSS / RACEBOX
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            if (odoState.raceBoxConectado) Color(0xFF065F46)
                                            else if (odoState.satelitesConectados) Color(0xFF047857)
                                            else RallyRed.copy(alpha = 0.2f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (odoState.raceBoxConectado) Color(0xFF34D399)
                                                else if (odoState.satelitesConectados) FredianiGreen
                                                else RallyRed
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (odoState.raceBoxConectado) {
                                            val b = odoState.raceBoxBateriaPct?.let { " $it%" } ?: ""
                                            val prec = if (odoState.precisionMetros > 0f) " ${String.format("%.1f", odoState.precisionMetros)}m" else ""
                                            if (odoState.raceBoxTieneFix) "GPS EXTERNO 25Hz$prec$b" else "GPS EXTERNO (BUSCANDO)$b"
                                        } else if (odoState.satelitesConectados) {
                                            "GPS TABLET ${String.format("%.1f", odoState.precisionMetros)}m"
                                        } else {
                                            "BUSCANDO GPS"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (odoState.raceBoxConectado || odoState.satelitesConectados) Color.White else RallyRed
                                    )
                                }

                                if (odoState.modoReverso) {
                                    Text(text = "•", color = textSecondary, fontSize = 10.sp)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDC2626)
                                    ) {
                                        Text(
                                            text = "◀ REVERSO (-)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    // BOTÓN VERDE + AÑADIR INDICACIÓN
                    Button(
                        onClick = { viewModel.presionarMarcarVineta() },
                        colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.AddLocation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ AÑADIR INDICACIÓN",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // BOTÓN MODO REVERSO (RESTA KILOMETRAJE AL RETROCEDER)
                    FilledTonalButton(
                        onClick = { viewModel.toggleModoReverso() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (odoState.modoReverso) Color(0xFFDC2626) else (if (isDark) RallySurface else Color(0xFFF1F5F9)),
                            contentColor = if (odoState.modoReverso) Color.White else (if (isDark) RallyCyan else FredianiCyanText)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Odómetro Reverso",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (odoState.modoReverso) "◀ REVERSO (-)" else "REVERSO",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // BOTÓN GESTIÓN DE WAYPOINTS (WPS)
                    FilledTonalButton(
                        onClick = { mostrarDialogoPcs = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (odoState.autoPcsHabilitado) Color(0xFF581C87) else (if (isDark) RallySurface else Color(0xFFF1F5F9)),
                            contentColor = if (odoState.autoPcsHabilitado) Color(0xFFE9D5FF) else Color(0xFFA855F7)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = "WayPoints",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (odoState.autoPcsHabilitado) "WP (AUTO ON)" else "WP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // BOTÓN INFO / TELEMETRÍA (ABRE MODAL DE ODÓMETRO COMPLETO)
                    FilledTonalButton(
                        onClick = { mostrarDialogoTelemetria = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9),
                            contentColor = if (isDark) RallyCyan else FredianiCyanText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Telemetría y Odómetro",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ODÓMETRO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // SIMULAR GPS
                    IconButton(
                        onClick = { viewModel.toggleSimulacion() },
                        modifier = Modifier
                            .background(
                                if (odoState.modoSimulacion) FredianiGreen.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (odoState.modoSimulacion) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = "Simular GPS",
                            tint = if (odoState.modoSimulacion) FredianiGreen else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // TEMA CLARO / OSCURO
                    IconButton(onClick = { ThemeManager.isDarkTheme = !ThemeManager.isDarkTheme }) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema",
                            tint = if (isDark) RallyAccentYellow else FredianiAmberText
                        )
                    }

                    // EXPORTAR
                    IconButton(onClick = { onNavigateToExportar(tramoId) }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar", tint = if (isDark) RallyCyan else FredianiCyanText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (isLandscape) {
                // =========================================================================
                // SPLIT SCREEN HORIZONTAL (RALLY NAVIGATOR PRO):
                // IZQUIERDA (~44%): MAPA SATELITAL MAPBOX / GOOGLE HYBRID CON STREET VIEW
                // DERECHA (~56%): GRILLA ROADBOOK OFICIAL CON EDICIÓN IN-SITU Y SHELF INFERIOR
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PANEL IZQUIERDO: MAPA SATELITAL
                    Card(
                        modifier = Modifier
                            .weight(0.44f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder)
                    ) {
                        RallyMapView(
                            latitud = odoState.latitud,
                            longitud = odoState.longitud,
                            rumbo = odoState.rumbo,
                            indicaciones = vinetas,
                            mapboxToken = mapboxToken,
                            snapToRoadInitial = snapToRoad,
                            vinetaSeleccionadaId = vinetaSeleccionadaId,
                            onMapClickWithRoadAndMode = { lat, lng, road, isOffRoad ->
                                viewModel.iniciarCreacionPuntoManual(lat, lng, road, isOffRoad)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // PANEL DERECHO: GRILLA ROADBOOK MAESTRA + EDICIÓN EN LA PROPIA CELDA
                    Box(
                        modifier = Modifier
                            .weight(0.56f)
                            .fillMaxHeight()
                    ) {
                        RoadbookMasterView(
                            tramo = tramo,
                            vinetas = vinetas,
                            vinetaSeleccionadaId = vinetaSeleccionadaId,
                            onSeleccionarVineta = { v ->
                                vinetaSeleccionadaId = v.id
                            },
                            onGuardarVineta = { v ->
                                viewModel.actualizarVineta(v)
                            },
                            onEliminarVineta = { v ->
                                viewModel.eliminarVineta(v)
                            },
                            onEliminarVinetas = { ids ->
                                viewModel.eliminarVinetasPorIds(ids)
                            },
                            onGuardarTramo = { t ->
                                viewModel.actualizarTramo(t)
                            },
                            onPropagarDiferencia = { desdeNum, deltaKm ->
                                viewModel.propagarDiferenciaKilometrica(desdeNum, deltaKm)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // =========================================================================
                // SPLIT SCREEN VERTICAL (PORTRAIT):
                // ARRIBA (40%): MAPA SATELITAL
                // ABAJO (60%): GRILLA ROADBOOK
                // =========================================================================
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.40f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorder)
                    ) {
                        RallyMapView(
                            latitud = odoState.latitud,
                            longitud = odoState.longitud,
                            rumbo = odoState.rumbo,
                            indicaciones = vinetas,
                            mapboxToken = mapboxToken,
                            snapToRoadInitial = snapToRoad,
                            vinetaSeleccionadaId = vinetaSeleccionadaId,
                            onMapClickWithRoadAndMode = { lat, lng, road, isOffRoad ->
                                viewModel.iniciarCreacionPuntoManual(lat, lng, road, isOffRoad)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.60f)
                    ) {
                        RoadbookMasterView(
                            tramo = tramo,
                            vinetas = vinetas,
                            vinetaSeleccionadaId = vinetaSeleccionadaId,
                            onSeleccionarVineta = { v ->
                                vinetaSeleccionadaId = v.id
                            },
                            onGuardarVineta = { v ->
                                viewModel.actualizarVineta(v)
                            },
                            onEliminarVineta = { v ->
                                viewModel.eliminarVineta(v)
                            },
                            onEliminarVinetas = { ids ->
                                viewModel.eliminarVinetasPorIds(ids)
                            },
                            onGuardarTramo = { t ->
                                viewModel.actualizarTramo(t)
                            },
                            onPropagarDiferencia = { desdeNum, deltaKm ->
                                viewModel.propagarDiferenciaKilometrica(desdeNum, deltaKm)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIÁLOGO MODAL: MARCADOR RÁPIDO AL PULSAR "+ MARCAR"
    // =========================================================================
    if (dialogoMarcado.visible) {
        DialogoMarcadoRapido(
            state = dialogoMarcado,
            onDiagramaSeleccionada = { viewModel.actualizarTulipaSeleccionada(it) },
            onNotaCambio = { viewModel.actualizarNota(it) },
            onPeligroCambio = { viewModel.actualizarPeligro(it) },
            onGuardar = { viewModel.guardarVineta() },
            onCancelar = { viewModel.cancelarMarcado() }
        )
    }

    // =========================================================================
    // DIÁLOGO MODAL: TRACKEO MANUAL (CLIC EN MAPA - REGLA 1RO INICIO / LUEGO WAYPOINT)
    // =========================================================================
    if (dialogoPuntoManual.visible) {
        DialogoNuevoPuntoManual(
            state = dialogoPuntoManual,
            onTipoSeleccionado = { viewModel.actualizarTipoPuntoManual(it) },
            onNotaCambio = { viewModel.actualizarNotaPuntoManual(it) },
            onConfirmar = { viewModel.confirmarPuntoManual() },
            onCancelar = { viewModel.cancelarPuntoManual() }
        )
    }

    // =========================================================================
    // DIÁLOGO MODAL: TELEMETRÍA Y CONTROL INTEGRAL DEL ODÓMETRO
    // =========================================================================
    if (mostrarDialogoTelemetria) {
        DialogoTelemetriaOdometro(
            odoState = odoState,
            onResetParcial = { viewModel.resetParcialManual() },
            onResetTotal = { viewModel.resetTotalManual() },
            onAjustarMetros = { delta -> viewModel.ajustarMetros(delta) },
            onToggleModoReverso = { viewModel.toggleModoReverso() },
            onToggleAutoPcs = { viewModel.toggleAutoPcs() },
            onNavigateToCalibrador = {
                mostrarDialogoTelemetria = false
                onNavigateToCalibrar()
            },
            onDismiss = { mostrarDialogoTelemetria = false }
        )
    }

    // =========================================================================
    // DIÁLOGO MODAL: GESTIÓN DE WAYPOINTS (WPS: MANUAL & AUTOMÁTICO)
    // =========================================================================
    if (mostrarDialogoPcs) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPcs = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFA855F7))
                    Text("WayPoints (WPs)", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Inserte un WayPoint en el kilometraje actual o configure la generación automática periódica.",
                        fontSize = 12.5.sp,
                        color = textSecondary
                    )

                    // TOGGLE AUTO-WPS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Generación Automática (Auto-WPs)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFA855F7))
                            Text("WP #1 a ~100m, luego cada ~350m (±15% aleatorio)", fontSize = 10.sp, color = textSecondary)
                        }
                        Switch(
                            checked = odoState.autoPcsHabilitado,
                            onCheckedChange = { viewModel.toggleAutoPcs() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7), checkedTrackColor = Color(0xFF581C87))
                        )
                    }

                    HorizontalDivider(color = cardBorder)

                    Text(
                        text = "Insertar WP Manual en Km ${String.format("%.3f", odoState.odometroTotalKm)}:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = textPrimary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("REGULARIDAD", "🎯 WP Regularidad", Color(0xFF7C3AED)),
                            Triple("PASO", "🚩 WP de Paso", Color(0xFF2563EB)),
                            Triple("CH", "⏱️ Control Horario (CH)", Color(0xFFD97706)),
                            Triple("RADAR", "📡 WP Radar de Velocidad", Color(0xFFDC2626))
                        ).forEach { (tipo, etiqueta, color) ->
                            Button(
                                onClick = {
                                    viewModel.insertarPuntoControlManual(tipo)
                                    mostrarDialogoPcs = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = color),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(etiqueta, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPcs = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // =========================================================================
    // DIÁLOGO MODAL: PÉRDIDA DE CONEXIÓN GPS EXTERNO (PREGUNTAR CAMBIO A TABLET)
    // =========================================================================
    if (odoState.preguntarCambioATablet) {
        AlertDialog(
            onDismissRequest = { viewModel.odometerEngine.cancelarPreguntaCambioATablet() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RallyRed)
                    Text(
                        text = "Conexión GPS Externo perdida",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = RallyRed
                    )
                }
            },
            text = {
                Text(
                    text = "Se ha interrumpido la conexión con el GPS Externo.\n\n¿Deseas pasar al GPS de la Tablet como respaldo o esperar a que se reconecte?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.odometerEngine.aceptarFallbackTablet() },
                    colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen)
                ) {
                    Text("Pasar a GPS Tablet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.odometerEngine.cancelarPreguntaCambioATablet() }
                ) {
                    Text("Esperar GPS Externo", color = Color.Gray)
                }
            }
        )
    }
}
