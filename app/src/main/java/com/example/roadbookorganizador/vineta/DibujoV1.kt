package com.example.roadbookorganizador.vineta

/**
 * Dibujo de una viñeta, formato versión 3 (ESPECIFICACION_VINETA.md, puntos 2 a 9).
 * Coordenadas x, y de 0 a 1 sobre el ancho y el alto del recuadro. Grosores y tamaños en
 * unidades (100 unidades = ancho del recuadro). Los dos recuadros miden 100 × 90.
 *
 * Nunca se pierde un dato al guardar: todo campo que este modelo no conoce (por ejemplo
 * "id") se guarda en `extra` y se vuelve a escribir tal cual.
 *
 * (La clase se sigue llamando DibujoV1 por compatibilidad con el resto del código.)
 */
const val VERSION_FORMATO = 3

/** Alto H de cada recuadro en unidades (el ancho siempre es 100): 50 × 45 mm. */
const val ALTO_DIBUJO = 90.0
const val ALTO_NOTAS = 90.0

/** Medidas de los formatos viejos (punto 9): dibujo cuadrado y notas de 100 × 64. */
const val ALTO_DIBUJO_V1 = 100.0
const val ALTO_NOTAS_V1 = 64.0

/** Palito del GPS (punto 6). largo = unidades (4 a 40); null = largo por defecto. */
data class Gps(
    val t: Double = 0.5,
    val lado: Int = 1,
    val largo: Double? = null,
    val extra: Map<String, Any?> = emptyMap()
)

/** Camino prohibido (punto 6 bis): t sobre la flecha, ang en grados (+ derecha), largo en unidades. */
data class Prohibido(
    val t: Double,
    val ang: Double,
    val largo: Double,
    val extra: Map<String, Any?> = emptyMap()
)

data class Flecha(
    val x0: Double, val y0: Double,
    val x1: Double, val y1: Double,
    val x2: Double, val y2: Double,
    val x3: Double, val y3: Double,
    val width: Double = 6.0,
    val color: String = "#000000",
    val gps: Gps? = null,
    val extra: Map<String, Any?> = emptyMap(),
    /** null = la flecha no trae el campo (no se escribe). */
    val prohibidos: List<Prohibido>? = null
)

data class Sello(
    val type: String,
    val x: Double,
    val y: Double,
    val scale: Double = 1.3,
    val rot: Double = 0.0,
    /** Solo en los símbolos que se pueden pintar (colorable en simbolos.json). null = colorPorDefecto. */
    val color: String? = null,
    val extra: Map<String, Any?> = emptyMap()
)

data class Punto(val x: Double, val y: Double)

data class Trazo(
    val pts: List<Punto>,
    val color: String = "#333333",
    val width: Double = 3.5,
    val extra: Map<String, Any?> = emptyMap()
)

/**
 * Cuadro de texto (punto 8 bis). (x, y) = esquina de arriba a la izquierda (0 a 1);
 * size = alto de la letra en unidades; text puede tener varias líneas.
 */
data class CuadroTexto(
    val x: Double,
    val y: Double,
    val text: String,
    val size: Double,
    val color: String = "#000000",
    val extra: Map<String, Any?> = emptyMap()
)

/** Tamaño por defecto de un cuadro de texto: 4,5 en los dos recuadros (punto 8 bis). */
const val TEXTO_DIBUJO = 4.5
const val TEXTO_NOTAS = 4.5

data class Capa(
    val arrows: List<Flecha> = emptyList(),
    val stamps: List<Sello> = emptyList(),
    val strokes: List<Trazo> = emptyList(),
    val texts: List<CuadroTexto> = emptyList(),
    /** Campos desconocidos de la capa de notas (en el dibujo van en la raíz). */
    val extra: Map<String, Any?> = emptyMap()
) {
    fun estaVacia(): Boolean = arrows.isEmpty() && stamps.isEmpty() && strokes.isEmpty() && texts.isEmpty()
}

