package com.example.roadbookorganizador.vineta

/**
 * Dibujo de una viñeta, versión 1 del formato (ESPECIFICACION_VINETA.md, puntos 2 a 8).
 * Coordenadas x, y de 0 a 1 sobre el ancho y el alto del recuadro. Grosores y tamaños en
 * unidades (100 unidades = ancho del recuadro).
 *
 * Los campos que este modelo no conoce (por ejemplo "id") se conservan en [extra] y se
 * vuelven a escribir tal cual al guardar.
 */
const val VERSION_FORMATO = 1

/** Proporción de cada recuadro: alto H en unidades (el ancho siempre es 100). */
const val ALTO_DIBUJO = 100.0
const val ALTO_NOTAS = 64.0

data class Gps(val t: Double = 0.5, val lado: Int = 1)

data class Flecha(
    val x0: Double, val y0: Double,
    val x1: Double, val y1: Double,
    val x2: Double, val y2: Double,
    val x3: Double, val y3: Double,
    val width: Double = 6.0,
    val color: String = "#000000",
    val gps: Gps? = null,
    val extra: Map<String, Any?> = emptyMap()
)

data class Sello(
    val type: String,
    val x: Double,
    val y: Double,
    val scale: Double = 1.3,
    val rot: Double = 0.0,
    val extra: Map<String, Any?> = emptyMap()
)

data class Punto(val x: Double, val y: Double)

data class Trazo(
    val pts: List<Punto>,
    val color: String = "#333333",
    val width: Double = 3.5,
    val extra: Map<String, Any?> = emptyMap()
)

data class Capa(
    val arrows: List<Flecha> = emptyList(),
    val stamps: List<Sello> = emptyList(),
    val strokes: List<Trazo> = emptyList()
) {
    fun estaVacia(): Boolean = arrows.isEmpty() && stamps.isEmpty() && strokes.isEmpty()
}

