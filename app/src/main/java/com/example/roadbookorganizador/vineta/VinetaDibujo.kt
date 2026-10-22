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
        else Formatos.deV1(ConversionViejo.convertir(bib, tulipSvgData, notasSvgData, tulipTipo, peligro))

    /**
     * Texto a guardar en la columna `dibujo` (siempre "v": 3) y el peligro calculado.
     * No se toca nada más: nunca se pierde un dato al guardar (si hubiera más de una flecha
     * con gps, se conservan; al dibujar vale la primera, punto 6).
     */
    fun paraGuardar(d: DibujoV1): Pair<String, String> = d.aJson() to Peligro.texto(d)
}