data class DibujoV1(
    val dibujo: Capa = Capa(),
    val notasCapa: Capa = Capa(),
    /** Tamaño de la letra de las notas en unidades (punto 12). null = automático. */
    val notasLetra: Double? = null,
    val extra: Map<String, Any?> = emptyMap()
) {
    fun estaVacio(): Boolean = dibujo.estaVacia() && notasCapa.estaVacia() && notasLetra == null

    /** Texto JSON a guardar, siempre con "v": 3. Un dibujo vacío (y sin notasLetra) se guarda como "". */
    fun aJson(): String {
        if (estaVacio()) return ""
        val raiz = LinkedHashMap<String, Any?>()
        raiz["v"] = VERSION_FORMATO
        escribirCapa(raiz, dibujo)
        if (!notasCapa.estaVacia()) {
            raiz["notasCapa"] = LinkedHashMap<String, Any?>().also {
                escribirCapa(it, notasCapa)
                for ((k, v) in notasCapa.extra) if (k !in it) it[k] = v
            }
        }
        notasLetra?.let { raiz["notasLetra"] = it }
        for ((k, v) in extra) if (k !in raiz) raiz[k] = v
        return JsonLite.write(raiz)
    }

    companion object {
        /** true si el texto tiene "v" (versión 1 o posterior). */
        fun esFormatoNuevo(json: String?): Boolean = versionDe(json) > 0

        /** Versión guardada en el texto: 0 si no tiene "v" o no es JSON. */
        fun versionDe(json: String?): Int {
            if (json.isNullOrBlank()) return 0
            return try {
                @Suppress("UNCHECKED_CAST")
                val raiz = JsonLite.parse(json) as? Map<String, Any?> ?: return 0
                (raiz["v"] as? Number)?.toInt() ?: 0
            } catch (e: Exception) {
                0
            }
        }

        /**
         * Lee un dibujo guardado y lo devuelve siempre en la versión 3 (punto 9):
         * v3 tal cual; v2 → v3; v1 o sin "v" → v2 → v3. Texto vacío o inválido = vacío.
         */
        fun desdeJson(json: String?): DibujoV1 {
            val crudo = leerCrudo(json)
            return when (versionDe(json)) {
                in 3..Int.MAX_VALUE -> crudo
                2 -> Formatos.deV2(crudo)
                else -> Formatos.deV1(crudo)
            }
        }

        /** Lee el JSON sin convertir nada (en las medidas de la versión que tenga). */
        fun leerCrudo(json: String?): DibujoV1 {
            if (json.isNullOrBlank()) return DibujoV1()
            return try {
                @Suppress("UNCHECKED_CAST")
                val raiz = JsonLite.parse(json) as? Map<String, Any?> ?: return DibujoV1()
                val notas = raiz.obj("notasCapa")
                DibujoV1(
                    dibujo = leerCapa(raiz, TEXTO_DIBUJO),
                    notasCapa = if (notas != null) leerCapa(notas, TEXTO_NOTAS).copy(extra = notas.filterKeys { it !in CLAVES_CAPA }) else Capa(),
                    notasLetra = raiz.numONull("notasLetra")?.takeIf { it > 0 },
                    extra = raiz.filterKeys { it !in CLAVES_CAPA && it !in setOf("v", "notasCapa", "notasLetra") }
                )
            } catch (e: Exception) {
                DibujoV1()
            }
        }

        private val CLAVES_CAPA = setOf("arrows", "stamps", "strokes", "texts")
        private val CLAVES_FLECHA = setOf("x0", "y0", "x1", "y1", "x2", "y2", "x3", "y3", "width", "color", "gps", "prohibidos")
        private val CLAVES_GPS = setOf("t", "lado", "largo")
        private val CLAVES_PROHIBIDO = setOf("t", "ang", "largo")
        private val CLAVES_SELLO = setOf("type", "x", "y", "scale", "rot", "color")
        private val CLAVES_TRAZO = setOf("color", "width", "pts")
        private val CLAVES_TEXTO = setOf("x", "y", "text", "size", "color")

        internal fun leerCapa(m: Map<String, Any?>, tamTexto: Double = TEXTO_DIBUJO): Capa = Capa(
            texts = m.lista("texts").objetos().map { leerTexto(it, tamTexto) },
            arrows = m.lista("arrows").objetos().map { leerFlecha(it) },
            stamps = m.lista("stamps").objetos().map { leerSello(it) },
            strokes = m.lista("strokes").objetos().map { leerTrazo(it) }
        )

        internal fun leerFlecha(a: Map<String, Any?>): Flecha {
            val g = a.obj("gps")
            val p = a["prohibidos"] as? List<*>
            return Flecha(
                x0 = a.num("x0", 0.5), y0 = a.num("y0", 0.9),
                x1 = a.num("x1", 0.5), y1 = a.num("y1", 0.63),
                x2 = a.num("x2", 0.5), y2 = a.num("y2", 0.37),
                x3 = a.num("x3", 0.5), y3 = a.num("y3", 0.1),
                width = a.numONull("width")?.takeIf { it > 0 } ?: 6.0,
                color = a.txt("color", "#000000"),
                gps = if (g != null) Gps(
                    t = g.num("t", 0.5),
                    lado = if (g.num("lado", 1.0) < 0) -1 else 1,
                    largo = g.numONull("largo")?.takeIf { it > 0 },
                    extra = g.filterKeys { it !in CLAVES_GPS }
                ) else null,
                extra = a.filterKeys { it !in CLAVES_FLECHA },
                prohibidos = p?.objetos()?.map {
                    Prohibido(it.num("t", 0.5), it.num("ang", 0.0), it.num("largo", 30.0), it.filterKeys { k -> k !in CLAVES_PROHIBIDO })
                }
            )
        }

        internal fun leerSello(s: Map<String, Any?>): Sello = Sello(
            type = s.txt("type", ""),
            x = s.num("x", 0.5),
            y = s.num("y", 0.5),
            scale = s.numONull("scale")?.takeIf { it > 0 } ?: 1.3,
            rot = s.num("rot", 0.0),
            color = s["color"] as? String,
            extra = s.filterKeys { it !in CLAVES_SELLO }
        )

        internal fun leerTexto(t: Map<String, Any?>, tamPorDefecto: Double): CuadroTexto = CuadroTexto(
            x = t.num("x", 0.0),
            y = t.num("y", 0.0),
            text = t.txt("text", ""),
            size = t.num("size", tamPorDefecto),
            color = t.txt("color", "#000000"),
            extra = t.filterKeys { it !in CLAVES_TEXTO }
        )

        /** Un trazo sin puntos también se conserva (nunca se pierde un dato). */
        internal fun leerTrazo(t: Map<String, Any?>): Trazo = Trazo(
            pts = t.lista("pts").objetos().map { Punto(it.num("x", 0.0), it.num("y", 0.0)) },
            color = t.txt("color", "#333333"),
            width = t.numONull("width")?.takeIf { it > 0 } ?: 3.5,
            extra = t.filterKeys { it !in CLAVES_TRAZO }
        )

        private fun escribirCapa(destino: MutableMap<String, Any?>, c: Capa) {
            destino["arrows"] = c.arrows.map { f ->
                LinkedHashMap<String, Any?>().apply {
                    put("x0", f.x0); put("y0", f.y0); put("x1", f.x1); put("y1", f.y1)
                    put("x2", f.x2); put("y2", f.y2); put("x3", f.x3); put("y3", f.y3)
                    put("width", f.width); put("color", f.color)
                    f.gps?.let { g ->
                        put("gps", LinkedHashMap<String, Any?>().apply {
                            put("t", g.t); put("lado", g.lado)
                            g.largo?.let { put("largo", it) }
                            for ((k, v) in g.extra) if (!containsKey(k)) put(k, v)
                        })
                    }
                    f.prohibidos?.let { lista ->
                        put("prohibidos", lista.map { p ->
                            LinkedHashMap<String, Any?>().apply {
                                put("t", p.t); put("ang", p.ang); put("largo", p.largo)
                                for ((k, v) in p.extra) if (!containsKey(k)) put(k, v)
                            }
                        })
                    }
                    for ((k, v) in f.extra) if (!containsKey(k)) put(k, v)
                }
            }
            destino["stamps"] = c.stamps.map { s ->
                LinkedHashMap<String, Any?>().apply {
                    put("type", s.type); put("x", s.x); put("y", s.y); put("scale", s.scale); put("rot", s.rot)
                    s.color?.let { put("color", it) }
                    for ((k, v) in s.extra) if (!containsKey(k)) put(k, v)
                }
            }
            destino["strokes"] = c.strokes.map { t ->
                LinkedHashMap<String, Any?>().apply {
                    put("color", t.color); put("width", t.width)
                    put("pts", t.pts.map { linkedMapOf("x" to it.x, "y" to it.y) })
                    for ((k, v) in t.extra) if (!containsKey(k)) put(k, v)
                }
            }
            if (c.texts.isNotEmpty()) destino["texts"] = c.texts.map { t ->
                LinkedHashMap<String, Any?>().apply {
                    put("x", t.x); put("y", t.y); put("text", t.text); put("size", t.size); put("color", t.color)
                    for ((k, v) in t.extra) if (!containsKey(k)) put(k, v)
                }
            }
        }
    }
}

