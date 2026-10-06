package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.data.model.DrawingData
import com.example.roadbookorganizador.data.model.ManeuverArrow
import com.example.roadbookorganizador.data.model.PointData
import com.example.roadbookorganizador.data.model.StampData
import com.example.roadbookorganizador.data.model.StrokeData
import com.example.roadbookorganizador.ui.theme.*

enum class NavigatorTab(val titulo: String) {
    MY_ICONS("MY ICONS"),
    ROAD_RALLY("CROSS COUNTRY"),
    LANDMARKS("LANDMARKS"),
    TERRAIN("TERRAIN"),
    SIGNS("NOTAS / PELIGROS"),
    SPEED("SPEED & RADAR")
}

enum class ActiveDrawTool {
    SELECT,
    PEN,
    RULER
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoadbookMasterView(
    tramo: TramoEntity?,
    vinetas: List<VinetaEntity>,
    vinetaSeleccionadaId: Long?,
    onSeleccionarVineta: (VinetaEntity) -> Unit,
    onGuardarVineta: (VinetaEntity) -> Unit,
    onEliminarVineta: (VinetaEntity) -> Unit = {},
    onEliminarVinetas: ((Set<Long>) -> Unit)? = null,
    onGuardarTramo: ((TramoEntity) -> Unit)? = null,
    onPropagarDiferencia: ((desdeNumero: Int, deltaKm: Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isDark) RallyCardBg else Color.White
    val cardBorder = if (isDark) RallySurface else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    var vinetaEnEdicionId by remember { mutableStateOf<Long?>(vinetaSeleccionadaId) }
    var drawingDraft by remember { mutableStateOf(DrawingData()) }
    var activeTool by remember { mutableStateOf(ActiveDrawTool.PEN) }
    var selectedStampIdx by remember { mutableStateOf<Int?>(null) }
    var selectedArrowIdx by remember { mutableStateOf<Int?>(null) }
    var asistenteTrazoActivo by remember { mutableStateOf(true) }
    var activeTab by remember { mutableStateOf(NavigatorTab.ROAD_RALLY) }
    var selectedStrokeColor by remember { mutableStateOf("#000000") }
    var selectedStrokeWidth by remember { mutableStateOf(7f) }

    var capaNumeroVisible by remember { mutableStateOf(true) }
    var capaTotalVisible by remember { mutableStateOf(true) }
    var capaParcialVisible by remember { mutableStateOf(true) }
    var capaIndicacionVisible by remember { mutableStateOf(true) }
    var capaAnotacionesVisible by remember { mutableStateOf(true) }
    var capaRegresivaVisible by remember { mutableStateOf(true) }

    var vinetaParaEditarNotas by remember { mutableStateOf<VinetaEntity?>(null) }
    var mostrarDialogoVelocidadesTramo by remember { mutableStateOf(false) }
    var datosPropagacionPendiente by remember { mutableStateOf<Triple<VinetaEntity, Double, Int>?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var sidebarExpanded by remember(isLandscape) { mutableStateOf(isLandscape) }
    var sidebarWidthDp by remember { mutableFloatStateOf(290f) }
    val density = LocalDensity.current

    // Selección múltiple para borrado de viñetas
    var vinetasSeleccionadasParaBorrar by remember { mutableStateOf(setOf<Long>()) }
    var mostrarDialogoConfirmarBorrado by remember { mutableStateOf(false) }

    // Sincronizar selección si cambian las viñetas
    LaunchedEffect(vinetas) {
        val idsExistentes = vinetas.map { it.id }.toSet()
        vinetasSeleccionadasParaBorrar = vinetasSeleccionadasParaBorrar.intersect(idsExistentes)
    }

    // Sincronizar selección externa de viñeta
    LaunchedEffect(vinetaSeleccionadaId) {
        if (vinetaSeleccionadaId != null) {
            vinetaEnEdicionId = vinetaSeleccionadaId
        }
    }

    // Sincronizar borrador al cambiar la viñeta en edición sin inyectar jamás flechas automáticas
    LaunchedEffect(vinetaEnEdicionId) {
        val cur = vinetas.find { it.id == vinetaEnEdicionId }
        if (cur != null) {
            val parsed = DrawingData.fromJson(cur.tulipSvgData)
            drawingDraft = parsed
            selectedStampIdx = if (parsed.stamps.isNotEmpty()) parsed.stamps.lastIndex else null
            selectedArrowIdx = if (parsed.arrows.isNotEmpty()) parsed.arrows.lastIndex else null
            activeTool = ActiveDrawTool.SELECT
        } else {
            drawingDraft = DrawingData()
            selectedStampIdx = null
            selectedArrowIdx = null
        }
    }

    val persistDraft: (DrawingData) -> Unit = { newDraft ->
        drawingDraft = newDraft
        val cur = vinetas.find { it.id == vinetaEnEdicionId }
        if (cur != null) {
            val newTulipTipo = if (newDraft.isEmpty()) "BLANCO" else cur.tulipTipo
            onGuardarVineta(cur.copy(
                tulipSvgData = newDraft.toJson(),
                tulipTipo = newTulipTipo
            ))
        }
    }

    val distanciaTotalEstimada = tramo?.distanciaTotalEstimada ?: 10.0
    val tableListState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll para centrar la indicación en pantalla al seleccionarla o editarla (desde tabla o mapa)
    LaunchedEffect(vinetaEnEdicionId, vinetaSeleccionadaId) {
        val targetId = vinetaEnEdicionId ?: vinetaSeleccionadaId
        if (targetId != null) {
            val idx = vinetas.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                tableListState.animateScrollToItem(idx)
            }
        }
    }

    val insertStampAction: (String) -> Unit = { stampCode ->
        val targetVineta = vinetas.find { it.id == vinetaEnEdicionId } ?: vinetas.firstOrNull()
        if (targetVineta != null) {
            if (vinetaEnEdicionId != targetVineta.id) {
                vinetaEnEdicionId = targetVineta.id
                onSeleccionarVineta(targetVineta)
            }
            if (stampCode == "FLECHA_MANIOBRA") {
                val na = drawingDraft.arrows.toMutableList()
                na.add(
                    ManeuverArrow(
                        p0 = PointData(0.5f, 0.85f),
                        p1 = PointData(0.5f, 0.60f),
                        p2 = PointData(0.65f, 0.40f),
                        p3 = PointData(0.80f, 0.25f),
                        strokeWidth = selectedStrokeWidth,
                        colorHex = selectedStrokeColor
                    )
                )
                persistDraft(drawingDraft.copy(arrows = na))
                selectedArrowIdx = na.lastIndex
                selectedStampIdx = null
                activeTool = ActiveDrawTool.SELECT
            } else if (stampCode == "RECTA") {
                val na = drawingDraft.arrows.toMutableList()
                na.add(
                    ManeuverArrow(
                        p0 = PointData(0.5f, 0.88f),
                        p1 = PointData(0.5f, 0.65f),
                        p2 = PointData(0.5f, 0.40f),
                        p3 = PointData(0.5f, 0.15f),
                        strokeWidth = selectedStrokeWidth,
                        colorHex = selectedStrokeColor
                    )
                )
                persistDraft(drawingDraft.copy(arrows = na))
                selectedArrowIdx = na.lastIndex
                selectedStampIdx = null
                activeTool = ActiveDrawTool.SELECT
            } else {
                val nst = drawingDraft.stamps.toMutableList()
                nst.add(
                    StampData(
                        type = stampCode,
                        x = 0.5f,
                        y = 0.5f,
                        scale = 1.0f,
                        rotation = 0.0f
                    )
                )
                persistDraft(drawingDraft.copy(stamps = nst))
                selectedStampIdx = nst.lastIndex
                selectedArrowIdx = null
                activeTool = ActiveDrawTool.SELECT
            }
        }
    }

    val appendNoteAction: (String) -> Unit = { noteShortcut ->
        val targetVineta = vinetas.find { it.id == vinetaEnEdicionId } ?: vinetas.firstOrNull()
        if (targetVineta != null) {
            val currentText = targetVineta.informacion.trim()
            val updatedText = if (currentText.isEmpty()) noteShortcut else "$currentText $noteShortcut"
            onGuardarVineta(targetVineta.copy(informacion = updatedText))
        }
    }

    val renderTableCardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 1. CABECERA TRAMO + CAPAS + BOTÓN BORRAR SELECCIONADAS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) RallySurface else Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (tramo?.tipo == "PE") RallyRed else RallyAccentAmber
                    ) {
                        Text(
                            text = tramo?.tipo ?: "PE",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLandscape) "ROADBOOK OFICIAL • ${vinetas.size} INDICACIONES" else "ROADBOOK • ${vinetas.size} IND.",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Chip de Regularidad y Velocidad Máxima del Tramo
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) RallyCyan.copy(alpha = 0.5f) else Color(0xFF94A3B8)),
                        modifier = Modifier.clickable { mostrarDialogoVelocidadesTramo = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val velPromObj = tramo?.velocidadPromedioObjetivoKmh ?: 0.0
                            val velMaxObj = tramo?.velocidadMaximaPermitidaKmh ?: 110.0
                            Text(
                                text = if (isLandscape) {
                                    "⚡ PROM: ${if (velPromObj > 0) "${velPromObj.toInt()} km/h" else "SIN DEF."} | 🔴 TOPE: ${velMaxObj.toInt()} km/h ⚙️"
                                } else {
                                    "⚡ ${if (velPromObj > 0) "${velPromObj.toInt()}k" else "-"} | 🔴 ${velMaxObj.toInt()}k ⚙️"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) RallyCyan else FredianiCyanText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // BOTÓN DE BORRADO MASIVO
                    if (vinetasSeleccionadasParaBorrar.isNotEmpty()) {
                        Button(
                            onClick = { mostrarDialogoConfirmarBorrado = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RallyRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BORRAR (${vinetasSeleccionadasParaBorrar.size})",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    if (vinetaEnEdicionId != null) {
                        val editandoNum = vinetas.find { it.id == vinetaEnEdicionId }?.numero ?: 1
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FredianiCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FredianiCyan)
                        ) {
                            Text(
                                text = if (isLandscape) "✏️ EDITANDO INDICACIÓN #$editandoNum" else "✏️ #$editandoNum",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isDark) RallyCyan else FredianiCyanText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { sidebarExpanded = !sidebarExpanded },
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (sidebarExpanded) (if (isDark) RallyCyan.copy(alpha = 0.15f) else FredianiCyan.copy(alpha = 0.15f)) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (sidebarExpanded) Icons.Default.ViewSidebar else Icons.Default.VerticalSplit,
                            contentDescription = if (sidebarExpanded) "Minimizar Símbolos" else "Mostrar Símbolos",
                            tint = if (sidebarExpanded) (if (isDark) RallyCyan else FredianiCyanText) else textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            // Perfil de elevación omitido temporalmente a pedido del usuario para maximizar espacio vertical

            // 3. BARRA UNIVERSAL SUPERIOR DE DIBUJO Y EDICIÓN
            RoadbookDrawingToolbar(
                vinetaEnEdicion = vinetas.find { it.id == vinetaEnEdicionId },
                activeTool = activeTool,
                selectedStrokeColor = selectedStrokeColor,
                strokeWidth = selectedStrokeWidth,
                asistenteTrazo = asistenteTrazoActivo,
                hasPinKm = selectedArrowIdx != null && selectedArrowIdx in drawingDraft.arrows.indices && drawingDraft.arrows[selectedArrowIdx!!].hasPin,
                onToggleAsistente = { asistenteTrazoActivo = !asistenteTrazoActivo },
                onAddArrow = {
                    val na = drawingDraft.arrows.toMutableList()
                    na.add(
                        ManeuverArrow(
                            p0 = PointData(0.5f, 0.85f),
                            p1 = PointData(0.5f, 0.60f),
                            p2 = PointData(0.65f, 0.40f),
                            p3 = PointData(0.80f, 0.25f),
                            strokeWidth = selectedStrokeWidth,
                            colorHex = selectedStrokeColor,
                            hasPin = true,
                            pinT = 0.5f,
                            pinSide = 1f
                        )
                    )
                    persistDraft(drawingDraft.copy(arrows = na))
                    selectedArrowIdx = na.lastIndex
                    selectedStampIdx = null
                    activeTool = ActiveDrawTool.SELECT
                },
                onTogglePinKm = {
                    if (selectedArrowIdx != null && selectedArrowIdx in drawingDraft.arrows.indices) {
                        val na = drawingDraft.arrows.toMutableList()
                        val cur = na[selectedArrowIdx!!]
                        val next = when {
                            cur.hasPin && cur.pinSide > 0 -> cur.copy(pinSide = -1f)
                            cur.hasPin && cur.pinSide < 0 -> cur.copy(hasPin = false)
                            else -> cur.copy(hasPin = true, pinSide = 1f)
                        }
                        na[selectedArrowIdx!!] = next
                        persistDraft(drawingDraft.copy(arrows = na))
                    }
                },
                onToolChange = { activeTool = it },
                onColorChange = { selectedStrokeColor = it },
                onStrokeWidthChange = { selectedStrokeWidth = it },
                onUndo = {
                    if (drawingDraft.strokes.isNotEmpty()) {
                        val nl = drawingDraft.strokes.toMutableList()
                        nl.removeAt(nl.lastIndex)
                        persistDraft(drawingDraft.copy(strokes = nl))
                    } else if (drawingDraft.arrows.isNotEmpty()) {
                        val na = drawingDraft.arrows.toMutableList()
                        na.removeAt(na.lastIndex)
                        selectedArrowIdx = null
                        persistDraft(drawingDraft.copy(arrows = na))
                    } else if (drawingDraft.stamps.isNotEmpty()) {
                        val nst = drawingDraft.stamps.toMutableList()
                        nst.removeAt(nst.lastIndex)
                        selectedStampIdx = null
                        persistDraft(drawingDraft.copy(stamps = nst))
                    }
                },
                onClear = {
                    if (selectedStampIdx != null && selectedStampIdx in drawingDraft.stamps.indices) {
                        val nst = drawingDraft.stamps.toMutableList()
                        nst.removeAt(selectedStampIdx!!)
                        selectedStampIdx = null
                        persistDraft(drawingDraft.copy(stamps = nst))
                    } else if (selectedArrowIdx != null && selectedArrowIdx in drawingDraft.arrows.indices) {
                        val na = drawingDraft.arrows.toMutableList()
                        na.removeAt(selectedArrowIdx!!)
                        selectedArrowIdx = null
                        persistDraft(drawingDraft.copy(arrows = na))
                    } else {
                        selectedStampIdx = null
                        selectedArrowIdx = null
                        persistDraft(DrawingData())
                    }
                },
                onGuardar = {
                    val cur = vinetas.find { it.id == vinetaEnEdicionId }
                    if (cur != null) {
                        val newTulipTipo = if (drawingDraft.isEmpty()) "BLANCO" else cur.tulipTipo
                        onGuardarVineta(cur.copy(
                            tulipSvgData = drawingDraft.toJson(),
                            tulipTipo = newTulipTipo
                        ))
                    }
                    vinetaEnEdicionId = null
                },
                onCerrar = {
                    vinetaEnEdicionId = null
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 4. SUB-CABECERA DE COLUMNAS CON CASILLA SELECCIONAR TODAS
            Surface(
                color = if (isDark) Color.Black else Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cabecera N° con checkbox para seleccionar todas
                    Box(
                        modifier = Modifier.weight(0.08f),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "N°",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (capaNumeroVisible) Color.White else Color.Gray,
                                modifier = Modifier.clickable { capaNumeroVisible = !capaNumeroVisible }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Checkbox(
                                checked = vinetas.isNotEmpty() && vinetas.all { it.id in vinetasSeleccionadasParaBorrar },
                                onCheckedChange = { checked ->
                                    vinetasSeleccionadasParaBorrar = if (checked) {
                                        vinetas.map { it.id }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = RallyRed,
                                    uncheckedColor = Color.White.copy(alpha = 0.8f),
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HeaderColumnaToggle("TOTAL", capaTotalVisible, { capaTotalVisible = !capaTotalVisible }, Modifier.weight(0.08f))
                    HeaderColumnaToggle("PARCIAL", capaParcialVisible, { capaParcialVisible = !capaParcialVisible }, Modifier.weight(0.08f))
                    HeaderColumnaToggle("DIBUJO", capaIndicacionVisible, { capaIndicacionVisible = !capaIndicacionVisible }, Modifier.weight(0.38f))
                    HeaderColumnaToggle("NOTAS", capaAnotacionesVisible, { capaAnotacionesVisible = !capaAnotacionesVisible }, Modifier.weight(0.30f))
                    HeaderColumnaToggle("REG.", capaRegresivaVisible, { capaRegresivaVisible = !capaRegresivaVisible }, Modifier.weight(0.08f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. LISTADO DE INDICACIONES CON EDICIÓN IN-PLACE DIRECTA
            if (vinetas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = textSecondary, modifier = Modifier.size(42.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sin indicaciones registradas. Pulse '+ AÑADIR INDICACIÓN' para registrar la primera.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = tableListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(vinetas, key = { it.id }) { v ->
                        val distRegresiva = (distanciaTotalEstimada - v.distanciaTotal).coerceAtLeast(0.0)
                        val isEditingThis = v.id == vinetaEnEdicionId

                        FilaRoadbookInPlace(
                            vineta = v,
                            distanciaRegresiva = distRegresiva,
                            isEditing = isEditingThis,
                            estaSeleccionadaParaBorrar = v.id in vinetasSeleccionadasParaBorrar,
                            onToggleSeleccionBorrar = {
                                vinetasSeleccionadasParaBorrar = if (v.id in vinetasSeleccionadasParaBorrar) {
                                    vinetasSeleccionadasParaBorrar - v.id
                                } else {
                                    vinetasSeleccionadasParaBorrar + v.id
                                }
                            },
                            drawingDraft = if (isEditingThis) drawingDraft else DrawingData.fromJson(v.tulipSvgData),
                            selectedStampIdx = if (isEditingThis) selectedStampIdx else null,
                            selectedArrowIdx = if (isEditingThis) selectedArrowIdx else null,
                            asistenteTrazoActivo = asistenteTrazoActivo,
                            activeTool = activeTool,
                            selectedColorHex = selectedStrokeColor,
                            strokeWidth = selectedStrokeWidth,
                            capaNumeroVisible = capaNumeroVisible,
                            capaTotalVisible = capaTotalVisible,
                            capaParcialVisible = capaParcialVisible,
                            capaIndicacionVisible = capaIndicacionVisible,
                            capaAnotacionesVisible = capaAnotacionesVisible,
                            capaRegresivaVisible = capaRegresivaVisible,
                            onTocarFila = {
                                onSeleccionarVineta(v)
                                vinetaEnEdicionId = v.id
                            },
                            onEditarIndicacion = {
                                onSeleccionarVineta(v)
                                vinetaEnEdicionId = v.id
                                vinetaParaEditarNotas = v
                            },
                            onActivarEdicionDiagrama = {
                                onSeleccionarVineta(v)
                                vinetaEnEdicionId = v.id
                            },
                            onTocarAnotaciones = {
                                vinetaParaEditarNotas = v
                            },
                            onUpdateDraft = { updatedDraft ->
                                persistDraft(updatedDraft)
                            },
                            onSelectStamp = { idx ->
                                selectedStampIdx = idx
                                if (idx != null) selectedArrowIdx = null
                            },
                            onSelectArrow = { idx ->
                                selectedArrowIdx = idx
                                if (idx != null) selectedStampIdx = null
                            },
                            onToolChange = { tool ->
                                activeTool = tool
                            },
                            onGuardarCambios = {
                                val saved = v.copy(tulipSvgData = drawingDraft.toJson())
                                onGuardarVineta(saved)
                                vinetaEnEdicionId = null
                            },
                            onCerrarEdicion = {
                                vinetaEnEdicionId = null
                            }
                        )
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(modifier = modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                renderTableCardContent()
            }

            // DIVISOR REDIMENSIONABLE / RESIZE SPLITTER
            if (sidebarExpanded) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaDp = dragAmount.x / density.density
                                sidebarWidthDp = (sidebarWidthDp - deltaDp).coerceIn(180f, 420f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    )
                }

                // PANEL DERECHO: PALETA DE SÍMBOLOS Y NOTAS FIA
                SidebarSymbolsPalette(
                    onInsertStamp = insertStampAction,
                    onAppendNoteText = appendNoteAction,
                    onCollapse = { sidebarExpanded = false },
                    vinetaNumeroActiva = vinetas.find { it.id == vinetaEnEdicionId }?.numero,
                    modifier = Modifier
                        .width(sidebarWidthDp.dp)
                        .fillMaxHeight()
                )
            } else {
                // Tira compacta colapsada (~28dp) para expandir con 1 solo clic
                Surface(
                    onClick = { sidebarExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) RallySurface else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Expandir Paleta de Símbolos",
                            tint = if (isDark) RallyCyan else FredianiCyanText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    } else {
        // =========================================================================
        // PORTRAIT: TABLA 100% ANCHO COMPLETO SIN DEFORMACIONES
        // PALETA DE SÍMBOLOS DOCKADA ABAJO SI EL USUARIO LA ABRE
        // =========================================================================
        Column(modifier = modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                renderTableCardContent()
            }

            if (sidebarExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                SidebarSymbolsPalette(
                    onInsertStamp = insertStampAction,
                    onAppendNoteText = appendNoteAction,
                    onCollapse = { sidebarExpanded = false },
                    vinetaNumeroActiva = vinetas.find { it.id == vinetaEnEdicionId }?.numero,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }
        }
    }

    // Modal de edición de texto y notas si se pulsa la columna de anotaciones
    vinetaParaEditarNotas?.let { v ->
        RoadbookNotesDialog(
            vineta = v,
            onGuardar = { nota, peligro, distTot, distPar, notasSvg, offRoad, velProm, velMax, tIdeal, rCap, esReinicio, distOculta, esOculta, mostrarWpt, esTierra, esPc, tipoPc ->
                val vinetaActualizada = v.copy(
                    informacion = nota,
                    peligro = peligro,
                    distanciaTotal = distTot,
                    distanciaParcial = distPar,
                    notasSvgData = notasSvg,
                    esOffRoad = offRoad,
                    velocidadPromedioSectorKmh = velProm,
                    velocidadMaximaSectorKmh = velMax,
                    tiempoIdealSegundos = tIdeal,
                    radioCapturaMetros = rCap,
                    esReinicioCero = esReinicio,
                    distanciaOculta = distOculta,
                    esOcultaOrg = esOculta,
                    mostrarWpt = mostrarWpt,
                    esTierra = esTierra,
                    esPuntoControl = esPc,
                    tipoPuntoControl = tipoPc
                )
                vinetaParaEditarNotas = null

                val deltaKm = distTot - v.distanciaTotal
                val deltaMetros = kotlin.math.round(deltaKm * 1000.0).toInt()
                if (deltaMetros != 0 && onPropagarDiferencia != null) {
                    datosPropagacionPendiente = Triple(vinetaActualizada, deltaKm, deltaMetros)
                } else {
                    onGuardarVineta(vinetaActualizada)
                }
            },
            onDismiss = { vinetaParaEditarNotas = null }
        )
    }

    // Modal de confirmación para propagar diferencia kilométrica a las viñetas posteriores
    datosPropagacionPendiente?.let { (vinetaActualizada, deltaKm, deltaMetros) ->
        val signo = if (deltaMetros > 0) "+$deltaMetros" else "$deltaMetros"
        AlertDialog(
            onDismissRequest = { datosPropagacionPendiente = null },
            title = {
                Text("Propagar Diferencia Kilométrica", fontWeight = FontWeight.Black)
            },
            text = {
                Text(
                    "El kilometraje total de la viñeta #${vinetaActualizada.numero} se modificó en $signo metros.\n\n" +
                    "¿Desea sumar la diferencia ($signo mts) a todas las próximas viñetas del tramo?",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGuardarVineta(vinetaActualizada)
                        onPropagarDiferencia?.invoke(vinetaActualizada.numero, deltaKm)
                        datosPropagacionPendiente = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen)
                ) {
                    Text("Sí, propagar a las siguientes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onGuardarVineta(vinetaActualizada)
                        datosPropagacionPendiente = null
                    }) {
                        Text("No, solo esta viñeta")
                    }
                    TextButton(onClick = { datosPropagacionPendiente = null }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Modal para configurar velocidades del tramo (regularidad y tope máximo)
    if (mostrarDialogoVelocidadesTramo && tramo != null) {
        ConfigurarVelocidadesTramoDialog(
            tramo = tramo,
            onGuardar = { velProm, velMax ->
                onGuardarTramo?.invoke(
                    tramo.copy(
                        velocidadPromedioObjetivoKmh = velProm,
                        velocidadMaximaPermitidaKmh = velMax
                    )
                )
                mostrarDialogoVelocidadesTramo = false
            },
            onDismiss = { mostrarDialogoVelocidadesTramo = false }
        )
    }

    // Diálogo de confirmación para borrado de viñetas seleccionadas
    if (mostrarDialogoConfirmarBorrado) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmarBorrado = false },
            title = {
                Text(
                    text = "Eliminar ${vinetasSeleccionadasParaBorrar.size} indicación(es)",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "¿Está seguro de que desea eliminar las ${vinetasSeleccionadasParaBorrar.size} indicaciones seleccionadas del roadbook? Esta acción no se puede deshacer.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = vinetasSeleccionadasParaBorrar.toSet()
                        if (onEliminarVinetas != null) {
                            onEliminarVinetas(idsToDelete)
                        } else {
                            val toDelete = vinetas.filter { it.id in idsToDelete }
                            toDelete.forEach { onEliminarVineta(it) }
                        }
                        if (vinetaEnEdicionId in idsToDelete) {
                            vinetaEnEdicionId = null
                        }
                        vinetasSeleccionadasParaBorrar = emptySet()
                        mostrarDialogoConfirmarBorrado = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RallyRed)
                ) {
                    Text("ELIMINAR", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirmarBorrado = false }) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilaRoadbookInPlace(
    vineta: VinetaEntity,
    distanciaRegresiva: Double,
    isEditing: Boolean,
    estaSeleccionadaParaBorrar: Boolean = false,
    onToggleSeleccionBorrar: () -> Unit = {},
    drawingDraft: DrawingData,
    selectedStampIdx: Int?,
    selectedArrowIdx: Int? = null,
    asistenteTrazoActivo: Boolean = true,
    activeTool: ActiveDrawTool,
    selectedColorHex: String,
    strokeWidth: Float = 7f,
    capaNumeroVisible: Boolean,
    capaTotalVisible: Boolean,
    capaParcialVisible: Boolean,
    capaIndicacionVisible: Boolean,
    capaAnotacionesVisible: Boolean,
    capaRegresivaVisible: Boolean,
    onTocarFila: () -> Unit,
    onEditarIndicacion: () -> Unit = onTocarFila,
    onActivarEdicionDiagrama: () -> Unit,
    onTocarAnotaciones: () -> Unit,
    onUpdateDraft: (DrawingData) -> Unit,
    onSelectStamp: (Int?) -> Unit,
    onSelectArrow: (Int?) -> Unit = {},
    onToolChange: (ActiveDrawTool) -> Unit,
    onGuardarCambios: () -> Unit,
    onCerrarEdicion: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    val cardBg = if (isEditing) {
        if (isDark) Color(0xFF0F2338) else Color(0xFFF0F9FF)
    } else {
        if (isDark) RallyCardBg else Color.White
    }
    val cardBorder = if (isEditing) FredianiCyan else (if (isDark) RallySurface else Color(0xFFE2E8F0))
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(if (isEditing) 2.dp else 1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onTocarFila() },
                onDoubleClick = { onEditarIndicacion() },
                onLongClick = { onEditarIndicacion() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isEditing) 200.dp else 160.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. NÚMERO Y CASILLA DE BORRADO DEBAJO
            Box(
                modifier = Modifier
                    .weight(0.08f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (capaNumeroVisible) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) FredianiCyan else Color(0xFF0F172A),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${vineta.numero}",
                                    color = if (isDark) Color.Black else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Checkbox(
                        checked = estaSeleccionadaParaBorrar,
                        onCheckedChange = { onToggleSeleccionBorrar() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = RallyRed,
                            uncheckedColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Gray,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 2. TOTAL KM
            Box(
                modifier = Modifier
                    .weight(0.08f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (capaTotalVisible) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (vineta.esReinicioCero) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = "0,00",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (vineta.distanciaOculta) "X.XX" else String.format("%.2f", vineta.distanciaTotal),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            color = if (vineta.distanciaOculta) Color(0xFFF59E0B) else textPrimary
                        )
                        Text(text = "KM TOT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    }
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 3. PARCIAL KM
            Box(
                modifier = Modifier
                    .weight(0.08f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (capaParcialVisible) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.2f", vineta.distanciaParcial),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            color = textPrimary
                        )
                        Text(text = "PARC", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    }
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 4. RECUADRO DIBUJO / DIAGRAMA CON EDICIÓN DIRECTA EN EL LUGAR
            Box(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(
                        width = if (isEditing) 2.dp else 1.dp,
                        color = if (isEditing) FredianiCyan else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                if (capaIndicacionVisible) {
                    if (isEditing) {
                        // MODO ACTIVO: LIENZO INTERACTIVO 100% ANCHO DE CELDA
                        InPlaceDiagramCanvas(
                            drawingData = drawingDraft,
                            selectedStampIdx = selectedStampIdx,
                            selectedArrowIdx = selectedArrowIdx,
                            asistenteTrazoActivo = asistenteTrazoActivo,
                            activeTool = activeTool,
                            selectedColorHex = selectedColorHex,
                            strokeWidth = strokeWidth,
                            fallbackTulip = "", // En modo edición interactiva nunca mostramos un fallback fantasma
                            onUpdateDraft = onUpdateDraft,
                            onSelectStamp = onSelectStamp,
                            onSelectArrow = onSelectArrow,
                            onToolChange = onToolChange
                        )
                    } else {
                        // MODO VISTA: DIAGRAMA VECTORIAL + BOTÓN EDITAR RÁPIDO
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .combinedClickable(
                                    onClick = { onActivarEdicionDiagrama() },
                                    onDoubleClick = { onEditarIndicacion() },
                                    onLongClick = { onEditarIndicacion() }
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                DiagramaCanvasRenderer.render(
                                    drawScope = this,
                                    drawingData = drawingDraft,
                                    fallbackTulip = if (vineta.tulipSvgData.isNotBlank() || vineta.tulipTipo == "BLANCO" || vineta.tulipTipo == "VACIO" || vineta.tulipTipo == "RECTA") "" else vineta.tulipTipo,
                                    primaryColor = Color.Black
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = FredianiCyan.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = FredianiCyanText, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 5. ANOTACIONES Y NOTAS
            Box(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) RallySurface else Color(0xFFF8FAFC))
                    .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onTocarAnotaciones() },
                        onDoubleClick = { onEditarIndicacion() },
                        onLongClick = { onEditarIndicacion() }
                    )
                    .padding(6.dp)
            ) {
                if (capaAnotacionesVisible) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badges de Peligro, Off-Road, DZ Max, y Regularidad Promedio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (vineta.peligro) {
                                    "! ATENCIÓN" -> FredianiAmberBg
                                    "!! PELIGRO", "!!!" -> FredianiRedBg
                                    "LARGADA" -> FredianiGreenBg
                                    else -> if (isDark) Color.Black else Color(0xFFE2E8F0)
                                }
                            ) {
                                Text(
                                    text = if (vineta.peligro.isNotBlank()) vineta.peligro else "NORMAL",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.5.sp,
                                    color = when (vineta.peligro) {
                                        "! ATENCIÓN" -> FredianiAmberText
                                        "!! PELIGRO", "!!!" -> FredianiRedText
                                        "LARGADA" -> FredianiGreenText
                                        else -> textSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            if (vineta.esOffRoad) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF78350F)
                                ) {
                                    Text(
                                        text = "🏜️ OFF-ROAD",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color(0xFFFEF3C7),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (vineta.velocidadMaximaSectorKmh > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF991B1B)
                                ) {
                                    Text(
                                        text = "🔴 DZ ${vineta.velocidadMaximaSectorKmh.toInt()}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (vineta.velocidadPromedioSectorKmh > 0) {
                                val segs = if (vineta.tiempoIdealSegundos > 0) vineta.tiempoIdealSegundos else {
                                    (vineta.distanciaParcial / vineta.velocidadPromedioSectorKmh) * 3600
                                }
                                val m = (segs / 60).toInt()
                                val s = (segs % 60).toInt()
                                val tLabel = String.format("%02d'%02d\"", m, s)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E3A8A)
                                ) {
                                    Text(
                                        text = "⚡ ${vineta.velocidadPromedioSectorKmh.toInt()}k ($tLabel)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (vineta.esReinicioCero) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF065F46)
                                ) {
                                    Text(
                                        text = "0,00 REINICIO",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color(0xFFA7F3D0),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (vineta.distanciaOculta) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF7C2D12)
                                ) {
                                    Text(
                                        text = "X.XX OCULTA",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color(0xFFFED7AA),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (vineta.esOcultaOrg) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF831843)
                                ) {
                                    Text(
                                        text = "🔒 OCUL",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color(0xFFFBCFE8),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (vineta.esTierra) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF713F12)
                                ) {
                                    Text(
                                        text = "DIRT",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color(0xFFFEF08A),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (vineta.esPuntoControl) {
                                val tipo = if (vineta.tipoPuntoControl.isNotBlank()) vineta.tipoPuntoControl else "PASO"
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF581C87)
                                ) {
                                    Text(
                                        text = "🎯 WP $tipo",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 7.5.sp,
                                        color = Color(0xFFE9D5FF),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (vineta.informacion.isNotBlank()) vineta.informacion else "Tocar para añadir notas...",
                            fontWeight = if (vineta.informacion.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.5.sp,
                            color = if (vineta.informacion.isNotBlank()) textPrimary else textSecondary,
                            maxLines = 2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RUMBO: ${String.format("%.0f°", vineta.rumbo)} • CAP",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                            if (vineta.mostrarWpt) {
                                Text(
                                    text = "WPT: ${String.format(java.util.Locale.US, "%.4f, %.4f", vineta.latitud, vineta.longitud)}",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) RallyCyan else FredianiCyanText
                                )
                            }
                            if (vineta.radioCapturaMetros != 30) {
                                Text(
                                    text = "WPV ${vineta.radioCapturaMetros}m",
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 6. REGRESIVA
            Box(
                modifier = Modifier
                    .weight(0.08f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (capaRegresivaVisible) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.2f", distanciaRegresiva),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = if (isDark) RallyCyanLight else FredianiCyanText
                        )
                        Text(text = "REGRESIVA", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    }
                }
            }
        }
    }
}

/**
 * LIENZO INTERACTIVO DE DIBUJO DENTRO DE LA MISMA CELDA DE VIÑETA (IN-PLACE)
 * Con ancho 100% de la celda de la columna DIBUJO (sin barras internas que reduzcan el área).
 * Controles universales ubicados en la barra superior (Rally Navigator Style).
 */
@Composable
fun InPlaceDiagramCanvas(
    drawingData: DrawingData,
    selectedStampIdx: Int?,
    selectedArrowIdx: Int? = null,
    asistenteTrazoActivo: Boolean = true,
    activeTool: ActiveDrawTool,
    selectedColorHex: String,
    strokeWidth: Float = 7f,
    fallbackTulip: String,
    onUpdateDraft: (DrawingData) -> Unit,
    onSelectStamp: (Int?) -> Unit,
    onSelectArrow: (Int?) -> Unit = {},
    onToolChange: (ActiveDrawTool) -> Unit
) {
    var strokes by remember { mutableStateOf(drawingData.strokes.toMutableList()) }
    var stamps by remember { mutableStateOf(drawingData.stamps.toMutableList()) }
    var arrows by remember { mutableStateOf(drawingData.arrows.toMutableList()) }
    var currentStrokePoints by remember { mutableStateOf<List<PointData>>(emptyList()) }
    var dragTarget by remember { mutableStateOf(DragTarget.NONE) }
    var rulerStartPoint by remember { mutableStateOf<PointData?>(null) }
    var rulerCurrentPoint by remember { mutableStateOf<PointData?>(null) }

    var activeStampIdx by remember { mutableStateOf(selectedStampIdx) }
    var activeArrowIdx by remember { mutableStateOf(selectedArrowIdx) }

    var resizeInitialScale by remember { mutableStateOf(1f) }
    var resizeInitialDist by remember { mutableStateOf(1f) }
    var resizeInitialPoints by remember { mutableStateOf<List<PointData>>(emptyList()) }
    var resizeCenter by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(drawingData) {
        if (dragTarget == DragTarget.NONE) {
            strokes = drawingData.strokes.toMutableList()
            stamps = drawingData.stamps.toMutableList()
            arrows = drawingData.arrows.toMutableList()
        }
    }
    LaunchedEffect(selectedStampIdx) {
        activeStampIdx = selectedStampIdx
    }
    LaunchedEffect(selectedArrowIdx) {
        activeArrowIdx = selectedArrowIdx
    }

    val currentStamps by rememberUpdatedState(stamps)
    val currentArrows by rememberUpdatedState(arrows)
    val currentStrokes by rememberUpdatedState(strokes)
    val currentSelectedStampIdx by rememberUpdatedState(selectedStampIdx)
    val currentSelectedArrowIdx by rememberUpdatedState(selectedArrowIdx)
    val currentActiveTool by rememberUpdatedState(activeTool)
    val currentSelectedColorHex by rememberUpdatedState(selectedColorHex)
    val currentStrokeWidth by rememberUpdatedState(strokeWidth)
    val currentAsistenteTrazo by rememberUpdatedState(asistenteTrazoActivo)
    val currentOnUpdateDraft by rememberUpdatedState(onUpdateDraft)
    val currentOnSelectStamp by rememberUpdatedState(onSelectStamp)
    val currentOnSelectArrow by rememberUpdatedState(onSelectArrow)
    val currentOnToolChange by rememberUpdatedState(onToolChange)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0 || h <= 0) return@detectDragGestures

                        // 1. ¿Toca tiradores o marco del sello actualmente seleccionado?
                        val selStampIdx = activeStampIdx ?: currentSelectedStampIdx
                        if (selStampIdx != null && selStampIdx in stamps.indices) {
                            val sel = stamps[selStampIdx]
                            val cx = sel.x * w
                            val cy = sel.y * h
                            val boxRadius = (60f * sel.scale).coerceAtLeast(42f)
                            val left = cx - boxRadius
                            val top = cy - boxRadius
                            val right = cx + boxRadius
                            val bottom = cy + boxRadius

                            // Botón borrar flotante (esquina superior derecha)
                            val delBadgeStamp = Offset(right + 12f, top - 12f)
                            if (Math.hypot((offset.x - delBadgeStamp.x).toDouble(), (offset.y - delBadgeStamp.y).toDouble()) <= 32f) {
                                val nst = stamps.toMutableList()
                                nst.removeAt(selStampIdx)
                                stamps = nst
                                activeStampIdx = null
                                currentOnSelectStamp(null)
                                dragTarget = DragTarget.NONE
                                currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                return@detectDragGestures
                            }

                            // 4 esquinas PowerPoint para redimensionar (TL, TR, BL, BR)
                            val corners = listOf(
                                Offset(left, top),
                                Offset(right, top),
                                Offset(left, bottom),
                                Offset(right, bottom)
                            )
                            for (c in corners) {
                                if (Math.hypot((offset.x - c.x).toDouble(), (offset.y - c.y).toDouble()) <= 42f) {
                                    activeStampIdx = selStampIdx
                                    resizeInitialScale = sel.scale
                                    resizeInitialDist = Math.hypot((offset.x - cx).toDouble(), (offset.y - cy).toDouble()).toFloat().coerceAtLeast(10f)
                                    dragTarget = DragTarget.RESIZE_STAMP
                                    return@detectDragGestures
                                }
                            }

                            // Toca dentro del sello actualmente seleccionado para moverlo
                            if (offset.x in (left - 16f)..(right + 16f) && offset.y in (top - 16f)..(bottom + 16f)) {
                                activeStampIdx = selStampIdx
                                dragTarget = DragTarget.MOVE_STAMP
                                return@detectDragGestures
                            }
                        }

                        // 2. ¿Toca tiradores o cuerpo de la flecha actualmente seleccionada?
                        val selArrIdx = activeArrowIdx ?: currentSelectedArrowIdx
                        if (selArrIdx != null && selArrIdx in arrows.indices) {
                            val arr = arrows[selArrIdx]
                            val p0 = Offset(arr.p0.x * w, arr.p0.y * h)
                            val p1 = Offset(arr.p1.x * w, arr.p1.y * h)
                            val p2 = Offset(arr.p2.x * w, arr.p2.y * h)
                            val p3 = Offset(arr.p3.x * w, arr.p3.y * h)

                            val minX = minOf(p0.x, p1.x, p2.x, p3.x)
                            val maxX = maxOf(p0.x, p1.x, p2.x, p3.x)
                            val minY = minOf(p0.y, p1.y, p2.y, p3.y)
                            val maxY = maxOf(p0.y, p1.y, p2.y, p3.y)
                            val pad = 32f
                            val left = minX - pad
                            val top = minY - pad
                            val right = maxX + pad
                            val bottom = maxY + pad
                            val cx = (minX + maxX) / 2f
                            val cy = (minY + maxY) / 2f

                            // Botón borrar flotante sobre esquina superior derecha de la flecha
                            val delBadgeArrow = Offset(right + 12f, top - 12f)
                            if (Math.hypot((offset.x - delBadgeArrow.x).toDouble(), (offset.y - delBadgeArrow.y).toDouble()) <= 32f) {
                                val na = arrows.toMutableList()
                                na.removeAt(selArrIdx)
                                arrows = na
                                activeArrowIdx = null
                                currentOnSelectArrow(null)
                                dragTarget = DragTarget.NONE
                                currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                return@detectDragGestures
                            }

                            // 4 esquinas PowerPoint para redimensionar la flecha
                            val cornersArrow = listOf(
                                Offset(left, top),
                                Offset(right, top),
                                Offset(left, bottom),
                                Offset(right, bottom)
                            )
                            for (c in cornersArrow) {
                                if (Math.hypot((offset.x - c.x).toDouble(), (offset.y - c.y).toDouble()) <= 42f) {
                                    activeArrowIdx = selArrIdx
                                    resizeCenter = Offset(cx, cy)
                                    resizeInitialPoints = listOf(arr.p0, arr.p1, arr.p2, arr.p3)
                                    resizeInitialDist = Math.hypot((offset.x - cx).toDouble(), (offset.y - cy).toDouble()).toFloat().coerceAtLeast(10f)
                                    dragTarget = DragTarget.RESIZE_ARROW
                                    return@detectDragGestures
                                }
                            }

                            // Tiradores individuales P0, P1, P2, P3
                            if (Math.hypot((offset.x - p0.x).toDouble(), (offset.y - p0.y).toDouble()) <= 38f) {
                                activeArrowIdx = selArrIdx
                                dragTarget = DragTarget.ARROW_P0
                                return@detectDragGestures
                            }
                            if (Math.hypot((offset.x - p1.x).toDouble(), (offset.y - p1.y).toDouble()) <= 38f) {
                                activeArrowIdx = selArrIdx
                                dragTarget = DragTarget.ARROW_P1
                                return@detectDragGestures
                            }
                            if (Math.hypot((offset.x - p2.x).toDouble(), (offset.y - p2.y).toDouble()) <= 38f) {
                                activeArrowIdx = selArrIdx
                                dragTarget = DragTarget.ARROW_P2
                                return@detectDragGestures
                            }
                            if (Math.hypot((offset.x - p3.x).toDouble(), (offset.y - p3.y).toDouble()) <= 38f) {
                                activeArrowIdx = selArrIdx
                                dragTarget = DragTarget.ARROW_P3
                                return@detectDragGestures
                            }

                            // Tirador del Pin de Kilometraje Oficial FIA (Art. 5.6.2)
                            if (arr.hasPin) {
                                val t = arr.pinT.coerceIn(0.15f, 0.85f)
                                val u = 1f - t
                                val bx = u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x
                                val by = u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y
                                val tdx = 3 * u * u * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * t * t * (p3.x - p2.x)
                                val tdy = 3 * u * u * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * t * t * (p3.y - p2.y)
                                val tAngle = Math.atan2(tdy.toDouble(), tdx.toDouble()).toFloat()
                                val sideSign = if (arr.pinSide >= 0) 1f else -1f
                                val pinAngle = tAngle + sideSign * (Math.PI.toFloat() * 0.72f)
                                val pinLen = 28f
                                val pinHead = Offset(
                                    (bx + pinLen * Math.cos(pinAngle.toDouble())).toFloat(),
                                    (by + pinLen * Math.sin(pinAngle.toDouble())).toFloat()
                                )
                                if (Math.hypot((offset.x - pinHead.x).toDouble(), (offset.y - pinHead.y).toDouble()) <= 42f) {
                                    activeArrowIdx = selArrIdx
                                    dragTarget = DragTarget.ARROW_PIN
                                    return@detectDragGestures
                                }
                            }

                            // Toca dentro del marco de la flecha o a lo largo del cuerpo para moverla
                            if (offset.x in left..right && offset.y in top..bottom) {
                                activeArrowIdx = selArrIdx
                                dragTarget = DragTarget.MOVE_ARROW
                                return@detectDragGestures
                            }
                        }

                        // 3. ¿TOCA DIRECTAMENTE ENCIMA DE CUALQUIER SELLO? (SIEMPRE LO SELECCIONA Y MUEVE)
                        for (i in stamps.indices.reversed()) {
                            val s = stamps[i]
                            val cx = s.x * w
                            val cy = s.y * h
                            val boxRadius = (60f * s.scale).coerceAtLeast(42f)
                            if (offset.x in (cx - boxRadius - 16f)..(cx + boxRadius + 16f) &&
                                offset.y in (cy - boxRadius - 16f)..(cy + boxRadius + 16f)) {
                                activeStampIdx = i
                                activeArrowIdx = null
                                currentOnSelectStamp(i)
                                currentOnSelectArrow(null)
                                currentOnToolChange(ActiveDrawTool.SELECT)
                                dragTarget = DragTarget.MOVE_STAMP
                                return@detectDragGestures
                            }
                        }

                        // 4. ¿TOCA DIRECTAMENTE ENCIMA DE CUALQUIER FLECHA?
                        for (i in arrows.indices.reversed()) {
                            val arr = arrows[i]
                            val p0 = Offset(arr.p0.x * w, arr.p0.y * h)
                            val p1 = Offset(arr.p1.x * w, arr.p1.y * h)
                            val p2 = Offset(arr.p2.x * w, arr.p2.y * h)
                            val p3 = Offset(arr.p3.x * w, arr.p3.y * h)
                            var hit = false
                            for (step in 0..20) {
                                val t = step / 20f
                                val bx = (1 - t) * (1 - t) * (1 - t) * p0.x + 3 * (1 - t) * (1 - t) * t * p1.x + 3 * (1 - t) * t * t * p2.x + t * t * t * p3.x
                                val by = (1 - t) * (1 - t) * (1 - t) * p0.y + 3 * (1 - t) * (1 - t) * t * p1.y + 3 * (1 - t) * t * t * p2.y + t * t * t * p3.y
                                if (Math.hypot((offset.x - bx).toDouble(), (offset.y - by).toDouble()) <= 44f) {
                                    hit = true
                                    break
                                }
                            }
                            if (hit) {
                                activeArrowIdx = i
                                activeStampIdx = null
                                currentOnSelectArrow(i)
                                currentOnSelectStamp(null)
                                currentOnToolChange(ActiveDrawTool.SELECT)
                                dragTarget = DragTarget.MOVE_ARROW
                                return@detectDragGestures
                            }
                        }

                        // 5. Toca en lienzo vacío
                        when (currentActiveTool) {
                            ActiveDrawTool.PEN -> {
                                dragTarget = DragTarget.DRAW_STROKE
                                currentStrokePoints = listOf(PointData(offset.x / w, offset.y / h))
                            }
                            ActiveDrawTool.RULER -> {
                                dragTarget = DragTarget.DRAW_STROKE
                                val pt = PointData(offset.x / w, offset.y / h)
                                rulerStartPoint = pt
                                rulerCurrentPoint = pt
                            }
                            ActiveDrawTool.SELECT -> {
                                activeStampIdx = null
                                activeArrowIdx = null
                                currentOnSelectStamp(null)
                                currentOnSelectArrow(null)
                                dragTarget = DragTarget.NONE
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0 || h <= 0) return@detectDragGestures

                        when (dragTarget) {
                            DragTarget.MOVE_STAMP -> {
                                val idx = activeStampIdx
                                if (idx != null && idx in stamps.indices) {
                                    val cur = stamps[idx]
                                    val dx = dragAmount.x / w
                                    val dy = dragAmount.y / h
                                    val nst = stamps.toMutableList()
                                    nst[idx] = cur.copy(
                                        x = (cur.x + dx).coerceIn(0.05f, 0.95f),
                                        y = (cur.y + dy).coerceIn(0.05f, 0.95f)
                                    )
                                    stamps = nst
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.RESIZE_STAMP -> {
                                val idx = activeStampIdx
                                if (idx != null && idx in stamps.indices && resizeInitialDist > 0f) {
                                    val cur = stamps[idx]
                                    val cx = cur.x * w
                                    val cy = cur.y * h
                                    val currentDist = Math.hypot((change.position.x - cx).toDouble(), (change.position.y - cy).toDouble()).toFloat()
                                    val ratio = currentDist / resizeInitialDist
                                    val newScale = (resizeInitialScale * ratio).coerceIn(0.35f, 4.0f)
                                    val nst = stamps.toMutableList()
                                    nst[idx] = cur.copy(scale = newScale)
                                    stamps = nst
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.RESIZE_ARROW -> {
                                val idx = activeArrowIdx
                                if (idx != null && idx in arrows.indices && resizeInitialPoints.size == 4 && resizeInitialDist > 0f) {
                                    val currentDist = Math.hypot((change.position.x - resizeCenter.x).toDouble(), (change.position.y - resizeCenter.y).toDouble()).toFloat()
                                    val ratio = (currentDist / resizeInitialDist).coerceIn(0.15f, 5.0f)
                                    val p0Init = resizeInitialPoints[0]
                                    val p1Init = resizeInitialPoints[1]
                                    val p2Init = resizeInitialPoints[2]
                                    val p3Init = resizeInitialPoints[3]
                                    val cxRel = resizeCenter.x / w
                                    val cyRel = resizeCenter.y / h

                                    fun scalePoint(pt: PointData): PointData {
                                        val nx = cxRel + (pt.x - cxRel) * ratio
                                        val ny = cyRel + (pt.y - cyRel) * ratio
                                        return PointData(nx.coerceIn(0f, 1f), ny.coerceIn(0f, 1f))
                                    }

                                    val na = arrows.toMutableList()
                                    na[idx] = na[idx].copy(
                                        p0 = scalePoint(p0Init),
                                        p1 = scalePoint(p1Init),
                                        p2 = scalePoint(p2Init),
                                        p3 = scalePoint(p3Init)
                                    )
                                    arrows = na
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.ARROW_P0 -> {
                                val idx = activeArrowIdx
                                if (idx != null && idx in arrows.indices) {
                                    val na = arrows.toMutableList()
                                    na[idx] = na[idx].copy(p0 = PointData((change.position.x / w).coerceIn(0f, 1f), (change.position.y / h).coerceIn(0f, 1f)))
                                    arrows = na
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.ARROW_P1 -> {
                                val idx = activeArrowIdx
                                if (idx != null && idx in arrows.indices) {
                                    val na = arrows.toMutableList()
                                    na[idx] = na[idx].copy(p1 = PointData((change.position.x / w).coerceIn(0f, 1f), (change.position.y / h).coerceIn(0f, 1f)))
                                    arrows = na
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.ARROW_P2 -> {
                                val idx = activeArrowIdx
                                if (idx != null && idx in arrows.indices) {
                                    val na = arrows.toMutableList()
                                    na[idx] = na[idx].copy(p2 = PointData((change.position.x / w).coerceIn(0f, 1f), (change.position.y / h).coerceIn(0f, 1f)))
                                    arrows = na
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.ARROW_P3 -> {
                                val idx = activeArrowIdx
                                if (idx != null && idx in arrows.indices) {
                                    val na = arrows.toMutableList()
                                    na[idx] = na[idx].copy(p3 = PointData((change.position.x / w).coerceIn(0f, 1f), (change.position.y / h).coerceIn(0f, 1f)))
                                    arrows = na
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.ARROW_PIN -> {
                                val idx = activeArrowIdx
                                if (idx != null && idx in arrows.indices) {
                                    val cur = arrows[idx]
                                    val p0 = Offset(cur.p0.x * w, cur.p0.y * h)
                                    val p1 = Offset(cur.p1.x * w, cur.p1.y * h)
                                    val p2 = Offset(cur.p2.x * w, cur.p2.y * h)
                                    val p3 = Offset(cur.p3.x * w, cur.p3.y * h)
                                    val touchPos = change.position

                                    // Muestreo paramétrico sobre la curva Bézier para encontrar el t más cercano
                                    var bestT = cur.pinT
                                    var bestDist = Float.MAX_VALUE
                                    for (step in 15..85 step 2) {
                                        val st = step / 100f
                                        val su = 1f - st
                                        val bx = su * su * su * p0.x + 3 * su * su * st * p1.x + 3 * su * st * st * p2.x + st * st * st * p3.x
                                        val by = su * su * su * p0.y + 3 * su * su * st * p1.y + 3 * su * st * st * p2.y + st * st * st * p3.y
                                        val d = Math.hypot((touchPos.x - bx).toDouble(), (touchPos.y - by).toDouble()).toFloat()
                                        if (d < bestDist) {
                                            bestDist = d
                                            bestT = st
                                        }
                                    }

                                    // Lado de la curva (orientación respecto al vector tangente)
                                    val u = 1f - bestT
                                    val bx = u * u * u * p0.x + 3 * u * u * bestT * p1.x + 3 * u * bestT * bestT * p2.x + bestT * bestT * bestT * p3.x
                                    val by = u * u * u * p0.y + 3 * u * u * bestT * p1.y + 3 * u * bestT * bestT * p2.y + bestT * bestT * bestT * p3.y
                                    val tdx = 3 * u * u * (p1.x - p0.x) + 6 * u * bestT * (p2.x - p1.x) + 3 * bestT * bestT * (p3.x - p2.x)
                                    val tdy = 3 * u * u * (p1.y - p0.y) + 6 * u * bestT * (p2.y - p1.y) + 3 * bestT * bestT * (p3.y - p2.y)

                                    val cross = (touchPos.x - bx) * tdy - (touchPos.y - by) * tdx
                                    val newSide = if (cross >= 0) 1f else -1f

                                    val na = arrows.toMutableList()
                                    na[idx] = cur.copy(pinT = bestT, pinSide = newSide)
                                    arrows = na
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.MOVE_ARROW -> {
                                val idx = activeArrowIdx
                                if (idx != null && idx in arrows.indices) {
                                    val cur = arrows[idx]
                                    val dx = dragAmount.x / w
                                    val dy = dragAmount.y / h
                                    val na = arrows.toMutableList()
                                    na[idx] = cur.copy(
                                        p0 = PointData((cur.p0.x + dx).coerceIn(0f, 1f), (cur.p0.y + dy).coerceIn(0f, 1f)),
                                        p1 = PointData((cur.p1.x + dx).coerceIn(0f, 1f), (cur.p1.y + dy).coerceIn(0f, 1f)),
                                        p2 = PointData((cur.p2.x + dx).coerceIn(0f, 1f), (cur.p2.y + dy).coerceIn(0f, 1f)),
                                        p3 = PointData((cur.p3.x + dx).coerceIn(0f, 1f), (cur.p3.y + dy).coerceIn(0f, 1f))
                                    )
                                    arrows = na
                                    currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                                }
                            }
                            DragTarget.DRAW_STROKE -> {
                                if (currentActiveTool == ActiveDrawTool.PEN) {
                                    currentStrokePoints = currentStrokePoints + PointData(
                                        (change.position.x / w).coerceIn(0f, 1f),
                                        (change.position.y / h).coerceIn(0f, 1f)
                                    )
                                } else if (currentActiveTool == ActiveDrawTool.RULER) {
                                    rulerCurrentPoint = PointData(
                                        (change.position.x / w).coerceIn(0f, 1f),
                                        (change.position.y / h).coerceIn(0f, 1f)
                                    )
                                }
                            }
                            DragTarget.NONE -> {}
                        }
                    },
                    onDragEnd = {
                        if (dragTarget == DragTarget.DRAW_STROKE) {
                            if (currentActiveTool == ActiveDrawTool.PEN && currentStrokePoints.size >= 2) {
                                if (currentAsistenteTrazo && currentStrokePoints.size >= 5) {
                                    val p0 = currentStrokePoints.first()
                                    val p3 = currentStrokePoints.last()
                                    val distTotal = Math.hypot((p3.x - p0.x).toDouble(), (p3.y - p0.y).toDouble())
                                    if (distTotal >= 0.12) {
                                        val idx1 = currentStrokePoints.size / 3
                                        val idx2 = (currentStrokePoints.size * 2) / 3
                                        val p1 = currentStrokePoints[idx1]
                                        val p2 = currentStrokePoints[idx2]
                                        val newArrow = ManeuverArrow(
                                            p0 = p0,
                                            p1 = p1,
                                            p2 = p2,
                                            p3 = p3,
                                            strokeWidth = currentStrokeWidth,
                                            colorHex = currentSelectedColorHex
                                        )
                                        val na = arrows.toMutableList()
                                        na.add(newArrow)
                                        arrows = na
                                        activeArrowIdx = arrows.lastIndex
                                        activeStampIdx = null
                                        currentOnSelectArrow(arrows.lastIndex)
                                        currentOnSelectStamp(null)
                                        currentOnToolChange(ActiveDrawTool.SELECT)
                                    } else {
                                        val nl = strokes.toMutableList()
                                        nl.add(StrokeData(points = currentStrokePoints, colorHex = currentSelectedColorHex, strokeWidth = currentStrokeWidth))
                                        strokes = nl
                                    }
                                } else {
                                    val nl = strokes.toMutableList()
                                    nl.add(StrokeData(points = currentStrokePoints, colorHex = currentSelectedColorHex, strokeWidth = currentStrokeWidth))
                                    strokes = nl
                                }
                            } else if (currentActiveTool == ActiveDrawTool.RULER && rulerStartPoint != null && rulerCurrentPoint != null) {
                                val nl = strokes.toMutableList()
                                nl.add(StrokeData(points = listOf(rulerStartPoint!!, rulerCurrentPoint!!), colorHex = currentSelectedColorHex, strokeWidth = currentStrokeWidth))
                                strokes = nl
                            }
                        }
                        currentStrokePoints = emptyList()
                        rulerStartPoint = null
                        rulerCurrentPoint = null
                        dragTarget = DragTarget.NONE
                        currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                    },
                    onDragCancel = {
                        currentStrokePoints = emptyList()
                        rulerStartPoint = null
                        rulerCurrentPoint = null
                        dragTarget = DragTarget.NONE
                        currentOnUpdateDraft(DrawingData(strokes = strokes, stamps = stamps, arrows = arrows))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val selectedArrowToRender = if (currentActiveTool == ActiveDrawTool.SELECT) (activeArrowIdx ?: currentSelectedArrowIdx) else null
            // Renderizar contenido base
            DiagramaCanvasRenderer.render(
                drawScope = this,
                drawingData = DrawingData(strokes = strokes, stamps = stamps, arrows = arrows),
                fallbackTulip = fallbackTulip,
                primaryColor = Color.Black,
                selectedArrowIdx = selectedArrowToRender
            )

            // Trazo libre temporal mientras se dibuja con PEN
            if (currentActiveTool == ActiveDrawTool.PEN && currentStrokePoints.size >= 2) {
                val path = androidx.compose.ui.graphics.Path()
                val first = currentStrokePoints[0]
                path.moveTo(first.x * size.width, first.y * size.height)
                for (i in 1 until currentStrokePoints.size) {
                    val pt = currentStrokePoints[i]
                    path.lineTo(pt.x * size.width, pt.y * size.height)
                }
                val strokeCol = try { Color(android.graphics.Color.parseColor(currentSelectedColorHex)) } catch (e: Exception) { Color.Black }
                drawPath(
                    path = path,
                    color = strokeCol,
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Trazo de regla recta temporal mientras se arrastra con RULER
            if (currentActiveTool == ActiveDrawTool.RULER && rulerStartPoint != null && rulerCurrentPoint != null) {
                val strokeCol = try { Color(android.graphics.Color.parseColor(currentSelectedColorHex)) } catch (e: Exception) { Color.Black }
                drawLine(
                    color = strokeCol,
                    start = Offset(rulerStartPoint!!.x * size.width, rulerStartPoint!!.y * size.height),
                    end = Offset(rulerCurrentPoint!!.x * size.width, rulerCurrentPoint!!.y * size.height),
                    strokeWidth = currentStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // RECUADRO DE TRANSFORMACIÓN POWERPOINT PARA SELLO SELECCIONADO
            val curSelStamp = if (currentActiveTool == ActiveDrawTool.SELECT) (activeStampIdx ?: currentSelectedStampIdx) else null
            if (curSelStamp != null && curSelStamp in stamps.indices) {
                val sel = stamps[curSelStamp]
                val cx = sel.x * size.width
                val cy = sel.y * size.height
                val boxRadius = (60f * sel.scale).coerceAtLeast(42f)
                val left = cx - boxRadius
                val top = cy - boxRadius
                val right = cx + boxRadius
                val bottom = cy + boxRadius

                // Marco rectangular discontinuo
                drawRect(
                    color = Color(0xFF0284C7),
                    topLeft = Offset(left, top),
                    size = Size(boxRadius * 2, boxRadius * 2),
                    style = Stroke(
                        width = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                    )
                )

                // 4 esquinas PowerPoint para redimensionar desde cualquiera
                val corners = listOf(
                    Offset(left, top),
                    Offset(right, top),
                    Offset(left, bottom),
                    Offset(right, bottom)
                )
                for (pt in corners) {
                    drawCircle(color = Color.White, radius = 8f, center = pt)
                    drawCircle(color = Color(0xFF0284C7), radius = 8f, center = pt, style = Stroke(width = 3f))
                    drawCircle(color = Color(0xFF0284C7), radius = 3f, center = pt)
                }

                // Botón borrar flotante en esquina superior derecha
                val delBadge = Offset(right + 12f, top - 12f)
                drawCircle(color = RallyRed, radius = 11f, center = delBadge)
                drawCircle(color = Color.White, radius = 11f, center = delBadge, style = Stroke(1.5f))
                drawLine(color = Color.White, start = delBadge + Offset(-4.5f, -4.5f), end = delBadge + Offset(4.5f, 4.5f), strokeWidth = 2.5f)
                drawLine(color = Color.White, start = delBadge + Offset(-4.5f, 4.5f), end = delBadge + Offset(4.5f, -4.5f), strokeWidth = 2.5f)
            }

            // RECUADRO DE TRANSFORMACIÓN POWERPOINT Y BORRADO PARA FLECHA SELECCIONADA
            val curSelArrow = if (currentActiveTool == ActiveDrawTool.SELECT) (activeArrowIdx ?: currentSelectedArrowIdx) else null
            if (curSelArrow != null && curSelArrow in arrows.indices) {
                val arr = arrows[curSelArrow]
                val p0 = Offset(arr.p0.x * size.width, arr.p0.y * size.height)
                val p1 = Offset(arr.p1.x * size.width, arr.p1.y * size.height)
                val p2 = Offset(arr.p2.x * size.width, arr.p2.y * size.height)
                val p3 = Offset(arr.p3.x * size.width, arr.p3.y * size.height)

                val minX = minOf(p0.x, p1.x, p2.x, p3.x)
                val maxX = maxOf(p0.x, p1.x, p2.x, p3.x)
                val minY = minOf(p0.y, p1.y, p2.y, p3.y)
                val maxY = maxOf(p0.y, p1.y, p2.y, p3.y)
                val pad = 32f
                val left = minX - pad
                val top = minY - pad
                val right = maxX + pad
                val bottom = maxY + pad

                // Marco rectangular discontinuo
                drawRect(
                    color = Color(0xFF0284C7).copy(alpha = 0.8f),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(
                        width = 2.0f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                    )
                )

                // 4 esquinas PowerPoint para redimensionar la flecha completa
                val cornersArrow = listOf(
                    Offset(left, top),
                    Offset(right, top),
                    Offset(left, bottom),
                    Offset(right, bottom)
                )
                for (pt in cornersArrow) {
                    drawCircle(color = Color.White, radius = 8f, center = pt)
                    drawCircle(color = Color(0xFF0284C7), radius = 8f, center = pt, style = Stroke(width = 3f))
                    drawCircle(color = Color(0xFF0284C7), radius = 3f, center = pt)
                }

                // Botón borrar flotante en esquina superior derecha del marco de la flecha
                val delBadgeArrow = Offset(right + 12f, top - 12f)
                drawCircle(color = RallyRed, radius = 11f, center = delBadgeArrow)
                drawCircle(color = Color.White, radius = 11f, center = delBadgeArrow, style = Stroke(1.5f))
                drawLine(color = Color.White, start = delBadgeArrow + Offset(-4.5f, -4.5f), end = delBadgeArrow + Offset(4.5f, 4.5f), strokeWidth = 2.5f)
                drawLine(color = Color.White, start = delBadgeArrow + Offset(-4.5f, 4.5f), end = delBadgeArrow + Offset(4.5f, -4.5f), strokeWidth = 2.5f)
            }
        }
    }
}

/**
 * PERFIL DE ELEVACIÓN GRÁFICO (RALLY NAVIGATOR STYLE)
 * Muestra el perfil de altitud a lo largo de la distancia del tramo con métricas y cursor en el waypoint activo.
 */
@Composable
fun ElevationProfileView(
    vinetas: List<VinetaEntity>,
    vinetaSeleccionadaId: Long?,
    onSelectVineta: (VinetaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkTheme
    var expandido by remember { mutableStateOf(true) }

    if (vinetas.isEmpty()) return

    val sortedVinetas = remember(vinetas) { vinetas.sortedBy { it.distanciaTotal } }
    val totalDist = remember(sortedVinetas) {
        val lastDist = sortedVinetas.lastOrNull()?.distanciaTotal ?: 0.0
        if (lastDist > 0.0) lastDist else 1.0
    }

    val startAlt = sortedVinetas.firstOrNull()?.altitud ?: 0.0
    val finishAlt = sortedVinetas.lastOrNull()?.altitud ?: 0.0
    val minAlt = remember(sortedVinetas) { sortedVinetas.minOfOrNull { it.altitud } ?: 0.0 }
    val maxAlt = remember(sortedVinetas) { sortedVinetas.maxOfOrNull { it.altitud } ?: 0.0 }

    // Calcular ganancia y pérdida acumulada
    val (ganancia, perdida) = remember(sortedVinetas) {
        var gain = 0.0
        var loss = 0.0
        for (i in 1 until sortedVinetas.size) {
            val diff = sortedVinetas[i].altitud - sortedVinetas[i - 1].altitud
            if (diff > 0) gain += diff else loss += Math.abs(diff)
        }
        Pair(gain, loss)
    }

    val selectedVineta = remember(vinetaSeleccionadaId, sortedVinetas) {
        sortedVinetas.find { it.id == vinetaSeleccionadaId }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) RallySurface else Color(0xFFF1F5F9)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) RallyCardBg else Color(0xFFCBD5E1)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Cabecera con Métricas Rally Navigator Style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📈 ELEVATION PROFILE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) RallyCyan else FredianiCyanText
                    )
                    Text(
                        text = "START: ${startAlt.toInt()}m",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                    )
                    Text(
                        text = "• MIN: ${minAlt.toInt()}m",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                    )
                    Text(
                        text = "• MAX: ${maxAlt.toInt()}m",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                    )
                    Text(
                        text = "• ACCUM: +${ganancia.toInt()}m / -${perdida.toInt()}m",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) RallyAccentYellow else FredianiAmberText
                    )
                    Text(
                        text = "• FINISH: ${finishAlt.toInt()}m",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF475569)
                    )
                }

                Icon(
                    imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }

            if (expandido) {
                Spacer(modifier = Modifier.height(4.dp))
                // Gráfico Canvas de Perfil de Elevación con cursor interactivo
                val profileHeight = 48.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(profileHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF09131F) else Color.White)
                        .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                        .pointerInput(sortedVinetas, totalDist) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val pct = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val targetDist = pct * totalDist
                                    val nearest = sortedVinetas.minByOrNull { Math.abs(it.distanciaTotal - targetDist) }
                                    if (nearest != null) onSelectVineta(nearest)
                                },
                                onDrag = { change, _ ->
                                    val pct = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val targetDist = pct * totalDist
                                    val nearest = sortedVinetas.minByOrNull { Math.abs(it.distanciaTotal - targetDist) }
                                    if (nearest != null) onSelectVineta(nearest)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
                        val w = size.width
                        val h = size.height
                        val altRange = (maxAlt - minAlt).coerceAtLeast(15.0)

                        // Líneas de referencia horizontales suaves (grid)
                        val lineY25 = h * 0.25f
                        val lineY50 = h * 0.50f
                        val lineY75 = h * 0.75f
                        val gridColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        drawLine(gridColor, Offset(0f, lineY25), Offset(w, lineY25), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, lineY50), Offset(w, lineY50), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, lineY75), Offset(w, lineY75), strokeWidth = 1f)

                        if (sortedVinetas.size >= 2) {
                            val path = androidx.compose.ui.graphics.Path()
                            val fillPath = androidx.compose.ui.graphics.Path()

                            var lastPoint: Offset? = null

                            sortedVinetas.forEachIndexed { idx, v ->
                                val x = ((v.distanciaTotal / totalDist).coerceIn(0.0, 1.0) * w).toFloat()
                                val normAlt = ((v.altitud - minAlt) / altRange).coerceIn(0.0, 1.0)
                                val y = (h - (normAlt * (h * 0.8f) + (h * 0.1f))).toFloat()

                                val pt = Offset(x, y)
                                if (idx == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                                lastPoint = pt
                            }

                            if (lastPoint != null) {
                                fillPath.lineTo(lastPoint!!.x, h)
                                fillPath.close()
                            }

                            // Relleno degradado sutil cian
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF00B4D8).copy(alpha = 0.35f),
                                        Color(0xFF00B4D8).copy(alpha = 0.03f)
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            // Línea del perfil de elevación
                            drawPath(
                                path = path,
                                color = Color(0xFF00B4D8),
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Puntos de cada waypoint
                            sortedVinetas.forEach { v ->
                                val x = ((v.distanciaTotal / totalDist).coerceIn(0.0, 1.0) * w).toFloat()
                                val normAlt = ((v.altitud - minAlt) / altRange).coerceIn(0.0, 1.0)
                                val y = (h - (normAlt * (h * 0.8f) + (h * 0.1f))).toFloat()
                                drawCircle(color = Color(0xFF00B4D8), radius = 2.5f, center = Offset(x, y))
                            }

                            // Cursor vertical en el Waypoint Seleccionado
                            if (selectedVineta != null) {
                                val selX = ((selectedVineta.distanciaTotal / totalDist).coerceIn(0.0, 1.0) * w).toFloat()
                                val normAlt = ((selectedVineta.altitud - minAlt) / altRange).coerceIn(0.0, 1.0)
                                val selY = (h - (normAlt * (h * 0.8f) + (h * 0.1f))).toFloat()

                                // Línea vertical discontinua roja
                                drawLine(
                                    color = Color(0xFFEF4444),
                                    start = Offset(selX, 0f),
                                    end = Offset(selX, h),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                                )

                                // Círculo exterior luminoso + punto interior
                                drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.3f), radius = 7f, center = Offset(selX, selY))
                                drawCircle(color = Color(0xFFEF4444), radius = 4f, center = Offset(selX, selY))
                                drawCircle(color = Color.White, radius = 2f, center = Offset(selX, selY))
                            }
                        }
                    }

                    // Badge flotante del Waypoint Seleccionado en la esquina
                    if (selectedVineta != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "WPT #${selectedVineta.numero} • ${selectedVineta.altitud.toInt()}m",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * BARRA UNIVERSAL SUPERIOR DE DIBUJO Y EDICIÓN
 * Proporciona acceso a herramientas (Seleccionar, Pluma, Regla, Deshacer, Borrar, Paleta, Guardar)
 * sin quitar espacio a la casilla de dibujo de la grilla.
 */
@Composable
fun RoadbookDrawingToolbar(
    vinetaEnEdicion: VinetaEntity?,
    activeTool: ActiveDrawTool,
    selectedStrokeColor: String,
    strokeWidth: Float,
    asistenteTrazo: Boolean = true,
    hasPinKm: Boolean = false,
    onToggleAsistente: () -> Unit = {},
    onAddArrow: () -> Unit = {},
    onTogglePinKm: () -> Unit = {},
    onToolChange: (ActiveDrawTool) -> Unit,
    onColorChange: (String) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkTheme
    val barBg = if (isDark) RallySurface else Color(0xFFF8FAFC)
    val barBorder = if (isDark) RallyCardBg else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = barBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, barBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        if (vinetaEnEdicion != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // SECCIÓN IZQUIERDA: BADGE WPT + HERRAMIENTAS DE DIBUJO
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = FredianiCyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FredianiCyan)
                    ) {
                        Text(
                            text = "✏️ #${vinetaEnEdicion.numero} (${String.format("%.2f", vinetaEnEdicion.distanciaTotal)} km)",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = if (isDark) RallyCyan else FredianiCyanText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Botón SELECT
                    ToolIconButton(
                        icon = Icons.Default.NearMe,
                        label = "Seleccionar",
                        selected = activeTool == ActiveDrawTool.SELECT,
                        onClick = { onToolChange(ActiveDrawTool.SELECT) }
                    )

                    // Botón PEN
                    ToolIconButton(
                        icon = Icons.Default.Edit,
                        label = "Pluma",
                        selected = activeTool == ActiveDrawTool.PEN,
                        onClick = { onToolChange(ActiveDrawTool.PEN) }
                    )

                    // Botón RULER (Línea Recta)
                    ToolIconButton(
                        icon = Icons.Default.HorizontalRule,
                        label = "Regla",
                        selected = activeTool == ActiveDrawTool.RULER,
                        onClick = { onToolChange(ActiveDrawTool.RULER) }
                    )

                    // Botón CURVA FLEX (Flecha Bézier paramétrica FIA)
                    ToolIconButton(
                        icon = Icons.Default.CallSplit,
                        label = "+ Curva Flex",
                        selected = false,
                        tint = FredianiCyan,
                        onClick = onAddArrow
                    )

                    // Botón PIN KM OFICIAL FIA (Art. 5.6.2)
                    ToolIconButton(
                        icon = Icons.Default.PinDrop,
                        label = if (hasPinKm) "Pin Km Activo" else "Pin Km",
                        selected = hasPinKm,
                        tint = if (hasPinKm) Color(0xFFF59E0B) else null,
                        onClick = onTogglePinKm
                    )

                    // Botón ASISTENTE DE TRAZO
                    ToolIconButton(
                        icon = Icons.Default.AutoFixHigh,
                        label = if (asistenteTrazo) "Asistente Activo" else "Asistente Inactivo",
                        selected = asistenteTrazo,
                        tint = if (asistenteTrazo) Color(0xFFF59E0B) else null,
                        onClick = onToggleAsistente
                    )

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp), color = barBorder)

                    // CHIPS DE COLOR
                    val colores = listOf(
                        "#000000" to Color.Black,
                        "#EF4444" to Color(0xFFEF4444),
                        "#0284C7" to Color(0xFF0284C7),
                        "#F59E0B" to Color(0xFFF59E0B),
                        "#10B981" to Color(0xFF10B981)
                    )
                    colores.forEach { (hex, col) ->
                        val sel = selectedStrokeColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (sel) 2.dp else 1.dp,
                                    color = if (sel) (if (isDark) Color.White else Color.Black) else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { onColorChange(hex) }
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp), color = barBorder)

                    // DESHACER
                    ToolIconButton(
                        icon = Icons.Default.Undo,
                        label = "Deshacer",
                        selected = false,
                        onClick = onUndo
                    )

                    // BORRAR
                    ToolIconButton(
                        icon = Icons.Default.Delete,
                        label = "Borrar",
                        selected = false,
                        tint = RallyRed,
                        onClick = onClear
                    )
                }

                // SECCIÓN DERECHA: BOTONES GUARDAR Y CERRAR
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onGuardar,
                        colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("GUARDAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    IconButton(
                        onClick = onCerrar,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = if (isDark) Color.White else Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            // Cuando no hay edición activa, mensaje claro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Toque una indicación del roadbook para dibujar directamente en su celda o estampar símbolos FIA.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) FredianiCyan else Color.Transparent,
        modifier = Modifier
            .size(26.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint ?: if (selected) Color.Black else (if (isDark) Color.White else Color(0xFF334155)),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * BARRA DOCKADA INFERIOR DE CATEGORÍAS (IDÉNTICA A RALLY NAVIGATOR)
 * Con pestañas: CROSS COUNTRY | LANDMARKS | TERRAIN | NOTAS / PELIGROS | SPEED & RADAR | MY ICONS
 */
@Composable
fun DockedRallyNavigatorBar(
    activeTab: NavigatorTab,
    onTabSelected: (NavigatorTab) -> Unit,
    onInsertStamp: (String) -> Unit,
    selectedStamp: StampData? = null,
    selectedArrow: ManeuverArrow? = null,
    onDeleteSelected: (() -> Unit)? = null,
    vinetaEnEdicionNumero: Int? = null,
    onGuardar: (() -> Unit)? = null,
    onCerrar: (() -> Unit)? = null
) {
    val isDark = ThemeManager.isDarkTheme
    val barBg = if (isDark) RallySurface else Color(0xFFF1F5F9)
    val barBorder = if (isDark) RallyCardBg else Color(0xFFCBD5E1)

    val stamps = remember {
        mapOf(
            NavigatorTab.ROAD_RALLY to listOf(
                "FLECHA_MANIOBRA" to "CURVA FLEX",
                "RECTA" to "RECTA",
                "CURVA_DER" to "CURVA D",
                "CURVA_IZQ" to "CURVA I",
                "DERECHA_90" to "90° DER",
                "IZQUIERDA_90" to "90° IZQ",
                "RETOME_DER" to "RETOME D",
                "RETOME_IZQ" to "RETOME I",
                "CRUCE_X" to "CRUCE +",
                "CRUCE_T" to "EMPALME T",
                "BIFURCACION_Y" to "DESVÍO Y",
                "ROTONDA" to "ROTONDA"
            ),
            NavigatorTab.LANDMARKS to listOf(
                "TRANQUERA" to "TRANQUERA",
                "GUARDAGANADO" to "GUARDAG.",
                "PUENTE" to "PUENTE",
                "ALCANTARILLA" to "ALCANT.",
                "VIAS_TREN" to "VÍAS TREN",
                "ANTENA" to "ANTENA",
                "MOLINO" to "MOLINO",
                "CASA" to "POBLADO",
                "IGLESIA" to "IGLESIA",
                "CIRCULO" to "MOJÓN"
            ),
            NavigatorTab.TERRAIN to listOf(
                "VADO" to "VADO",
                "SALTO" to "SALTO",
                "ZANJA" to "ZANJA",
                "DUNAS" to "DUNAS",
                "PIEDRAS" to "PIEDRAS",
                "BARRO" to "HUELLAS"
            ),
            NavigatorTab.SIGNS to listOf(
                "PELIGRO_1" to "! ATENCIÓN",
                "PELIGRO_2" to "!! PELIGRO",
                "PELIGRO_3" to "!!! GRAVE",
                "PRECAUCION" to "PRECAUCIÓN",
                "STOP" to "STOP"
            ),
            NavigatorTab.SPEED to listOf(
                "RADAR_DZ" to "DZ RADAR",
                "RADAR_FZ" to "FZ FIN",
                "RESET_0000" to "0000 RESET",
                "GPS_POINT" to "WPV GPS"
            ),
            NavigatorTab.MY_ICONS to listOf(
                "TC" to "TC CONTROL",
                "LARGADA" to "LARGADA",
                "LLEGADA" to "LLEGADA",
                "SURTIDOR" to "COMBUST.",
                "ASISTENCIA" to "ASISTENCIA",
                "MEDICO" to "MÉDICO"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(barBg, RoundedCornerShape(10.dp))
            .border(1.5.dp, barBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // FILA 1: PESTAÑAS DE CATEGORÍAS RALLY NAVIGATOR
        ScrollableTabRow(
            selectedTabIndex = NavigatorTab.values().indexOf(activeTab),
            containerColor = Color.Transparent,
            divider = {},
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigatorTab.values().forEach { tab ->
                val sel = activeTab == tab
                Tab(
                    selected = sel,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.titulo,
                            fontSize = 9.5.sp,
                            fontWeight = if (sel) FontWeight.Black else FontWeight.Bold,
                            color = if (sel) (if (isDark) RallyCyan else FredianiCyanText) else Color.Gray
                        )
                    }
                )
            }
        }

        // FILA 2: BANDEJA DE BALDOSAS CON SÍMBOLOS VECTORIALES GRANDES (ESTILO RALLY NAVIGATOR)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val activeStamps = stamps[activeTab] ?: emptyList()
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
            ) {
                items(activeStamps) { (codigo, nombre) ->
                    Card(
                        modifier = Modifier
                            .width(74.dp)
                            .height(82.dp)
                            .clickable { onInsertStamp(codigo) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) RallyCardBg else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, barBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    withTransform({
                                        translate(left = cx, top = cy)
                                        scale(scaleX = 0.66f, scaleY = 0.66f, pivot = Offset.Zero)
                                    }) {
                                        DiagramaCanvasRenderer.drawStampShape(
                                            this,
                                            codigo,
                                            if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = nombre,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF1E293B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Botón Guardar opcional
            if (onGuardar != null) {
                Button(
                    onClick = onGuardar,
                    colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("GUARDAR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }
    }
}

/**
 * MODAL PARA EDICIÓN DE TEXTO DE NOTAS, PELIGROS Y CONTROL DE REGULARIDAD / VELOCIDADES
 */
@Composable
fun RoadbookNotesDialog(
    vineta: VinetaEntity,
    onGuardar: (
        nota: String,
        peligro: String,
        distTotal: Double,
        distParcial: Double,
        notasSvg: String,
        esOffRoad: Boolean,
        velPromedio: Double,
        velMaxima: Double,
        tiempoIdealSeg: Double,
        radioCaptura: Int,
        esReinicioCero: Boolean,
        distanciaOculta: Boolean,
        esOcultaOrg: Boolean,
        mostrarWpt: Boolean,
        esTierra: Boolean,
        esPuntoControl: Boolean,
        tipoPuntoControl: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = ThemeManager.isDarkTheme
    var nota by remember { mutableStateOf(vineta.informacion) }
    var peligro by remember { mutableStateOf(vineta.peligro) }
    var distTotStr by remember { mutableStateOf(String.format(java.util.Locale.US, "%.3f", vineta.distanciaTotal)) }
    var distParStr by remember { mutableStateOf(String.format(java.util.Locale.US, "%.3f", vineta.distanciaParcial)) }

    var esOffRoad by remember { mutableStateOf(vineta.esOffRoad) }
    var esReinicioCero by remember { mutableStateOf(vineta.esReinicioCero) }
    var distanciaOculta by remember { mutableStateOf(vineta.distanciaOculta) }
    var esOcultaOrg by remember { mutableStateOf(vineta.esOcultaOrg) }
    var mostrarWpt by remember { mutableStateOf(vineta.mostrarWpt) }
    var esTierra by remember { mutableStateOf(vineta.esTierra) }
    var esPuntoControl by remember { mutableStateOf(vineta.esPuntoControl) }
    var tipoPuntoControl by remember { mutableStateOf(if (vineta.tipoPuntoControl.isNotBlank()) vineta.tipoPuntoControl else "PASO") }

    var velPromStr by remember {
        mutableStateOf(if (vineta.velocidadPromedioSectorKmh > 0) vineta.velocidadPromedioSectorKmh.toInt().toString() else "")
    }
    var tiempoIdealSegundosState by remember { mutableStateOf(vineta.tiempoIdealSegundos) }
    var tiempoMinStr by remember {
        val initialMin = if (vineta.tiempoIdealSegundos > 0) {
            vineta.tiempoIdealSegundos / 60.0
        } else if (vineta.velocidadPromedioSectorKmh > 0 && vineta.distanciaParcial > 0) {
            (vineta.distanciaParcial / vineta.velocidadPromedioSectorKmh) * 60.0
        } else 0.0
        mutableStateOf(if (initialMin > 0) String.format(java.util.Locale.US, "%.2f", initialMin) else "")
    }
    var velMaxStr by remember {
        mutableStateOf(if (vineta.velocidadMaximaSectorKmh > 0) vineta.velocidadMaximaSectorKmh.toInt().toString() else "")
    }
    var radioCapturaState by remember { mutableStateOf(vineta.radioCapturaMetros) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Editar Indicación #${vineta.numero}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (esOffRoad) Color(0xFF78350F) else Color(0xFF047857)
                ) {
                    Text(
                        text = if (esOffRoad) "OFICIAL OFF-ROAD" else "OFICIAL EN CAMINO",
                        fontWeight = FontWeight.Black,
                        fontSize = 8.5.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. INSTRUCCIÓN / NOTA
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota de Navegación / Instrucción") },
                    placeholder = { Text("Ej: P.P. Curva cerrada / Vadeo") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. PELIGRO
                Text("Grado de Peligro:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("", "! ATENCIÓN", "!! PELIGRO", "!!!").forEach { p ->
                        val sel = peligro == p
                        FilterChip(
                            selected = sel,
                            onClick = { peligro = p },
                            label = { Text(if (p.isEmpty()) "Normal" else p, fontSize = 10.sp) }
                        )
                    }
                }

                // 3. KILOMETRAJE
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = distTotStr,
                        onValueChange = { distTotStr = it },
                        label = { Text("Km Total") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = distParStr,
                        onValueChange = {
                            distParStr = it
                            val par = it.toDoubleOrNull() ?: 0.0
                            val v = velPromStr.toDoubleOrNull() ?: 0.0
                            if (par > 0 && v > 0) {
                                val tSeg = (par / v) * 3600.0
                                tiempoIdealSegundosState = tSeg
                                tiempoMinStr = String.format(java.util.Locale.US, "%.2f", tSeg / 60.0)
                            }
                        },
                        label = { Text("Km Parcial") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = if (isDark) RallySurface else Color(0xFFE2E8F0))

                // 4. MODO DE TRAZADO: CAMINO VS CAMPO TRAVIESA (OFF-ROAD)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (esOffRoad) "🏜️ Fuera de Pista (Off-Road)" else "🛣️ Conectar vía Caminos (Snap)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (esOffRoad) Color(0xFFF59E0B) else (if (isDark) RallyCyan else FredianiCyanText)
                        )
                        Text(
                            text = if (esOffRoad) "Trazo recto / campo traviesa sin forzar rutas" else "Ajuste magnético inteligente a calzada Mapbox",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = esOffRoad,
                        onCheckedChange = { esOffRoad = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF59E0B),
                            checkedTrackColor = Color(0xFF78350F)
                        )
                    )
                }

                HorizontalDivider(color = if (isDark) RallySurface else Color(0xFFE2E8F0))

                // 5. REGULARIDAD: VELOCIDAD PROMEDIO Y TIEMPO BIDIRECCIONAL
                Text("⚡ Control de Regularidad (Sector)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = velPromStr,
                        onValueChange = {
                            velPromStr = it
                            val v = it.toDoubleOrNull()
                            val par = distParStr.toDoubleOrNull() ?: vineta.distanciaParcial
                            if (v != null && v > 0 && par > 0) {
                                val tSeg = (par / v) * 3600.0
                                tiempoIdealSegundosState = tSeg
                                tiempoMinStr = String.format(java.util.Locale.US, "%.2f", tSeg / 60.0)
                            }
                        },
                        label = { Text("Vel. Prom (km/h)") },
                        placeholder = { Text("Ej: 60") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = tiempoMinStr,
                        onValueChange = {
                            tiempoMinStr = it
                            val tMin = it.toDoubleOrNull()
                            val par = distParStr.toDoubleOrNull() ?: vineta.distanciaParcial
                            if (tMin != null && tMin > 0 && par > 0) {
                                val v = par / (tMin / 60.0)
                                tiempoIdealSegundosState = tMin * 60.0
                                velPromStr = String.format(java.util.Locale.US, "%.0f", v)
                            }
                        },
                        label = { Text("Tiempo (minutos)") },
                        placeholder = { Text("Ej: 3.5") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 6. CONTROL DE VELOCIDAD MÁXIMA (ZONA DZ / RADAR)
                Text("🔴 Límite de Velocidad Máxima (DZ / Radar)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                OutlinedTextField(
                    value = velMaxStr,
                    onValueChange = { velMaxStr = it },
                    label = { Text("Velocidad Máxima Permitida en km/h (0 = sin límite especial)") },
                    placeholder = { Text("Ej: 30 (paso por pueblo) o 50") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 7. RADIO DE CAPTURA WPV
                Text("📍 Radio de Captura WPV (Metros):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(20, 30, 50, 90).forEach { r ->
                        val sel = radioCapturaState == r
                        FilterChip(
                            selected = sel,
                            onClick = { radioCapturaState = r },
                            label = { Text("${r}m", fontSize = 10.sp) }
                        )
                    }
                }

                HorizontalDivider(color = if (isDark) RallySurface else Color(0xFFE2E8F0))

                // 8. OPCIONES ESPECIALES DE VIÑETA
                Text("⚙️ Opciones Especiales de Viñeta:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = esReinicioCero,
                        onClick = { esReinicioCero = !esReinicioCero },
                        label = { Text("0,00 Reinicio", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = distanciaOculta,
                        onClick = { distanciaOculta = !distanciaOculta },
                        label = { Text("X.XX Oculta", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = esOcultaOrg,
                        onClick = { esOcultaOrg = !esOcultaOrg },
                        label = { Text("🔒 OCUL Org", fontSize = 10.sp) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = mostrarWpt,
                        onClick = { mostrarWpt = !mostrarWpt },
                        label = { Text("🌐 WPT Coordenadas", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = esTierra,
                        onClick = { esTierra = !esTierra },
                        label = { Text("DIRT Tierra", fontSize = 10.sp) }
                    )
                }

                // 9. WAYPOINT (WP)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF2E1065) else Color(0xFFF3E8FF), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎯 WayPoint (WP)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFA855F7))
                        Text("Marca esta viñeta como WayPoint", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = esPuntoControl,
                        onCheckedChange = { esPuntoControl = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFA855F7), checkedTrackColor = Color(0xFF581C87))
                    )
                }

                if (esPuntoControl) {
                    Text("Tipo de WayPoint:", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PASO", "REGULARIDAD", "CH", "RADAR").forEach { t ->
                            val sel = tipoPuntoControl == t
                            FilterChip(
                                selected = sel,
                                onClick = { tipoPuntoControl = t },
                                label = { Text(t, fontSize = 9.5.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tot = distTotStr.toDoubleOrNull() ?: vineta.distanciaTotal
                    val par = distParStr.toDoubleOrNull() ?: vineta.distanciaParcial
                    val vProm = velPromStr.toDoubleOrNull() ?: 0.0
                    val vMax = velMaxStr.toDoubleOrNull() ?: 0.0
                    val tSeg = if (tiempoIdealSegundosState > 0) {
                        tiempoIdealSegundosState
                    } else if (vProm > 0 && par > 0) {
                        (par / vProm) * 3600.0
                    } else 0.0

                    onGuardar(
                        nota,
                        peligro,
                        tot,
                        par,
                        vineta.notasSvgData,
                        esOffRoad,
                        vProm,
                        vMax,
                        tSeg,
                        radioCapturaState,
                        esReinicioCero,
                        distanciaOculta,
                        esOcultaOrg,
                        mostrarWpt,
                        esTierra,
                        esPuntoControl,
                        tipoPuntoControl
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen)
            ) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * MODAL PARA CONFIGURAR VELOCIDADES GENERALES DEL TRAMO
 */
@Composable
fun ConfigurarVelocidadesTramoDialog(
    tramo: TramoEntity,
    onGuardar: (velPromedio: Double, velMax: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var velPromStr by remember {
        mutableStateOf(if (tramo.velocidadPromedioObjetivoKmh > 0) tramo.velocidadPromedioObjetivoKmh.toInt().toString() else "")
    }
    var velMaxStr by remember {
        mutableStateOf(if (tramo.velocidadMaximaPermitidaKmh > 0) tramo.velocidadMaximaPermitidaKmh.toInt().toString() else "110")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Velocidades del Tramo (${tramo.tipo} - ${tramo.identificador})", fontWeight = FontWeight.Black, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ajuste el promedio general de regularidad del tramo y el tope de velocidad máxima permitida (alarma de velocidad).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = velPromStr,
                    onValueChange = { velPromStr = it },
                    label = { Text("Velocidad Promedio Exigida (km/h)") },
                    placeholder = { Text("Ej: 65 (dejar vacío si no aplica)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = velMaxStr,
                    onValueChange = { velMaxStr = it },
                    label = { Text("Velocidad Máxima Permitida (km/h)") },
                    placeholder = { Text("Ej: 110 (tope general de seguridad)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = velPromStr.toDoubleOrNull() ?: 0.0
                    val m = velMaxStr.toDoubleOrNull() ?: 110.0
                    onGuardar(p, m)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen)
            ) {
                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun HeaderColumnaToggle(
    titulo: String,
    activo: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkTheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = titulo,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = if (activo) (if (isDark) RallyCyan else FredianiCyanText) else (if (isDark) Color.Gray else Color.LightGray),
            textAlign = TextAlign.Center
        )
    }
}

