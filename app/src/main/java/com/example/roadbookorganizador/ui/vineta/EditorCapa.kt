package com.example.roadbookorganizador.ui.vineta

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.roadbookorganizador.ui.components.ActiveDrawTool
import com.example.roadbookorganizador.vineta.ALTO_DIBUJO
import com.example.roadbookorganizador.vineta.ALTO_NOTAS
import com.example.roadbookorganizador.vineta.Capa
import com.example.roadbookorganizador.vineta.Colocar
import com.example.roadbookorganizador.vineta.Goma
import com.example.roadbookorganizador.vineta.CuadroTexto
import com.example.roadbookorganizador.vineta.Flecha
import com.example.roadbookorganizador.vineta.FranjaCarteles
import com.example.roadbookorganizador.vineta.Geometria
import com.example.roadbookorganizador.vineta.Gps
import com.example.roadbookorganizador.vineta.Pu
import com.example.roadbookorganizador.vineta.Punto
import com.example.roadbookorganizador.vineta.Sello
import com.example.roadbookorganizador.vineta.Trazo
import kotlin.math.hypot
import kotlin.math.max

/** Qué elemento está seleccionado en el recuadro que se edita. */
data class SeleccionEditor(val sello: Int? = null, val flecha: Int? = null, val texto: Int? = null)

/** Grosores del lápiz de la plataforma: fino, medio y grueso (punto 8, Colores). */
val ANCHOS_LAPIZ = listOf(2.5, 4.0, 6.0)

/** Colores de la plataforma (punto 8, Colores). */
val COLORES_PLATAFORMA = listOf(
    "#000000", "#DC2626", "#EA580C", "#EAB308", "#16A34A", "#2563EB", "#7C3AED", "#6B7280", "#FFFFFF"
)

/** Largo del palito del GPS que se puede elegir, en unidades (punto 6). */
private const val GPS_LARGO_MIN = 4.0
private const val GPS_LARGO_MAX = 40.0

private val AZUL_UI = Color(0xFF0284C7)
private val ROJO_UI = Color(0xFFEF4444)
private val AMBAR_UI = Color(0xFFF59E0B)
private val CELESTE_UI = Color(0xFF00B4D8)

/**
 * Recuadro de solo lectura: dibujo o capa de notas (100 × 90 los dos) con la franja de
 * carteles. El tamaño lo da el modifier, que tiene que respetar la proporción del recuadro.
 * [palito] = preferencia "Palito del GPS en las flechas" (solo en el dibujo).
 */
@Composable
fun VistaCapa(
    capa: Capa,
    esNotas: Boolean,
    franja: FranjaCarteles?,
    dibujante: DibujanteVineta,
    modifier: Modifier = Modifier,
    textoNotas: String = "",
    notasLetra: Double? = null,
    palito: Boolean = false
) {
    Canvas(modifier) {
        drawIntoCanvas { c ->
            if (esNotas) dibujante.dibujarNotas(c.nativeCanvas, capa, franja, 0f, 0f, size.width, textoNotas, notasLetra)
            else dibujante.dibujarDibujo(c.nativeCanvas, capa, 0f, 0f, size.width, palito)
        }
    }
}

private enum class Toque {
    NADA, MOVER_SELLO, ESCALAR_SELLO, MOVER_FLECHA, ESCALAR_FLECHA, P0, P1, P2, P3,
    PALITO, PALITO_LARGO, PROH_BASE, PROH_PUNTA, TRAZO, MOVER_TEXTO, ESCALAR_TEXTO, GOMA
}

// ---------------------------------------------------------------- ayudas de geometría en píxeles

private fun Flecha.puntosPx(w: Float, h: Float) = listOf(
    Offset((x0 * w).toFloat(), (y0 * h).toFloat()), Offset((x1 * w).toFloat(), (y1 * h).toFloat()),
    Offset((x2 * w).toFloat(), (y2 * h).toFloat()), Offset((x3 * w).toFloat(), (y3 * h).toFloat())
)

private fun Flecha.conPunto(i: Int, x: Double, y: Double): Flecha = when (i) {
    0 -> copy(x0 = x, y0 = y)
    1 -> copy(x1 = x, y1 = y)
    2 -> copy(x2 = x, y2 = y)
    else -> copy(x3 = x, y3 = y)
}

private fun Flecha.desplazada(dx: Double, dy: Double) = copy(
    x0 = x0 + dx, y0 = y0 + dy, x1 = x1 + dx, y1 = y1 + dy,
    x2 = x2 + dx, y2 = y2 + dy, x3 = x3 + dx, y3 = y3 + dy
)

