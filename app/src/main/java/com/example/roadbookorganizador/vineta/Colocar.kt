package com.example.roadbookorganizador.vineta

/**
 * Valores iniciales de lo que se pone en un recuadro (punto 8, revisión 8). Los dos recuadros
 * miden 50 × 45 mm; lo nuevo sale del tamaño de los dibujos viejos (un cuadrado de 29 mm).
 * Mismas cuentas que la plataforma (RoadbookWorkstation / InteractiveCroquisCanvas).
 */
object Colocar {
    const val ANCHO_MM = 50.0
    const val ALTO_MM = 45.0
    const val CUADRADO_MM = 29.0

    /** Escala de un símbolo nuevo, igual en los dos recuadros: 29 / 50. */
    const val ESCALA = CUADRADO_MM / ANCHO_MM

    /** Símbolo nuevo: scale 0,58; x, y entre 0,05 y 0,95. */
    val ESCALA_SIMBOLO: Double = Math.round(ESCALA * 100) / 100.0

    /** Texto nuevo: 8 × 0,58, redondeado a 0,5 → 4,5. */
    val TAMANO_TEXTO: Double = Math.round(8 * ESCALA * 2) / 2.0

    /** Flecha nueva: 6 × 0,58, redondeado a 0,1 → 3,5. */
    val ANCHO_FLECHA: Double = Math.round(6 * ESCALA * 10) / 10.0

    /** Grosor del lápiz en unidades para fino 2,5 / medio 4 / grueso 6. */
    fun anchoLapiz(grosor: Double): Double = grosor * ESCALA

    /** Ancho de la flecha elegida al tocar un grosor (fino, medio, grueso). */
    fun anchoFlechaParaGrosor(grosor: Double): Double = Math.round(grosor * 1.5 * ESCALA * 10) / 10.0

    fun posicion(v: Double) = v.coerceIn(0.05, 0.95)

    /**
     * Flecha armada (hecha en un cuadrado de 100 × 100) puesta como un cuadrado de 29 mm en el
     * recuadro de 50 × 45, con su centro en (x, y) y sin salirse del recuadro.
     */
    fun armadaEnCaja(a: Flecha, x: Double = 0.5, y: Double = 0.5): Flecha {
        val sx = CUADRADO_MM / ANCHO_MM
        val sy = CUADRADO_MM / ALTO_MM
        val xs = listOf(a.x0, a.x1, a.x2, a.x3)
        val ys = listOf(a.y0, a.y1, a.y2, a.y3)
        val cx = (xs.min() + xs.max()) / 2
        val cy = (ys.min() + ys.max()) / 2
        fun ex(v: Double) = cx + (v - cx) * sx
        fun ey(v: Double) = cy + (v - cy) * sy
        val chica = a.copy(
            x0 = ex(a.x0), x1 = ex(a.x1), x2 = ex(a.x2), x3 = ex(a.x3),
            y0 = ey(a.y0), y1 = ey(a.y1), y2 = ey(a.y2), y3 = ey(a.y3),
            width = Math.round(a.width * sx * 10) / 10.0,
            prohibidos = a.prohibidos?.map { it.copy(largo = Math.round(it.largo * sx * 10) / 10.0) }
        )
        return moverA(chica, x, y)
    }

    /** Corre la flecha para que su centro quede en (x, y), sin salirse del recuadro. */
    fun moverA(a: Flecha, x: Double, y: Double): Flecha {
        val xs = listOf(a.x0, a.x1, a.x2, a.x3)
        val ys = listOf(a.y0, a.y1, a.y2, a.y3)
        val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
        val dx = (x - (minX + maxX) / 2).coerceIn(-minX, 1 - maxX)
        val dy = (y - (minY + maxY) / 2).coerceIn(-minY, 1 - maxY)
        return a.copy(
            x0 = a.x0 + dx, x1 = a.x1 + dx, x2 = a.x2 + dx, x3 = a.x3 + dx,
            y0 = a.y0 + dy, y1 = a.y1 + dy, y2 = a.y2 + dy, y3 = a.y3 + dy
        )
    }

    /** Un camino prohibido nuevo para la flecha: en un lugar libre, alternando los lados. */
    fun nuevoProhibido(a: Flecha, escala: Double = ESCALA): Prohibido {
        val n = a.prohibidos?.size ?: 0
        val lugares = listOf(0.5, 0.62, 0.38, 0.74, 0.26, 0.56, 0.44, 0.68)
        val t = lugares[n % lugares.size]
        val ang = (if (n % 2 == 0) -1.0 else 1.0) * (if (n % 4 < 2) 60.0 else 110.0)
        return Prohibido(t, ang, Math.round(30 * escala * 10) / 10.0)
    }

    /** Cuántos caminos prohibidos trae una flecha armada por su código (0, 1 o 2). */
    fun prohibidosDelCodigo(codigo: String): Int = when {
        codigo.endsWith("_2p") -> 2
        codigo.endsWith("_1p") -> 1
        else -> 0
    }
}
