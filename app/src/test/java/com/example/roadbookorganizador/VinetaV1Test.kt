package com.example.roadbookorganizador

import com.example.roadbookorganizador.vineta.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Tests del formato de viñeta versión 1 contra los archivos de la especificación
 * (app/src/main/assets/especificacion). ESPECIFICACION_VINETA.md, revisión 4.
 */
class VinetaV1Test {

    private val bib: Biblioteca by lazy {
        val dir = listOf("src/main/assets/especificacion", "app/src/main/assets/especificacion")
            .map { File(it) }.first { it.isDirectory }
        Biblioteca.desdeJson(
            File(dir, "simbolos.json").readText(),
            File(dir, "flechas_armadas.json").readText(),
            File(dir, "equivalencias_app.json").readText()
        )
    }

    private fun cerca(esperado: Double, real: Double, tol: Double = 1e-9) =
        assertTrue("esperado $esperado, real $real", abs(esperado - real) <= tol)

    // ------------------------------------------------------------ biblioteca

    @Test
    fun cargaLaBibliotecaCompleta() {
        assertEquals(101, bib.simbolos.size)
        assertEquals(11, bib.flechasArmadas.size)
        assertEquals(5, bib.categorias.size)
    }

    @Test
    fun todosLosSimbolosSeLeenSinPerderElementos() {
        for (s in bib.listaSimbolos) {
            val esperados = Regex("<(line|circle|ellipse|rect|polygon|path|text)[\\s>]").findAll(s.svg).count()
            assertEquals("Símbolo ${s.codigo}", esperados, s.ops.size)
        }
    }

    @Test
    fun herenciaDeFillNoneDesdeLaRaiz() {
        // EXCL_1: rects con fill propio negro; nada sin fill debe rellenarse
        val ops = bib.simbolo("EXCL_1")!!.ops
        assertTrue(ops.all { it.estilo.fill == "#000000" })
        // Todo elemento sin fill propio hereda "none" de la raíz
        val sinFill = SvgSimple.parse("""<svg viewBox="0 0 100 100" fill="none"><line x1="0" y1="0" x2="10" y2="10" stroke="#000" stroke-width="2"/></svg>""")
        assertNull(sinFill[0].estilo.fill)
        assertEquals("#000", sinFill[0].estilo.stroke)
        // Reglas normales de SVG: sin fill en ningún lado = negro; stroke y grosor se heredan de la raíz
        val porDefecto = SvgSimple.parse("""<svg viewBox="0 0 100 100" stroke="#ff0000" stroke-width="3"><rect x="1" y="1" width="5" height="5"/></svg>""")
        assertEquals("#000000", porDefecto[0].estilo.fill)
        assertEquals("#ff0000", porDefecto[0].estilo.stroke)
        assertEquals(3.0, porDefecto[0].estilo.strokeWidth, 0.0)
    }

    @Test
    fun textosDeSimbolosConFamiliaYLineaDeBase() {
        val textos = bib.listaSimbolos.flatMap { it.ops }.filterIsInstance<OpSvg.Texto>()
        assertEquals(24, textos.size)
        val dz = bib.simbolo("SPEED_DZ")!!.ops.filterIsInstance<OpSvg.Texto>().single()
        assertEquals("DZ", dz.texto)
        assertEquals("middle", dz.anchor)
        assertEquals(900, dz.fontWeight)
        assertEquals(60.0, dz.y, 0.0)
    }

    // ------------------------------------------------------------ JSON v1