private fun Flecha.escalada(cx: Double, cy: Double, k: Double) = copy(
    x0 = cx + (x0 - cx) * k, y0 = cy + (y0 - cy) * k, x1 = cx + (x1 - cx) * k, y1 = cy + (y1 - cy) * k,
    x2 = cx + (x2 - cx) * k, y2 = cy + (y2 - cy) * k, x3 = cx + (x3 - cx) * k, y3 = cy + (y3 - cy) * k
)

private fun bezierPx(p: List<Offset>, t: Float): Offset {
    val u = 1 - t
    return p[0] * (u * u * u) + p[1] * (3 * u * u * t) + p[2] * (3 * u * t * t) + p[3] * (t * t * t)
}

private fun dist(a: Offset, b: Offset) = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

private data class Marco(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val esquinas get() = listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
    val borrar get() = Offset(right + 12f, top - 12f)
    fun contiene(p: Offset, margen: Float = 0f) = p.x in (left - margen)..(right + margen) && p.y in (top - margen)..(bottom + margen)
}

/**
 * Editor de un recuadro (dibujo o capa de notas) en el formato versión 3.
 * Coordenadas 0..1 sobre ancho y alto del recuadro; tamaños en unidades (100 = ancho).
 * El modifier tiene que dar la proporción del recuadro (100 : 90).
 *
 * Durante un arrastre solo cambia el estado local; [onCambio] se llama al soltar.
 */
