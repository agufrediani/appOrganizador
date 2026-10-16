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
data class SeleccionEditor(val sello: Int? = null, val flecha: Int? = null)

/** Grosores por defecto, en unidades (punto 5 y 7). */
const val ANCHO_FLECHA = 6.0
const val ANCHO_TRAZO = 3.5

/** Escala de un símbolo nuevo (punto 8). */
const val ESCALA_NUEVA = 1.3

private val AZUL_UI = Color(0xFF0284C7)
private val ROJO_UI = Color(0xFFEF4444)
private val AMBAR_UI = Color(0xFFF59E0B)

/**
 * Recuadro de solo lectura: dibujo (100 × 100) o capa de notas (100 × 64) con la franja de
 * carteles. El tamaño lo da el modifier, que tiene que respetar la proporción del recuadro.
 */
@Composable
fun VistaCapa(
    capa: Capa,
    esNotas: Boolean,
    franja: FranjaCarteles?,
    dibujante: DibujanteVineta,
    modifier: Modifier = Modifier,
    textoNotas: String = "",
    notasLetra: Double? = null
) {
    Canvas(modifier) {
        drawIntoCanvas { c ->
            if (esNotas) dibujante.dibujarNotas(c.nativeCanvas, capa, franja, 0f, 0f, size.width, textoNotas, notasLetra)
            else dibujante.dibujarDibujo(c.nativeCanvas, capa, 0f, 0f, size.width)
        }
    }
}

