package com.example.roadbookorganizador.ui.vineta

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.Base64
import androidx.core.graphics.PathParser
import com.example.roadbookorganizador.vineta.ALTO_DIBUJO
import com.example.roadbookorganizador.vineta.ALTO_NOTAS
import com.example.roadbookorganizador.vineta.Biblioteca
import com.example.roadbookorganizador.vineta.Capa
import com.example.roadbookorganizador.vineta.Carteles
import com.example.roadbookorganizador.vineta.CuadroTexto
import com.example.roadbookorganizador.vineta.Estilo
import com.example.roadbookorganizador.vineta.FranjaCarteles
import com.example.roadbookorganizador.vineta.Geometria
import com.example.roadbookorganizador.vineta.LetraNotas
import com.example.roadbookorganizador.vineta.OpSvg
import com.example.roadbookorganizador.vineta.Sello
import com.example.roadbookorganizador.vineta.Trazo
import com.example.roadbookorganizador.vineta.Flecha

/**
 * Dibuja una viñeta versión 1 en un Canvas de Android, sin WebView
 * (ESPECIFICACION_VINETA.md, puntos 3 a 8 y 10).
 *
 * Todo se dibuja en unidades: el Canvas se escala para que 100 unidades = ancho del recuadro,
 * igual en horizontal y vertical (el dibujo nunca se estira) y se recorta al recuadro.
 */