    @Test
    fun ejemploDeLaEspecificacionIdaYVuelta() {
        val ejemplo = """{"v":1,"arrows":[{"x0":0.5,"y0":0.88,"x1":0.5,"y1":0.55,"x2":0.5,"y2":0.35,"x3":0.5,"y3":0.15,"width":6,"color":"#000000","gps":{"t":0.5,"lado":1},"id":"a1"}],"stamps":[{"type":"GUARDAGANADO","x":0.72,"y":0.4,"scale":1.3,"rot":0,"id":"s1"}],"strokes":[{"color":"#000000","width":3.5,"pts":[{"x":0.2,"y":0.5},{"x":0.25,"y":0.47}]}],"notasCapa":{"arrows":[],"strokes":[],"stamps":[{"type":"EXCL_2","x":0.15,"y":0.75,"scale":1.3,"rot":0}]}}"""
        val d = DibujoV1.desdeJson(ejemplo)
        assertTrue(DibujoV1.esFormatoNuevo(ejemplo))
        assertEquals(1, d.dibujo.arrows.size)
        assertEquals("a1", d.dibujo.arrows[0].extra["id"]) // el id se conserva
        assertEquals("s1", d.dibujo.stamps[0].extra["id"])
        assertEquals("EXCL_2", d.notasCapa.stamps[0].type)
        // Releer lo escrito da lo mismo
        assertEquals(d, DibujoV1.desdeJson(d.aJson()))
        assertTrue(d.aJson().startsWith("{\"v\":1,"))
    }

    @Test
    fun dibujoVacioSeGuardaComoTextoVacio() {
        assertEquals("", DibujoV1().aJson())
        assertTrue(DibujoV1.desdeJson("").estaVacio())
        assertFalse(DibujoV1.esFormatoNuevo(""))
        assertFalse(DibujoV1.esFormatoNuevo("""{"strokes":[],"stamps":[],"arrows":[]}"""))
    }

    @Test
    fun codigoDesconocidoSeConservaAlGuardar() {
        val d = DibujoV1(dibujo = Capa(stamps = listOf(Sello("APP:CAIRN", 0.3, 0.3), Sello("NO_EXISTE", 0.6, 0.6))))
        val vuelta = DibujoV1.desdeJson(d.aJson())
        assertEquals(listOf("APP:CAIRN", "NO_EXISTE"), vuelta.dibujo.stamps.map { it.type })
    }

    // ------------------------------------------------------------ geometría

    @Test
    fun puntaDeFlechaRectaApuntaParaArriba() {
        val recta = bib.flechasArmadas["recta"]!!.flecha
        val p = Geometria.puntosEnUnidades(recta, ALTO_DIBUJO)
        val t = Geometria.punta(p, 6.0)
        // h = max(10, 1.8·6) = 10.8; el vértice principal queda 10.8 unidades arriba de P3
        cerca(50.0, t[0].x, 1e-9)
        cerca(10.0 - 10.8, t[0].y, 1e-9)
    }

    @Test
    fun puntaConP3IgualP2UsaP1yLuegoP0() {
        val f = Flecha(0.5, 0.9, 0.5, 0.9, 0.8, 0.5, 0.8, 0.5) // P3 = P2, P1 = P0
        val t = Geometria.punta(Geometria.puntosEnUnidades(f, 100.0), 6.0)
        // Dirección P3 - P1 = (30, -40): la punta avanza en esa dirección
        val dx = t[0].x - 80.0
        val dy = t[0].y - 50.0
        cerca(0.6, dx / hypot(dx, dy), 1e-9)
        cerca(-0.8, dy / hypot(dx, dy), 1e-9)
    }

    @Test
    fun palitoDelGpsSegunLaFormula() {
        val recta = bib.flechasArmadas["recta"]!!.flecha
        val p = Geometria.puntosEnUnidades(recta, ALTO_DIBUJO)
        val pal = Geometria.palito(p, 6.0, Gps(0.5, 1))
        // Recta vertical hacia arriba: T = (0,-1), N (lado 1) = (1, 0), D = N cos35 - T sin35
        val largo = 9.0 // max(7, 1.5·6)
        cerca(50.0, pal.desde.x, 1e-9)
        cerca(50.0, pal.desde.y, 1e-9)
        cerca(50.0 + largo * Math.cos(Math.toRadians(35.0)), pal.hasta.x, 1e-9)
        cerca(50.0 + largo * Math.sin(Math.toRadians(35.0)), pal.hasta.y, 1e-9)
        cerca(1.8, pal.grosor, 1e-9) // max(1.6, 0.3·6)
    }

