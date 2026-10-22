package com.example.roadbookorganizador.vineta

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Carteles de velocidad (ESPECIFICACION_VINETA.md, punto 10). No son parte del dibujo:
 * salen de tres campos de la viñeta. 0 o vacío = sin valor.
 */
data class ValoresCartel(
    val tiempoSegundos: Double?,
    val promedioKmh: Double?,
    val maximaKmh: Double?
) {
    val tieneAlguno: Boolean get() = tiempoSegundos != null || promedioKmh != null || maximaKmh != null

    companion object {
        /** Desde los campos de la app: tiempoIdealSegundos, velocidadPromedioSectorKmh, velocidadMaximaSectorKmh. */
        fun desdeCampos(tiempo: Double, promedio: Double, maxima: Double) = ValoresCartel(
            tiempoSegundos = tiempo.takeIf { it > 0.0 },
            promedioKmh = promedio.takeIf { it > 0.0 },
            maximaKmh = maxima.takeIf { it > 0.0 }
        )
    }
}

data class FranjaCarteles(
    val tiempo: String?,     // texto ya formateado, null = celda vacía
    val promedio: String?,
    val maxima: String?,
    val gris: Boolean        // aviso de cambio respecto de la viñeta anterior
)

object Carteles {

    const val ALTO_FRANJA = 20.0          // unidades de la capa de notas (10 mm de 45)
    const val MARGEN_CELDA = 1.6          // unidades (0,8 mm)
    const val GRIS_CAMBIO = "#d4d4d8"

    /** Tiempo en m:ss (por ejemplo 2:05). */
    fun formatoTiempo(segundos: Double): String {
        val total = Math.round(segundos).toLong().coerceAtLeast(0)
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    /** Velocidad con un decimal como máximo y coma decimal (52,5 o 60). */
    fun formatoVelocidad(kmh: Double): String {
        val bd = BigDecimal.valueOf(kmh).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString().replace('.', ',')
    }

    /**
     * Calcula la franja de cada viñeta de un tramo (en orden). Devuelve null en todas si
     * ninguna viñeta del tramo tiene valores (la franja no existe).
     */
    fun franjasDelTramo(valores: List<ValoresCartel>): List<FranjaCarteles?> {
        if (valores.none { it.tieneAlguno }) return valores.map { null }
        var anterior: Triple<String?, String?, String?>? = null
        return valores.map { v ->
            val actual = Triple(
                v.tiempoSegundos?.let { formatoTiempo(it) },
                v.promedioKmh?.let { formatoVelocidad(it) },
                v.maximaKmh?.let { formatoVelocidad(it) }
            )
            val gris = if (anterior == null) v.tieneAlguno else actual != anterior
            anterior = actual
            FranjaCarteles(actual.first, actual.second, actual.third, gris)
        }
    }

    /** Factor del tamaño del valor en promedio y máxima según la cantidad de caracteres. */
    fun factorTamanoVelocidad(texto: String): Double = when {
        texto.length <= 2 -> 1.0
        texto.length == 3 -> 0.8
        else -> 0.66
    }
}
