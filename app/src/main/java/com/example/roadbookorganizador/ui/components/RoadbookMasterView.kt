package com.example.roadbookorganizador.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
import com.example.roadbookorganizador.ui.vineta.ANCHOS_LAPIZ
import com.example.roadbookorganizador.ui.vineta.COLORES_PLATAFORMA
import com.example.roadbookorganizador.vineta.CuadroTexto
import com.example.roadbookorganizador.vineta.Colocar
import com.example.roadbookorganizador.data.local.PreferenciasVista
import com.example.roadbookorganizador.data.local.Preferencias
import com.example.roadbookorganizador.data.local.preferenciasVista
import com.example.roadbookorganizador.ui.vineta.ArrastreSimbolo
import com.example.roadbookorganizador.ui.vineta.BibliotecaVineta
import com.example.roadbookorganizador.ui.vineta.CapaEditada
import com.example.roadbookorganizador.ui.vineta.DibujanteVineta
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
    RULER,
    TEXT,
    GOMA
}

/** Escala de un símbolo puesto en la capa de notas (punto 8: 40 / 72). */

/** Cuadro de texto que se está escribiendo: recuadro, índice (null = nuevo) y lugar. */
private data class TextoPendiente(val capa: CapaEditada, val indice: Int?, val x: Double, val y: Double, val texto: String)

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
    /** "+ Indicación": viñeta vacía después de la elegida (id) o al final (null). */
    onInsertarIndicacion: ((previaId: Long?) -> Unit)? = null,
    onDuplicarIndicacion: ((VinetaEntity) -> Unit)? = null,
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
    var anchoLapiz by remember { mutableStateOf(ANCHOS_LAPIZ[1]) }
    var textoPendiente by remember { mutableStateOf<TextoPendiente?>(null) }
    /** Tamaño de las viñetas (0,5 a 1), como el control de zoom de la plataforma. */
    var zoomVinetas by remember { mutableFloatStateOf(1f) }
    val focoTeclado = remember { androidx.compose.ui.focus.FocusRequester() }

    // Dibujo de la viñeta en edición (formato versión 1 de la especificación)
    val context = LocalContext.current
    val bib = remember { BibliotecaVineta.obtener(context) }
    val dibujante = remember(bib) { DibujanteVineta(bib) }
    val arrastre = remember { ArrastreSimbolo() }
    val prefs = preferenciasVista()
    var mostrarPreferencias by remember { mutableStateOf(false) }
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
    // Cómo se acomoda (cambia sola al girar): GRANDE = paleta al costado; MEDIANO = paleta como
    // panel desde la derecha; CHICO (celular) = paleta desde abajo y el dibujo se edita en grande.
    val tamano = tamanoPantalla()
    var sidebarExpanded by remember(tamano) { mutableStateOf(tamano == TamanoPantalla.GRANDE) }
    var sidebarWidthDp by remember { mutableFloatStateOf(236f) }
    /** Tablet acostada: qué parte del alto ocupa la paleta (debajo de la hoja). */
    var paletaAlto by remember { mutableFloatStateOf(0.45f) }
    var altoTotalPx by remember { mutableFloatStateOf(0f) }
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
        try { focoTeclado.requestFocus() } catch (_: Exception) {}
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

    /** Deshacer en el dibujo seleccionado: saca lo último que se agregó. */
    val deshacer: () -> Unit = {
        val c = capaDe(borrador, capaActiva)
        val cc = when {
            c.strokes.isNotEmpty() -> c.copy(strokes = c.strokes.dropLast(1))
            c.texts.isNotEmpty() -> c.copy(texts = c.texts.dropLast(1))
            c.arrows.isNotEmpty() -> c.copy(arrows = c.arrows.dropLast(1))
            c.stamps.isNotEmpty() -> c.copy(stamps = c.stamps.dropLast(1))
            else -> null
        }
        if (cc != null) {
            seleccion = SeleccionEditor()
            persistir(conCapa(borrador, capaActiva, cc))
        }
    }

    /** Borrar la flecha, el símbolo o el texto seleccionado (tecla Supr). Sin selección, nada. */
    val borrarSeleccion: () -> Unit = {
        val c = capaDe(borrador, capaActiva)
        val si = seleccion.sello
        val fi = seleccion.flecha
        val ti = seleccion.texto
        val cc = when {
            si != null && si in c.stamps.indices -> c.copy(stamps = c.stamps.filterIndexed { j, _ -> j != si })
            fi != null && fi in c.arrows.indices -> c.copy(arrows = c.arrows.filterIndexed { j, _ -> j != fi })
            ti != null && ti in c.texts.indices -> c.copy(texts = c.texts.filterIndexed { j, _ -> j != ti })
            else -> null
        }
        if (cc != null) {
            seleccion = SeleccionEditor()
            persistir(conCapa(borrador, capaActiva, cc))
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

    /** Inserta un símbolo o una flecha armada en un recuadro, en (x, y) de 0 a 1. */
    fun insertarEn(v: VinetaEntity, capa: CapaEditada, item: ItemPaleta, x: Double, y: Double) {
        abrirEdicion(v, capa)
        val c = capaDe(borrador, capa)
        val nueva = when (item) {
            is ItemPaleta.SimboloItem -> {
                // Punto 8: scale 0,58 en los dos recuadros; el color, solo si se puede pintar
                val pintable = bib.simbolo(item.codigo)?.colorable == true
                val nuevo = Sello(
                    item.codigo, Colocar.posicion(x), Colocar.posicion(y), Colocar.ESCALA_SIMBOLO, 0.0,
                    color = if (pintable) selectedStrokeColor else null
                )
                val cc = c.copy(stamps = c.stamps + nuevo)
                seleccion = SeleccionEditor(sello = cc.stamps.lastIndex)
                cc
            }
            is ItemPaleta.ArmadaItem -> {
                // Las flechas armadas se ponen como un cuadrado de 29 mm, centradas donde se
                // soltaron, con el color elegido. El gps va solo si es la primera flecha del
                // dibujo; en la capa de notas, nunca.
                val conGps = capa == CapaEditada.DIBUJO && c.arrows.isEmpty()
                val base = item.flecha.copy(gps = if (conGps) item.flecha.gps else null, color = selectedStrokeColor)
                val f = Colocar.armadaEnCaja(base, x, y)
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
        // Como la plataforma: en el dibujo un poco arriba del centro (0,5; 0,45), en las notas al centro
        if (destino != null) insertarEn(destino, capaActiva, item, 0.5, if (capaActiva == CapaEditada.DIBUJO) 0.45 else 0.5)
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
            // BARRAS DE EDICIÓN COPIADAS DE LA PLATAFORMA (RoadbookWorkstation):
            // 1) dock: + Indicación, Duplicar, Eliminar … Guardado
            // 2) dibujo: Mover, Flecha, Lápiz, Goma, Texto, Deshacer, Borrar, Prohibido · A↓ Auto A↑ ·
            //    colores · grosor · tamaño de las viñetas
            val enEdicion = vinetas.find { it.id == vinetaEnEdicionId }
            BarrasEdicionPlataforma(
                hayVineta = enEdicion != null,
                activeTool = activeTool,
                onToolChange = { activeTool = it },
                colorActual = run {
                    // Con algo pintable elegido, se marca su color (como la web)
                    val c = capaDe(borrador, capaActiva)
                    seleccion.flecha?.let { c.arrows.getOrNull(it)?.color }
                        ?: seleccion.texto?.let { c.texts.getOrNull(it)?.color }
                        ?: seleccion.sello?.let { c.stamps.getOrNull(it)?.color }
                        ?: selectedStrokeColor
                },
                letraNotasAuto = borrador.notasLetra == null,
                letraNotasValor = enEdicion?.let {
                    dibujante.letraNotas(LetraNotas.textoCompleto(it.informacion), borrador.notasLetra, franjas[it.id] != null)
                },
                onLetraNotas = { nueva -> persistir(borrador.copy(notasLetra = nueva)) },
                onColorChange = { hex ->
                    selectedStrokeColor = hex
                    // Si hay algo elegido que se puede pintar, se pinta con ese color
                    val c = capaDe(borrador, capaActiva)
                    val si = seleccion.sello
                    val fi = seleccion.flecha
                    val ti = seleccion.texto
                    val cc = when {
                        fi != null && fi in c.arrows.indices ->
                            c.copy(arrows = c.arrows.toMutableList().also { it[fi] = it[fi].copy(color = hex) })
                        ti != null && ti in c.texts.indices ->
                            c.copy(texts = c.texts.toMutableList().also { it[ti] = it[ti].copy(color = hex) })
                        si != null && si in c.stamps.indices && bib.simbolo(c.stamps[si].type)?.colorable == true ->
                            c.copy(stamps = c.stamps.toMutableList().also { it[si] = it[si].copy(color = hex) })
                        else -> null
                    }
                    if (cc != null) persistir(conCapa(borrador, capaActiva, cc))
                },
                anchoLapiz = anchoLapiz,
                onAnchoLapiz = { g ->
                    anchoLapiz = g
                    // La flecha elegida toma un ancho acorde (fino, medio, grueso), como en la web
                    val c = capaDe(borrador, capaActiva)
                    val fi = seleccion.flecha
                    if (fi != null && fi in c.arrows.indices) {
                        val cc = c.copy(arrows = c.arrows.toMutableList().also { it[fi] = it[fi].copy(width = Colocar.anchoFlechaParaGrosor(g)) })
                        persistir(conCapa(borrador, capaActiva, cc))
                    }
                },
                hayFlechaElegida = seleccion.flecha != null,
                onAgregarProhibido = {
                    val c = capaDe(borrador, capaActiva)
                    val fi = seleccion.flecha
                    if (fi != null && fi in c.arrows.indices) {
                        val f = c.arrows[fi]
                        val cc = c.copy(arrows = c.arrows.toMutableList().also {
                            it[fi] = f.copy(prohibidos = f.prohibidos.orEmpty() + Colocar.nuevoProhibido(f))
                        })
                        seleccion = SeleccionEditor(flecha = fi)
                        activeTool = ActiveDrawTool.SELECT
                        persistir(conCapa(borrador, capaActiva, cc))
                    }
                },
                onUndo = deshacer,
                onBorrar = borrarSeleccion,
                zoom = zoomVinetas,
                onZoom = { zoomVinetas = it.coerceIn(0.5f, 1f) },
                onNuevaIndicacion = { onInsertarIndicacion?.invoke(vinetaEnEdicionId ?: vinetaSeleccionadaId) },
                onDuplicar = { enEdicion?.let { onDuplicarIndicacion?.invoke(it) } },
                onEliminar = {
                    if (vinetasSeleccionadasParaBorrar.isEmpty()) enEdicion?.let { vinetasSeleccionadasParaBorrar = setOf(it.id) }
                    if (vinetasSeleccionadasParaBorrar.isNotEmpty()) mostrarDialogoConfirmarBorrado = true
                },
                cantidadParaBorrar = vinetasSeleccionadasParaBorrar.size,
                onGuardar = {
                    val cur = vinetas.find { it.id == vinetaEnEdicionId }
                    if (cur != null && cargadoId == cur.id) onGuardarVineta(cur.conDibujo(borrador))
                },
                paletaVisible = sidebarExpanded,
                onPaleta = { sidebarExpanded = !sidebarExpanded },
                compacto = tamano == TamanoPantalla.CHICO,
                onDatosTramo = { mostrarDialogoVelocidadesTramo = true },
                onPreferencias = { mostrarPreferencias = true }
            )

            // CELULAR: la viñeta que se edita se dibuja en grande (en la hoja queda muy chica)
            val editorGrande = tamano == TamanoPantalla.CHICO && enEdicion != null && cargadoId == enEdicion.id
            if (editorGrande && enEdicion != null) {
                EditorGrandeCelular(
                    vineta = enEdicion,
                    capaActiva = capaActiva,
                    onCapa = { c -> abrirEdicion(enEdicion, c) },
                    onCerrar = { vinetaEnEdicionId = null },
                    isDark = isDark
                ) {
                    RecuadroCapa(
                        clave = "grande-${enEdicion.id}-${capaActiva.name}",
                        capa = capaDe(borrador, capaActiva),
                        esNotas = capaActiva == CapaEditada.NOTAS,
                        franja = if (capaActiva == CapaEditada.NOTAS) franjas[enEdicion.id] else null,
                        dibujante = dibujante,
                        editando = true,
                        seleccion = seleccion,
                        activeTool = activeTool,
                        colorHex = selectedStrokeColor,
                        asistente = asistenteTrazoActivo,
                        arrastre = arrastre,
                        onCambio = { persistir(conCapa(borrador, capaActiva, it)) },
                        onSeleccion = { seleccion = it },
                        onHerramienta = { activeTool = it },
                        onActivar = {},
                        onDobleToque = { if (capaActiva == CapaEditada.NOTAS) vinetaParaEditarNotas = enEdicion },
                        onSoltar = { item, x, y -> insertarEn(enEdicion, capaActiva, item, x, y) },
                        anchoLapiz = anchoLapiz,
                        onPedirTexto = { x, y -> textoPendiente = TextoPendiente(capaActiva, null, x, y, "") },
                        onEditarTexto = { i ->
                            capaDe(borrador, capaActiva).texts.getOrNull(i)?.let { t ->
                                textoPendiente = TextoPendiente(capaActiva, i, t.x, t.y, t.text)
                            }
                        },
                        textoNotas = if (capaActiva == CapaEditada.NOTAS) LetraNotas.textoCompleto(enEdicion.informacion.trim()) else "",
                        notasLetra = if (capaActiva == CapaEditada.NOTAS) borrador.notasLetra else null,
                        palito = capaActiva == CapaEditada.DIBUJO && prefs.mostrarPalitoGps,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 4 y 5. LA HOJA: encabezado y viñetas con las medidas de la plataforma (punto 1)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
            val mm = mmParaAncho(maxWidth * zoomVinetas)
            Column(modifier = Modifier.fillMaxHeight().width(maxWidth * zoomVinetas)) {
            EncabezadoVinetas(mm) {
                Checkbox(
                    checked = vinetas.isNotEmpty() && vinetas.all { it.id in vinetasSeleccionadasParaBorrar },
                    onCheckedChange = { checked ->
                        vinetasSeleccionadasParaBorrar = if (checked) vinetas.map { it.id }.toSet() else emptySet()
                    },
                    colors = CheckboxDefaults.colors(checkedColor = RallyRed, uncheckedColor = Color.Gray, checkmarkColor = Color.White),
                    modifier = Modifier.size(20.dp)
                )
            }
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
                            text = "Sin indicaciones registradas. Tocá el mapa o «+ Indicación» para cargar la primera.",
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
                        .weight(1f)
                ) {
                    items(vinetas, key = { it.id }) { v ->
                        // Dirección en gris en todas las viñetas, salvo que el tramo sea un enlace
                        val enPe = tramo != null && !tramo.tipo.equals("ENLACE", ignoreCase = true)
                        val distRegresiva = (distanciaTotalEstimada - v.distanciaTotal).coerceAtLeast(0.0)
                        val isEditingThis = v.id == vinetaEnEdicionId && cargadoId == v.id
                        val dibujoGuardado = remember(v.dibujo, v.tulipSvgData, v.notasSvgData, v.tulipTipo, v.peligro) { v.dibujoV1(bib) }
                        val dibujoFila = if (isEditingThis) borrador else dibujoGuardado

                        val siguiente = vinetas.getOrNull(vinetas.indexOf(v) + 1)
                        FilaRoadbookInPlace(
                            vineta = v,
                            enPe = enPe,
                            rayaAbajo = when {
                                siguiente == null -> "ninguna"
                                siguiente.distanciaParcial < 0.2 -> "punteada"
                                else -> "normal"
                            },
                            distanciaRegresiva = distRegresiva,
                            isEditing = isEditingThis,
                            editarEnFila = !editorGrande,
                            mm = mm,
                            prefs = prefs,
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
                            // Borrar varias (revisión 24): mantener apretada una viñeta empieza a marcar;
                            // mientras haya marcadas, tocar una la suma o la saca de la marca.
                            onTocarFila = {
                                if (vinetasSeleccionadasParaBorrar.isNotEmpty()) vinetasSeleccionadasParaBorrar = alternarMarca(vinetasSeleccionadasParaBorrar, v.id)
                                else abrirEdicion(v, capaActiva)
                            },
                            onEditarIndicacion = {
                                abrirEdicion(v, capaActiva)
                                vinetaParaEditarNotas = v
                            },
                            onMantener = {
                                vinetasSeleccionadasParaBorrar = alternarMarca(vinetasSeleccionadasParaBorrar, v.id)
                            },
                            onActivarCapa = { c ->
                                if (vinetasSeleccionadasParaBorrar.isNotEmpty()) vinetasSeleccionadasParaBorrar = alternarMarca(vinetasSeleccionadasParaBorrar, v.id)
                                else abrirEdicion(v, c)
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
                            },
                            anchoLapiz = anchoLapiz,
                            onPedirTexto = { c, x, y ->
                                textoPendiente = TextoPendiente(c, null, x, y, "")
                            },
                            onEditarTexto = { c, i ->
                                capaDe(borrador, c).texts.getOrNull(i)?.let { t ->
                                    textoPendiente = TextoPendiente(c, i, t.x, t.y, t.text)
                                }
                            }
                        )
                    }
                }
            }
            }
            }
        }
    }

    val raizCoords = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
    Box(modifier = modifier
        .fillMaxSize()
        .onGloballyPositioned { raizCoords[0] = it }
        .focusRequester(focoTeclado)
        .onKeyEvent { e ->
            // Mismos atajos que la plataforma (InteractiveCroquisCanvas): Supr / Retroceso borra
            // lo seleccionado y Esc suelta la selección. Un campo de texto con el foco los usa antes.
            if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown || vinetaEnEdicionId == null) return@onKeyEvent false
            when (e.key) {
                androidx.compose.ui.input.key.Key.Delete, androidx.compose.ui.input.key.Key.Backspace -> { borrarSeleccion(); true }
                androidx.compose.ui.input.key.Key.Escape -> { seleccion = SeleccionEditor(); true }
                else -> false
            }
        }
        .focusable()) {
    if (tamano == TamanoPantalla.GRANDE) {
        // =========================================================================
        // GRANDE (tablet acostada): la hoja y la paleta al costado, como la plataforma.
        // La raya del medio se arrastra para hacer la paleta más ancha o más angosta.
        // =========================================================================
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

            if (sidebarExpanded) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaDp = dragAmount.x / density.density
                                sidebarWidthDp = (sidebarWidthDp - deltaDp).coerceIn(170f, 460f)
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
                SidebarSymbolsPalette(
                    onInsertar = insertarTocando,
                    onAppendNoteText = appendNoteAction,
                    onCollapse = { sidebarExpanded = false },
                    vinetaNumeroActiva = vinetas.find { it.id == vinetaEnEdicionId }?.numero,
                    arrastre = arrastre,
                    modifier = Modifier
                        .width(sidebarWidthDp.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    } else {
        // =========================================================================
        // MEDIANO y CHICO: la hoja ocupa todo; la paleta se abre encima (desde la derecha en la
        // tablet parada, desde abajo en el celular). Mientras se arrastra un símbolo se vuelve
        // transparente, para ver dónde se suelta.
        // =========================================================================
        Box(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
            ) {
                renderTableCardContent()
            }

            if (sidebarExpanded) {
                val enCelular = tamano == TamanoPantalla.CHICO
                Surface(
                    shape = if (enCelular) RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp) else RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
                    shadowElevation = 12.dp,
                    color = Color.Transparent,
                    modifier = Modifier
                        .align(if (enCelular) Alignment.BottomCenter else Alignment.CenterEnd)
                        .then(if (enCelular) Modifier.fillMaxWidth().fillMaxHeight(0.55f) else Modifier.width(320.dp).fillMaxHeight())
                        .alpha(if (arrastre.item != null) 0.15f else 1f)
                ) {
                    SidebarSymbolsPalette(
                        onInsertar = insertarTocando,
                        onAppendNoteText = appendNoteAction,
                        onCollapse = { sidebarExpanded = false },
                        vinetaNumeroActiva = vinetas.find { it.id == vinetaEnEdicionId }?.numero,
                        arrastre = arrastre,
                        modifier = Modifier.fillMaxSize()
                    )
                }
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

    // Preferencias (como el engranaje de la plataforma)
    if (mostrarPreferencias) {
        DialogoPreferencias(onCerrar = { mostrarPreferencias = false })
    }

    // Cuadro de texto nuevo o editado (punto 8 bis)
    textoPendiente?.let { tp ->
        var texto by remember(tp) { mutableStateOf(tp.texto) }
        AlertDialog(
            onDismissRequest = { textoPendiente = null },
            title = { Text(if (tp.indice == null) "Texto nuevo" else "Editar texto", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Texto (puede tener varias líneas)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = capaDe(borrador, tp.capa)
                        val i = tp.indice
                        val cc = when {
                            i == null && texto.isNotBlank() -> {
                                val tam = Colocar.TAMANO_TEXTO // 4,5 en los dos recuadros (punto 8 bis)
                                c.copy(texts = c.texts + CuadroTexto(tp.x, tp.y, texto, tam, selectedStrokeColor))
                            }
                            i != null && i in c.texts.indices && texto.isBlank() ->
                                c.copy(texts = c.texts.filterIndexed { j, _ -> j != i })
                            i != null && i in c.texts.indices ->
                                c.copy(texts = c.texts.toMutableList().also { it[i] = it[i].copy(text = texto) })
                            else -> null
                        }
                        if (cc != null) {
                            seleccion = if (texto.isNotBlank()) SeleccionEditor(texto = i ?: cc.texts.lastIndex) else SeleccionEditor()
                            activeTool = ActiveDrawTool.SELECT
                            persistir(conCapa(borrador, tp.capa, cc))
                        }
                        textoPendiente = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FredianiGreen)
                ) { Text("Listo", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { textoPendiente = null }) { Text("Cancelar") } }
        )
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

/**
 * Medidas de la hoja (ESPECIFICACION_VINETA.md, punto 1), en milímetros, como la plataforma:
 * Total 25 | Parcial 25 | Dirección 50 | Informaciones 50 | Regresiva 25 — alto 45.
 * En pantalla se dibuja todo con un "milímetro" en dp que sale del ancho disponible.
 */
val COLUMNAS_MM = listOf(25f, 25f, 50f, 50f, 25f)
const val ANCHO_HOJA_MM = 175f
const val ALTO_FILA_MM = 45f
/** Columna propia de la app (casilla para borrar), a la izquierda de la hoja. */
val ANCHO_COLUMNA_APP = 30.dp

/** La raya de la derecha de una casilla de la hoja (las de abajo las pone la fila). */
private fun Modifier.rayaDerecha(ancho: androidx.compose.ui.unit.Dp, color: Color): Modifier = this.drawBehind {
    val w = ancho.toPx()
    drawLine(color, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
}

/** Suma la viñeta a las marcadas para borrar, o la saca si ya estaba. */
private fun alternarMarca(marcadas: Set<Long>, id: Long): Set<Long> = if (id in marcadas) marcadas - id else marcadas + id

/** Fondo gris de la parte cronometrada (P.E.), el mismo de la plataforma (#d4d4d8). */
val GRIS_PE = Color(0xFFD4D4D8)

/**
 * Parte cronometrada del tramo, en km (como la manda la plataforma en `rangoPe` del detalle del
 * tramo). Si la plataforma dice null, no hay P.E. Si el tramo no vino de la plataforma con ese
 * dato y es de tipo PE, se toma entero (la misma regla que usa la plataforma).
 */
fun rangoPeDelTramo(tramo: TramoEntity?, kmFinal: Double): Pair<Double, Double>? {
    if (tramo == null) return null
    val o = tramo.datosServidor?.let { runCatching { com.google.gson.JsonParser.parseString(it).asJsonObject }.getOrNull() }
    val clave = o?.keySet()?.firstOrNull { it.equals("rangoPe", ignoreCase = true) }
    if (o != null && clave != null) {
        val r = o.get(clave)
        if (r == null || r.isJsonNull || !r.isJsonObject) return null
        val ro = r.asJsonObject
        fun num(k: String) = ro.keySet().firstOrNull { it.equals(k, ignoreCase = true) }?.let { runCatching { ro.get(it).asDouble }.getOrNull() }
        val desde = num("desdeKm") ?: return null
        val hasta = num("hastaKm") ?: return null
        return desde to hasta
    }
    return if (tramo.tipo.equals("PE", ignoreCase = true)) 0.0 to maxOf(kmFinal, tramo.distanciaTotalEstimada) else null
}

/** Tamaño de letra de la hoja: n puntos tipográficos a mm (1 pt = 0,35278 mm). */
private fun ptEnMm(pt: Float) = pt * 0.35278f

/** "Milímetro" en dp para que la hoja entre en [ancho] (entre 2 y 4,6 dp). */
fun mmParaAncho(ancho: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
    ((ancho - ANCHO_COLUMNA_APP) / ANCHO_HOJA_MM).coerceIn(2.0.dp, 4.6.dp)

/** Encabezado de las columnas, como en la hoja del rally mundial (blanco, con rayas). */
@Composable
fun EncabezadoVinetas(mm: androidx.compose.ui.unit.Dp, borde: Color = Color(0xFF0F172A), columnaApp: @Composable () -> Unit = {}) {
    val density = LocalDensity.current
    // Con la hoja achicada el tamaño "real" queda ilegible: nunca menos de 8 sp (10 sp los títulos grandes)
    fun pt(n: Float) = with(density) {
        val real = (mm * ptEnMm(n)).toSp()
        val minimo = if (n >= 10f) 10.sp else 8.sp
        if (real.value < minimo.value) minimo else real
    }
    // Sin el espacio extra arriba y abajo de la letra: en los dos renglones de DISTANCIA no se corta
    val sinRelleno = androidx.compose.ui.text.TextStyle(
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        lineHeight = 10.sp
    )
    val raya = 1.dp
    Row(
        modifier = Modifier
            .height(maxOf(mm * 11f, 42.dp))
            .background(Color.White)
    ) {
        Box(Modifier.width(ANCHO_COLUMNA_APP).fillMaxHeight().background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) { columnaApp() }
        Column(Modifier.width(mm * 50f).fillMaxHeight().border(raya, borde)) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("DISTANCIA", fontSize = pt(7f), fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, style = sinRelleno)
            }
            HorizontalDivider(color = borde, thickness = raya)
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("TOTAL", fontSize = pt(6.5f), fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, style = sinRelleno)
                }
                VerticalDivider(color = borde, thickness = raya)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text("PARCIAL", fontSize = pt(6.5f), fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, style = sinRelleno)
                }
            }
        }
        Box(Modifier.width(mm * 50f).fillMaxHeight().border(raya, borde), contentAlignment = Alignment.Center) {
            Text("DIRECCIÓN", fontSize = pt(10f), fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
        }
        Box(Modifier.width(mm * 50f).fillMaxHeight().border(raya, borde), contentAlignment = Alignment.Center) {
            Text("INFORMACIONES", fontSize = pt(10f), fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
        }
        Box(Modifier.width(mm * 25f).fillMaxHeight().border(raya, borde), contentAlignment = Alignment.Center) {
            Text("DIST. TOTAL\nREGRESIVA", fontSize = pt(6.5f), fontWeight = FontWeight.Bold, color = Color.Black,
                textAlign = TextAlign.Center, lineHeight = pt(7.5f))
        }
    }
}

/**
 * UNA VIÑETA COMO EN LA HOJA DE RUTA, igual que la plataforma (FilaVineta.tsx): el dibujo ocupa
 * toda la celda de Dirección con el número en una cajita abajo a la izquierda; la celda de
 * Informaciones tiene el texto de las notas, encima la capa de notas y encima los carteles.
 * Las distancias van arriba, centradas. A la izquierda, la casilla de la app para borrar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilaRoadbookInPlace(
    vineta: VinetaEntity,
    /** La viñeta está dentro de la parte cronometrada (P.E.): la Dirección va con fondo gris, como en la plataforma. */
    enPe: Boolean = false,
    distanciaRegresiva: Double,
    isEditing: Boolean,
    mm: androidx.compose.ui.unit.Dp,
    prefs: PreferenciasVista,
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
    /** Mantener apretada la viñeta: la marca para borrar (revisión 24). */
    onMantener: () -> Unit = onEditarIndicacion,
    /**
     * La raya de abajo (revisión 25): "normal", "punteada" (la siguiente está a menos de 200 m)
     * o "ninguna" (la última de la hoja).
     */
    rayaAbajo: String = "normal",
    onActivarCapa: (CapaEditada) -> Unit,
    onTocarAnotaciones: () -> Unit,
    onCambioCapa: (CapaEditada, Capa) -> Unit,
    onSeleccion: (SeleccionEditor) -> Unit,
    onToolChange: (ActiveDrawTool) -> Unit,
    onSoltar: (CapaEditada, ItemPaleta, Double, Double) -> Unit,
    anchoLapiz: Double = ANCHOS_LAPIZ[1],
    onPedirTexto: (CapaEditada, Double, Double) -> Unit = { _, _, _ -> },
    onEditarTexto: (CapaEditada, Int) -> Unit = { _, _ -> },
    /** false = la fila solo muestra (en el celular el dibujo se edita en grande, arriba). */
    editarEnFila: Boolean = true
) {
    val density = LocalDensity.current
    fun pt(n: Float) = with(density) { (mm * ptEnMm(n)).toSp() }
    val borde = Color(0xFF0F172A)
    val raya = 1.dp
    val oculta = vineta.distanciaOculta

    // Etiquetas propias de la app (no son parte de la hoja), chiquitas debajo de las distancias
    val etiquetas = buildList {
        if (vineta.esReinicioCero) add("0,00" to Color(0xFF059669))
        if (oculta) add("X.XX" to Color(0xFFD97706))
        if (vineta.esOffRoad) add("OFF-ROAD" to Color(0xFF78350F))
        if (vineta.esOcultaOrg) add("OCUL" to Color(0xFF831843))
        if (vineta.esTierra) add("DIRT" to Color(0xFF713F12))
        if (vineta.esPuntoControl) add("WP ${vineta.tipoPuntoControl.ifBlank { "PASO" }}" to Color(0xFF581C87))
        if (vineta.radioCapturaMetros != 30) add("WPV ${vineta.radioCapturaMetros}m" to Color(0xFF475569))
        if (prefs.mostrarRumboCap && vineta.rumbo != 0f) add("CAP ${String.format("%.0f°", vineta.rumbo)}" to Color(0xFF334155))
    }

    @Composable
    fun Distancia(valor: String, visible: Boolean, extra: @Composable ColumnScope.() -> Unit = {}) {
        Column(
            modifier = Modifier
                .width(mm * 25f)
                .fillMaxHeight()
                .rayaDerecha(raya, borde)
                .padding(top = mm * 3f, start = 2.dp, end = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (visible) {
                Text(
                    text = valor,
                    fontSize = pt(13f),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }
            extra()
        }
    }

    Row(
        modifier = Modifier
            .height(mm * ALTO_FILA_MM)
            .background(if (estaSeleccionadaParaBorrar) Color(0xFFFFE4E6) else Color.White)
            .border(
                if (isEditing || estaSeleccionadaParaBorrar) 2.dp else 0.dp,
                when { estaSeleccionadaParaBorrar -> Color(0xFFF43F5E); isEditing -> FredianiCyan; else -> Color.Transparent }
            )
            .combinedClickable(
                onClick = { onTocarFila() },
                onDoubleClick = { onEditarIndicacion() },
                onLongClick = { onMantener() }
            )
            // Rayas de la hoja como la plataforma: la de la izquierda de la hoja y la de abajo
            // (llena, punteada gruesa si la siguiente está a menos de 200 m, o ninguna en la última)
            .drawWithContent {
                drawContent()
                val x0 = ANCHO_COLUMNA_APP.toPx()
                val w = raya.toPx()
                drawLine(borde, Offset(x0 + w / 2, 0f), Offset(x0 + w / 2, size.height), w)
                when (rayaAbajo) {
                    "punteada" -> {
                        val g = 2.5.dp.toPx()
                        drawLine(
                            borde, Offset(x0, size.height - g / 2), Offset(size.width, size.height - g / 2), g,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, g * 2f), 0f)
                        )
                    }
                    "ninguna" -> Unit
                    else -> drawLine(borde, Offset(x0, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
                }
            }
    ) {
        // Columna de la app: casilla para borrar
        Box(
            modifier = Modifier
                .width(ANCHO_COLUMNA_APP)
                .fillMaxHeight()
                .background(if (isEditing) FredianiCyan.copy(alpha = 0.15f) else Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = estaSeleccionadaParaBorrar,
                onCheckedChange = { onToggleSeleccionBorrar() },
                colors = CheckboxDefaults.colors(checkedColor = RallyRed, uncheckedColor = Color.Gray, checkmarkColor = Color.White),
                modifier = Modifier.size(20.dp)
            )
        }

        // TOTAL
        Distancia(if (oculta) "" else prefs.km(vineta.distanciaTotal), capaTotalVisible) {
            Spacer(Modifier.height(mm * 1.5f))
            etiquetas.forEach { (texto, color) ->
                Surface(shape = RoundedCornerShape(3.dp), color = color, modifier = Modifier.padding(bottom = 2.dp)) {
                    Text(texto, fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1,
                        modifier = Modifier.padding(horizontal = 3.dp))
                }
            }
        }

        // PARCIAL
        Distancia(if (oculta) "" else prefs.km(vineta.distanciaParcial), capaParcialVisible)

        // DIRECCIÓN: el dibujo ocupa toda la celda (100 × 90) y el número va abajo a la izquierda
        Box(
            modifier = Modifier
                .width(mm * 50f)
                .fillMaxHeight()
                .rayaDerecha(raya, borde)
        ) {
            if (capaIndicacionVisible) {
                RecuadroCapa(
                    fondo = if (enPe) GRIS_PE else Color.White,
                    clave = "${vineta.id}-D",
                    capa = dibujo.dibujo,
                    esNotas = false,
                    franja = null,
                    dibujante = dibujante,
                    editando = editarEnFila && isEditing && capaActiva == CapaEditada.DIBUJO,
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
                    anchoLapiz = anchoLapiz,
                    onPedirTexto = { x, y -> onPedirTexto(CapaEditada.DIBUJO, x, y) },
                    onEditarTexto = { i -> onEditarTexto(CapaEditada.DIBUJO, i) },
                    palito = prefs.mostrarPalitoGps,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (capaNumeroVisible) {
                // Cajita del número: borde negro, abajo a la izquierda (1 mm del borde), encima del dibujo
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = mm, bottom = mm)
                        .defaultMinSize(minWidth = mm * 6f, minHeight = mm * 5f)
                        .border(1.dp, borde)
                        .background(Color.White)
                        .padding(horizontal = mm * 0.8f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${vineta.numero}", fontSize = pt(10f), fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                }
            }
        }

        // INFORMACIONES: texto de las notas, encima la capa de notas y encima los carteles (toda la celda)
        Box(
            modifier = Modifier
                .width(mm * 50f)
                .fillMaxHeight()
                .rayaDerecha(raya, borde)
        ) {
            if (capaAnotacionesVisible) {
                RecuadroCapa(
                    clave = "${vineta.id}-N",
                    capa = dibujo.notasCapa,
                    esNotas = true,
                    franja = franja,
                    dibujante = dibujante,
                    editando = editarEnFila && isEditing && capaActiva == CapaEditada.NOTAS,
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
                    anchoLapiz = anchoLapiz,
                    onPedirTexto = { x, y -> onPedirTexto(CapaEditada.NOTAS, x, y) },
                    onEditarTexto = { i -> onEditarTexto(CapaEditada.NOTAS, i) },
                    // Punto 12: la app tiene un solo texto, que es la descripción
                    textoNotas = LetraNotas.textoCompleto(vineta.informacion.trim()),
                    notasLetra = dibujo.notasLetra,
                    modifier = Modifier.fillMaxSize()
                )
                // Coordenada explícita: chica, abajo a la izquierda, encima del texto (punto 12)
                if (vineta.mostrarWpt && !(vineta.latitud == 0.0 && vineta.longitud == 0.0)) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.5f, %.5f", vineta.latitud, vineta.longitud),
                        fontSize = pt(7f),
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 4.dp, bottom = if (franja != null) mm * 10.5f else 2.dp)
                            .background(Color.White.copy(alpha = 0.8f))
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }

        // REGRESIVA (y abajo, el acceso a los datos de la viñeta)
        Box(
            modifier = Modifier
                .width(mm * 25f)
                .fillMaxHeight()
                .rayaDerecha(raya, borde)
        ) {
            if (capaRegresivaVisible && !oculta) {
                Text(
                    text = prefs.km(distanciaRegresiva),
                    fontSize = pt(13f),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = mm * 3f)
                )
            }
            Surface(
                onClick = onTocarAnotaciones,
                shape = RoundedCornerShape(6.dp),
                color = FredianiCyanBg,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Icon(Icons.Default.EditNote, contentDescription = "Datos de la viñeta", tint = FredianiCyanText, modifier = Modifier.size(14.dp))
                    Text("DATOS", fontSize = 8.sp, fontWeight = FontWeight.Black, color = FredianiCyanText)
                }
            }
        }
    }
}

/**
 * Celular: la viñeta que se edita, en grande, con las pestañas DIBUJO / NOTAS.
 * El recuadro mantiene la proporción de la hoja (100 × 90).
 */
@Composable
private fun EditorGrandeCelular(
    vineta: VinetaEntity,
    capaActiva: CapaEditada,
    onCapa: (CapaEditada) -> Unit,
    onCerrar: () -> Unit,
    isDark: Boolean,
    recuadro: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) Color(0xFF131B2A) else Color(0xFFF1F5F9))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#${vineta.numero}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (isDark) Color.White else Color(0xFF0F172A))
            Spacer(Modifier.width(10.dp))
            listOf(CapaEditada.DIBUJO to "DIBUJO", CapaEditada.NOTAS to "NOTAS").forEach { (c, nombre) ->
                val sel = c == capaActiva
                Text(
                    nombre, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    color = if (sel) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) Color(0xFF0284C7) else Color.Transparent)
                        .border(1.dp, if (sel) Color(0xFF0284C7) else Color(0xFF94A3B8), RoundedCornerShape(10.dp))
                        .clickable { onCapa(c) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onCerrar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Terminar de editar", tint = Color(0xFF94A3B8))
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .aspectRatio(100f / 90f)
                .align(Alignment.CenterHorizontally)
                .border(1.dp, Color(0xFF0F172A))
        ) { recuadro() }
    }
}

/**
 * Un recuadro de la viñeta (dibujo o capa de notas). Si es el que se está editando muestra el
 * editor; si no, solo lo dibuja. También recibe los símbolos que se sueltan arrastrando.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecuadroCapa(
    fondo: Color = Color.White,
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
    notasLetra: Double? = null,
    anchoLapiz: Double = ANCHOS_LAPIZ[1],
    onPedirTexto: (Double, Double) -> Unit = { _, _ -> },
    onEditarTexto: (Int) -> Unit = {},
    palito: Boolean = false
) {
    val onSoltarAct by rememberUpdatedState(onSoltar)
    DisposableEffect(clave) { onDispose { arrastre.quitar(clave) } }
    val resaltado = arrastre.item != null && arrastre.destinoBajoDedo == clave
    val forma = androidx.compose.ui.graphics.RectangleShape
    Box(
        modifier = modifier
            .clip(forma)
            .background(fondo)
            .onGloballyPositioned { c ->
                val origen = c.localToWindow(Offset.Zero)
                val completo = androidx.compose.ui.geometry.Rect(origen, Size(c.size.width.toFloat(), c.size.height.toFloat()))
                arrastre.registrar(clave, c.boundsInWindow(), completo) { item, x, y -> onSoltarAct(item, x, y) }
            }
            .border(
                // La raya de la celda la pone la fila; acá solo se marca el recuadro que se edita
                width = if (editando || resaltado) 2.dp else 0.dp,
                color = when {
                    resaltado -> Color(0xFFF59E0B)
                    editando -> FredianiCyan
                    else -> Color.Transparent
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
                notasLetra = notasLetra,
                anchoLapiz = anchoLapiz,
                onPedirTexto = onPedirTexto,
                onEditarTexto = onEditarTexto,
                palito = palito
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
                notasLetra = notasLetra,
                palito = palito
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

private val AZUL_PLATAFORMA = Color(0xFF0284C7)
private val CELESTE_PLATAFORMA = Color(0xFF00B4D8)

/**
 * BARRAS DE EDICIÓN, copiadas de la plataforma (RoadbookWorkstation):
 *
 * 1. Dock: [+ Indicación] [Duplicar] [Eliminar] … [Datos del tramo] [Paleta] [⚙] [Guardado]
 * 2. Dibujo (se aplica a la viñeta seleccionada):
 *    MOVER · FLECHA · LÁPIZ · GOMA · TEXTO · Deshacer · Borrar (Supr) · PROHIBIDO
 *    | A↓ Auto A↑ | colores | grosor | tamaño de las viñetas (− barra +)
 * La barra de dibujo baja de renglón cuando no entra (nunca hay que deslizarla).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BarrasEdicionPlataforma(
    hayVineta: Boolean,
    activeTool: ActiveDrawTool,
    onToolChange: (ActiveDrawTool) -> Unit,
    colorActual: String,
    letraNotasAuto: Boolean,
    letraNotasValor: Double?,
    onLetraNotas: (Double?) -> Unit,
    onColorChange: (String) -> Unit,
    anchoLapiz: Double,
    onAnchoLapiz: (Double) -> Unit,
    hayFlechaElegida: Boolean,
    onAgregarProhibido: () -> Unit,
    onUndo: () -> Unit,
    onBorrar: () -> Unit,
    zoom: Float,
    onZoom: (Float) -> Unit,
    onNuevaIndicacion: () -> Unit,
    onDuplicar: () -> Unit,
    onEliminar: () -> Unit,
    cantidadParaBorrar: Int,
    onGuardar: () -> Unit,
    paletaVisible: Boolean,
    onPaleta: () -> Unit,
    onDatosTramo: () -> Unit,
    onPreferencias: () -> Unit,
    /** Celular: el dock se puede deslizar y algunos botones quedan solo con el ícono. */
    compacto: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkTheme
    val fondo = if (isDark) Color(0xFF131B2A) else Color.White
    val borde = if (isDark) Color(0xFF2A374D) else Color(0xFFCBD5E1)
    val texto = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val fondoBoton = if (isDark) Color(0xFF1C273A) else Color(0xFFF1F5F9)

    @Composable
    fun BotonIcono(icono: androidx.compose.ui.graphics.vector.ImageVector, ayuda: String, color: Color = texto,
                   fondoB: Color = fondoBoton, bordeB: Color = borde, habilitado: Boolean = true, onClick: () -> Unit) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .alpha(if (habilitado) 1f else 0.4f)
                .clip(RoundedCornerShape(8.dp))
                .background(fondoB)
                .border(1.dp, bordeB, RoundedCornerShape(8.dp))
                .clickable(enabled = habilitado) { onClick() }
        ) { Icon(icono, contentDescription = ayuda, tint = color, modifier = Modifier.size(15.dp)) }
    }

    @Composable
    fun BotonHerramienta(icono: androidx.compose.ui.graphics.vector.ImageVector, nombre: String, activo: Boolean,
                         habilitado: Boolean = true, onClick: () -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(30.dp)
                .alpha(if (habilitado) 1f else 0.4f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (activo) AZUL_PLATAFORMA else Color.Transparent)
                .border(1.dp, if (activo) AZUL_PLATAFORMA else borde, RoundedCornerShape(8.dp))
                .clickable(enabled = habilitado) { onClick() }
                .padding(horizontal = 7.dp)
        ) {
            // Solo el ícono (se entiende mejor y deja lugar); el nombre queda para la ayuda de accesibilidad
            Icon(icono, contentDescription = nombre, tint = if (activo) Color.White else texto, modifier = Modifier.size(17.dp))
        }
    }

    // Letra de las notas, colores, grosor y tamaño de las viñetas (en el celular van en otro renglón)
    val restoBarra: @Composable RowScope.() -> Unit = {

            // Tamaño de la letra de las notas: A↓ · Auto · A↑
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .alpha(if (letraNotasValor != null) 1f else 0.4f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, borde, RoundedCornerShape(8.dp))
            ) {
                val v = letraNotasValor
                Text("A↓", fontSize = 11.sp, fontWeight = FontWeight.Black, color = texto,
                    modifier = Modifier.clickable(enabled = v != null) { v?.let { onLetraNotas(LetraNotas.menos(it)) } }.padding(horizontal = 8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 52.dp)
                        .background(if (v != null && letraNotasAuto) AZUL_PLATAFORMA else Color.Transparent)
                        .border(1.dp, borde)
                        .clickable(enabled = v != null) { onLetraNotas(null) }
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        if (v == null || letraNotasAuto) "Auto" else String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0").replace('.', ','),
                        fontSize = 11.sp, fontWeight = FontWeight.Black,
                        color = if (v != null && letraNotasAuto) Color.White else texto
                    )
                }
                Text("A↑", fontSize = 11.sp, fontWeight = FontWeight.Black, color = texto,
                    modifier = Modifier.clickable(enabled = v != null) { v?.let { onLetraNotas(LetraNotas.mas(it)) } }.padding(horizontal = 8.dp))
            }

            // Colores (para lo nuevo y para lo seleccionado)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, borde, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = texto, modifier = Modifier.size(14.dp))
                COLORES_PLATAFORMA.forEach { hex ->
                    val activo = colorActual.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(if (activo) 19.dp else 17.dp)
                            .then(if (activo) Modifier.border(2.dp, AZUL_PLATAFORMA, CircleShape).padding(2.dp) else Modifier)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .border(1.dp, if (hex == "#FFFFFF") Color(0xFF94A3B8) else Color.Black.copy(alpha = 0.2f), CircleShape)
                            .clickable { onColorChange(hex) }
                    )
                }
            }

            // Grosor del lápiz (y de la flecha seleccionada)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, borde, RoundedCornerShape(8.dp))
            ) {
                ANCHOS_LAPIZ.forEach { g ->
                    val sel = anchoLapiz == g
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(28.dp)
                            .background(if (sel) AZUL_PLATAFORMA else Color.Transparent)
                            .clickable { onAnchoLapiz(g) }
                    ) {
                        Box(
                            Modifier
                                .size(width = 16.dp, height = maxOf(1.5, g * 0.6).dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)))
                        )
                    }
                }
            }

            // Tamaño de las viñetas (en el celular la hoja ya ocupa todo el ancho)
            if (!compacto) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
            // Tamaño de las viñetas (juntos, para que no se separen de renglón)
            IconButton(onClick = { onZoom(zoom - 0.1f) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Viñetas más chicas", tint = texto, modifier = Modifier.size(18.dp))
            }
            Slider(
                value = zoom, onValueChange = onZoom, valueRange = 0.5f..1f, steps = 9,
                colors = SliderDefaults.colors(thumbColor = AZUL_PLATAFORMA, activeTrackColor = AZUL_PLATAFORMA),
                modifier = Modifier.width(96.dp)
            )
            IconButton(onClick = { onZoom(zoom + 0.1f) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Viñetas más grandes", tint = texto, modifier = Modifier.size(18.dp))
            }
            }
    }

    Column(modifier = modifier.fillMaxWidth().background(fondo)) {
        // ---------------- 1. DOCK ----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compacto) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compacto) Arrangement.spacedBy(6.dp) else Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CELESTE_PLATAFORMA)
                        .clickable { onNuevaIndicacion() }
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Insertar nueva indicación", tint = Color.Black, modifier = Modifier.size(16.dp))
                    if (!compacto) {
                        Spacer(Modifier.width(3.dp))
                        Text("Indicación", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    }
                }
                BotonIcono(Icons.Default.ContentCopy, "Duplicar indicación seleccionada", habilitado = hayVineta, onClick = onDuplicar)
                BotonIcono(Icons.Default.Delete, "Eliminar indicación seleccionada", color = Color(0xFFFB7185),
                    fondoB = Color(0x1AF43F5E), bordeB = Color(0x4DF43F5E), habilitado = hayVineta || cantidadParaBorrar > 0, onClick = onEliminar)
                if (cantidadParaBorrar > 1) {
                    Text("$cantidadParaBorrar marcadas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFB7185))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(fondoBoton)
                        .border(1.dp, borde, RoundedCornerShape(10.dp))
                        .clickable { onDatosTramo() }
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = "Datos del tramo", tint = if (isDark) Color.White else Color(0xFF1E293B), modifier = Modifier.size(14.dp))
                    if (!compacto) {
                        Spacer(Modifier.width(4.dp))
                        Text("Datos del tramo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color(0xFF1E293B))
                    }
                }
                BotonIcono(
                    if (paletaVisible) Icons.Default.ViewSidebar else Icons.Default.VerticalSplit,
                    if (paletaVisible) "Ocultar la paleta" else "Mostrar la paleta",
                    color = if (paletaVisible) CELESTE_PLATAFORMA else texto, onClick = onPaleta
                )
                BotonIcono(Icons.Default.Settings, "Preferencias", onClick = onPreferencias)
                // La tablet guarda cada cambio en el momento: siempre "Guardado" (tocarlo vuelve a guardar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C273A))
                        .border(1.dp, Color(0xFF2A374D), RoundedCornerShape(8.dp))
                        .clickable { onGuardar() }
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Guardar cambios del tramo", tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                    if (!compacto) {
                        Spacer(Modifier.width(4.dp))
                        Text("Guardado", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFCBD5E1))
                    }
                }
            }
        }
        HorizontalDivider(color = borde)

        // ---------------- 2. BARRA DE DIBUJO (en varios renglones si hace falta) ----------------
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            BotonHerramienta(Icons.Default.OpenWith, "Mover", activeTool == ActiveDrawTool.SELECT) { onToolChange(ActiveDrawTool.SELECT) }
            BotonHerramienta(Icons.Default.CallMade, "Flecha", activeTool == ActiveDrawTool.RULER) { onToolChange(ActiveDrawTool.RULER) }
            // Camino prohibido: palito tachado que sale de la flecha seleccionada
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(30.dp)
                    .alpha(if (hayFlechaElegida) 1f else 0.4f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, borde, RoundedCornerShape(8.dp))
                    .clickable(enabled = hayFlechaElegida) { onAgregarProhibido() }
                    .padding(horizontal = 8.dp)
            ) {
                Canvas(Modifier.size(17.dp)) {
                    val k = size.width / 16f
                    fun linea(a: Float, b: Float, c: Float, d: Float) =
                        drawLine(texto, Offset(a * k, b * k), Offset(c * k, d * k), strokeWidth = 1.6f * k, cap = StrokeCap.Round)
                    linea(3f, 14f, 13f, 2f); linea(7.5f, 6.5f, 10.5f, 9.5f); linea(9f, 5f, 12f, 8f)
                }
                Spacer(Modifier.width(4.dp))
                Text("PROHIBIDO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = texto, maxLines = 1, softWrap = false)
            }

            BotonHerramienta(Icons.Default.Edit, "Lápiz", activeTool == ActiveDrawTool.PEN) { onToolChange(ActiveDrawTool.PEN) }
            BotonHerramienta(Icons.Default.AutoFixOff, "Goma", activeTool == ActiveDrawTool.GOMA) { onToolChange(ActiveDrawTool.GOMA) }
            BotonHerramienta(Icons.Default.TextFields, "Texto", activeTool == ActiveDrawTool.TEXT) { onToolChange(ActiveDrawTool.TEXT) }
            BotonIcono(Icons.Default.Undo, "Deshacer en el dibujo seleccionado", color = Color(0xFFF59E0B), fondoB = Color.Transparent, habilitado = hayVineta, onClick = onUndo)
            BotonIcono(Icons.Default.Delete, "Borrar la flecha o el símbolo seleccionado (tecla Supr)", color = Color(0xFFF43F5E), fondoB = Color.Transparent, habilitado = hayVineta, onClick = onBorrar)
            restoBarra()
        }
        HorizontalDivider(color = borde)
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