class DibujanteVineta(
    private val bib: Biblioteca,
    /** Íconos propios del rally ya decodificados, por id (type = "PROPIO:<id>"). */
    private val iconosPropios: Map<String, Bitmap> = emptyMap()
) {

    /**
     * Dibuja una capa en el rectángulo (left, top, anchoPx × anchoPx·alto/100).
     * @param alto alto del recuadro en unidades: 90 en los dos recuadros (formato v3).
     * @param conPalito true solo en el recuadro de dibujo y si la preferencia "Palito del GPS"
     *   está prendida (punto 6; por defecto apagada). Apagado no se dibuja, pero el gps no se toca.
     */
    fun dibujarCapa(canvas: Canvas, capa: Capa, left: Float, top: Float, anchoPx: Float, alto: Double, conPalito: Boolean) {
        if (anchoPx <= 0f) return
        val k = anchoPx / 100f
        canvas.save()
        canvas.translate(left, top)
        canvas.clipRect(0f, 0f, anchoPx, (anchoPx * alto / 100.0).toFloat())
        canvas.scale(k, k)

        // Orden de abajo hacia arriba: flechas, trazos, símbolos (punto 4)
        val conGps = if (conPalito) Geometria.flechaConGps(capa.arrows) else null
        capa.arrows.forEachIndexed { i, f ->
            dibujarFlecha(canvas, f, alto, if (conGps != null && conGps.first == i) conGps.second else null)
        }
        capa.strokes.forEach { dibujarTrazo(canvas, it, alto) }
        capa.stamps.forEach { dibujarSello(canvas, it, alto) }
        capa.texts.forEach { dibujarCuadroTexto(canvas, it, alto) }
        canvas.restore()
    }

    fun dibujarDibujo(canvas: Canvas, capa: Capa, left: Float, top: Float, anchoPx: Float, conPalito: Boolean = false) =
        dibujarCapa(canvas, capa, left, top, anchoPx, ALTO_DIBUJO, conPalito)

    /**
     * Celda de notas completa (puntos 1, 4, 10 y 12), de abajo hacia arriba: el texto de las
     * notas, la capa de notas y la franja de carteles.
     * @param texto descripción y notas ya unidas (ver LetraNotas.textoCompleto).
     */
    fun dibujarNotas(
        canvas: Canvas, capa: Capa, franja: FranjaCarteles?, left: Float, top: Float, anchoPx: Float,
        texto: String = "", notasLetra: Double? = null
    ) {
        if (texto.isNotEmpty()) dibujarTextoNotas(canvas, texto, notasLetra, franja != null, left, top, anchoPx)
        dibujarCapa(canvas, capa, left, top, anchoPx, ALTO_NOTAS, conPalito = false)
        if (franja != null) dibujarFranja(canvas, franja, left, top, anchoPx)
    }

    // ---------------------------------------------------------------- texto de las notas (punto 12)

    /** Roboto negrita, negro. Se mide con letra 100 y se escala (el ancho es proporcional). */
    private val pinturaTexto = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        color = 0xFF000000.toInt()
        textSize = 100f
    }

    private fun anchoConLetra(t: String, letra: Double): Double =
        synchronized(pinturaTexto) { pinturaTexto.textSize = 100f; pinturaTexto.measureText(t) } / 100.0 * letra

    private var cacheLetra: Triple<String, Double?, Boolean>? = null
    private var cacheLetraValor = 0.0

    /** Letra efectiva de las notas en unidades: notasLetra, o la automática para este texto. */
    fun letraNotas(texto: String, notasLetra: Double?, conCarteles: Boolean): Double {
        if (notasLetra != null) return notasLetra
        val clave = Triple(texto, notasLetra, conCarteles)
        if (clave == cacheLetra) return cacheLetraValor
        val l = LetraNotas.letra(texto, null, conCarteles, ::anchoConLetra)
        cacheLetra = clave
        cacheLetraValor = l
        return l
    }

    private fun dibujarTextoNotas(
        canvas: Canvas, texto: String, notasLetra: Double?, conCarteles: Boolean,
        left: Float, top: Float, anchoPx: Float
    ) {
        if (anchoPx <= 0f) return
        val letra = letraNotas(texto, notasLetra, conCarteles)
        val lineas = LetraNotas.lineas(texto, LetraNotas.anchoDisponible()) { anchoConLetra(it, letra) }
        val m = LetraNotas.MARGEN.toFloat()
        val k = anchoPx / 100f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(k, k)
        // Lo que no entra abajo se corta
        canvas.clipRect(m, m, m + LetraNotas.anchoDisponible().toFloat(), m + LetraNotas.altoDisponible(conCarteles).toFloat())
        synchronized(pinturaTexto) {
            pinturaTexto.textSize = letra.toFloat()
            val fm = pinturaTexto.fontMetrics
            val altoLinea = (letra * LetraNotas.INTERLINEADO).toFloat()
            // Cada línea centrada en su renglón de 1,2 × letra (como el interlineado de la web)
            val dentro = (altoLinea - (fm.descent - fm.ascent)) / 2f - fm.ascent
            lineas.forEachIndexed { i, linea ->
                if (linea.isNotEmpty()) canvas.drawText(linea, m, m + i * altoLinea + dentro, pinturaTexto)
            }
            pinturaTexto.textSize = 100f
        }
        canvas.restore()
    }

    // ---------------------------------------------------------------- flechas (puntos 5 y 6)

    private fun dibujarFlecha(canvas: Canvas, f: Flecha, alto: Double, gps: com.example.roadbookorganizador.vineta.Gps?) {
        val p = Geometria.puntosEnUnidades(f, alto)
        val w = f.width.toFloat()
        val color = ColoresSvg.parse(f.color) ?: 0xFF000000.toInt()

        val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND; this.color = color
        }

        // Caminos prohibidos (punto 6 bis): antes que la flecha, así la flecha queda encima
        f.prohibidos?.forEach { c ->
            val d = Geometria.prohibido(p, f.width, c)
            val pp = Paint(trazo).apply { strokeWidth = d.grosor.toFloat() }
            canvas.drawLine(d.desde.x.toFloat(), d.desde.y.toFloat(), d.hasta.x.toFloat(), d.hasta.y.toFloat(), pp)
            d.rayitas.forEach { r -> canvas.drawLine(r.desde.x.toFloat(), r.desde.y.toFloat(), r.hasta.x.toFloat(), r.hasta.y.toFloat(), pp) }
        }
        val curva = Path().apply {
            moveTo(p[0].x.toFloat(), p[0].y.toFloat())
            cubicTo(p[1].x.toFloat(), p[1].y.toFloat(), p[2].x.toFloat(), p[2].y.toFloat(), p[3].x.toFloat(), p[3].y.toFloat())
        }
        canvas.drawPath(curva, trazo)

        val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color }
        canvas.drawCircle(p[0].x.toFloat(), p[0].y.toFloat(), Geometria.radioEntrada(f.width).toFloat(), relleno)

        val t = Geometria.punta(p, f.width)
        canvas.drawPath(Path().apply {
            moveTo(t[0].x.toFloat(), t[0].y.toFloat())
            lineTo(t[1].x.toFloat(), t[1].y.toFloat())
            lineTo(t[2].x.toFloat(), t[2].y.toFloat())
            close()
        }, relleno)

        if (gps != null) {
            val pal = Geometria.palito(p, f.width, gps)
            val g = pal.grosor.toFloat()
            canvas.drawLine(
                pal.desde.x.toFloat(), pal.desde.y.toFloat(), pal.hasta.x.toFloat(), pal.hasta.y.toFloat(),
                Paint(trazo).apply { strokeWidth = g }
            )
            canvas.drawCircle(pal.hasta.x.toFloat(), pal.hasta.y.toFloat(), g, relleno)
        }
    }

    // ---------------------------------------------------------------- trazos (punto 7)

    private fun dibujarTrazo(canvas: Canvas, t: Trazo, alto: Double) {
        if (t.pts.size < 2) return
        val path = Path().apply {
            moveTo((t.pts[0].x * 100).toFloat(), (t.pts[0].y * alto).toFloat())
            for (i in 1 until t.pts.size) lineTo((t.pts[i].x * 100).toFloat(), (t.pts[i].y * alto).toFloat())
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = t.width.toFloat()
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            color = ColoresSvg.parse(t.color) ?: 0xFF333333.toInt()
        })
    }

    // ---------------------------------------------------------------- símbolos (punto 8)

    private fun dibujarSello(canvas: Canvas, s: Sello, alto: Double) {
        val tam = (30.0 * s.scale).toFloat()
        canvas.save()
        canvas.translate((s.x * 100).toFloat(), (s.y * alto).toFloat())
        canvas.rotate(s.rot.toFloat())
        when {
            s.type.startsWith("PROPIO:") -> {
                val bmp = iconosPropios[s.type.removePrefix("PROPIO:")]
                if (bmp != null) dibujarIcono(canvas, bmp, tam) else dibujarFaltante(canvas, tam)
            }
            else -> {
                val sim = bib.simbolo(s.type)
                if (sim == null) {
                    dibujarFaltante(canvas, tam) // también los "APP:<código>"
                } else {
                    val f = tam / 100f
                    canvas.scale(f, f)
                    canvas.translate(-50f, -50f)
                    DibujanteSvg.dibujar(canvas, sim.opsParaColor(s.color))
                }
            }
        }
        canvas.restore()
    }

    /** Ícono propio: dentro del cuadrado S × S, sin deformar (ajustado al lado más largo). */
    private fun dibujarIcono(canvas: Canvas, bmp: Bitmap, tam: Float) {
        val mayor = maxOf(bmp.width, bmp.height).toFloat().coerceAtLeast(1f)
        val w = tam * bmp.width / mayor
        val h = tam * bmp.height / mayor
        canvas.drawBitmap(bmp, null, RectF(-w / 2, -h / 2, w / 2, h / 2), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    /** Código desconocido: cuadrado punteado gris con un "?" (el símbolo se conserva). */
    private fun dibujarFaltante(canvas: Canvas, tam: Float) {
        val m = tam / 2
        val gris = 0xFF9CA3AF.toInt()
        canvas.drawRect(-m, -m, m, m, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = tam * 0.04f; color = gris
            pathEffect = DashPathEffect(floatArrayOf(tam * 0.1f, tam * 0.07f), 0f)
        })
        DibujanteSvg.textoCentrado(canvas, "?", 0f, 0f, tam * 0.6f, gris, 700, "sans-serif")
    }

    // ---------------------------------------------------------------- cuadros de texto (punto 8 bis)

    /** Roboto negrita. (x, y) arriba a la izquierda; base de la 1.ª línea a 0,8 × size; renglón 1,2 × size. */
    private fun dibujarCuadroTexto(canvas: Canvas, t: CuadroTexto, alto: Double) {
        if (t.text.isEmpty() || t.size <= 0.0) return
        val p = pinturaCuadro(t.size.toFloat(), ColoresSvg.parse(t.color) ?: 0xFF000000.toInt())
        val x = (t.x * 100).toFloat()
        val y0 = (t.y * alto + 0.8 * t.size).toFloat()
        t.text.replace("\r\n", "\n").split('\n').forEachIndexed { i, linea ->
            if (linea.isNotEmpty()) canvas.drawText(linea, x, y0 + (i * 1.2 * t.size).toFloat(), p)
        }
    }

    private fun pinturaCuadro(tam: Float, c: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            color = c
            textSize = tam
        }

    /** Ancho y alto de un cuadro de texto en unidades (para elegirlo y moverlo en el editor). */
    fun medidaCuadroTexto(t: CuadroTexto): Pair<Double, Double> {
        val lineas = t.text.replace("\r\n", "\n").split('\n')
        val p = pinturaCuadro(100f, 0xFF000000.toInt())
        val ancho = lineas.maxOfOrNull { p.measureText(it).toDouble() / 100.0 * t.size } ?: 0.0
        val alto = t.size * (0.8 + 0.2 + 1.2 * (lineas.size - 1)).coerceAtLeast(1.0)
        return ancho to alto
    }

    // ---------------------------------------------------------------- carteles (punto 10)

    private fun dibujarFranja(canvas: Canvas, fr: FranjaCarteles, left: Float, top: Float, anchoPx: Float) {
        val k = anchoPx / 100f
        canvas.save()
        canvas.translate(left, top)
        canvas.clipRect(0f, 0f, anchoPx, (anchoPx * ALTO_NOTAS / 100.0).toFloat())
        canvas.scale(k, k)
        val y0 = (ALTO_NOTAS - Carteles.ALTO_FRANJA).toFloat()
        val celda = 100f / 3f
        val fondo = if (fr.gris) ColoresSvg.parse(Carteles.GRIS_CAMBIO)!! else 0xFFFFFFFF.toInt()
        canvas.drawRect(0f, y0, 100f, ALTO_NOTAS.toFloat(), Paint().apply { style = Paint.Style.FILL; color = fondo })
        val linea = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.3f; color = 0xFF94A3B8.toInt() }
        canvas.drawLine(0f, y0, 100f, y0, linea)
        canvas.drawLine(celda, y0, celda, ALTO_NOTAS.toFloat(), linea)
        canvas.drawLine(2 * celda, y0, 2 * celda, ALTO_NOTAS.toFloat(), linea)

        val m = Carteles.MARGEN_CELDA.toFloat()
        val altoDisp = Carteles.ALTO_FRANJA.toFloat() - 2 * m
        fun encajar(i: Int, anchoDiseno: Float, altoDiseno: Float, dibujo: (Canvas) -> Unit) {
            val anchoDisp = celda - 2 * m
            val f = minOf(anchoDisp / anchoDiseno, altoDisp / altoDiseno)
            val cx = celda * i + celda / 2
            val cy = y0 + Carteles.ALTO_FRANJA.toFloat() / 2
            canvas.save()
            canvas.translate(cx - anchoDiseno * f / 2, cy - altoDiseno * f / 2)
            canvas.scale(f, f)
            dibujo(canvas)
            canvas.restore()
        }
        fr.tiempo?.let { v -> encajar(0, 140f, 100f) { c -> cartelTiempo(c, v) } }
        fr.promedio?.let { v -> encajar(1, 100f, 100f) { c -> cartelCirculo(c, v, 0xFF0EA5E9.toInt(), 8f, 47f, 40f, "PROM", 76f, 0xFF0284C7.toInt()) } }
        fr.maxima?.let { v -> encajar(2, 100f, 100f) { c -> cartelCirculo(c, v, 0xFFDC2626.toInt(), 10f, 45f, 38f, "MAX", 73f, 0xFFDC2626.toInt()) } }
        canvas.restore()
    }

    private val colorValor = 0xFF0F172A.toInt()

    private fun cartelTiempo(c: Canvas, valor: String) {
        val r = RectF(5f, 14f, 135f, 86f)
        c.drawRoundRect(r, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFFFFFFFF.toInt() })
        c.drawRoundRect(r, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; color = 0xFF16A34A.toInt() })
        DibujanteSvg.textoCentrado(c, valor, 70f, 47f, if (valor.length > 4) 32f else 38f, colorValor, 700, "sans-serif")
        DibujanteSvg.textoCentrado(c, "TIEMPO", 70f, 74f, 12f, 0xFF15803D.toInt(), 700, "sans-serif")
    }

    private fun cartelCirculo(c: Canvas, valor: String, borde: Int, grosor: Float, yValor: Float, tamValor: Float, rotulo: String, yRotulo: Float, colorRotulo: Int) {
        c.drawCircle(50f, 50f, 45f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xFFFFFFFF.toInt() })
        c.drawCircle(50f, 50f, 45f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = grosor; color = borde })
        val tam = (tamValor * Carteles.factorTamanoVelocidad(valor)).toFloat()
        DibujanteSvg.textoCentrado(c, valor, 50f, yValor, tam, colorValor, 700, "sans-serif")
        DibujanteSvg.textoCentrado(c, rotulo, 50f, yRotulo, 13f, colorRotulo, 700, "sans-serif")
    }

    companion object {
        /** Decodifica los íconos propios de un rally (lista iconosPropios, punto 8). */
        fun decodificarIconos(iconos: List<Triple<String, String, String>>): Map<String, Bitmap> =
            iconos.mapNotNull { (id, _, datosBase64) ->
                try {
                    val bytes = Base64.decode(datosBase64.substringAfter("base64,"), Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { id to it }
                } catch (e: Exception) {
                    null
                }
            }.toMap()
    }
}