data class DibujoV1(
    val dibujo: Capa = Capa(),
    val notasCapa: Capa = Capa(),
    /** Tamaño de la letra de las notas en unidades (punto 12). null = automático. */
    val notasLetra: Double? = null,
    val extra: Map<String, Any?> = emptyMap()
) {
    fun estaVacio(): Boolean = dibujo.estaVacia() && notasCapa.estaVacia() && notasLetra == null

    /** Texto JSON a guardar. Un dibujo vacío se guarda como "" (punto 2). */
    fun aJson(): String {
        if (estaVacio()) return ""
        val raiz = LinkedHashMap<String, Any?>()
        raiz["v"] = VERSION_FORMATO
        escribirCapa(raiz, dibujo)
        if (!notasCapa.estaVacia()) {
            raiz["notasCapa"] = LinkedHashMap<String, Any?>().also { escribirCapa(it, notasCapa) }
        }
        notasLetra?.let { raiz["notasLetra"] = it }
        for ((k, v) in extra) if (k !in raiz) raiz[k] = v
        return JsonLite.write(raiz)
    }

    companion object {
        /** true si el texto está en la versión 1 o posterior (tiene "v"). */
        fun esFormatoNuevo(json: String?): Boolean {
            if (json.isNullOrBlank()) return false
            return try {
                @Suppress("UNCHECKED_CAST")
                val raiz = JsonLite.parse(json) as? Map<String, Any?> ?: return false
                raiz["v"] is Number
            } catch (e: Exception) {
                false
            }
        }

        /** Lee un dibujo versión 1. Texto vacío o inválido = dibujo vacío. */
        fun desdeJson(json: String?): DibujoV1 {
            if (json.isNullOrBlank()) return DibujoV1()
            return try {
                @Suppress("UNCHECKED_CAST")
                val raiz = JsonLite.parse(json) as? Map<String, Any?> ?: return DibujoV1()
                val notas = raiz.obj("notasCapa")
                DibujoV1(
                    dibujo = leerCapa(raiz),
                    notasCapa = if (notas != null) leerCapa(notas) else Capa(),
                    notasLetra = raiz.numONull("notasLetra"),
                    extra = raiz.filterKeys { it !in setOf("v", "arrows", "stamps", "strokes", "notasCapa", "notasLetra") }
                )
            } catch (e: Exception) {
                DibujoV1()
            }
        }

        private val CLAVES_FLECHA = setOf("x0", "y0", "x1", "y1", "x2", "y2", "x3", "y3", "width", "color", "gps")
        private val CLAVES_SELLO = setOf("type", "x", "y", "scale", "rot")
        private val CLAVES_TRAZO = setOf("color", "width", "pts")

        internal fun leerCapa(m: Map<String, Any?>): Capa = Capa(
            arrows = m.lista("arrows").objetos().map { leerFlecha(it) },
            stamps = m.lista("stamps").objetos().map { leerSello(it) },
            strokes = m.lista("strokes").objetos().mapNotNull { leerTrazo(it) }
        )

        internal fun leerFlecha(a: Map<String, Any?>): Flecha {
            val g = a.obj("gps")
            return Flecha(
                x0 = a.num("x0", 0.5), y0 = a.num("y0", 0.9),
                x1 = a.num("x1", 0.5), y1 = a.num("y1", 0.63),
                x2 = a.num("x2", 0.5), y2 = a.num("y2", 0.37),
                x3 = a.num("x3", 0.5), y3 = a.num("y3", 0.1),
                width = a.num("width", 6.0),
                color = a.txt("color", "#000000"),
                gps = if (g != null) Gps(t = g.num("t", 0.5), lado = if (g.num("lado", 1.0) < 0) -1 else 1) else null,
                extra = a.filterKeys { it !in CLAVES_FLECHA }
            )
        }

        internal fun leerSello(s: Map<String, Any?>): Sello = Sello(
            type = s.txt("type", ""),
            x = s.num("x", 0.5),
            y = s.num("y", 0.5),
            scale = s.num("scale", 1.3),
            rot = s.num("rot", 0.0),
            extra = s.filterKeys { it !in CLAVES_SELLO }
        )

        internal fun leerTrazo(t: Map<String, Any?>): Trazo? {
            val pts = t.lista("pts").objetos().map { Punto(it.num("x", 0.0), it.num("y", 0.0)) }
            if (pts.isEmpty()) return null
            return Trazo(
                pts = pts,
                color = t.txt("color", "#333333"),
                width = t.num("width", 3.5),
                extra = t.filterKeys { it !in CLAVES_TRAZO }
            )
        }

        private fun escribirCapa(destino: MutableMap<String, Any?>, c: Capa) {
            destino["arrows"] = c.arrows.map { f ->
                LinkedHashMap<String, Any?>().apply {
                    put("x0", f.x0); put("y0", f.y0); put("x1", f.x1); put("y1", f.y1)
                    put("x2", f.x2); put("y2", f.y2); put("x3", f.x3); put("y3", f.y3)
                    put("width", f.width); put("color", f.color)
                    f.gps?.let { put("gps", linkedMapOf("t" to it.t, "lado" to it.lado)) }
                    for ((k, v) in f.extra) if (!containsKey(k)) put(k, v)
                }
            }
            destino["stamps"] = c.stamps.map { s ->
                LinkedHashMap<String, Any?>().apply {
                    put("type", s.type); put("x", s.x); put("y", s.y); put("scale", s.scale); put("rot", s.rot)
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
        }
    }
}

/** Texto de las notas (punto 12). Tamaños en unidades (100 = ancho de la celda de notas). */
object LetraNotas {
    const val MIN = 3.0
    const val MAX = 20.0
    const val PASO = 0.5
    const val AUTO_MIN = 4.5
    const val AUTO_MAX = 14.0
    const val INTERLINEADO = 1.2
    const val MARGEN = 2.0

    /** Lugar para el texto: 96 × 60 sin carteles, 96 × 46 con carteles. */
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