/**
 * Paso de los formatos viejos a la versión 3 (punto 9), sin cambiar cómo se ven en la hoja.
 * Se redondea a 4 decimales, como la plataforma.
 */
object Formatos {
    // v1 → v2: el cuadrado ocupa todo el alto del recuadro de 34 × 29, centrado
    private const val K_DIBUJO = 29.0 / 34.0
    private const val X0_DIBUJO = (1 - K_DIBUJO) / 2
    private const val K_NOTAS_Y = 64.0 / 90.0
    // v2 → v3: el recuadro de 34 × 29 queda donde estaba dentro de la celda de 50 × 45
    private const val K_V3 = 34.0 / 50.0
    private const val X0_V3 = 8.0 / 50.0
    private const val Y0_V3 = 8.0 / 45.0
    private const val KY_V3 = 29.0 / 45.0

    private fun r4(v: Double) = Math.round(v * 10000.0) / 10000.0

    fun pasarCapa(c: Capa, px: (Double) -> Double, py: (Double) -> Double, k: Double): Capa = c.copy(
        arrows = c.arrows.map { a ->
            a.copy(
                x0 = r4(px(a.x0)), x1 = r4(px(a.x1)), x2 = r4(px(a.x2)), x3 = r4(px(a.x3)),
                y0 = r4(py(a.y0)), y1 = r4(py(a.y1)), y2 = r4(py(a.y2)), y3 = r4(py(a.y3)),
                width = r4(a.width * k),
                gps = a.gps?.let { g -> if (g.largo != null) g.copy(largo = r4(g.largo * k)) else g },
                prohibidos = a.prohibidos?.map { it.copy(largo = r4(it.largo * k)) }
            )
        },
        stamps = c.stamps.map { it.copy(x = r4(px(it.x)), y = r4(py(it.y)), scale = r4(it.scale * k)) },
        strokes = c.strokes.map { t -> t.copy(width = r4(t.width * k), pts = t.pts.map { Punto(r4(px(it.x)), r4(py(it.y))) }) },
        texts = c.texts.map { it.copy(x = r4(px(it.x)), y = r4(py(it.y)), size = r4(it.size * k)) }
    )