/**
 * PREFERENCIAS — las mismas de la plataforma (solapa General): se guardan en esta tablet y se
 * aplican al instante en el editor.
 */
@Composable
fun DialogoPreferencias(onCerrar: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val p = preferenciasVista()
    @Composable
    fun Fila(titulo: String, detalle: String, on: Boolean, cambiar: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text(detalle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = on, onCheckedChange = cambiar)
        }
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("PREFERENCIAS", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Se guardan en esta tablet y se aplican al editor.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Fila(
                    "Palito del GPS en las flechas",
                    "El palito con la bolita que marca en qué punto de la flecha se tomó la coordenada. Apagado no se borra: si lo volvés a prender, aparece donde estaba.",
                    p.mostrarPalitoGps
                ) { Preferencias.cambiar(context, p.copy(mostrarPalitoGps = it)) }
                Fila(
                    "Ruta automática (estilo navegador)",
                    "En el mapa del editor, al tocar un punto se arma el camino desde la viñeta anterior hasta ahí y se agrega una viñeta, con su flecha, en cada giro. Los cruces donde se sigue derecho no los marca: esos van a mano. Funciona bien en rutas y calles; en huellas y caminos de campo puede no encontrar camino.",
                    p.rutaAutomatica
                ) { Preferencias.cambiar(context, p.copy(rutaAutomatica = it)) }
                Fila(
                    "Rumbo (CAP)",
                    "Muestra el rumbo de la viñeta cuando lo tiene cargado.",
                    p.mostrarRumboCap
                ) { Preferencias.cambiar(context, p.copy(mostrarRumboCap = it)) }
                Fila(
                    "Distancias con 3 decimales",
                    "En el editor de tramos: 0,000 km en lugar de 0,00 km.",
                    p.decimalesKm == 3
                ) { Preferencias.cambiar(context, p.copy(decimalesKm = if (it) 3 else 2)) }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar", fontWeight = FontWeight.Bold) } }
    )
}
