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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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
import com.example.roadbookorganizador.ui.vineta.ANCHO_FLECHA
import com.example.roadbookorganizador.ui.vineta.ArrastreSimbolo
import com.example.roadbookorganizador.ui.vineta.BibliotecaVineta
import com.example.roadbookorganizador.ui.vineta.CapaEditada
import com.example.roadbookorganizador.ui.vineta.DibujanteVineta
import com.example.roadbookorganizador.ui.vineta.ESCALA_NUEVA
import com.example.roadbookorganizador.ui.vineta.EditorCapa
import com.example.roadbookorganizador.ui.vineta.ItemPaleta
import com.example.roadbookorganizador.ui.vineta.SeleccionEditor
import com.example.roadbookorganizador.ui.vineta.VistaCapa
import com.example.roadbookorganizador.ui.vineta.conDibujo
import com.example.roadbookorganizador.ui.vineta.dibujoV1
import com.example.roadbookorganizador.vineta.ALTO_NOTAS
import com.example.roadbookorganizador.vineta.Capa
import com.example.roadbookorganizador.vineta.Carteles
import com.example.roadbookorganizador.vineta.DibujoV1
import com.example.roadbookorganizador.vineta.Flecha
import com.example.roadbookorganizador.vineta.FranjaCarteles
import com.example.roadbookorganizador.vineta.LetraNotas
import com.example.roadbookorganizador.vineta.Sello
import com.example.roadbookorganizador.vineta.ValoresCartel

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
    var activeTool by remember { mutableStateOf(ActiveDrawTool.PEN) }
    var asistenteTrazoActivo by remember { mutableStateOf(true) }
    var activeTab by remember { mutableStateOf(NavigatorTab.ROAD_RALLY) }
    var selectedStrokeColor by remember { mutableStateOf("#000000") }
    var selectedStrokeWidth by remember { mutableStateOf(7f) }

    // Dibujo de la viñeta en edición (formato versión 1 de la especificación)
    val context = LocalContext.current
    val bib = remember { BibliotecaVineta.obtener(context) }
    val dibujante = remember(bib) { DibujanteVineta(bib) }
    val arrastre = remember { ArrastreSimbolo() }
    var borrador by remember { mutableStateOf(DibujoV1()) }
    var cargadoId by remember { mutableStateOf<Long?>(null) }
    var capaActiva by remember { mutableStateOf(CapaEditada.DIBUJO) }
    var seleccion by remember { mutableStateOf(SeleccionEditor()) }

    // Franja de carteles de velocidad de cada viñeta (punto 10), en el orden del tramo
    val franjas: Map<Long, FranjaCarteles?> = remember(vinetas) {
        val lista = Carteles.franjasDelTramo(vinetas.map {
            ValoresCartel.desdeCampos(it.tiempoIdealSegundos, it.velocidadPromedioSectorKmh, it.velocidadMaximaSectorKmh)
        })
        vinetas.mapIndexed { i, v -> v.id to lista[i] }.toMap()
    }

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

    // Cargar el dibujo de la viñeta en edición una sola vez (cuando ya está en la lista).
    // Nunca se agregan flechas automáticas.
    LaunchedEffect(vinetaEnEdicionId, vinetas) {
        val id = vinetaEnEdicionId
        if (id == null) {
            cargadoId = null
            borrador = DibujoV1()
            seleccion = SeleccionEditor()
            return@LaunchedEffect
        }
        if (cargadoId == id) return@LaunchedEffect
        val cur = vinetas.find { it.id == id } ?: return@LaunchedEffect
        borrador = cur.dibujoV1(bib)
        cargadoId = id
        seleccion = SeleccionEditor()
        activeTool = ActiveDrawTool.SELECT
    }

    /** Abre la viñeta para editar (cargando su dibujo en el momento) y elige el recuadro. */
    fun abrirEdicion(v: VinetaEntity, capa: CapaEditada) {
        if (vinetaEnEdicionId != v.id || cargadoId != v.id) {
            borrador = v.dibujoV1(bib)
            cargadoId = v.id
            vinetaEnEdicionId = v.id
            seleccion = SeleccionEditor()
            onSeleccionarVineta(v)
        }
        if (capaActiva != capa) {
            capaActiva = capa
            seleccion = SeleccionEditor()
        }
    }

    /** Guarda el dibujo (versión 1) y el peligro calculado de la viñeta en edición. */
    val persistir: (DibujoV1) -> Unit = { nuevo ->
        borrador = nuevo
        val cur = vinetas.find { it.id == vinetaEnEdicionId }
        if (cur != null && cargadoId == cur.id) {
            onGuardarVineta(cur.conDibujo(nuevo))
        }
    }

    fun capaDe(d: DibujoV1, c: CapaEditada): Capa = if (c == CapaEditada.DIBUJO) d.dibujo else d.notasCapa
    fun conCapa(d: DibujoV1, c: CapaEditada, capa: Capa): DibujoV1 =
        if (c == CapaEditada.DIBUJO) d.copy(dibujo = capa) else d.copy(notasCapa = capa)

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

    /** Inserta un símbolo o una flecha armada en un recuadro, en (x, y) de 0 a 1. */
    fun insertarEn(v: VinetaEntity, capa: CapaEditada, item: ItemPaleta, x: Double, y: Double) {
        abrirEdicion(v, capa)
        val c = capaDe(borrador, capa)
        val nueva = when (item) {
            is ItemPaleta.SimboloItem -> {
                val cc = c.copy(stamps = c.stamps + Sello(item.codigo, x, y, ESCALA_NUEVA, 0.0))
                seleccion = SeleccionEditor(sello = cc.stamps.lastIndex)
                cc
            }
            is ItemPaleta.ArmadaItem -> {
                // La flecha armada se usa tal cual; en la capa de notas va sin palito del GPS
                val f = if (capa == CapaEditada.NOTAS) item.flecha.copy(gps = null) else item.flecha
                val cc = c.copy(arrows = c.arrows + f)
                seleccion = SeleccionEditor(flecha = cc.arrows.lastIndex)
                cc
            }
        }
        activeTool = ActiveDrawTool.SELECT
        persistir(conCapa(borrador, capa, nueva))
    }

    /** Tocar un símbolo de la barra: va al centro del recuadro que se está editando. */
    val insertarTocando: (ItemPaleta) -> Unit = { item ->
        val destino = vinetas.find { it.id == vinetaEnEdicionId } ?: vinetas.firstOrNull()
        if (destino != null) insertarEn(destino, capaActiva, item, 0.5, 0.5)
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
                capaActiva = capaActiva,
                onCapaChange = { c -> vinetas.find { it.id == vinetaEnEdicionId }?.let { abrirEdicion(it, c) } },
                letraNotasAuto = borrador.notasLetra == null,
                letraNotasValor = vinetas.find { it.id == vinetaEnEdicionId }?.let {
                    dibujante.letraNotas(LetraNotas.textoCompleto(it.informacion), borrador.notasLetra, franjas[it.id] != null)
                },
                onLetraNotas = { nueva -> persistir(borrador.copy(notasLetra = nueva)) },
                onToggleAsistente = { asistenteTrazoActivo = !asistenteTrazoActivo },
                onAddArrow = {
                    val c = capaDe(borrador, capaActiva)
                    val nueva = Flecha(
                        x0 = 0.5, y0 = 0.85, x1 = 0.5, y1 = 0.60,
                        x2 = 0.65, y2 = 0.40, x3 = 0.80, y3 = 0.25,
                        width = ANCHO_FLECHA, color = selectedStrokeColor
                    )
                    val cc = c.copy(arrows = c.arrows + nueva)
                    seleccion = SeleccionEditor(flecha = cc.arrows.lastIndex)
                    activeTool = ActiveDrawTool.SELECT
                    persistir(conCapa(borrador, capaActiva, cc))
                },
                onToolChange = { activeTool = it },
                onColorChange = { selectedStrokeColor = it },
                onStrokeWidthChange = { selectedStrokeWidth = it },
                onUndo = {
                    val c = capaDe(borrador, capaActiva)
                    val cc = when {
                        c.strokes.isNotEmpty() -> c.copy(strokes = c.strokes.dropLast(1))
                        c.arrows.isNotEmpty() -> c.copy(arrows = c.arrows.dropLast(1))
                        c.stamps.isNotEmpty() -> c.copy(stamps = c.stamps.dropLast(1))
                        else -> null
                    }
                    if (cc != null) {
                        seleccion = SeleccionEditor()
                        persistir(conCapa(borrador, capaActiva, cc))
                    }
                },
                onClear = {
                    val c = capaDe(borrador, capaActiva)
                    val si = seleccion.sello
                    val fi = seleccion.flecha
                    val cc = when {
                        si != null && si in c.stamps.indices -> c.copy(stamps = c.stamps.filterIndexed { j, _ -> j != si })
                        fi != null && fi in c.arrows.indices -> c.copy(arrows = c.arrows.filterIndexed { j, _ -> j != fi })
                        else -> Capa()
                    }
                    seleccion = SeleccionEditor()
                    persistir(conCapa(borrador, capaActiva, cc))
                },
                onGuardar = {
                    val cur = vinetas.find { it.id == vinetaEnEdicionId }
                    if (cur != null && cargadoId == cur.id) {
                        onGuardarVineta(cur.conDibujo(borrador))
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
                        modifier = Modifier.width(44.dp),
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
                                maxLines = 1,
                                softWrap = false,
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
                    HeaderColumnaToggle("TOTAL", capaTotalVisible, { capaTotalVisible = !capaTotalVisible }, Modifier.width(58.dp))
                    HeaderColumnaToggle("PARCIAL", capaParcialVisible, { capaParcialVisible = !capaParcialVisible }, Modifier.width(54.dp))
                    HeaderColumnaToggle("DIBUJO", capaIndicacionVisible, { capaIndicacionVisible = !capaIndicacionVisible }, Modifier.width(TAM_DIBUJO + 6.dp))
                    HeaderColumnaToggle("NOTAS", capaAnotacionesVisible, { capaAnotacionesVisible = !capaAnotacionesVisible }, Modifier.weight(1f))
                    HeaderColumnaToggle("REG.", capaRegresivaVisible, { capaRegresivaVisible = !capaRegresivaVisible }, Modifier.width(52.dp))
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
                        val isEditingThis = v.id == vinetaEnEdicionId && cargadoId == v.id
                        val dibujoGuardado = remember(v.dibujo, v.tulipSvgData, v.notasSvgData, v.tulipTipo, v.peligro) { v.dibujoV1(bib) }
                        val dibujoFila = if (isEditingThis) borrador else dibujoGuardado

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
                            dibujo = dibujoFila,
                            franja = franjas[v.id],
                            dibujante = dibujante,
                            arrastre = arrastre,
                            capaActiva = capaActiva,
                            seleccion = if (isEditingThis) seleccion else SeleccionEditor(),
                            asistenteTrazoActivo = asistenteTrazoActivo,
                            activeTool = activeTool,
                            selectedColorHex = selectedStrokeColor,
                            capaNumeroVisible = capaNumeroVisible,
                            capaTotalVisible = capaTotalVisible,
                            capaParcialVisible = capaParcialVisible,
                            capaIndicacionVisible = capaIndicacionVisible,
                            capaAnotacionesVisible = capaAnotacionesVisible,
                            capaRegresivaVisible = capaRegresivaVisible,
                            onTocarFila = {
                                abrirEdicion(v, capaActiva)
                            },
                            onEditarIndicacion = {
                                abrirEdicion(v, capaActiva)
                                vinetaParaEditarNotas = v
                            },
                            onActivarCapa = { c ->
                                abrirEdicion(v, c)
                            },
                            onTocarAnotaciones = {
                                vinetaParaEditarNotas = v
                            },
                            onCambioCapa = { c, capa ->
                                if (isEditingThis) persistir(conCapa(borrador, c, capa))
                            },
                            onSeleccion = { s -> seleccion = s },
                            onToolChange = { tool ->
                                activeTool = tool
                            },
                            onSoltar = { c, item, x, y ->
                                insertarEn(v, c, item, x, y)
                            }
                        )
                    }
                }
            }
        }
    }

    val raizCoords = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { raizCoords[0] = it }) {
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
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
                                sidebarWidthDp = (sidebarWidthDp - deltaDp).coerceIn(160f, 320f)
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
                    onInsertar = insertarTocando,
                    onAppendNoteText = appendNoteAction,
                    onCollapse = { sidebarExpanded = false },
                    vinetaNumeroActiva = vinetas.find { it.id == vinetaEnEdicionId }?.numero,
                    arrastre = arrastre,
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
        Column(modifier = Modifier.fillMaxSize()) {
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
                    onInsertar = insertarTocando,
                    onAppendNoteText = appendNoteAction,
                    onCollapse = { sidebarExpanded = false },
                    vinetaNumeroActiva = vinetas.find { it.id == vinetaEnEdicionId }?.numero,
                    arrastre = arrastre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }
        }
    }

    // Símbolo que se está arrastrando desde la barra: sigue al dedo por encima de todo
    arrastre.item?.let { item ->
        val origen = raizCoords[0]?.takeIf { it.isAttached }?.localToWindow(Offset.Zero) ?: Offset.Zero
        val tamPx = with(density) { 64.dp.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        (arrastre.posicion.x - origen.x - tamPx / 2f).toInt(),
                        (arrastre.posicion.y - origen.y - tamPx / 2f).toInt()
                    )
                }
                .size(64.dp)
                .alpha(0.85f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
        ) {
            MuestraItemPaleta(item, bib, dibujante, Modifier.fillMaxSize().padding(4.dp))
        }
    }
    }

    // Modal de edición de texto y notas si se pulsa la columna de anotaciones
    vinetaParaEditarNotas?.let { v ->
        RoadbookNotesDialog(
            vineta = v,
            onGuardar = { nota, distTot, distPar, offRoad, velProm, velMax, tIdeal, rCap, esReinicio, distOculta, esOculta, mostrarWpt, esTierra, esPc, tipoPc ->
                // Se parte de la versión más nueva de la viñeta (y del dibujo en edición) para no
                // pisar un dibujo guardado mientras el diálogo estaba abierto.
                val actual = vinetas.find { it.id == v.id } ?: v
                val base = if (cargadoId == v.id && vinetaEnEdicionId == v.id) actual.conDibujo(borrador) else actual
                val vinetaActualizada = base.copy(
                    informacion = nota,
                    distanciaTotal = distTot,
                    distanciaParcial = distPar,
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
    dibujo: DibujoV1,
    franja: FranjaCarteles?,
    dibujante: DibujanteVineta,
    arrastre: ArrastreSimbolo,
    capaActiva: CapaEditada,
    seleccion: SeleccionEditor,
    asistenteTrazoActivo: Boolean = true,
    activeTool: ActiveDrawTool,
    selectedColorHex: String,
    capaNumeroVisible: Boolean,
    capaTotalVisible: Boolean,
    capaParcialVisible: Boolean,
    capaIndicacionVisible: Boolean,
    capaAnotacionesVisible: Boolean,
    capaRegresivaVisible: Boolean,
    onTocarFila: () -> Unit,
    onEditarIndicacion: () -> Unit = onTocarFila,
    onActivarCapa: (CapaEditada) -> Unit,
    onTocarAnotaciones: () -> Unit,
    onCambioCapa: (CapaEditada, Capa) -> Unit,
    onSeleccion: (SeleccionEditor) -> Unit,
    onToolChange: (ActiveDrawTool) -> Unit,
    onSoltar: (CapaEditada, ItemPaleta, Double, Double) -> Unit
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
                .height(ALTO_FILA)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. NÚMERO Y CASILLA DE BORRADO DEBAJO
            Box(
                modifier = Modifier
                    .width(44.dp)
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
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${vineta.numero}",
                                    color = if (isDark) Color.Black else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
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
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 2. TOTAL KM
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (capaTotalVisible) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
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
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (vineta.distanciaOculta) "X.XX" else String.format(java.util.Locale.US, "%.2f", vineta.distanciaTotal),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = if (vineta.distanciaOculta) Color(0xFFF59E0B) else textPrimary
                        )
                        Text(
                            text = "KM TOT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 3. PARCIAL KM
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (capaParcialVisible) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f", vineta.distanciaParcial),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = textPrimary
                        )
                        Text(
                            text = "PARC",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 4. RECUADRO DE DIBUJO (cuadrado, 100 × 100 unidades) CON EDICIÓN EN EL LUGAR
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                if (capaIndicacionVisible) {
                    RecuadroCapa(
                        clave = "${vineta.id}-D",
                        capa = dibujo.dibujo,
                        esNotas = false,
                        franja = null,
                        dibujante = dibujante,
                        editando = isEditing && capaActiva == CapaEditada.DIBUJO,
                        seleccion = seleccion,
                        activeTool = activeTool,
                        colorHex = selectedColorHex,
                        asistente = asistenteTrazoActivo,
                        arrastre = arrastre,
                        onCambio = { onCambioCapa(CapaEditada.DIBUJO, it) },
                        onSeleccion = onSeleccion,
                        onHerramienta = onToolChange,
                        onActivar = { onActivarCapa(CapaEditada.DIBUJO) },
                        onDobleToque = onEditarIndicacion,
                        onSoltar = { item, x, y -> onSoltar(CapaEditada.DIBUJO, item, x, y) },
                        modifier = Modifier.size(TAM_DIBUJO)
                    )
                } else {
                    Spacer(modifier = Modifier.size(TAM_DIBUJO))
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 5. NOTAS: capa de notas (100 × 64 unidades) con la franja de carteles y la nota escrita
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) RallySurface else Color(0xFFF8FAFC))
                    .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (capaAnotacionesVisible) {
                    // Etiquetas de la viñeta (no son parte del dibujo)
                    val neutroBg = if (isDark) Color.Black else Color(0xFFE2E8F0)
                    val etiquetas = buildList {
                        if (vineta.esOffRoad) add(Triple("🏜️ OFF-ROAD", Color(0xFF78350F), Color(0xFFFEF3C7)))
                        if (vineta.esReinicioCero) add(Triple("0,00 REINICIO", Color(0xFF065F46), Color(0xFFA7F3D0)))
                        if (vineta.distanciaOculta) add(Triple("X.XX OCULTA", Color(0xFF7C2D12), Color(0xFFFED7AA)))
                        if (vineta.esOcultaOrg) add(Triple("🔒 OCUL", Color(0xFF831843), Color(0xFFFBCFE8)))
                        if (vineta.esTierra) add(Triple("DIRT", Color(0xFF713F12), Color(0xFFFEF08A)))
                        if (vineta.esPuntoControl) {
                            val tipo = if (vineta.tipoPuntoControl.isNotBlank()) vineta.tipoPuntoControl else "PASO"
                            add(Triple("🎯 WP $tipo", Color(0xFF581C87), Color(0xFFE9D5FF)))
                        }
                        if (vineta.mostrarWpt) add(Triple(String.format(java.util.Locale.US, "WPT %.4f, %.4f", vineta.latitud, vineta.longitud), neutroBg, if (isDark) RallyCyan else FredianiCyanText))
                        if (vineta.radioCapturaMetros != 30) add(Triple("WPV ${vineta.radioCapturaMetros}m", neutroBg, textSecondary))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        etiquetas.forEach { (texto, fondo, colorTexto) ->
                            Surface(shape = RoundedCornerShape(4.dp), color = fondo) {
                                Text(
                                    text = texto,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.5.sp,
                                    color = colorTexto,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Recuadro de la capa de notas, con su proporción fija
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        RecuadroCapa(
                            clave = "${vineta.id}-N",
                            capa = dibujo.notasCapa,
                            esNotas = true,
                            franja = franja,
                            dibujante = dibujante,
                            editando = isEditing && capaActiva == CapaEditada.NOTAS,
                            seleccion = seleccion,
                            activeTool = activeTool,
                            colorHex = selectedColorHex,
                            asistente = asistenteTrazoActivo,
                            arrastre = arrastre,
                            onCambio = { onCambioCapa(CapaEditada.NOTAS, it) },
                            onSeleccion = onSeleccion,
                            onHerramienta = onToolChange,
                            onActivar = { onActivarCapa(CapaEditada.NOTAS) },
                            onDobleToque = onTocarAnotaciones,
                            onSoltar = { item, x, y -> onSoltar(CapaEditada.NOTAS, item, x, y) },
                            modifier = Modifier.aspectRatio((100.0 / ALTO_NOTAS).toFloat(), matchHeightConstraintsFirst = true),
                            // Punto 12: la app tiene un solo texto, que es la descripción
                            textoNotas = LetraNotas.textoCompleto(vineta.informacion),
                            notasLetra = dibujo.notasLetra
                        )
                    }

                    // Nota escrita y acceso a los datos de la viñeta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onTocarAnotaciones() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (vineta.informacion.isNotBlank()) "Editar nota y datos" else "Tocar para escribir la nota...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = "Datos de la viñeta",
                            tint = if (isDark) RallyCyan else FredianiCyanText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            VerticalDivider(color = cardBorder, modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp))

            // 6. REGRESIVA
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (capaRegresivaVisible) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f", distanciaRegresiva),
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = if (isDark) RallyCyanLight else FredianiCyanText
                        )
                        Text(
                            text = "REGRESIVA",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/** Lado del recuadro de dibujo (cuadrado) y alto de cada fila del roadbook. */
private val TAM_DIBUJO = 160.dp
private val ALTO_FILA = TAM_DIBUJO + 8.dp

/**
 * Un recuadro de la viñeta (dibujo o capa de notas). Si es el que se está editando muestra el
 * editor; si no, solo lo dibuja. También recibe los símbolos que se sueltan arrastrando.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecuadroCapa(
    clave: String,
    capa: Capa,
    esNotas: Boolean,
    franja: FranjaCarteles?,
    dibujante: DibujanteVineta,
    editando: Boolean,
    seleccion: SeleccionEditor,
    activeTool: ActiveDrawTool,
    colorHex: String,
    asistente: Boolean,
    arrastre: ArrastreSimbolo,
    onCambio: (Capa) -> Unit,
    onSeleccion: (SeleccionEditor) -> Unit,
    onHerramienta: (ActiveDrawTool) -> Unit,
    onActivar: () -> Unit,
    onDobleToque: () -> Unit,
    onSoltar: (ItemPaleta, Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    textoNotas: String = "",
    notasLetra: Double? = null
) {
    val onSoltarAct by rememberUpdatedState(onSoltar)
    DisposableEffect(clave) { onDispose { arrastre.quitar(clave) } }
    val resaltado = arrastre.item != null && arrastre.destinoBajoDedo == clave
    val forma = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(forma)
            .background(Color.White)
            .onGloballyPositioned { c ->
                val origen = c.localToWindow(Offset.Zero)
                val completo = androidx.compose.ui.geometry.Rect(origen, Size(c.size.width.toFloat(), c.size.height.toFloat()))
                arrastre.registrar(clave, c.boundsInWindow(), completo) { item, x, y -> onSoltarAct(item, x, y) }
            }
            .border(
                width = if (editando || resaltado) 2.dp else 1.dp,
                color = when {
                    resaltado -> Color(0xFFF59E0B)
                    editando -> FredianiCyan
                    else -> Color(0xFFCBD5E1)
                },
                shape = forma
            )
    ) {
        if (editando) {
            EditorCapa(
                capa = capa,
                esNotas = esNotas,
                franja = franja,
                dibujante = dibujante,
                seleccion = seleccion,
                herramienta = activeTool,
                colorHex = colorHex,
                asistente = asistente,
                onCambio = onCambio,
                onSeleccion = onSeleccion,
                onHerramienta = onHerramienta,
                modifier = Modifier.fillMaxSize(),
                textoNotas = textoNotas,
                notasLetra = notasLetra
            )
        } else {
            VistaCapa(
                capa = capa,
                esNotas = esNotas,
                franja = franja,
                dibujante = dibujante,
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = onActivar,
                        onDoubleClick = onDobleToque,
                        onLongClick = onDobleToque
                    ),
                textoNotas = textoNotas,
                notasLetra = notasLetra
            )
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
    capaActiva: CapaEditada = CapaEditada.DIBUJO,
    onCapaChange: (CapaEditada) -> Unit = {},
    letraNotasAuto: Boolean = true,
    letraNotasValor: Double? = null,
    onLetraNotas: (Double?) -> Unit = {},
    onToggleAsistente: () -> Unit = {},
    onAddArrow: () -> Unit = {},
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
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
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

                    // Recuadro que se edita: dibujo o capa de notas
                    listOf(CapaEditada.DIBUJO to "DIBUJO", CapaEditada.NOTAS to "NOTAS").forEach { (c, nombre) ->
                        val sel = capaActiva == c
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sel) FredianiCyan else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (sel) FredianiCyan else barBorder),
                            modifier = Modifier.clickable { onCapaChange(c) }
                        ) {
                            Text(
                                text = nombre,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = if (sel) Color.Black else (if (isDark) Color.White else Color(0xFF334155)),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp), color = barBorder)

                    // Letra del texto de las notas (punto 12): A↓ · Auto · A↑
                    if (letraNotasValor != null) {
                        val textoBoton = if (isDark) Color.White else Color(0xFF334155)
                        TextButton(
                            onClick = { onLetraNotas(LetraNotas.menos(letraNotasValor)) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(26.dp)
                        ) { Text("A↓", fontSize = 10.sp, fontWeight = FontWeight.Black, color = textoBoton) }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (letraNotasAuto) FredianiCyan else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (letraNotasAuto) FredianiCyan else barBorder),
                            modifier = Modifier.clickable { onLetraNotas(null) }
                        ) {
                            val valor = String.format(java.util.Locale.US, "%.1f", letraNotasValor).removeSuffix(".0").replace('.', ',')
                            Text(
                                text = if (letraNotasAuto) "Auto $valor" else valor,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = if (letraNotasAuto) Color.Black else textoBoton,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        TextButton(
                            onClick = { onLetraNotas(LetraNotas.mas(letraNotasValor)) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(26.dp)
                        ) { Text("A↑", fontSize = 10.sp, fontWeight = FontWeight.Black, color = textoBoton) }

                        VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp), color = barBorder)
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

                    // El palito del GPS siempre está en la flecha: se mueve arrastrando su punta.


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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 78.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "GUARDAR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
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
        distTotal: Double,
        distParcial: Double,
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

                // El peligro no se elige acá: se calcula de los símbolos EXCL / PELIGRO dibujados.

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
                        tot,
                        par,
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
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