    /** v1 (o sin "v") → v2. */
    fun aV2(d: DibujoV1): DibujoV1 = d.copy(
        dibujo = pasarCapa(d.dibujo, { X0_DIBUJO + it * K_DIBUJO }, { it }, K_DIBUJO),
        notasCapa = pasarCapa(d.notasCapa, { it }, { it * K_NOTAS_Y }, 1.0),
        notasLetra = d.notasLetra?.let { Math.round(it * 72.0 / 50.0 * 2) / 2.0 }
    )

    /** v2 → v3: solo cambia el dibujo. */
    fun deV2(d: DibujoV1): DibujoV1 = d.copy(
        dibujo = pasarCapa(d.dibujo, { X0_V3 + it * K_V3 }, { Y0_V3 + it * KY_V3 }, K_V3)
    )

    /** v1 (o sin "v") → v3. */
    fun deV1(d: DibujoV1): DibujoV1 = deV2(aV2(d))
}

/** Texto de las notas (punto 12). Tamaños en unidades (100 = ancho de la celda de notas). */
object LetraNotas {
    const val MIN = 4.0
    const val MAX = 28.0
    const val PASO = 0.5
    const val AUTO_MIN = 6.5
    const val AUTO_MAX = 20.0
    const val INTERLINEADO = 1.2
    const val MARGEN = 2.0