    @Test
    fun gpsEnLaPrimeraFlechaQueLoTenga() {
        val a = Flecha(0.5, 0.9, 0.5, 0.6, 0.5, 0.4, 0.5, 0.1)
        val b = a.copy(gps = Gps(0.3, -1))
        assertEquals(1 to Gps(0.3, -1), Geometria.flechaConGps(listOf(a, b)))
        assertEquals(0 to Gps(0.5, 1), Geometria.flechaConGps(listOf(a, a)))
        assertNull(Geometria.flechaConGps(emptyList()))
    }

    @Test
    fun girarFlecha90GradosHorario() {
        val recta = bib.flechasArmadas["recta"]!!.flecha
        val g = Geometria.girarFlecha(recta, 90.0, ALTO_DIBUJO)
        // La punta (0.5, 0.1) girada 90° horario alrededor del centro queda a la derecha (0.9, 0.5)
        cerca(0.9, g.x3, 1e-9)
        cerca(0.5, g.y3, 1e-9)
        cerca(0.1, g.x0, 1e-9)
    }

    // ------------------------------------------------------------ formato viejo (punto 9)

    @Test
    fun conversionDeFlechaViejaConPinYColorAzul() {
        val viejo = """{"strokes":[],"stamps":[],"arrows":[
            {"x0":0.5,"y0":0.88,"x1":0.5,"y1":0.6,"x2":0.5,"y2":0.35,"x3":0.5,"y3":0.12,"width":10,"color":"#0284C7","hasPin":true,"pinT":0.4,"pinSide":-1},
            {"x0":0.2,"y0":0.8,"x1":0.2,"y1":0.6,"x2":0.3,"y2":0.4,"x3":0.4,"y3":0.2,"width":5,"color":"#FF0000","hasPin":true,"pinT":0.6,"pinSide":1}]}"""
        val d = ConversionViejo.convertir(bib, viejo, null, "DERECHA_90", null)
        val f = d.dibujo.arrows
        assertEquals(2, f.size)
        cerca(6.0, f[0].width) // 10 × 0.6
        assertEquals("#000000", f[0].color) // azul por defecto → negro
        assertEquals(Gps(0.4, -1), f[0].gps)
        assertEquals("#FF0000", f[1].color) // otro color se conserva
        assertNull(f[1].gps) // gps solo en la primera
        cerca(3.0, f[1].width)
        // Con dibujo, tulipTipo no se usa
        assertTrue(d.dibujo.stamps.isEmpty())
    }

    @Test
    fun conversionDeSellosViejos() {
        val viejo = """{"strokes":[{"color":"#000000","width":6,"pts":[{"x":0.1,"y":0.1},{"x":0.2,"y":0.2}]}],"arrows":[],"stamps":[
            {"type":"TRANQUERA","x":0.7,"y":0.3,"scale":1.0,"rot":15},
            {"type":"CAIRN","x":0.2,"y":0.2,"scale":2.0,"rot":0},
            {"type":"CRUZ_ROJA","x":0.4,"y":0.4,"scale":1.0,"rot":0},
            {"type":"PIN_KM","x":0.5,"y":0.5,"scale":1.0,"rot":0},
            {"type":"RECTA","x":0.3,"y":0.8,"scale":1.0,"rot":90}]}"""
        val d = ConversionViejo.convertir(bib, viejo, null, null, null)
        val s = d.dibujo.stamps
        assertEquals(listOf("FIA_TRANQUERA", "APP:CAIRN", "APP:CRUZ_ROJA"), s.map { it.type })
        cerca(1.3, s[0].scale)
        cerca(15.0, s[0].rot)
        cerca(2.6, s[1].scale)
        // RECTA girada 90°: flecha armada recta girada, que apunta a la derecha, con gps
        val f = d.dibujo.arrows.single()
        cerca(0.9, f.x3, 1e-9)
        cerca(0.5, f.y3, 1e-9)
        assertEquals(Gps(0.5, 1), f.gps)
        cerca(3.6, d.dibujo.strokes.single().width, 1e-9)
    }

