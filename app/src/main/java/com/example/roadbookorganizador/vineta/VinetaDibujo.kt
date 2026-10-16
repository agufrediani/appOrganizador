package com.example.roadbookorganizador.vineta

/**
 * Lectura y guardado del dibujo de una viñeta, sin depender de Android ni de Room.
 *
 * - La columna `dibujo` tiene el formato versión 1. Si nunca se guardó (null), el dibujo se
 *   arma convirtiendo las columnas viejas (punto 9), que quedan intactas como respaldo.
 * - Al guardar, el campo peligro se calcula desde los símbolos EXCL_n / PELIGRO_n (punto 11).
 */
object VinetaDibujo {

    fun leer(
        bib: Biblioteca,
        dibujo: String?,
        tulipSvgData: String?,
        notasSvgData: String?,
        tulipTipo: String?,
        peligro: String?
    ): DibujoV1 =
        if (dibujo != null) DibujoV1.desdeJson(dibujo)
        else ConversionViejo.convertir(bib, tulipSvgData, notasSvgData, tulipTipo, peligro)

    /**
     * Deja el dibujo listo para guardar: solo la primera flecha del dibujo con gps lo conserva
     * y la capa de notas nunca lleva gps (punto 6).
     */
    fun normalizar(d: DibujoV1): DibujoV1 {
        var visto = false
        val flechas = d.dibujo.arrows.map { f ->
            if (f.gps == null) f
            else if (!visto) { visto = true; f }
            else f.copy(gps = null)
        }
        return d.copy(
            dibujo = d.dibujo.copy(arrows = flechas),
            notasCapa = d.notasCapa.copy(arrows = d.notasCapa.arrows.map { if (it.gps != null) it.copy(gps = null) else it })
        )
    }

    /** Texto a guardar en la columna `dibujo` y el peligro calculado. */
    fun paraGuardar(d: DibujoV1): Pair<String, String> {
        val n = normalizar(d)
        return n.aJson() to Peligro.texto(n)
    }
}