    /** Lugar para el texto: 96 × 86 sin carteles, 96 × 66 con carteles. */
    fun anchoDisponible(): Double = 100.0 - 2 * MARGEN
    fun altoDisponible(conCarteles: Boolean): Double =
        (if (conCarteles) ALTO_NOTAS - Carteles.ALTO_FRANJA else ALTO_NOTAS) - 2 * MARGEN

    /** Texto a dibujar: descripción y, en la línea siguiente, notas. Si uno está vacío, va el otro. */
    fun textoCompleto(descripcion: String?, notas: String? = null): String =
        listOfNotNull(descripcion?.takeIf { it.isNotEmpty() }, notas?.takeIf { it.isNotEmpty() }).joinToString("\n")

    /** Lleva un valor al rango 3–20, de a 0,5. */
    fun ajustar(v: Double): Double = (Math.round(v / PASO) * PASO).coerceIn(MIN, MAX)

    /** Botón A↑ (desde automático parte del tamaño que se está viendo). */
    fun mas(actual: Double): Double = ajustar(actual + PASO)

    /** Botón A↓. */
    fun menos(actual: Double): Double = ajustar(actual - PASO)

    /**
     * Corta el texto en líneas para un ancho dado. [ancho] mide un texto con esta letra
     * (en las mismas unidades que [anchoMax]). Respeta los saltos de línea del usuario,
     * corta entre palabras y, si una palabra sola no entra, la corta donde haga falta.
     */
    fun lineas(texto: String, anchoMax: Double, ancho: (String) -> Double): List<String> {
        if (texto.isEmpty()) return emptyList()
        val salida = mutableListOf<String>()
        for (parrafo in texto.replace("\r\n", "\n").split('\n')) {
            if (parrafo.isEmpty()) { salida.add(""); continue }
            var linea = ""
            for (palabra in parrafo.split(' ')) {
                val prueba = if (linea.isEmpty()) palabra else "$linea $palabra"
                if (ancho(prueba) <= anchoMax + 1e-9) { linea = prueba; continue }
                if (linea.isNotEmpty()) { salida.add(linea); linea = "" }
                // La palabra sola: si no entra, se corta por letras
                var resto = palabra
                while (resto.isNotEmpty() && ancho(resto) > anchoMax + 1e-9) {
                    var n = 1
                    while (n < resto.length && ancho(resto.substring(0, n + 1)) <= anchoMax + 1e-9) n++
                    salida.add(resto.substring(0, n))
                    resto = resto.substring(n)
                }
                linea = resto
            }
            salida.add(linea)
        }
        return salida
    }

    /** Alto que ocupan n líneas con esta letra. */
    fun altoDe(nLineas: Int, letra: Double): Double = nLineas * letra * INTERLINEADO

    /**
     * Letra a usar: [notasLetra] si está; si no, la más grande entre 14 y 4,5, de a 0,5,
     * con la que todo el texto entra. [anchoConLetra] mide un texto con una letra dada.
     */
    fun letra(
        texto: String,
        notasLetra: Double?,
        conCarteles: Boolean,
        anchoConLetra: (texto: String, letra: Double) -> Double
    ): Double {
        if (notasLetra != null) return notasLetra
        val alto = altoDisponible(conCarteles)
        return automatica(alto) { l -> altoDe(lineas(texto, anchoDisponible()) { anchoConLetra(it, l) }.size, l) }
    }

    /**
     * Tamaño automático: el más grande entre 14 y 4,5, de a 0,5, con el que todo el texto
     * entra en el alto disponible. [altoTexto] devuelve el alto que ocupa el texto con esa
     * letra. Si no entra ni con 4,5, queda 4,5.
     */
    fun automatica(altoDisponible: Double, altoTexto: (letra: Double) -> Double): Double {
        var letra = AUTO_MAX
        while (letra >= AUTO_MIN - 1e-9) {
            if (altoTexto(letra) <= altoDisponible + 1e-9) return letra
            letra -= PASO
        }
        return AUTO_MIN
    }
}
