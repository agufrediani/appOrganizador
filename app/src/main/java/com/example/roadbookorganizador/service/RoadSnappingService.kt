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
        private const val TAG = "RoadSnappingService"
    }

    private val gson = Gson()

    /**
     * Ajusta un punto geográfico específico al camino o pista transitable más cercana.
     * Cascada: Mapbox (si token) -> OSRM Project -> OSM.de
     */
    suspend fun snapPointToNearestRoad(
        lat: Double,
        lng: Double,
        mapboxToken: String? = null
    ): SnappedPoint? = withContext(Dispatchers.IO) {
        val token = mapboxToken?.takeIf { it.isNotBlank() && it.startsWith("pk.") }

        // 1. Intento primario: Mapbox Map Matching API (radio 150m) si hay token configurado
        if (token != null) {
            try {
                val lng2 = lng + 0.00008
                val lat2 = lat + 0.00008
                val mapboxUrl = String.format(
                    Locale.US,
                    "https://api.mapbox.com/matching/v5/mapbox/driving/%.6f,%.6f;%.6f,%.6f?radiuses=150;150&geometries=geojson&access_token=%s",
                    lng, lat, lng2, lat2, token
                )
                val conn = URL(mapboxUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3500
                conn.readTimeout = 3500
                conn.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
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
                            android.util.Log.d(TAG, "Snap Mapbox exitoso: $roadName")
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
                android.util.Log.w(TAG, "Mapbox Snap falló, pasando a OSRM: ${e.message}")
            }
        }

        // 2. Respaldo secundario: OSRM Project nearest
        val osrmServers = listOf(
            "https://router.project-osrm.org/nearest/v1/driving/%.6f,%.6f",
            "https://routing.openstreetmap.de/routed-car/nearest/v1/driving/%.6f,%.6f"
        )

        for (srv in osrmServers) {
            try {
                val urlStr = String.format(Locale.US, srv, lng, lat)
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3500
                conn.readTimeout = 3500
                conn.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
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
                                "Camino rural / Calzada"
                            }
                            val distance = if (wp.has("distance")) wp.get("distance").asDouble else 0.0
                            android.util.Log.d(TAG, "Snap OSRM exitoso: $roadName (dist: ${distance}m)")
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
                android.util.Log.w(TAG, "OSRM server $srv falló: ${e.message}")
            }
        }

        null
    }

    /**
     * Ajusta una secuencia de coordenadas para generar una polilínea continua
     * que sigue fielmente las curvas de la carretera o camino real.
     * Cascada: Mapbox (si token) -> OSRM Project -> OSM.de
     */
    suspend fun snapTraceToRoad(
        points: List<Pair<Double, Double>>,
        mapboxToken: String? = null
    ): SnappedRoute? = withContext(Dispatchers.IO) {
        if (points.size < 2) return@withContext null
        val token = mapboxToken?.takeIf { it.isNotBlank() && it.startsWith("pk.") }

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

        val coordsStr = samplePoints.joinToString(";") { (lat, lng) ->
            String.format(Locale.US, "%.6f,%.6f", lng, lat)
        }

        // 1. Intento primario con Mapbox Directions API si hay token
        if (token != null) {
            try {
                val urlStr = "https://api.mapbox.com/directions/v5/mapbox/driving/$coordsStr?geometries=geojson&overview=full&access_token=$token"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseRouteGeoJson(json)
                    if (parsed != null) {
                        android.util.Log.d(TAG, "Ruta Mapbox exitosa: ${parsed.puntos.size} puntos, ${parsed.distanciaMetros}m")
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Mapbox Directions falló: ${e.message}")
            }
        }

        // 2. Respaldo secundario: OSRM Project & OSM.de
        val routeServers = listOf(
            "https://router.project-osrm.org/route/v1/driving/$coordsStr?overview=full&geometries=geojson",
            "https://routing.openstreetmap.de/routed-car/route/v1/driving/$coordsStr?overview=full&geometries=geojson"
        )

        for (srv in routeServers) {
            try {
                val conn = URL(srv).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "FredianiRoadbookApp/1.0")

                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseRouteGeoJson(json)
                    if (parsed != null) {
                        android.util.Log.d(TAG, "Ruta OSRM ($srv) exitosa: ${parsed.puntos.size} puntos, ${parsed.distanciaMetros}m")
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Error consultando servidor OSRM $srv: ${e.message}")
            }
        }

        null
    }

    private fun parseRouteGeoJson(json: String): SnappedRoute? {
        return try {
            val root = gson.fromJson(json, JsonObject::class.java)
            if (!root.has("routes")) return null
            val routes = root.getAsJsonArray("routes")
            if (routes == null || routes.size() == 0) return null

            val firstRoute = routes[0].asJsonObject
            val dist = if (firstRoute.has("distance")) firstRoute.get("distance").asDouble else 0.0
            val dur = if (firstRoute.has("duration")) firstRoute.get("duration").asDouble else 0.0
            val geom = firstRoute.getAsJsonObject("geometry") ?: return null
            val coords = geom.getAsJsonArray("coordinates") ?: return null

            val resultPoints = mutableListOf<Pair<Double, Double>>()
            coords.forEach { elem ->
                val pt = elem.asJsonArray
                val pLng = pt[0].asDouble
                val pLat = pt[1].asDouble
                resultPoints.add(Pair(pLat, pLng))
            }

            if (resultPoints.isEmpty()) null
            else SnappedRoute(puntos = resultPoints, distanciaMetros = dist, duracionSegundos = dur)
        } catch (e: Exception) {
            null
        }
    }
}