/** Dibuja las operaciones de un símbolo de simbolos.json (viewBox 100 × 100). */
object DibujanteSvg {

    private val cacheTrayectos = HashMap<String, Path?>()

    fun dibujar(canvas: Canvas, ops: List<OpSvg>) {
        for (op in ops) {
            val e = op.estilo
            val capa = if (e.opacity < 1.0) canvas.saveLayerAlpha(null, (e.opacity * 255).toInt().coerceIn(0, 255)) else -1
            when (op) {
                is OpSvg.Linea -> trazo(e)?.let { canvas.drawLine(op.x1.f, op.y1.f, op.x2.f, op.y2.f, it) }
                is OpSvg.Circulo -> pintar(e) { canvas.drawCircle(op.cx.f, op.cy.f, op.r.f, it) }
                is OpSvg.Elipse -> pintar(e) { canvas.drawOval(RectF((op.cx - op.rx).f, (op.cy - op.ry).f, (op.cx + op.rx).f, (op.cy + op.ry).f), it) }
                is OpSvg.Rect -> pintar(e) {
                    val r = RectF(op.x.f, op.y.f, (op.x + op.w).f, (op.y + op.h).f)
                    if (op.rx > 0 || op.ry > 0) canvas.drawRoundRect(r, op.rx.f, op.ry.f, it) else canvas.drawRect(r, it)
                }
                is OpSvg.Poligono -> if (op.puntos.size >= 2) {
                    val path = Path().apply {
                        moveTo(op.puntos[0].first.f, op.puntos[0].second.f)
                        for (i in 1 until op.puntos.size) lineTo(op.puntos[i].first.f, op.puntos[i].second.f)
                        close()
                    }
                    pintar(e) { canvas.drawPath(path, it) }
                }
                is OpSvg.Trayecto -> trayecto(op.d)?.let { path -> pintar(e) { canvas.drawPath(path, it) } }
                is OpSvg.Texto -> {
                    val color = e.fill?.let { ColoresSvg.parse(it) }
                    if (color != null) texto(canvas, op, color)
                }
            }
            if (capa >= 0) canvas.restoreToCount(capa)
        }
    }

