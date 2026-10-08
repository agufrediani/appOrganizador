package com.example.roadbookorganizador

import com.example.roadbookorganizador.service.FuenteGps
import com.example.roadbookorganizador.service.Geo
import com.example.roadbookorganizador.service.GpsFix
import com.example.roadbookorganizador.service.OdometerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tests del motor de odometría con recorridos sintéticos.
 * Cuando haya recorridos reales grabados (GPX del RaceBox y de la tablet), sumarlos acá
 * para comparar el kilometraje antes y después de cada cambio.
 */
class OdometerEngineTest {

    // ---------------------------------------------------------------- utilidades

    private val a = 6378137.0
    private val e2 = (1 / 298.257223563) * (2 - 1 / 298.257223563)

    /** Desplaza un punto [metros] hacia el rumbo [azGrados] (aproximación local WGS84). */
    private fun desplazar(lat: Double, lon: Double, azGrados: Double, metros: Double): Pair<Double, Double> {
        val phi = Math.toRadians(lat)
        val s = sin(phi)
        val w = 1 - e2 * s * s
        val m = a * (1 - e2) / (w * sqrt(w))
        val n = a / sqrt(w)
        val az = Math.toRadians(azGrados)
        val dLat = Math.toDegrees(metros * cos(az) / m)
        val dLon = Math.toDegrees(metros * sin(az) / (n * cos(phi)))
        return Pair(lat + dLat, lon + dLon)
    }

    /** Recorrido en línea recta a velocidad constante. */
    private fun recta(
        velKmh: Double,
        hz: Int,
        segundos: Double,
        fuente: FuenteGps,
        lat0: Double = -33.0450,
        lon0: Double = -61.1650,
        az: Double = 37.0,
        t0: Long = 1_000_000L,
        precision: Float = if (fuente == FuenteGps.RACEBOX) 0.5f else 4f
    ): List<GpsFix> {
        val n = (segundos * hz).toInt()
        val paso = velKmh / 3.6 / hz
        val dtMs = 1000L / hz
        return (0..n).map { i ->
            val (lat, lon) = desplazar(lat0, lon0, az, paso * i)
            GpsFix(
                latitud = lat,
                longitud = lon,
                velocidadKmh = velKmh.toFloat(),
                rumbo = az.toFloat(),
                precisionMetros = precision,
                timeMs = t0 + i * dtMs,
                fuente = fuente,
                gpsTimeMs = if (fuente == FuenteGps.RACEBOX) t0 + i * dtMs else null
            )
        }
    }

    private fun OdometerEngine.totalMetros() = state.value.odometroTotalKm * 1000.0

    // ---------------------------------------------------------------- distancia

    @Test
    fun distanciaCoincideConGeodesicaWgs84() {
        // Valores de referencia calculados con GeographicLib (geodésica exacta WGS84)
        assertEquals(1000.0, Geo.distanciaMetros(-33.045, -61.165, -33.03598328842337, -61.165), 0.01)
        assertEquals(1000.0, Geo.distanciaMetros(-33.045, -61.165, -33.04499954063787, -61.154294020507464), 0.01)
        assertEquals(500.0, Geo.distanciaMetros(-31.42, -64.19, -31.416811237854787, -64.18628176450758), 0.01)
    }

    @Test
    fun tablet1HzUnKilometro() {
        val engine = OdometerEngine()
        recta(60.0, 1, 60.0, FuenteGps.TABLET).forEach { engine.procesarFix(it) }
        assertEquals(1000.0, engine.totalMetros(), 1.0)
    }

    @Test
    fun raceBox25HzUnKilometro() {
        val engine = OdometerEngine()
        engine.setRaceBoxBleConectado(true)
        recta(90.0, 25, 40.0, FuenteGps.RACEBOX).forEach { engine.procesarFix(it) }
        assertEquals(1000.0, engine.totalMetros(), 0.5)
    }

    @Test
    fun factorDeCalibracionSeAplica() {
        val engine = OdometerEngine()
        engine.setFactorCalibracion(1.02)
        recta(60.0, 1, 60.0, FuenteGps.TABLET).forEach { engine.procesarFix(it) }
        assertEquals(1020.0, engine.totalMetros(), 1.0)
    }

    // ---------------------------------------------------------------- filtros

    @Test
    fun vehiculoDetenidoConDerivaNoSuma() {
        val engine = OdometerEngine()
        val rnd = Random(7)
        var lat = -33.0450
        var lon = -61.1650
        repeat(600) { i -> // 10 minutos detenido, el GPS deriva de a centímetros
            val (nLat, nLon) = desplazar(lat, lon, rnd.nextDouble() * 360, rnd.nextDouble() * 0.4)
            lat = nLat; lon = nLon
            engine.procesarFix(
                GpsFix(lat, lon, velocidadKmh = (rnd.nextDouble() * 1.0).toFloat(), precisionMetros = 4f,
                    timeMs = 1_000_000L + i * 1000L, fuente = FuenteGps.TABLET)
            )
        }
        assertTrue("Detenido sumó ${engine.totalMetros()} m", engine.totalMetros() < 1.0)
    }