    @Test
    fun tulipTipoSinDibujo() {
        fun conv(t: String) = ConversionViejo.convertir(bib, "", null, t, null).dibujo
        assertTrue(conv("BLANCO").estaVacia())
        assertTrue(conv("RECTA").estaVacia())
        assertEquals(bib.flechasArmadas["der_leve"]!!.flecha, conv("DERECHA_1").arrows.single())
        assertEquals(bib.flechasArmadas["der_90"]!!.flecha, conv("DERECHA_90").arrows.single())
        assertEquals(bib.flechasArmadas["izq_135"]!!.flecha, conv("IZQUIERDA_4").arrows.single())
        assertEquals(Sello("ROTONDA", 0.5, 0.5, 1.3, 0.0), conv("ROTONDA_2").stamps.single())
        assertEquals(Sello("STOP", 0.5, 0.5, 1.3, 0.0), conv("STOP_FIN").stamps.single())
        assertEquals("FIA_CH", conv("CONTROL_HORARIO").stamps.single().type)
        assertEquals("APP:CHICANA_DER", conv("CHICANA_DER").stamps.single().type)
        assertEquals("APP:LARGADA", conv("LARGADA").stamps.single().type)
        assertEquals("APP:CRUZ_ROJA", conv("CRUZ_ROJA").stamps.single().type)
    }

    @Test
    fun notasViejasPasanACapaDeNotasSinGps() {
        val notas = """{"strokes":[],"stamps":[{"type":"VADO","x":0.5,"y":0.5,"scale":1.0,"rot":0},{"type":"FLECHA","x":0.5,"y":0.5,"scale":1.0,"rot":0}]}"""
        val d = ConversionViejo.convertir(bib, "", notas, null, null)
        assertEquals("VADO", d.notasCapa.stamps.single().type)
        assertNull(d.notasCapa.arrows.single().gps)
    }

    @Test
    fun peligroViejoPasaAExclSinDuplicar() {
        val d = ConversionViejo.convertir(bib, "", null, null, "!!")
        assertEquals(Sello("EXCL_2", 0.15, 0.56, 0.8, 0.0), d.notasCapa.stamps.single())
        // Ya había un PELIGRO_3 dibujado: no se agrega otro
        val conPeligro = """{"arrows":[],"strokes":[],"stamps":[{"type":"PELIGRO_3","x":0.2,"y":0.2,"scale":1.0,"rot":0}]}"""
        val d2 = ConversionViejo.convertir(bib, conPeligro, null, null, "!")
        assertTrue(d2.notasCapa.stamps.isEmpty())
        assertEquals("!!!", Peligro.texto(d2))
    }

    @Test
    fun peligroSeCalculaDesdeLosSimbolos() {
        val d = DibujoV1(
            dibujo = Capa(stamps = listOf(Sello("PELIGRO_1", 0.5, 0.5))),
            notasCapa = Capa(stamps = listOf(Sello("EXCL_2", 0.15, 0.56, 0.8)))
        )
        assertEquals("!!", Peligro.texto(d))
        assertEquals("", Peligro.texto(DibujoV1()))
    }

    @Test
    fun todasLasEquivalenciasApuntanAAlgoQueExiste() {
        val dir = listOf("src/main/assets/especificacion", "app/src/main/assets/especificacion").map { File(it) }.first { it.isDirectory }
        @Suppress("UNCHECKED_CAST")
        val eq = JsonLite.parse(File(dir, "equivalencias_app.json").readText()) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val codigos = (eq["equivalencias"] as List<Map<String, Any?>>).flatMap { it["codigos_app"] as List<String> }
        for (c in codigos) {
            when (val r = bib.convertirCodigoApp(c)) {
                is Equivalencia.Simbolo -> assertTrue("$c → ${r.codigoOficial}", bib.simbolo(r.codigoOficial) != null)
                is Equivalencia.FlechaArmadaEq -> assertTrue("$c → ${r.codigoArmada}", bib.flechasArmadas.containsKey(r.codigoArmada))
                is Equivalencia.Nuevo -> assertTrue(r.tipoMientrasTanto.startsWith("APP:"))
                else -> Unit
            }
        }
    }

    // ------------------------------------------------------------ carteles (punto 10)