    private val Double.f get() = this.toFloat()

    private fun trayecto(d: String): Path? = synchronized(cacheTrayectos) {
        cacheTrayectos.getOrPut(d) {
            try { PathParser.createPathFromPathData(d) } catch (ex: Exception) { null }
        }
    }

    /** Relleno primero, borde encima (orden de SVG). */
    private inline fun pintar(e: Estilo, dibujo: (Paint) -> Unit) {
        e.fill?.let { ColoresSvg.parse(it) }?.let { c ->
            dibujo(Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL; color = c
                if (e.fillOpacity < 1.0) alpha = (android.graphics.Color.alpha(c) * e.fillOpacity).toInt().coerceIn(0, 255)
            })
        }
        trazo(e)?.let(dibujo)
    }

    private fun trazo(e: Estilo): Paint? {
        val c = e.stroke?.let { ColoresSvg.parse(it) } ?: return null
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = c
            strokeWidth = e.strokeWidth.toFloat()
            strokeCap = when (e.linecap) { "round" -> Paint.Cap.ROUND; "square" -> Paint.Cap.SQUARE; else -> Paint.Cap.BUTT }
            strokeJoin = when (e.linejoin) { "round" -> Paint.Join.ROUND; "bevel" -> Paint.Join.BEVEL; else -> Paint.Join.MITER }
            strokeMiter = 4f // valor por defecto de SVG
            e.dash?.let { d ->
                val arr = if (d.size % 2 == 1) d + d else d // SVG repite la lista si es impar
                pathEffect = DashPathEffect(arr.map { it.toFloat() }.toFloatArray(), 0f)
            }
        }
    }

    private fun texto(canvas: Canvas, t: OpSvg.Texto, color: Int) {
        val p = paintTexto(t.fontSize.toFloat(), color, t.fontWeight, t.fontFamily)
        p.textAlign = when (t.anchor) { "middle" -> Paint.Align.CENTER; "end" -> Paint.Align.RIGHT; else -> Paint.Align.LEFT }
        canvas.drawText(t.texto, t.x.f, t.y.f, p) // en los símbolos la y es la línea de base (punto 8)
    }

    /** Texto centrado en x e y (la y es el centro del texto): se usa en los carteles. */
    fun textoCentrado(canvas: Canvas, texto: String, x: Float, y: Float, tam: Float, color: Int, peso: Int, familia: String) {
        val p = paintTexto(tam, color, peso, familia).apply { textAlign = Paint.Align.CENTER }
        val fm = p.fontMetrics
        canvas.drawText(texto, x, y - (fm.ascent + fm.descent) / 2f, p)
    }

    private val tipografias = HashMap<String, Typeface>()

    private fun paintTexto(tam: Float, color: Int, peso: Int, familia: String) =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
            this.color = color
            textSize = tam
            typeface = tipografia(familia, peso)
        }

    private fun tipografia(familia: String, peso: Int): Typeface = synchronized(tipografias) {
        tipografias.getOrPut("$familia-$peso") {
            val base = Typeface.create(if (familia == "monospace") "monospace" else "sans-serif", Typeface.NORMAL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Typeface.create(base, peso.coerceIn(1, 1000), false)
            else Typeface.create(base, if (peso >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }
}

/** Colores de SVG: #rgb, #rrggbb, #aarrggbb y algunos nombres. */
object ColoresSvg {
    fun parse(s: String?): Int? {
        if (s == null) return null
        val v = s.trim()
        return try {
            when {
                v.startsWith("#") && v.length == 4 -> {
                    val r = v[1].toString().repeat(2); val g = v[2].toString().repeat(2); val b = v[3].toString().repeat(2)
                    android.graphics.Color.parseColor("#$r$g$b")
                }
                else -> android.graphics.Color.parseColor(v)
            }
        } catch (e: Exception) {
            null
        }
    }
}

/** Carga la biblioteca de la especificación desde los assets de la app (una sola vez). */
object BibliotecaVineta {
    @Volatile private var instancia: Biblioteca? = null

    fun obtener(context: Context): Biblioteca = instancia ?: synchronized(this) {
        instancia ?: run {
            val a = context.applicationContext.assets
            fun leer(nombre: String) = a.open("especificacion/$nombre").bufferedReader(Charsets.UTF_8).use { it.readText() }
            Biblioteca.desdeJson(leer("simbolos.json"), leer("flechas_armadas.json"), leer("equivalencias_app.json"))
                .also { instancia = it }
        }
    }
}
