package com.example.roadbookorganizador.vineta

/**
 * Conversión del formato viejo de la app Kotlin (columnas tulipSvgData y notasSvgData,
 * más tulipTipo y peligro) a la versión 1. ESPECIFICACION_VINETA.md, punto 9.
 */
object ConversionViejo {

    private const val AZUL_VIEJO = "#0284C7"
    private const val FACTOR_ANCHO = 0.6
    private const val FACTOR_ESCALA = 1.3

    fun convertir(
        bib: Biblioteca,
        tulipSvgData: String?,
        notasSvgData: String?,
        tulipTipo: String?,
        peligro: String?
    ): DibujoV1 {
        var dibujo = if (!tulipSvgData.isNullOrBlank()) capaVieja(bib, tulipSvgData, ALTO_DIBUJO, conGps = true) else Capa()

        // tulipTipo solo si el dibujo viejo estaba vacío
        if (tulipSvgData.isNullOrBlank() && !tulipTipo.isNullOrBlank()) {
            dibujo = desdeTulipTipo(bib, tulipTipo.trim())
        }

        var notas = if (!notasSvgData.isNullOrBlank()) capaVieja(bib, notasSvgData, ALTO_NOTAS, conGps = false) else Capa()

        // Peligro del campo viejo → EXCL en la capa de notas, salvo que ya haya uno igual o mayor
        val nivelCampo = Peligro.nivelDeTexto(peligro)
        if (nivelCampo > 0) {
            val nivelDibujado = maxOf(Peligro.nivelDeCapa(dibujo), Peligro.nivelDeCapa(notas))
            if (nivelDibujado < nivelCampo) {
                notas = notas.copy(stamps = notas.stamps + Sello("EXCL_$nivelCampo", 0.15, 0.56, 0.8, 0.0))
            }
        }
        return DibujoV1(dibujo = dibujo, notasCapa = notas)
    }

    /** Lee una capa en formato viejo y la pasa a la versión 1. */
    fun capaVieja(bib: Biblioteca, json: String, alto: Double, conGps: Boolean): Capa {
        @Suppress("UNCHECKED_CAST")
        val raiz = try { JsonLite.parse(json) as? Map<String, Any?> } catch (e: Exception) { null } ?: return Capa()
        val flechas = mutableListOf<Flecha>()
        var gpsAsignado = false

        for (a in raiz.lista("arrows").objetos()) {
            val tienePin = a.bool("hasPin", true)
            val gps = if (conGps && tienePin && !gpsAsignado) {
                gpsAsignado = true
                Gps(t = a.num("pinT", 0.5), lado = if (a.num("pinSide", 1.0) < 0) -1 else 1)
            } else null
            val color = a.txt("color", AZUL_VIEJO)
            flechas.add(
                Flecha(
                    x0 = a.num("x0", 0.5), y0 = a.num("y0", 0.88),
                    x1 = a.num("x1", 0.5), y1 = a.num("y1", 0.60),
                    x2 = a.num("x2", 0.5), y2 = a.num("y2", 0.35),
                    x3 = a.num("x3", 0.5), y3 = a.num("y3", 0.12),
                    width = a.num("width", 10.0) * FACTOR_ANCHO,
                    color = if (color.equals(AZUL_VIEJO, ignoreCase = true)) "#000000" else color,
                    gps = gps
                )
            )
        }

        val sellos = mutableListOf<Sello>()
        for (s in raiz.lista("stamps").objetos()) {
            val codigo = s.txt("type", "")
            val x = s.num("x", 0.5)
            val y = s.num("y", 0.5)
            val escala = s.num("scale", 1.0) * FACTOR_ESCALA
            val rot = s.num("rot", 0.0)
            when (val eq = bib.convertirCodigoApp(codigo)) {
                is Equivalencia.Simbolo -> sellos.add(Sello(eq.codigoOficial, x, y, escala, rot))
                is Equivalencia.Nuevo -> sellos.add(Sello(eq.tipoMientrasTanto, x, y, escala, rot))
                is Equivalencia.FlechaArmadaEq -> armada(bib, eq.codigoArmada, rot, alto, conGps && !gpsAsignado)?.let {
                    if (it.gps != null) gpsAsignado = true
                    flechas.add(it)
                }
                Equivalencia.PalitoGps, Equivalencia.Vacio -> Unit // se eliminan
            }
        }

        val trazos = raiz.lista("strokes").objetos().mapNotNull { t ->
            val pts = t.lista("pts").objetos().map { Punto(it.num("x", 0.0), it.num("y", 0.0)) }
            if (pts.isEmpty()) null
            else Trazo(pts = pts, color = t.txt("color", "#000000"), width = t.num("width", 6.0) * FACTOR_ANCHO)
        }
        return Capa(arrows = flechas, stamps = sellos, strokes = trazos)
    }

    /** Flecha armada girada alrededor del centro del recuadro; gps solo si corresponde. */
    private fun armada(bib: Biblioteca, codigo: String, rot: Double, alto: Double, conGps: Boolean): Flecha? {
        val base = bib.flechasArmadas[codigo]?.flecha ?: return null
        val girada = Geometria.girarFlecha(base, rot, alto)
        return if (conGps) girada else girada.copy(gps = null)
    }

    private fun desdeTulipTipo(bib: Biblioteca, codigo: String): Capa =
        when (val eq = bib.convertirTulipTipo(codigo)) {
            Equivalencia.Vacio, Equivalencia.PalitoGps -> Capa()
            is Equivalencia.FlechaArmadaEq -> Capa(arrows = listOfNotNull(armada(bib, eq.codigoArmada, 0.0, ALTO_DIBUJO, true)))
            is Equivalencia.Simbolo -> Capa(stamps = listOf(Sello(eq.codigoOficial, 0.5, 0.5, 1.3, 0.0)))
            is Equivalencia.Nuevo -> Capa(stamps = listOf(Sello(eq.tipoMientrasTanto, 0.5, 0.5, 1.3, 0.0)))
        }
}

/** Nivel de peligro (punto 11): el más alto entre los símbolos EXCL_n y PELIGRO_n. */
object Peligro {
    private val re = Regex("^(EXCL|PELIGRO)_([123])$")

    fun nivelDeCodigo(codigo: String): Int = re.find(codigo)?.groupValues?.get(2)?.toInt() ?: 0

    fun nivelDeCapa(c: Capa): Int = c.stamps.maxOfOrNull { nivelDeCodigo(it.type) } ?: 0

    fun nivel(d: DibujoV1): Int = maxOf(nivelDeCapa(d.dibujo), nivelDeCapa(d.notasCapa))

    /** Texto del campo peligro de la viñeta: '', '!', '!!' o '!!!'. Se calcula al guardar. */
    fun texto(d: DibujoV1): String = "!".repeat(nivel(d))

    fun nivelDeTexto(peligro: String?): Int = when (peligro?.trim()) {
        "!" -> 1
        "!!" -> 2
        "!!!" -> 3
        else -> 0
    }
}