    @Test
    fun formatosDeCarteles() {
        assertEquals("2:05", Carteles.formatoTiempo(125.0))
        assertEquals("0:09", Carteles.formatoTiempo(9.0))
        assertEquals("52,5", Carteles.formatoVelocidad(52.5))
        assertEquals("60", Carteles.formatoVelocidad(60.0))
        assertEquals("52,5", Carteles.formatoVelocidad(52.46))
        assertEquals(0.8, Carteles.factorTamanoVelocidad("100"), 0.0)
        assertEquals(0.66, Carteles.factorTamanoVelocidad("52,5"), 0.0)
    }

    @Test
    fun franjaDelTramoYAvisoDeCambio() {
        val v = listOf(
            ValoresCartel.desdeCampos(125.0, 52.5, 60.0), // primera con valores: gris
            ValoresCartel.desdeCampos(125.0, 52.5, 60.0), // igual: blanco
            ValoresCartel.desdeCampos(125.0, 55.0, 60.0), // cambia el promedio: gris
            ValoresCartel.desdeCampos(0.0, 0.0, 0.0)      // sin valores: celdas vacías
        )
        val f = Carteles.franjasDelTramo(v)
        assertEquals(FranjaCarteles("2:05", "52,5", "60", true), f[0])
        assertFalse(f[1]!!.gris)
        assertTrue(f[2]!!.gris)
        assertNull(f[3]!!.tiempo)
        // Ninguna viñeta con valores: la franja no existe
        assertTrue(Carteles.franjasDelTramo(listOf(ValoresCartel.desdeCampos(0.0, 0.0, 0.0))).all { it == null })
        // Primera sin valores: blanco
        assertFalse(Carteles.franjasDelTramo(listOf(ValoresCartel.desdeCampos(0.0, 0.0, 0.0), v[0]))[0]!!.gris)
    }

    @Test
    fun jsonLiteConEscapesYUnicode() {
        val s = """{"a":"línea \"uno\"\nñ","b":[1,2.5,-3e2,true,false,null],"c":{}}"""
        @Suppress("UNCHECKED_CAST")
        val m = JsonLite.parse(s) as Map<String, Any?>
        assertEquals("línea \"uno\"\nñ", m["a"])
        assertEquals(listOf(1.0, 2.5, -300.0, true, false, null), m["b"])
        assertEquals(m, JsonLite.parse(JsonLite.write(m)))
    }

    // ------------------------------------------------------------ columna dibujo (parte B)

    @Test
    fun dibujoNullConvierteYVacioQuedaVacio() {
        val viejo = """{"strokes":[],"stamps":[{"type":"CONTROL_HORARIO","x":0.3,"y":0.4,"scale":1.0,"rot":0}],"arrows":[]}"""
        // Nunca guardada en v1: se convierte desde las columnas viejas
        val convertido = VinetaDibujo.leer(bib, null, viejo, "", "BLANCO", "")
        assertEquals("FIA_CH", convertido.dibujo.stamps.single().type)
        // Guardada en v1 como vacía: no resucita el dibujo viejo
        val vacio = VinetaDibujo.leer(bib, "", viejo, "", "BLANCO", "!!")
        assertTrue(vacio.estaVacio())
    }

    @Test
    fun alGuardarSoloLaPrimeraFlechaConGpsLoConservaYNotasSinGps() {
        val f = Flecha(0.5, 0.9, 0.5, 0.6, 0.5, 0.4, 0.5, 0.1)
        val d = DibujoV1(
            dibujo = Capa(arrows = listOf(f, f.copy(gps = Gps(0.3, -1)), f.copy(gps = Gps(0.7, 1)))),
            notasCapa = Capa(arrows = listOf(f.copy(gps = Gps(0.5, 1))), stamps = listOf(Sello("EXCL_2", 0.1, 0.5)))
        )
        val (json, peligro) = VinetaDibujo.paraGuardar(d)
        val leido = DibujoV1.desdeJson(json)
        assertEquals(listOf(null, Gps(0.3, -1), null), leido.dibujo.arrows.map { it.gps })
        assertNull(leido.notasCapa.arrows.single().gps)
        assertEquals("!!", peligro)
    }

    @Test
    fun guardarVacioDaTextoVacioYSinPeligro() {
        assertEquals("" to "", VinetaDibujo.paraGuardar(DibujoV1()))
    }