    @Test
    fun posicionConPrecisionMalaSeDescarta() {
        val engine = OdometerEngine()
        val ruta = recta(60.0, 1, 60.0, FuenteGps.TABLET)
        ruta.forEachIndexed { i, f ->
            // Del segundo 20 al 30 la precisión es de 50 m y el punto está corrido 40 m
            val fix = if (i in 20..30) {
                val (lat, lon) = desplazar(f.latitud, f.longitud, 127.0, 40.0)
                f.copy(latitud = lat, longitud = lon, precisionMetros = 50f)
            } else f
            engine.procesarFix(fix)
        }
        assertEquals(1000.0, engine.totalMetros(), 1.0)
        assertEquals(11, engine.state.value.posicionesDescartadasPrecision)
    }

    @Test
    fun saltoFalsoSeIgnora() {
        val engine = OdometerEngine()
        val ruta = recta(60.0, 1, 60.0, FuenteGps.TABLET)
        ruta.forEachIndexed { i, f ->
            val fix = if (i == 30) {
                val (lat, lon) = desplazar(f.latitud, f.longitud, 127.0, 300.0)
                f.copy(latitud = lat, longitud = lon) // salto de 300 m con buena precisión
            } else f
            engine.procesarFix(fix)
        }
        assertEquals(1000.0, engine.totalMetros(), 1.0)
        assertEquals(1, engine.state.value.saltosDescartados)
    }

    @Test
    fun referenciaMalaSeRecuperaSinTrabarse() {
        val engine = OdometerEngine()
        // Primer punto falso a 2 km del recorrido, auto detenido
        val (latMala, lonMala) = desplazar(-33.0450, -61.1650, 200.0, 2000.0)
        engine.procesarFix(
            GpsFix(latMala, lonMala, velocidadKmh = 0f, precisionMetros = 4f, timeMs = 999_000L, fuente = FuenteGps.TABLET)
        )
        recta(60.0, 1, 60.0, FuenteGps.TABLET).forEach { engine.procesarFix(it) }
        // Se pierden solo los primeros segundos hasta re-anclar, nunca se suman los 2 km
        val total = engine.totalMetros()
        assertTrue("Total $total m", total in 900.0..1000.5)
    }

    @Test
    fun restaurarRetomaElOdometroSinSumarElSalto() {
        val engine = OdometerEngine()
        engine.restaurar(12.345, 0.678)
        // La primera posición tras reiniciar está lejos de la última conocida: no se suma
        recta(60.0, 1, 60.0, FuenteGps.TABLET).forEach { engine.procesarFix(it) }
        assertEquals(12345.0 + 1000.0, engine.totalMetros(), 1.0)
        assertEquals(678.0 + 1000.0, engine.state.value.odometroParcialKm * 1000.0, 1.0)
    }

    // ---------------------------------------------------------------- RaceBox

    @Test
    fun reconexionDelRaceBoxNoPierdeDistancia() {
        val engine = OdometerEngine()
        engine.setRaceBoxBleConectado(true)
        val ruta = recta(100.0, 25, 36.0, FuenteGps.RACEBOX) // 1000 m
        // Corte de Bluetooth de 5 s (125 paquetes) en la mitad del recorrido
        ruta.filterIndexed { i, _ -> i !in 400 until 525 }.forEach { engine.procesarFix(it) }
        assertEquals(1000.0, engine.totalMetros(), 0.5)
    }

    @Test
    fun tabletIgnoradaConRaceBoxConectado() {
        val engine = OdometerEngine()
        engine.setRaceBoxBleConectado(true)
        recta(60.0, 1, 60.0, FuenteGps.TABLET).forEach { assertFalse(engine.procesarFix(it)) }
        assertEquals(0.0, engine.totalMetros(), 0.0)
    }

    @Test
    fun segundaDesconexionNoCancelaLaPregunta() {
        val engine = OdometerEngine()
        engine.setRaceBoxBleConectado(true)
        engine.setRaceBoxBleConectado(false)
        engine.setRaceBoxBleConectado(false) // segundo aviso (otro observador del mismo estado)
        assertTrue(engine.state.value.preguntarCambioATablet)
    }

    @Test
    fun tabletCuentaSoloTrasAceptarElCambio() {
        val engine = OdometerEngine()
        engine.setRaceBoxBleConectado(true)
        engine.setRaceBoxBleConectado(false)
        val ruta = recta(60.0, 1, 60.0, FuenteGps.TABLET)
        ruta.take(30).forEach { engine.procesarFix(it) }
        assertEquals(0.0, engine.totalMetros(), 0.0)
        engine.aceptarFallbackTablet()
        ruta.drop(30).forEach { engine.procesarFix(it) }
        assertEquals(500.0, engine.totalMetros(), 1.0)
    }

    @Test
    fun dosHilosEnParaleloNoCorrompenElTotal() {
        val engine = OdometerEngine()
        engine.setRaceBoxBleConectado(true)
        val raceBox = recta(90.0, 25, 40.0, FuenteGps.RACEBOX)
        val tablet = recta(90.0, 1, 40.0, FuenteGps.TABLET, az = 200.0)
        val t1 = Thread { raceBox.forEach { engine.procesarFix(it) } }
        val t2 = Thread { repeat(50) { tablet.forEach { engine.procesarFix(it) } } }
        t1.start(); t2.start(); t1.join(); t2.join()
        assertEquals(1000.0, engine.totalMetros(), 0.5)
    }
}