@Composable
fun EditorCapa(
    capa: Capa,
    esNotas: Boolean,
    franja: FranjaCarteles?,
    dibujante: DibujanteVineta,
    seleccion: SeleccionEditor,
    herramienta: ActiveDrawTool,
    colorHex: String,
    asistente: Boolean,
    onCambio: (Capa) -> Unit,
    onSeleccion: (SeleccionEditor) -> Unit,
    onHerramienta: (ActiveDrawTool) -> Unit,
    modifier: Modifier = Modifier,
    textoNotas: String = "",
    notasLetra: Double? = null,
    /** Grosor del lápiz elegido (fino 2,5 / medio 4 / grueso 6); se dibuja × 0,58. */
    anchoLapiz: Double = ANCHOS_LAPIZ[1],
    /** Herramienta de texto: se tocó un lugar vacío (x, y de 0 a 1) para escribir un texto nuevo. */
    onPedirTexto: (Double, Double) -> Unit = { _, _ -> },
    /** Se tocó un cuadro de texto ya elegido: editar su texto. */
    onEditarTexto: (Int) -> Unit = {},
    /** Preferencia "Palito del GPS en las flechas" (por defecto apagada). */
    palito: Boolean = false
) {
    val alto = if (esNotas) ALTO_NOTAS else ALTO_DIBUJO
    // Lo nuevo (lápiz, flechas) usa el mismo factor en los dos recuadros: 29 / 50 (punto 8)
    val factor = Colocar.ESCALA
    val conPalito = palito && !esNotas

    var local by remember { mutableStateOf(capa) }
    var sel by remember { mutableStateOf(seleccion) }
    var toque by remember { mutableStateOf(Toque.NADA) }
    var trazoActual by remember { mutableStateOf<List<Punto>>(emptyList()) }
    var reglaDesde by remember { mutableStateOf<Punto?>(null) }
    var reglaHasta by remember { mutableStateOf<Punto?>(null) }

    var escalaInicial by remember { mutableFloatStateOf(1f) }
    var distInicial by remember { mutableFloatStateOf(1f) }
    var flechaInicial by remember { mutableStateOf<Flecha?>(null) }
    var centroInicial by remember { mutableStateOf(Offset.Zero) }
    var prohIdx by remember { mutableStateOf(-1) }
    var gomaEn by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(capa) { if (toque == Toque.NADA) local = capa }
    LaunchedEffect(seleccion) { sel = seleccion }

    val herramientaAct by rememberUpdatedState(herramienta)
    val colorAct by rememberUpdatedState(colorHex)
    val asistenteAct by rememberUpdatedState(asistente)
    val onCambioAct by rememberUpdatedState(onCambio)
    val onSeleccionAct by rememberUpdatedState(onSeleccion)
    val onHerramientaAct by rememberUpdatedState(onHerramienta)
    val anchoLapizAct by rememberUpdatedState(anchoLapiz)
    val onPedirTextoAct by rememberUpdatedState(onPedirTexto)
    val onEditarTextoAct by rememberUpdatedState(onEditarTexto)

    val densidad = LocalDensity.current
    val toqueMin = with(densidad) { 18.dp.toPx() }      // medio lado mínimo del marco de un símbolo
    val radioTirador = with(densidad) { 20.dp.toPx() }  // radio para agarrar un tirador

    fun seleccionar(s: SeleccionEditor) {
        sel = s
        onSeleccionAct(s)
    }

    fun publicar() = onCambioAct(local)

    fun marcoSello(s: Sello, w: Float, h: Float): Marco {
        val u = w / 100f
        val cx = (s.x * w).toFloat()
        val cy = (s.y * h).toFloat()
        val m = max((15.0 * s.scale).toFloat() * u, toqueMin)
        return Marco(cx - m, cy - m, cx + m, cy + m)
    }

    fun marcoFlecha(f: Flecha, w: Float, h: Float): Marco {
        val p = f.puntosPx(w, h)
        val pad = with(densidad) { 12.dp.toPx() }
        return Marco(p.minOf { it.x } - pad, p.minOf { it.y } - pad, p.maxOf { it.x } + pad, p.maxOf { it.y } + pad)
    }

    /** Palito completo (base y punta) en px si la flecha [idx] lo lleva. */
    fun palitoEntero(c: Capa, idx: Int, w: Float): Pair<Offset, Offset>? {
        if (!conPalito) return null
        val (i, g) = Geometria.flechaConGps(c.arrows) ?: return null
        if (i != idx) return null
        val f = c.arrows[i]
        val pal = Geometria.palito(Geometria.puntosEnUnidades(f, alto), f.width, g)
        val u = w / 100f
        return Offset(pal.desde.x.toFloat() * u, pal.desde.y.toFloat() * u) to Offset(pal.hasta.x.toFloat() * u, pal.hasta.y.toFloat() * u)
    }

    /** Base y punta de cada camino prohibido de la flecha, en px. */
    fun prohibidosPx(f: Flecha, w: Float): List<Pair<Offset, Offset>> {
        val u = w / 100f
        val p = Geometria.puntosEnUnidades(f, alto)
        return f.prohibidos.orEmpty().map { c ->
            val d = Geometria.prohibido(p, f.width, c)
            Offset(d.desde.x.toFloat() * u, d.desde.y.toFloat() * u) to Offset(d.hasta.x.toFloat() * u, d.hasta.y.toFloat() * u)
        }
    }

    /** Lugar del botón ✕ de un camino prohibido: un poco más allá de su punta. */
    fun cruzProhibido(base: Offset, punta: Offset): Offset {
        val dx = punta.x - base.x
        val dy = punta.y - base.y
        val l = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
        val extra = with(densidad) { 16.dp.toPx() }
        return Offset(punta.x + dx / l * extra, punta.y + dy / l * extra)
    }

    /** t más cercano de la flecha a un punto en unidades. */
    fun tMasCercano(p: List<Pu>, dedo: Pu, desde: Double, hasta: Double): Double {
        var mejorT = 0.5
        var mejorD = Double.MAX_VALUE
        var t = desde
        while (t <= hasta + 1e-9) {
            val q = Geometria.bezier(p, t)
            val d = hypot(dedo.x - q.x, dedo.y - q.y)
            if (d < mejorD) { mejorD = d; mejorT = t }
            t += 0.005
        }
        return mejorT
    }

    fun borrarConGoma(pos: Offset, w: Float, h: Float) {
        gomaEn = pos
        val nuevos = Goma.borrar(local.strokes, (pos.x / w).toDouble(), (pos.y / h).toDouble(), alto)
        if (nuevos != local.strokes) local = local.copy(strokes = nuevos)
    }

    fun tocaFlecha(f: Flecha, pos: Offset, w: Float, h: Float): Boolean {
        val p = f.puntosPx(w, h)
        for (step in 0..24) if (dist(bezierPx(p, step / 24f), pos) <= radioTirador) return true
        return false
    }

    fun marcoTexto(t: CuadroTexto, w: Float, h: Float): Marco {
        val u = w / 100f
        val (an, al) = dibujante.medidaCuadroTexto(t)
        val l = (t.x * w).toFloat()
        val tp = (t.y * h).toFloat()
        val pad = with(densidad) { 4.dp.toPx() }
        return Marco(l - pad, tp - pad, l + max(an.toFloat() * u, toqueMin) + pad, tp + max(al.toFloat() * u, toqueMin / 2f) + pad)
    }

    /** Busca qué hay debajo del dedo, de arriba hacia abajo: textos, símbolos y después flechas. */
    fun elementoEn(pos: Offset, w: Float, h: Float): SeleccionEditor? {
        for (i in local.texts.indices.reversed()) {
            if (marcoTexto(local.texts[i], w, h).contiene(pos)) return SeleccionEditor(texto = i)
        }
        for (i in local.stamps.indices.reversed()) {
            if (marcoSello(local.stamps[i], w, h).contiene(pos)) return SeleccionEditor(sello = i)
        }
        for (i in local.arrows.indices.reversed()) {
            if (tocaFlecha(local.arrows[i], pos, w, h)) return SeleccionEditor(flecha = i)
        }
        return null
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    if (w <= 0f || h <= 0f) return@detectTapGestures
                    if (herramientaAct == ActiveDrawTool.GOMA) {
                        val antes = local.strokes
                        borrarConGoma(pos, w, h)
                        gomaEn = null
                        if (local.strokes != antes) publicar()
                        return@detectTapGestures
                    }
                    // ✕ de un camino prohibido de la flecha elegida: lo saca
                    sel.flecha?.takeIf { it in local.arrows.indices }?.let { i ->
                        prohibidosPx(local.arrows[i], w).forEachIndexed { k, (b, pnt) ->
                            if (dist(cruzProhibido(b, pnt), pos) <= radioTirador * 0.8f) {
                                val f = local.arrows[i]
                                val resto = f.prohibidos.orEmpty().filterIndexed { j, _ -> j != k }
                                local = local.copy(arrows = local.arrows.toMutableList().also { it[i] = f.copy(prohibidos = resto) })
                                publicar(); return@detectTapGestures
                            }
                        }
                    }
                    // Botones de borrar del elemento seleccionado
                    sel.sello?.takeIf { it in local.stamps.indices }?.let { i ->
                        if (dist(marcoSello(local.stamps[i], w, h).borrar, pos) <= radioTirador) {
                            local = local.copy(stamps = local.stamps.filterIndexed { j, _ -> j != i })
                            seleccionar(SeleccionEditor()); publicar(); return@detectTapGestures
                        }
                    }
                    sel.flecha?.takeIf { it in local.arrows.indices }?.let { i ->
                        if (dist(marcoFlecha(local.arrows[i], w, h).borrar, pos) <= radioTirador) {
                            local = local.copy(arrows = local.arrows.filterIndexed { j, _ -> j != i })
                            seleccionar(SeleccionEditor()); publicar(); return@detectTapGestures
                        }
                    }
                    sel.texto?.takeIf { it in local.texts.indices }?.let { i ->
                        val m = marcoTexto(local.texts[i], w, h)
                        if (dist(m.borrar, pos) <= radioTirador) {
                            local = local.copy(texts = local.texts.filterIndexed { j, _ -> j != i })
                            seleccionar(SeleccionEditor()); publicar(); return@detectTapGestures
                        }
                        if (m.contiene(pos)) { onEditarTextoAct(i); return@detectTapGestures }
                    }
                    val e = elementoEn(pos, w, h)
                    if (e != null) {
                        seleccionar(e)
                        onHerramientaAct(ActiveDrawTool.SELECT)
                    } else if (herramientaAct == ActiveDrawTool.TEXT) {
                        onPedirTextoAct((pos.x / w).toDouble().coerceIn(0.0, 1.0), (pos.y / h).toDouble().coerceIn(0.0, 1.0))
                    } else if (herramientaAct == ActiveDrawTool.SELECT) {
                        seleccionar(SeleccionEditor())
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0f || h <= 0f) return@detectDragGestures

                        // Goma: borra lo dibujado con el lápiz por donde pasa
                        if (herramientaAct == ActiveDrawTool.GOMA) {
                            toque = Toque.GOMA
                            borrarConGoma(pos, w, h)
                            return@detectDragGestures
                        }

                        // 0. Cuadro de texto seleccionado: esquinas para agrandar o cuerpo para mover
                        sel.texto?.takeIf { it in local.texts.indices }?.let { i ->
                            val t = local.texts[i]
                            val m = marcoTexto(t, w, h)
                            if (m.esquinas.any { dist(it, pos) <= radioTirador }) {
                                escalaInicial = t.size.toFloat()
                                distInicial = dist(pos, Offset((t.x * w).toFloat(), (t.y * h).toFloat())).coerceAtLeast(10f)
                                toque = Toque.ESCALAR_TEXTO
                                return@detectDragGestures
                            }
                            if (m.contiene(pos, 8f)) { toque = Toque.MOVER_TEXTO; return@detectDragGestures }
                        }

                        // 1. Símbolo seleccionado: esquinas para escalar o cuerpo para mover
                        sel.sello?.takeIf { it in local.stamps.indices }?.let { i ->
                            val s = local.stamps[i]
                            val m = marcoSello(s, w, h)
                            if (m.esquinas.any { dist(it, pos) <= radioTirador }) {
                                escalaInicial = s.scale.toFloat()
                                distInicial = dist(pos, Offset((s.x * w).toFloat(), (s.y * h).toFloat())).coerceAtLeast(10f)
                                toque = Toque.ESCALAR_SELLO
                                return@detectDragGestures
                            }
                            if (m.contiene(pos, 8f)) { toque = Toque.MOVER_SELLO; return@detectDragGestures }
                        }

                        // 2. Flecha seleccionada: palito, puntos, esquinas o cuerpo
                        sel.flecha?.takeIf { it in local.arrows.indices }?.let { i ->
                            val f = local.arrows[i]
                            // Palito del GPS: la bolita lo alarga o acorta; el palito se corre por la flecha
                            palitoEntero(local, i, w)?.let { (b, pnt) ->
                                if (dist(pnt, pos) <= radioTirador) { toque = Toque.PALITO_LARGO; return@detectDragGestures }
                                val medio = Offset(b.x + (pnt.x - b.x) * 0.55f, b.y + (pnt.y - b.y) * 0.55f)
                                if (dist(medio, pos) <= radioTirador * 0.8f) { toque = Toque.PALITO; return@detectDragGestures }
                            }
                            // Caminos prohibidos: la base se corre por la flecha; la punta gira y alarga
                            prohibidosPx(f, w).forEachIndexed { k, (b, pnt) ->
                                if (dist(pnt, pos) <= radioTirador) { prohIdx = k; toque = Toque.PROH_PUNTA; return@detectDragGestures }
                                if (dist(b, pos) <= radioTirador * 0.8f) { prohIdx = k; toque = Toque.PROH_BASE; return@detectDragGestures }
                            }
                            val p = f.puntosPx(w, h)
                            val tiradores = listOf(Toque.P3, Toque.P0, Toque.P1, Toque.P2)
                            for ((k, t) in listOf(3, 0, 1, 2).zip(tiradores)) {
                                if (dist(p[k], pos) <= radioTirador) { toque = t; return@detectDragGestures }
                            }
                            val m = marcoFlecha(f, w, h)
                            if (m.esquinas.any { dist(it, pos) <= radioTirador }) {
                                flechaInicial = f
                                centroInicial = Offset((m.left + m.right) / 2f, (m.top + m.bottom) / 2f)
                                distInicial = dist(pos, centroInicial).coerceAtLeast(10f)
                                toque = Toque.ESCALAR_FLECHA
                                return@detectDragGestures
                            }
                            if (m.contiene(pos)) { toque = Toque.MOVER_FLECHA; return@detectDragGestures }
                        }

                        // 3. Cualquier otro símbolo o flecha: se selecciona y se mueve
                        val e = elementoEn(pos, w, h)
                        if (e != null) {
                            seleccionar(e)
                            onHerramientaAct(ActiveDrawTool.SELECT)
                            toque = when {
                                e.texto != null -> Toque.MOVER_TEXTO
                                e.sello != null -> Toque.MOVER_SELLO
                                else -> Toque.MOVER_FLECHA
                            }
                            return@detectDragGestures
                        }

                        // 4. Lugar vacío: pluma o regla
                        val pt = Punto((pos.x / w).toDouble().coerceIn(0.0, 1.0), (pos.y / h).toDouble().coerceIn(0.0, 1.0))
                        when (herramientaAct) {
                            ActiveDrawTool.PEN -> { trazoActual = listOf(pt); toque = Toque.TRAZO }
                            ActiveDrawTool.RULER -> { reglaDesde = pt; reglaHasta = pt; toque = Toque.TRAZO }
                            ActiveDrawTool.SELECT, ActiveDrawTool.TEXT, ActiveDrawTool.GOMA -> { seleccionar(SeleccionEditor()); toque = Toque.NADA }
                        }
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0f || h <= 0f) return@detectDragGestures
                        val pos = change.position
                        val nx = (pos.x / w).toDouble().coerceIn(0.0, 1.0)
                        val ny = (pos.y / h).toDouble().coerceIn(0.0, 1.0)
                        val si = sel.sello
                        val fi = sel.flecha
                        val ti = sel.texto
                        when (toque) {
                            Toque.MOVER_TEXTO -> if (ti != null && ti in local.texts.indices) {
                                val t = local.texts[ti]
                                local = local.copy(texts = local.texts.toMutableList().also {
                                    it[ti] = t.copy(x = (t.x + delta.x / w).coerceIn(0.0, 1.0), y = (t.y + delta.y / h).coerceIn(0.0, 1.0))
                                })
                            }
                            Toque.ESCALAR_TEXTO -> if (ti != null && ti in local.texts.indices) {
                                val t = local.texts[ti]
                                val d = dist(pos, Offset((t.x * w).toFloat(), (t.y * h).toFloat()))
                                val k = Math.round((escalaInicial * d / distInicial).toDouble().coerceIn(2.0, 40.0) * 2) / 2.0
                                local = local.copy(texts = local.texts.toMutableList().also { it[ti] = t.copy(size = k) })
                            }
                            Toque.MOVER_SELLO -> if (si != null && si in local.stamps.indices) {
                                val s = local.stamps[si]
                                local = local.copy(stamps = local.stamps.toMutableList().also {
                                    it[si] = s.copy(x = (s.x + delta.x / w).coerceIn(0.0, 1.0), y = (s.y + delta.y / h).coerceIn(0.0, 1.0))
                                })
                            }
                            Toque.ESCALAR_SELLO -> if (si != null && si in local.stamps.indices) {
                                val s = local.stamps[si]
                                val d = dist(pos, Offset((s.x * w).toFloat(), (s.y * h).toFloat()))
                                val k = (escalaInicial * d / distInicial).toDouble().coerceIn(0.4, 5.0)
                                local = local.copy(stamps = local.stamps.toMutableList().also { it[si] = s.copy(scale = k) })
                            }
                            Toque.MOVER_FLECHA -> if (fi != null && fi in local.arrows.indices) {
                                val f = local.arrows[fi]
                                local = local.copy(arrows = local.arrows.toMutableList().also {
                                    it[fi] = f.desplazada((delta.x / w).toDouble(), (delta.y / h).toDouble())
                                })
                            }
                            Toque.ESCALAR_FLECHA -> {
                                val f0 = flechaInicial
                                if (fi != null && fi in local.arrows.indices && f0 != null) {
                                    val k = (dist(pos, centroInicial) / distInicial).toDouble().coerceIn(0.15, 5.0)
                                    local = local.copy(arrows = local.arrows.toMutableList().also {
                                        it[fi] = f0.escalada((centroInicial.x / w).toDouble(), (centroInicial.y / h).toDouble(), k)
                                    })
                                }
                            }
                            Toque.P0, Toque.P1, Toque.P2, Toque.P3 -> if (fi != null && fi in local.arrows.indices) {
                                val k = when (toque) { Toque.P0 -> 0; Toque.P1 -> 1; Toque.P2 -> 2; else -> 3 }
                                local = local.copy(arrows = local.arrows.toMutableList().also { it[fi] = it[fi].conPunto(k, nx, ny) })
                            }
                            Toque.PALITO -> if (local.arrows.isNotEmpty()) {
                                // El palito del GPS se pega a la flecha más cercana (t y lado), con su largo (punto 6)
                                val u = w / 100.0
                                val dedo = Pu(pos.x / u, pos.y / u)
                                var mejor: Triple<Int, Double, Double>? = null
                                local.arrows.forEachIndexed { k, f ->
                                    val p = Geometria.puntosEnUnidades(f, alto)
                                    val t = tMasCercano(p, dedo, 0.0, 1.0)
                                    val q = Geometria.bezier(p, t)
                                    val d = hypot(dedo.x - q.x, dedo.y - q.y)
                                    if (mejor == null || d < mejor!!.third) mejor = Triple(k, t, d)
                                }
                                val (k, t, _) = mejor!!
                                val f = local.arrows[k]
                                val p = Geometria.puntosEnUnidades(f, alto)
                                val q = Geometria.bezier(p, t)
                                val tan = Geometria.derivada(p, t)
                                val lado = if (tan.x * (dedo.y - q.y) - tan.y * (dedo.x - q.x) >= 0) 1 else -1
                                val largo = local.arrows.firstOrNull { it.gps != null }?.gps?.largo
                                val anterior = local.arrows.firstOrNull { it.gps != null }?.gps
                                val nuevoGps = Gps(Math.round(t.coerceIn(0.02, 0.98) * 1000) / 1000.0, lado, largo, anterior?.extra ?: emptyMap())
                                local = local.copy(arrows = local.arrows.mapIndexed { j, a ->
                                    if (j == k) a.copy(gps = nuevoGps) else if (a.gps != null) a.copy(gps = null) else a
                                })
                                seleccionar(SeleccionEditor(flecha = k))
                            }
                            Toque.PALITO_LARGO -> if (local.arrows.isNotEmpty()) {
                                // Alargar o acortar el palito: se mide sobre su propia dirección
                                var k = local.arrows.indexOfFirst { it.gps != null }
                                val g = if (k >= 0) local.arrows[k].gps!! else Gps(0.5, 1)
                                if (k < 0) k = 0
                                val f = local.arrows[k]
                                val p = Geometria.puntosEnUnidades(f, alto)
                                val diez = Geometria.palito(p, f.width, g.copy(largo = 10.0))
                                val ux = (diez.hasta.x - diez.desde.x) / 10
                                val uy = (diez.hasta.y - diez.desde.y) / 10
                                val u = w / 100.0
                                val proy = (pos.x / u - diez.desde.x) * ux + (pos.y / u - diez.desde.y) * uy
                                val largo = Math.round(proy.coerceIn(GPS_LARGO_MIN, GPS_LARGO_MAX) * 10) / 10.0
                                local = local.copy(arrows = local.arrows.toMutableList().also { it[k] = f.copy(gps = g.copy(largo = largo)) })
                            }
                            Toque.PROH_BASE, Toque.PROH_PUNTA -> if (fi != null && fi in local.arrows.indices) {
                                val f = local.arrows[fi]
                                val c = f.prohibidos?.getOrNull(prohIdx)
                                if (c != null) {
                                    val p = Geometria.puntosEnUnidades(f, alto)
                                    val u = w / 100.0
                                    val dedo = Pu(pos.x / u, pos.y / u)
                                    val nuevo = if (toque == Toque.PROH_BASE) {
                                        c.copy(t = Math.round(tMasCercano(p, dedo, 0.0, 1.0) * 1000) / 1000.0)
                                    } else {
                                        val (ang, largo) = Geometria.anguloYLargo(p, c.t, dedo)
                                        c.copy(ang = ang, largo = largo.coerceIn(4.0, 80.0))
                                    }
                                    val lista = f.prohibidos!!.toMutableList().also { it[prohIdx] = nuevo }
                                    local = local.copy(arrows = local.arrows.toMutableList().also { it[fi] = f.copy(prohibidos = lista) })
                                }
                            }
                            Toque.GOMA -> borrarConGoma(pos, w, h)
                            Toque.TRAZO -> {
                                val pt = Punto(nx, ny)
                                if (herramientaAct == ActiveDrawTool.PEN) trazoActual = trazoActual + pt
                                else if (herramientaAct == ActiveDrawTool.RULER) reglaHasta = pt
                            }
                            Toque.NADA -> Unit
                        }
                    },
                    onDragEnd = {
                        if (toque == Toque.TRAZO) {
                            val pts = trazoActual
                            if (herramientaAct == ActiveDrawTool.PEN && pts.size >= 2) {
                                val a = pts.first()
                                val b = pts.last()
                                if (asistenteAct && pts.size >= 5 && hypot(b.x - a.x, b.y - a.y) >= 0.12) {
                                    // Asistente: el trazo se convierte en flecha
                                    val c1 = pts[pts.size / 3]
                                    val c2 = pts[(pts.size * 2) / 3]
                                    val nueva = Flecha(a.x, a.y, c1.x, c1.y, c2.x, c2.y, b.x, b.y, width = Colocar.ANCHO_FLECHA, color = colorAct)
                                    local = local.copy(arrows = local.arrows + nueva)
                                    seleccionar(SeleccionEditor(flecha = local.arrows.lastIndex))
                                    onHerramientaAct(ActiveDrawTool.SELECT)
                                } else {
                                    local = local.copy(strokes = local.strokes + Trazo(pts, colorAct, anchoLapizAct * factor))
                                }
                            } else if (herramientaAct == ActiveDrawTool.RULER) {
                                val d0 = reglaDesde
                                val d1 = reglaHasta
                                // Herramienta FLECHA (como la plataforma): de donde sale a donde apunta, recta
                                if (d0 != null && d1 != null && hypot(d1.x - d0.x, d1.y - d0.y) >= 0.04) {
                                    val nueva = Flecha(
                                        d0.x, d0.y,
                                        d0.x + (d1.x - d0.x) / 3, d0.y + (d1.y - d0.y) / 3,
                                        d0.x + (d1.x - d0.x) * 2 / 3, d0.y + (d1.y - d0.y) * 2 / 3,
                                        d1.x, d1.y,
                                        width = Colocar.ANCHO_FLECHA, color = colorAct
                                    )
                                    local = local.copy(arrows = local.arrows + nueva)
                                    seleccionar(SeleccionEditor(flecha = local.arrows.lastIndex))
                                }
                            }
                        }
                        val huboCambio = toque != Toque.NADA
                        trazoActual = emptyList(); reglaDesde = null; reglaHasta = null; gomaEn = null
                        toque = Toque.NADA
                        if (huboCambio) publicar()
                    },
                    onDragCancel = {
                        trazoActual = emptyList(); reglaDesde = null; reglaHasta = null; gomaEn = null
                        val huboCambio = toque != Toque.NADA && toque != Toque.TRAZO
                        toque = Toque.NADA
                        if (huboCambio) publicar()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawIntoCanvas { c ->
                if (esNotas) dibujante.dibujarNotas(c.nativeCanvas, local, franja, 0f, 0f, w, textoNotas, notasLetra)
                else dibujante.dibujarDibujo(c.nativeCanvas, local, 0f, 0f, w, conPalito)
            }
            val u = w / 100f
            val color = ColoresSvg.parse(colorHex)?.let { Color(it) } ?: Color.Black

            // Trazo y regla mientras se dibujan
            if (trazoActual.size >= 2) {
                val path = Path().apply {
                    moveTo((trazoActual[0].x * w).toFloat(), (trazoActual[0].y * h).toFloat())
                    for (i in 1 until trazoActual.size) lineTo((trazoActual[i].x * w).toFloat(), (trazoActual[i].y * h).toFloat())
                }
                drawPath(path, color, style = Stroke(width = (anchoLapiz * factor * u).toFloat(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val r0 = reglaDesde
            val r1 = reglaHasta
            if (r0 != null && r1 != null) {
                drawLine(color, Offset((r0.x * w).toFloat(), (r0.y * h).toFloat()), Offset((r1.x * w).toFloat(), (r1.y * h).toFloat()),
                    strokeWidth = (anchoLapiz * factor * u).toFloat(), cap = StrokeCap.Round)
            }

            // Marco del cuadro de texto seleccionado
            sel.texto?.takeIf { it in local.texts.indices }?.let { i ->
                dibujarMarco(marcoTexto(local.texts[i], w, h))
            }
            // Marco del símbolo seleccionado
            sel.sello?.takeIf { it in local.stamps.indices }?.let { i ->
                dibujarMarco(marcoSello(local.stamps[i], w, h))
            }
            // Marco, tiradores y palito de la flecha seleccionada
            sel.flecha?.takeIf { it in local.arrows.indices }?.let { i ->
                val f = local.arrows[i]
                dibujarMarco(marcoFlecha(f, w, h))
                val p = f.puntosPx(w, h)
                drawLine(AZUL_UI.copy(alpha = 0.6f), p[0], p[1], strokeWidth = 2f)
                drawLine(AZUL_UI.copy(alpha = 0.6f), p[3], p[2], strokeWidth = 2f)
                p.forEachIndexed { k, pt ->
                    val r = if (k == 0 || k == 3) 9f else 7f
                    drawCircle(Color.White, r, pt)
                    drawCircle(AZUL_UI, r, pt, style = Stroke(3f))
                }
                palitoEntero(local, i, w)?.let { (b, pnt) ->
                    // Bolita: alarga o acorta. Rombo en el medio: corre el palito por la flecha
                    val medio = Offset(b.x + (pnt.x - b.x) * 0.55f, b.y + (pnt.y - b.y) * 0.55f)
                    drawCircle(Color.White, 6f, medio)
                    drawCircle(AMBAR_UI, 6f, medio, style = Stroke(3f))
                    drawCircle(Color.White, 10f, pnt)
                    drawCircle(CELESTE_UI, 10f, pnt, style = Stroke(3.5f))
                }
                // Caminos prohibidos: base (ámbar), punta (celeste) y ✕ para sacarlo
                prohibidosPx(f, w).forEach { (b, pnt) ->
                    drawCircle(AMBAR_UI, 8f, b); drawCircle(Color.White, 8f, b, style = Stroke(1.5f))
                    drawCircle(CELESTE_UI, 8f, pnt); drawCircle(Color.White, 8f, pnt, style = Stroke(1.5f))
                    val x = cruzProhibido(b, pnt)
                    drawCircle(ROJO_UI, 9f, x)
                    drawLine(Color.White, x + Offset(-3.5f, -3.5f), x + Offset(3.5f, 3.5f), strokeWidth = 2f)
                    drawLine(Color.White, x + Offset(-3.5f, 3.5f), x + Offset(3.5f, -3.5f), strokeWidth = 2f)
                }
            }

            // Goma: círculo donde borra
            gomaEn?.let { g ->
                val r = (Goma.RADIO * u).toFloat()
                drawCircle(Color.White.copy(alpha = 0.5f), r, g)
                drawCircle(Color(0xFF0F172A), r, g, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)))
            }
        }
    }
}

private fun DrawScope.dibujarMarco(m: Marco) {
    drawRect(
        color = AZUL_UI,
        topLeft = Offset(m.left, m.top),
        size = Size(m.right - m.left, m.bottom - m.top),
        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f))
    )
    for (pt in m.esquinas) {
        drawCircle(Color.White, 8f, pt)
        drawCircle(AZUL_UI, 8f, pt, style = Stroke(3f))
        drawCircle(AZUL_UI, 3f, pt)
    }
    val b = m.borrar
    drawCircle(ROJO_UI, 11f, b)
    drawCircle(Color.White, 11f, b, style = Stroke(1.5f))
    drawLine(Color.White, b + Offset(-4.5f, -4.5f), b + Offset(4.5f, 4.5f), strokeWidth = 2.5f)
    drawLine(Color.White, b + Offset(-4.5f, 4.5f), b + Offset(4.5f, -4.5f), strokeWidth = 2.5f)
}
