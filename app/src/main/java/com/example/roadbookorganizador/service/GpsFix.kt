package com.example.roadbookorganizador.service

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Origen de una posición que entra al odómetro. */
enum class FuenteGps { RACEBOX, TABLET }

/**
 * Posición GNSS independiente de Android (`android.location.Location`), para que el
 * motor de odometría se pueda probar con tests de JVM comunes.
 *
 * @param timeMs reloj del dispositivo al recibir la posición (ms).
 * @param gpsTimeMs reloj propio del receptor (ms), si lo informa (RaceBox: iTOW). Se usa
 *        para medir el tiempo entre posiciones sin el jitter del Bluetooth.
 */
data class GpsFix(
    val latitud: Double,
    val longitud: Double,
    val altitud: Double = 0.0,
    val velocidadKmh: Float? = null,
    val rumbo: Float? = null,
    val precisionMetros: Float? = null,
    val timeMs: Long,
    val fuente: FuenteGps,
    val gpsTimeMs: Long? = null
)

object Geo {
    private const val WGS84_A = 6378137.0
    private const val WGS84_F = 1.0 / 298.257223563
    private const val WGS84_E2 = WGS84_F * (2 - WGS84_F)

    /**
     * Distancia en metros entre dos puntos cercanos sobre el elipsoide WGS84, usando los radios
     * de curvatura en la latitud media. Para tramos de hasta unos pocos km el error frente a la
     * geodésica exacta es menor a 1 mm por km, mucho mejor que una esfera (que llega a 0,5 %).
     */
    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latMedia = Math.toRadians((lat1 + lat2) / 2.0)
        val s = sin(latMedia)
        val w = 1.0 - WGS84_E2 * s * s
        val radioMeridiano = WGS84_A * (1.0 - WGS84_E2) / (w * sqrt(w))
        val radioNormal = WGS84_A / sqrt(w)
        val dNorte = Math.toRadians(lat2 - lat1) * radioMeridiano
        var dLon = lon2 - lon1
        if (dLon > 180.0) dLon -= 360.0
        if (dLon < -180.0) dLon += 360.0
        val dEste = Math.toRadians(dLon) * radioNormal * cos(latMedia)
        return sqrt(dNorte * dNorte + dEste * dEste)
    }
}