private enum class Toque { NADA, MOVER_SELLO, ESCALAR_SELLO, MOVER_FLECHA, ESCALAR_FLECHA, P0, P1, P2, P3, PALITO, TRAZO }

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
 * Editor de un recuadro (dibujo o capa de notas) en el formato versión 1.
 * Coordenadas 0..1 sobre ancho y alto del recuadro; tamaños en unidades (100 = ancho).
 * El modifier tiene que dar la proporción del recuadro (1 : 1 o 100 : 64).
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
    notasLetra: Double? = null
) {
    val alto = if (esNotas) ALTO_NOTAS else ALTO_DIBUJO
    val conPalito = !esNotas

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

    LaunchedEffect(capa) { if (toque == Toque.NADA) local = capa }
    LaunchedEffect(seleccion) { sel = seleccion }

    val herramientaAct by rememberUpdatedState(herramienta)
    val colorAct by rememberUpdatedState(colorHex)
    val asistenteAct by rememberUpdatedState(asistente)
    val onCambioAct by rememberUpdatedState(onCambio)
    val onSeleccionAct by rememberUpdatedState(onSeleccion)
    val onHerramientaAct by rememberUpdatedState(onHerramienta)

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

    /** Punta del palito en px si la flecha [idx] es la que lo lleva. */
    fun palitoPx(c: Capa, idx: Int, w: Float): Offset? {
        if (!conPalito) return null
        val (i, g) = Geometria.flechaConGps(c.arrows) ?: return null
        if (i != idx) return null
        val f = c.arrows[i]
        val pal = Geometria.palito(Geometria.puntosEnUnidades(f, alto), f.width, g)
        val u = w / 100f
        return Offset(pal.hasta.x.toFloat() * u, pal.hasta.y.toFloat() * u)
    }

    fun tocaFlecha(f: Flecha, pos: Offset, w: Float, h: Float): Boolean {
        val p = f.puntosPx(w, h)
        for (step in 0..24) if (dist(bezierPx(p, step / 24f), pos) <= radioTirador) return true
        return false
    }

    /** Busca qué hay debajo del dedo, de arriba hacia abajo: símbolos y después flechas. */
    fun elementoEn(pos: Offset, w: Float, h: Float): SeleccionEditor? {
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
                    val e = elementoEn(pos, w, h)
                    if (e != null) {
                        seleccionar(e)
                        onHerramientaAct(ActiveDrawTool.SELECT)
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
                            palitoPx(local, i, w)?.let { pp ->
                                if (dist(pp, pos) <= radioTirador) { toque = Toque.PALITO; return@detectDragGestures }
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
                            toque = if (e.sello != null) Toque.MOVER_SELLO else Toque.MOVER_FLECHA
                            return@detectDragGestures
                        }

                        // 4. Lugar vacío: pluma o regla
                        val pt = Punto((pos.x / w).toDouble().coerceIn(0.0, 1.0), (pos.y / h).toDouble().coerceIn(0.0, 1.0))
                        when (herramientaAct) {
                            ActiveDrawTool.PEN -> { trazoActual = listOf(pt); toque = Toque.TRAZO }
                            ActiveDrawTool.RULER -> { reglaDesde = pt; reglaHasta = pt; toque = Toque.TRAZO }
                            ActiveDrawTool.SELECT -> { seleccionar(SeleccionEditor()); toque = Toque.NADA }
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
                        when (toque) {
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
                            Toque.PALITO -> if (fi != null && fi in local.arrows.indices) {
                                // El palito se mueve a lo largo de la flecha (t) y cambia de lado (punto 6)
                                val f = local.arrows[fi]
                                val p = Geometria.puntosEnUnidades(f, alto)
                                val u = w / 100.0
                                val dedo = Pu(pos.x / u, pos.y / u)
                                var mejorT = 0.5
                                var mejorD = Double.MAX_VALUE
                                var t = 0.02
                                while (t <= 0.98 + 1e-9) {
                                    val q = Geometria.bezier(p, t)
                                    val d = hypot(dedo.x - q.x, dedo.y - q.y)
                                    if (d < mejorD) { mejorD = d; mejorT = t }
                                    t += 0.01
                                }
                                val q = Geometria.bezier(p, mejorT)
                                val tan = Geometria.derivada(p, mejorT)
                                val lado = if ((dedo.x - q.x) * (-tan.y) + (dedo.y - q.y) * tan.x >= 0) 1 else -1
                                val tRed = Math.round(mejorT * 100.0) / 100.0
                                local = local.copy(arrows = local.arrows.toMutableList().also { it[fi] = f.copy(gps = Gps(tRed, lado)) })
                            }
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
                                    val nueva = Flecha(a.x, a.y, c1.x, c1.y, c2.x, c2.y, b.x, b.y, width = ANCHO_FLECHA, color = colorAct)
                                    local = local.copy(arrows = local.arrows + nueva)
                                    seleccionar(SeleccionEditor(flecha = local.arrows.lastIndex))
                                    onHerramientaAct(ActiveDrawTool.SELECT)
                                } else {
                                    local = local.copy(strokes = local.strokes + Trazo(pts, colorAct, ANCHO_TRAZO))
                                }
                            } else if (herramientaAct == ActiveDrawTool.RULER) {
                                val d0 = reglaDesde
                                val d1 = reglaHasta
                                if (d0 != null && d1 != null && (d0.x != d1.x || d0.y != d1.y)) {
                                    local = local.copy(strokes = local.strokes + Trazo(listOf(d0, d1), colorAct, ANCHO_TRAZO))
                                }
                            }
                        }
                        val huboCambio = toque != Toque.NADA
                        trazoActual = emptyList(); reglaDesde = null; reglaHasta = null
                        toque = Toque.NADA
                        if (huboCambio) publicar()
                    },
                    onDragCancel = {
                        trazoActual = emptyList(); reglaDesde = null; reglaHasta = null
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
                else dibujante.dibujarDibujo(c.nativeCanvas, local, 0f, 0f, w)
            }
            val u = w / 100f
            val color = ColoresSvg.parse(colorHex)?.let { Color(it) } ?: Color.Black

            // Trazo y regla mientras se dibujan
            if (trazoActual.size >= 2) {
                val path = Path().apply {
                    moveTo((trazoActual[0].x * w).toFloat(), (trazoActual[0].y * h).toFloat())
                    for (i in 1 until trazoActual.size) lineTo((trazoActual[i].x * w).toFloat(), (trazoActual[i].y * h).toFloat())
                }
                drawPath(path, color, style = Stroke(width = (ANCHO_TRAZO * u).toFloat(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            val r0 = reglaDesde
            val r1 = reglaHasta
            if (r0 != null && r1 != null) {
                drawLine(color, Offset((r0.x * w).toFloat(), (r0.y * h).toFloat()), Offset((r1.x * w).toFloat(), (r1.y * h).toFloat()),
                    strokeWidth = (ANCHO_TRAZO * u).toFloat(), cap = StrokeCap.Round)
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
                palitoPx(local, i, w)?.let { pp ->
                    drawCircle(Color.White, 10f, pp)
                    drawCircle(AMBAR_UI, 10f, pp, style = Stroke(3.5f))
                }
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
