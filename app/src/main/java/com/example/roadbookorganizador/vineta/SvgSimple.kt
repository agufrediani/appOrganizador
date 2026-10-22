package com.example.roadbookorganizador.vineta

/**
 * Lector del SVG restringido de simbolos.json (ESPECIFICACION_VINETA.md, punto 8):
 * elementos line, circle, ellipse, rect, polygon, path y text, con fill/stroke heredados
 * de la raíz. Devuelve una lista de operaciones de dibujo en unidades del viewBox 100 × 100.
 * No depende de Android: el dibujante de Android recorre esta lista.
 */
data class Estilo(
    val fill: String?,           // null = sin relleno
    val stroke: String?,         // null = sin borde
    val strokeWidth: Double,
    val linecap: String,         // butt | round | square
    val linejoin: String,        // miter | round | bevel
    val dash: List<Double>?,
    val opacity: Double,
    val fillOpacity: Double = 1.0
)

sealed class OpSvg {
    abstract val estilo: Estilo
    data class Linea(val x1: Double, val y1: Double, val x2: Double, val y2: Double, override val estilo: Estilo) : OpSvg()
    data class Circulo(val cx: Double, val cy: Double, val r: Double, override val estilo: Estilo) : OpSvg()
    data class Elipse(val cx: Double, val cy: Double, val rx: Double, val ry: Double, override val estilo: Estilo) : OpSvg()
    data class Rect(val x: Double, val y: Double, val w: Double, val h: Double, val rx: Double, val ry: Double, override val estilo: Estilo) : OpSvg()
    data class Poligono(val puntos: List<Pair<Double, Double>>, override val estilo: Estilo) : OpSvg()
    data class Trayecto(val d: String, override val estilo: Estilo) : OpSvg()
    data class Texto(
        val x: Double, val y: Double, val texto: String,
        val anchor: String,          // start | middle | end
        val fontSize: Double,
        val fontWeight: Int,
        val fontFamily: String,      // sans-serif | monospace
        override val estilo: Estilo
    ) : OpSvg()
}

object SvgSimple {

