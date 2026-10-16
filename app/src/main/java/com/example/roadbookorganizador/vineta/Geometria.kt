package com.example.roadbookorganizador.vineta

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Punto en unidades del recuadro (100 de ancho, H de alto). */
data class Pu(val x: Double, val y: Double) {
    operator fun plus(o: Pu) = Pu(x + o.x, y + o.y)
    operator fun minus(o: Pu) = Pu(x - o.x, y - o.y)
    operator fun times(k: Double) = Pu(x * k, y * k)
}

/** Geometría de flechas y palito del GPS en unidades (ESPECIFICACION_VINETA.md, puntos 5 y 6). */
object Geometria {

    fun puntosEnUnidades(f: Flecha, alto: Double): List<Pu> = listOf(
        Pu(f.x0 * 100.0, f.y0 * alto), Pu(f.x1 * 100.0, f.y1 * alto),
        Pu(f.x2 * 100.0, f.y2 * alto), Pu(f.x3 * 100.0, f.y3 * alto)
    )

    fun bezier(p: List<Pu>, t: Double): Pu {
        val u = 1 - t
        return p[0] * (u * u * u) + p[1] * (3 * u * u * t) + p[2] * (3 * u * t * t) + p[3] * (t * t * t)
    }

    /** Derivada de la Bézier cúbica en t. */
    fun derivada(p: List<Pu>, t: Double): Pu {
        val u = 1 - t
        return (p[1] - p[0]) * (3 * u * u) + (p[2] - p[1]) * (6 * u * t) + (p[3] - p[2]) * (3 * t * t)
    }

    private fun normalizar(v: Pu): Pu {
        val l = hypot(v.x, v.y)
        return if (l < 1e-9) Pu(0.0, -1.0) else Pu(v.x / l, v.y / l)
    }

    fun radioEntrada(w: Double) = max(4.0, 0.75 * w)

    /** Triángulo de la punta, ya trasladado y girado (punto 5). */
    fun punta(p: List<Pu>, w: Double): List<Pu> {
        val h = max(10.0, 1.8 * w)
        var dir = p[3] - p[2]
        if (hypot(dir.x, dir.y) < 1e-9) dir = p[3] - p[1]
        if (hypot(dir.x, dir.y) < 1e-9) dir = p[3] - p[0]
        val ang = atan2(dir.y, dir.x) + Math.PI / 2
        val c = cos(ang)
        val s = sin(ang)
        return listOf(Pu(0.0, -h), Pu(0.65 * h, 0.4 * h), Pu(-0.65 * h, 0.4 * h)).map { v ->
            Pu(p[3].x + v.x * c - v.y * s, p[3].y + v.x * s + v.y * c)
        }
    }

    data class Palito(val desde: Pu, val hasta: Pu, val grosor: Double)

    /** Palito del GPS (punto 6). */
    fun palito(p: List<Pu>, w: Double, gps: Gps): Palito {
        val t = gps.t.coerceIn(0.02, 0.98)
        val lado = if (gps.lado < 0) -1.0 else 1.0
        val q = bezier(p, t)
        val tan = normalizar(derivada(p, t))
        val n = Pu(-tan.y * lado, tan.x * lado)
        val a = Math.toRadians(35.0)
        val d = n * cos(a) - tan * sin(a)
        val largo = max(7.0, 1.5 * w)
        return Palito(q, q + d * largo, max(1.6, 0.3 * w))
    }

    /**
     * Qué flecha lleva el palito: la primera con gps; si ninguna, la primera con t 0.5 y
     * lado 1 (punto 6). Devuelve null si la capa no tiene flechas.
     */
    fun flechaConGps(flechas: List<Flecha>): Pair<Int, Gps>? {
        if (flechas.isEmpty()) return null
        val i = flechas.indexOfFirst { it.gps != null }
        return if (i >= 0) i to flechas[i].gps!! else 0 to Gps(0.5, 1)
    }

    /**
     * Gira una flecha alrededor del centro del recuadro (0.5, 0.5) `rot` grados en sentido
     * horario. El giro se hace en unidades para no deformar en la capa de notas (100 × 64).
     */
    fun girarFlecha(f: Flecha, rotGrados: Double, alto: Double): Flecha {
        if (rotGrados % 360.0 == 0.0) return f
        val a = Math.toRadians(rotGrados)
        val c = cos(a)
        val s = sin(a)
        val cx = 50.0
        val cy = alto / 2.0
        fun g(x: Double, y: Double): Pair<Double, Double> {
            val dx = x * 100.0 - cx
            val dy = y * alto - cy
            return Pair((cx + dx * c - dy * s) / 100.0, (cy + dx * s + dy * c) / alto)
        }
        val (a0, b0) = g(f.x0, f.y0)
        val (a1, b1) = g(f.x1, f.y1)
        val (a2, b2) = g(f.x2, f.y2)
        val (a3, b3) = g(f.x3, f.y3)
        return f.copy(x0 = a0, y0 = b0, x1 = a1, y1 = b1, x2 = a2, y2 = b2, x3 = a3, y3 = b3)
    }
}
