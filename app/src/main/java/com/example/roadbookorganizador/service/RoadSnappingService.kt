package com.example.roadbookorganizador.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class SnappedPoint(
    val latitud: Double,
    val longitud: Double,
    val nombreCamino: String,
    val distanciaOriginalMetros: Double
)

data class SnappedRoute(
    val puntos: List<Pair<Double, Double>>,
    val distanciaMetros: Double,
    val duracionSegundos: Double
)

/**
 * Servicio de Snap-to-Road (Ajuste inteligente a la traza y red de caminos).
 * Compatible con:
 * 1. OSRM (Open Source Routing Machine) - 100% libre, sin token ni costo.
 * 2. Mapbox Map Matching / Directions API - Cuando se configura el Token del organizador.
 */
class RoadSnappingService {

    companion object {
        const val DEFAULT_MAPBOX_TOKEN = ""
    }

    private val gson = Gson()

    /**
     * Ajusta un punto geográfico específico (ej. un clic manual o un waypoint)
     * al camino o pista transitable más cercana usando Mapbox Map Matching.
     */
    suspend fun snapPointToNearestRoad(
        lat: Double,
        lng: Double,
        mapboxToken: String? = null
    ): SnappedPoint? = withContext(Dispatchers.IO) {
        val token = mapboxToken?.takeIf { it.isNotBlank() } ?: DEFAULT_MAPBOX_TOKEN

        // 1. Intento primario: Mapbox Map Matching API (radio 150m) si hay token configurado
        if (token.isNotBlank()) {
            try {
            val lng2 = lng + 0.00008
            val lat2 = lat + 0.00008
            val mapboxUrl = String.format(
                Locale.US,
                "https://api.mapbox.com/matching/v5/mapbox/driving/%.6f,%.6f;%.6f,%.6f?radiuses=150;150&geometries=geojson&access_token=%s",
                lng, lat, lng2, lat2, token
            )
            val connection = URL(mapboxUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val root = gson.fromJson(json, JsonObject::class.java)
                if (root.has("code") && root.get("code").asString == "Ok" && root.has("tracepoints")) {
                    val tracepoints = root.getAsJsonArray("tracepoints")
                    if (tracepoints != null && tracepoints.size() > 0 && !tracepoints[0].isJsonNull) {
                        val tp = tracepoints[0].asJsonObject
                        val loc = tp.getAsJsonArray("location")
                        val snappedLng = loc[0].asDouble
                        val snappedLat = loc[1].asDouble
                        val roadName = if (tp.has("name") && !tp.get("name").asString.isNullOrBlank()) {
                            tp.get("name").asString
                        } else {
                            "Camino detectado (Mapbox)"
                        }
                        return@withContext SnappedPoint(
                            latitud = snappedLat,
                            longitud = snappedLng,
                            nombreCamino = roadName,
                            distanciaOriginalMetros = 0.0
                        )
                    }
                }
            }
            } catch (e: Exception) {
                // Continuar al fallback
            }
        }

        // 2. Respaldo secundario: OSRM nearest
        try {
            val urlStr = String.format(
                Locale.US,
                "https://router.project-osrm.org/nearest/v1/driving/%.6f,%.6f",
                lng,
                lat
            )
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val root = gson.fromJson(json, JsonObject::class.java)
                if (root.has("code") && root.get("code").asString == "Ok") {
                    val waypoints = root.getAsJsonArray("waypoints")
                    if (waypoints != null && waypoints.size() > 0) {
                        val wp = waypoints[0].asJsonObject
                        val loc = wp.getAsJsonArray("location")
                        val snappedLng = loc[0].asDouble
                        val snappedLat = loc[1].asDouble
                        val roadName = if (wp.has("name") && !wp.get("name").asString.isNullOrBlank()) {
                            wp.get("name").asString
                        } else {
                            "Camino secundario / Huella"
                        }
                        val distance = if (wp.has("distance")) wp.get("distance").asDouble else 0.0
                        return@withContext SnappedPoint(
                            latitud = snappedLat,
                            longitud = snappedLng,
                            nombreCamino = roadName,
                            distanciaOriginalMetros = distance
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Ajusta una secuencia de puntos de coordenadas para generar una polilínea
     * continua que sigue perfectamente las curvas de la carretera o camino usando Mapbox.
     */
    suspend fun snapTraceToRoad(
        points: List<Pair<Double, Double>>,
        mapboxToken: String? = null
    ): SnappedRoute? = withContext(Dispatchers.IO) {
        if (points.size < 2) return@withContext null
        val token = mapboxToken?.takeIf { it.isNotBlank() } ?: DEFAULT_MAPBOX_TOKEN

        // Subconjunto de muestra para respetar los límites de la API si hay muchos puntos
        val samplePoints = if (points.size > 25) {
            val step = (points.size - 1).toDouble() / 24.0
            val sampled = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until 24) {
                val index = (i * step).toInt().coerceIn(0, points.size - 1)
                sampled.add(points[index])
            }
            sampled.add(points.last())
            sampled
        } else {
            points
        }

        if (token.isNotBlank()) {
            try {
                val coordsStr = samplePoints.joinToString(";") { (lat, lng) ->
                    String.format(Locale.US, "%.6f,%.6f", lng, lat)
                }

                // 1. Intento primario con Mapbox Directions API
                val urlStr = "https://api.mapbox.com/directions/v5/mapbox/driving/$coordsStr?geometries=geojson&overview=full&access_token=$token"

                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

                if (connection.responseCode == 200) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    val root = gson.fromJson(json, JsonObject::class.java)
                    val routes = root.getAsJsonArray("routes")
                    if (routes != null && routes.size() > 0) {
                        val firstRoute = routes[0].asJsonObject
                        val dist = firstRoute.get("distance").asDouble
                        val dur = firstRoute.get("duration").asDouble
                        val geom = firstRoute.getAsJsonObject("geometry")
                        val coords = geom.getAsJsonArray("coordinates")

                        val resultPoints = mutableListOf<Pair<Double, Double>>()
                        coords.forEach { elem ->
                            val pt = elem.asJsonArray
                            val pLng = pt[0].asDouble
                            val pLat = pt[1].asDouble
                            resultPoints.add(Pair(pLat, pLng))
                        }

                        return@withContext SnappedRoute(
                            puntos = resultPoints,
                            distanciaMetros = dist,
                            duracionSegundos = dur
                        )
                    }
                }
            } catch (e: Exception) {
                // Intentar fallback OSRM
            }
        }

        // 2. Respaldo secundario: OSRM
        try {
            val coordsStr = samplePoints.joinToString(";") { (lat, lng) ->
                String.format(Locale.US, "%.6f,%.6f", lng, lat)
            }
            val urlStr = "https://router.project-osrm.org/route/v1/driving/$coordsStr?overview=full&geometries=geojson"
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val root = gson.fromJson(json, JsonObject::class.java)
                val routes = root.getAsJsonArray("routes")
                if (routes != null && routes.size() > 0) {
                    val firstRoute = routes[0].asJsonObject
                    val dist = firstRoute.get("distance").asDouble
                    val dur = firstRoute.get("duration").asDouble
                    val geom = firstRoute.getAsJsonObject("geometry")
                    val coords = geom.getAsJsonArray("coordinates")

                    val resultPoints = mutableListOf<Pair<Double, Double>>()
                    coords.forEach { elem ->
                        val pt = elem.asJsonArray
                        val pLng = pt[0].asDouble
                        val pLat = pt[1].asDouble
                        resultPoints.add(Pair(pLat, pLng))
                    }

                    return@withContext SnappedRoute(
                        puntos = resultPoints,
                        distanciaMetros = dist,
                        duracionSegundos = dur
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