    private val reEtiqueta = Regex("""<(/?)([a-zA-Z]+)((?:\s+[a-zA-Z][a-zA-Z0-9:\-]*\s*=\s*"[^"]*")*)\s*(/?)>""")
    private val reAtributo = Regex("""([a-zA-Z][a-zA-Z0-9:\-]*)\s*=\s*"([^"]*)"""")

    fun parse(svg: String): List<OpSvg> {
        val ops = mutableListOf<OpSvg>()
        // Pila de atributos heredables (fill, stroke y compañía)
        val pila = ArrayDeque<Map<String, String>>()
        pila.addLast(mapOf("fill" to "#000000"))
        var textoAbierto: Map<String, String>? = null
        var fin = 0
        for (m in reEtiqueta.findAll(svg)) {
            val cierre = m.groupValues[1] == "/"
            val nombre = m.groupValues[2]
            val atributos = reAtributo.findAll(m.groupValues[3]).associate { it.groupValues[1] to it.groupValues[2] }
            val autoCerrada = m.groupValues[4] == "/"

            if (cierre) {
                if (nombre == "text" && textoAbierto != null) {
                    val contenido = decodificar(svg.substring(fin, m.range.first))
                    ops.add(texto(textoAbierto, contenido, heredado(pila)))
                    textoAbierto = null
                }
                if (nombre == "svg" || nombre == "g") if (pila.size > 1) pila.removeLast()
                fin = m.range.last + 1
                continue
            }
            fin = m.range.last + 1
            when (nombre) {
                "svg", "g" -> if (!autoCerrada) pila.addLast(heredado(pila) + heredables(atributos))
                "text" -> if (autoCerrada) Unit else textoAbierto = atributos
                else -> elemento(nombre, atributos, heredado(pila))?.let { ops.add(it) }
            }
        }
        return ops
    }

    private val HEREDABLES = setOf(
        "fill", "fill-opacity", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "stroke-dasharray",
        "font-size", "font-weight", "font-family", "text-anchor"
    )

    private fun heredables(a: Map<String, String>) = a.filterKeys { it in HEREDABLES }

    private fun heredado(pila: ArrayDeque<Map<String, String>>): Map<String, String> = pila.last()

    private fun n(a: Map<String, String>, k: String, def: Double = 0.0): Double =
        a[k]?.trim()?.removeSuffix("px")?.toDoubleOrNull() ?: def

    private fun estilo(a: Map<String, String>, h: Map<String, String>): Estilo {
        val todo = h + a
        fun color(v: String?): String? = when {
            v == null -> null
            v.equals("none", true) || v.equals("transparent", true) -> null
            else -> v.trim()
        }
        val dash = todo["stroke-dasharray"]?.takeIf { !it.equals("none", true) }
            ?.split(Regex("[,\\s]+"))?.mapNotNull { it.toDoubleOrNull() }?.takeIf { it.isNotEmpty() }
        return Estilo(
            fill = color(todo["fill"]),
            stroke = color(todo["stroke"]),
            strokeWidth = todo["stroke-width"]?.toDoubleOrNull() ?: 1.0,
            linecap = todo["stroke-linecap"] ?: "butt",
            linejoin = todo["stroke-linejoin"] ?: "miter",
            dash = dash,
            opacity = a["opacity"]?.toDoubleOrNull() ?: 1.0,
            fillOpacity = todo["fill-opacity"]?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 1.0
        )
    }

    private fun elemento(nombre: String, a: Map<String, String>, h: Map<String, String>): OpSvg? {
        val e = estilo(a, h)
        return when (nombre) {
            "line" -> OpSvg.Linea(n(a, "x1"), n(a, "y1"), n(a, "x2"), n(a, "y2"), e)
            "circle" -> OpSvg.Circulo(n(a, "cx"), n(a, "cy"), n(a, "r"), e)
            "ellipse" -> OpSvg.Elipse(n(a, "cx"), n(a, "cy"), n(a, "rx"), n(a, "ry"), e)
            "rect" -> {
                // Como en SVG: si falta rx o ry, se usa el otro
                val rx = a["rx"]?.toDoubleOrNull()
                val ry = a["ry"]?.toDoubleOrNull()
                OpSvg.Rect(n(a, "x"), n(a, "y"), n(a, "width"), n(a, "height"), rx ?: ry ?: 0.0, ry ?: rx ?: 0.0, e)
            }
            "polygon" -> {
                val nums = (a["points"] ?: "").trim().split(Regex("[,\\s]+")).mapNotNull { it.toDoubleOrNull() }
                OpSvg.Poligono(nums.chunked(2).filter { it.size == 2 }.map { it[0] to it[1] }, e)
            }
            "path" -> a["d"]?.let { OpSvg.Trayecto(it, e) }
            else -> null // Elemento fuera de la especificación: se ignora
        }
    }

    private fun texto(a: Map<String, String>, contenido: String, h: Map<String, String>): OpSvg.Texto {
        val todo = h + a
        val peso = when (val w = todo["font-weight"]?.trim()) {
            null -> 400
            "bold" -> 700
            "normal" -> 400
            else -> w.toIntOrNull() ?: 400
        }
        return OpSvg.Texto(
            x = n(a, "x"), y = n(a, "y"), texto = contenido.trim(),
            anchor = todo["text-anchor"] ?: "start",
            fontSize = todo["font-size"]?.removeSuffix("px")?.toDoubleOrNull() ?: 16.0,
            fontWeight = peso,
            fontFamily = if (todo["font-family"]?.contains("mono", true) == true) "monospace" else "sans-serif",
            estilo = estilo(a, h)
        )
    }

    private fun decodificar(s: String) = s
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
        .replace("&apos;", "'").replace("&amp;", "&")
}
