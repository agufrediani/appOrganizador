package com.example.roadbookorganizador.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

/**
 * RUTA AUTOMÁTICA "estilo navegador" (CAMBIOS_PARA_APPS revisión 22), igual que la plataforma:
 * se pide el camino a Mapbox Directions desde la viñeta anterior (A) hasta el punto marcado (B)
 * y sale una viñeta en cada maniobra (giro, bifurcación, rotonda) con su flecha armada, más una
 * al final del camino sin flecha.
 */
object RutaAutomatica {

    /** El mismo token público de Mapbox que usa la plataforma (si en Ajustes no hay otro). Sale de secretos.properties. */
    val TOKEN_PLATAFORMA: String = com.example.roadbookorganizador.BuildConfig.MAPBOX_TOKEN

    /** Las maniobras a menos de esto de A se descartan. */
    const val MINIMO_DESDE_A_M = 5.0

    /**
     * Una viñeta nueva: dónde va, a cuántos metros de A y qué flecha lleva (null = la última, sin
     * flecha). [camino] = los puntos del camino entre la viñeta anterior y esta (sin las puntas).
     */
    data class Punto(val lat: Double, val lng: Double, val metrosDesdeA: Double, val flecha: String?,
                     val camino: List<Pair<Double, Double>> = emptyList())

    private val TIPOS_MANIOBRA = setOf(
        "turn", "end of road", "fork", "merge", "on ramp", "off ramp",
        "roundabout", "rotary", "roundabout turn", "exit roundabout", "exit rotary"
    )

    /** Flecha armada (id de flechas_armadas.json, sin prohibidos) según el giro en grados (+ der, − izq). */
    fun flechaPara(giro: Double): String {
        val g = abs(giro)
        val lado = if (giro >= 0) "der" else "izq"
        return when {
            g < 20 -> "recta"
            g < 40 -> "${lado}_leve"
            g < 70 -> "${lado}_45"
            g < 115 -> "${lado}_90"
            g < 160 -> "${lado}_135"
            else -> "${lado}_vuelta"
        }
    }

    /** Lleva un ángulo a (−180, 180], como la plataforma. */
    private fun normalizar(a: Double): Double {
        var x = a
        while (x > 180) x -= 360
        while (x <= -180) x += 360
        return x
    }

    /**
     * Lee la respuesta de Mapbox (con `steps=true`) y arma las viñetas: una por maniobra y la
     * última en el final del camino. null si no hay camino.
     */
    fun leer(json: String): List<Punto>? {
        val raiz = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
        if (raiz.texto("code") != "Ok") return null
        val ruta = raiz.getAsJsonArray("routes")?.firstOrNull()?.asJsonObject ?: return null
        val pasos = ruta.getAsJsonArray("legs")?.firstOrNull()?.asJsonObject?.getAsJsonArray("steps") ?: return null
        val puntos = mutableListOf<Punto>()
        var metros = 0.0
        for (el in pasos) {
            val paso = el.asJsonObject
            val m = paso.getAsJsonObject("maneuver") ?: continue
            val tipo = m.texto("type")
            val modificador = m.texto("modifier")
            val esManiobra = tipo != "depart" && tipo != "arrive" && (
                tipo in TIPOS_MANIOBRA ||
                    ((tipo == "continue" || tipo == "new name") && modificador.isNotBlank() && modificador != "straight")
                )
            if (esManiobra) {
                val antes = m.numero("bearing_before") ?: 0.0
                val despues = m.numero("bearing_after") ?: 0.0
                var giro = normalizar(despues - antes)
                if (modificador == "uturn") giro = if (giro >= 0 && abs(giro) < 179) 180.0 else -180.0
                val loc = m.getAsJsonArray("location")
                // Nada pegado al inicio: se descartan las maniobras a menos de 5 m de A
                if (loc != null && loc.size() >= 2 && metros > MINIMO_DESDE_A_M) {
                    puntos += Punto(lat = loc[1].asDouble, lng = loc[0].asDouble, metrosDesdeA = metros, flecha = flechaPara(giro))
                }
            }
            metros += paso.numero("distance") ?: 0.0
        }
        // El final del camino (la última viñeta, sin flecha)
        val total = ruta.numero("distance") ?: metros
        val coords = ruta.getAsJsonObject("geometry")?.getAsJsonArray("coordinates")
        if (coords == null || coords.size() < 2) return null
        val fin = coords.last().asJsonArray
        puntos += Punto(lat = fin[1].asDouble, lng = fin[0].asDouble, metrosDesdeA = total, flecha = null)
        // El pedazo del camino entre cada par de viñetas (para dibujarlo sin volver a pedirlo)
        val geo = coords.map { c -> c.asJsonArray.let { it[1].asDouble to it[0].asDouble } }
        var desde = 0
        return puntos.mapIndexed { k, p ->
            val hasta = if (k == puntos.lastIndex) geo.lastIndex else masCercano(geo, desde, p.lat, p.lng)
            val adentro = if (hasta > desde + 1) geo.subList(desde + 1, hasta) else emptyList()
            desde = hasta
            p.copy(camino = adentro.toList())
        }
    }

    /** El punto del camino más cercano a (lat, lng), buscando hacia adelante desde [desde]. */
    private fun masCercano(geo: List<Pair<Double, Double>>, desde: Int, lat: Double, lng: Double): Int {
        var mejor = desde
        var dMejor = Double.MAX_VALUE
        for (i in desde until geo.size) {
            val dLat = geo[i].first - lat
            val dLng = (geo[i].second - lng) * Math.cos(Math.toRadians(lat))
            val d = dLat * dLat + dLng * dLng
            if (d < dMejor) { dMejor = d; mejor = i }
        }
        return mejor
    }

    /** El camino con las dos viñetas en las puntas, en el JSON que guarda la tablet (`[[lat,lng],...]`). */
    fun caminoJson(latA: Double, lngA: Double, p: Punto): String {
        fun r6(x: Double) = String.format(Locale.US, "%.6f", x)
        val todos = listOf(latA to lngA) + p.camino + listOf(p.lat to p.lng)
        return todos.joinToString(",", "[", "]") { (la, lo) -> "[${r6(la)},${r6(lo)}]" }
    }

    /** Pide el camino de A a B. null si no hay señal o Mapbox no encuentra camino. */
    suspend fun pedir(latA: Double, lngA: Double, latB: Double, lngB: Double, token: String): List<Punto>? = withContext(Dispatchers.IO) {
        val coords = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", lngA, latA, lngB, latB)
        // Primero el token de Ajustes (si hay); si Mapbox no lo acepta, el de la plataforma
        val tokens = listOfNotNull(token.trim().takeIf { it.startsWith("pk.") && it != TOKEN_PLATAFORMA }, TOKEN_PLATAFORMA.takeIf { it.startsWith("pk.") })
        for (t in tokens) {
            val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$coords?geometries=geojson&overview=full&steps=true&language=es&access_token=$t"
            val r = try {
                val c = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 10000 }
                try {
                    if (c.responseCode != 200) null else leer(c.inputStream.bufferedReader().use { it.readText() })
                } finally { c.disconnect() }
            } catch (e: Exception) { null }
            if (r != null) return@withContext r
        }
        null
    }

    private fun JsonObject.texto(k: String): String = try { get(k)?.takeIf { !it.isJsonNull }?.asString ?: "" } catch (e: Exception) { "" }
    private fun JsonObject.numero(k: String): Double? = try { get(k)?.takeIf { !it.isJsonNull }?.asDouble } catch (e: Exception) { null }
}
