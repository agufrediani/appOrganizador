package com.example.roadbookorganizador.vineta

import kotlin.math.hypot

/**
 * Goma de borrar (punto 7): corta los trazos del lápiz. Lo que toca se borra y un trazo
 * cortado al medio queda como dos trazos. Mismo algoritmo que la plataforma.
 */
object Goma {
    /** Radio de la goma en unidades (100 = ancho del recuadro). */
    const val RADIO = 4.0

    private fun r4(v: Double) = Math.round(v * 10000.0) / 10000.0

    private class P(val x: Double, val y: Double, val orig: Boolean)

    /** Borra con la goma centrada en (x, y) (0 a 1) en un recuadro de 100 × [alto] unidades. */
    fun borrar(strokes: List<Trazo>, x: Double, y: Double, alto: Double, radio: Double = RADIO): List<Trazo> {
        val px = x * 100
        val py = y * alto
        val res = mutableListOf<Trazo>()
        strokes.forEachIndexed { k, s ->
            val alcance = radio + s.width / 2
            fun dentro(q: P) = hypot(q.x * 100 - px, q.y * alto - py) <= alcance
            // Se agregan puntos cada ~0,5 unidades para poder cortar en el medio de un tramo largo
            val densos = mutableListOf<P>()
            s.pts.forEachIndexed { i, q ->
                if (i > 0) {
                    val a = s.pts[i - 1]
                    val largo = hypot((q.x - a.x) * 100, (q.y - a.y) * alto)
                    val n = Math.floor(largo / 0.5).toInt()
                    for (j in 1 until n) densos.add(P(a.x + (q.x - a.x) * j / n, a.y + (q.y - a.y) * j / n, false))
                }
                densos.add(P(q.x, q.y, true))
            }
            if (densos.none { dentro(it) }) { res.add(s); return@forEachIndexed }
            var pedazo = mutableListOf<P>()
            var n = 0
            val base = (s.extra["id"] as? String) ?: "str_$k"
            fun cerrar() {
                val pts = pedazo.filterIndexed { i, q -> q.orig || i == 0 || i == pedazo.lastIndex }
                if (pts.size >= 2 && pedazo.size >= 2) {
                    res.add(s.copy(pts = pts.map { Punto(r4(it.x), r4(it.y)) }, extra = s.extra + ("id" to "${base}_${n++}")))
                }
                pedazo = mutableListOf()
            }
            for (q in densos) if (dentro(q)) cerrar() else pedazo.add(q)
            cerrar()
        }
        return res
    }
}