    // ------------------------------------------------------------ letra de las notas (punto 12)

    @Test
    fun notasLetraSeLeeYSeGuarda() {
        val d = DibujoV1.desdeJson("""{"v":1,"arrows":[],"stamps":[],"strokes":[],"notasLetra":7.5}""")
        assertEquals(7.5, d.notasLetra!!, 0.0)
        assertTrue(d.extra.isEmpty())
        assertEquals(7.5, DibujoV1.desdeJson(d.aJson()).notasLetra!!, 0.0)
        // Sin notasLetra = automático, y no se escribe
        val auto = DibujoV1.desdeJson("""{"v":1,"arrows":[],"stamps":[],"strokes":[]}""")
        assertNull(auto.notasLetra)
        assertFalse(DibujoV1(dibujo = Capa(stamps = listOf(Sello("STOP", 0.5, 0.5)))).aJson().contains("notasLetra"))
    }

    @Test
    fun botonesDeLetraRespetanRangoYPaso() {
        assertEquals(20.0, LetraNotas.mas(20.0), 0.0)
        assertEquals(3.0, LetraNotas.menos(3.0), 0.0)
        assertEquals(8.0, LetraNotas.mas(7.5), 0.0)
        assertEquals(7.5, LetraNotas.ajustar(7.4), 0.0)
    }

    @Test
    fun letraAutomaticaBuscaDe14ParaAbajo() {
        // alto del texto = 3 líneas × letra × 1,2
        val alto = { l: Double -> 3 * l * LetraNotas.INTERLINEADO }
        assertEquals(14.0, LetraNotas.automatica(100.0, alto), 0.0)
        assertEquals(8.0, LetraNotas.automatica(29.0, alto), 0.0)   // 8 → 28,8 entra; 8,5 → 30,6 no
        assertEquals(4.5, LetraNotas.automatica(5.0, alto), 0.0)    // no entra ni con 4,5
    }

    @Test
    fun lineasCortanEntrePalabrasYPartenPalabrasLargas() {
        val ancho = { t: String -> t.length.toDouble() } // 1 unidad por letra
        assertEquals(listOf("hola que", "tal"), LetraNotas.lineas("hola que tal", 8.0, ancho))
        assertEquals(listOf("abcde", "fgh x"), LetraNotas.lineas("abcdefgh x", 5.0, ancho))
        assertEquals(listOf("abcde", "fgh", "xyz"), LetraNotas.lineas("abcdefgh xyz", 5.0, ancho))
        assertEquals(listOf("uno", "", "dos"), LetraNotas.lineas("uno\n\ndos", 10.0, ancho))
        assertTrue(LetraNotas.lineas("", 10.0, ancho).isEmpty())
    }

    @Test
    fun lugarDelTextoYTextoCompleto() {
        assertEquals(96.0, LetraNotas.anchoDisponible(), 0.0)
        assertEquals(60.0, LetraNotas.altoDisponible(false), 0.0)
        assertEquals(46.0, LetraNotas.altoDisponible(true), 0.0)
        assertEquals("a\nb", LetraNotas.textoCompleto("a", "b"))
        assertEquals("b", LetraNotas.textoCompleto("", "b"))
        assertEquals("a", LetraNotas.textoCompleto("a"))
    }

    @Test
    fun letraConValorFijoOAutomatica() {
        // cada letra mide 0,6 × tamaño
        val medir = { t: String, l: Double -> t.length * 0.6 * l }
        assertEquals(8.0, LetraNotas.letra("x", 8.0, false, medir), 0.0)
        // Texto corto: entra con 14
        assertEquals(14.0, LetraNotas.letra("BTW", null, false, medir), 0.0)
        // Con carteles hay menos alto: 4 líneas de 14 (67,2) no entran en 46
        val largo = "uno\ndos\ntres\ncuatro"
        val conCarteles = LetraNotas.letra(largo, null, true, medir)
        assertTrue(conCarteles < 14.0 && 4 * conCarteles * 1.2 <= 46.0 && 4 * (conCarteles + 0.5) * 1.2 > 46.0)
    }
}

