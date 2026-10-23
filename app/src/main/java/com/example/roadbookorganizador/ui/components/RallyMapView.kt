package com.example.roadbookorganizador.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.roadbookorganizador.data.local.entity.PuntoInteresEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.service.RoadSnappingService

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RallyMapView(
    latitud: Double,
    longitud: Double,
    rumbo: Float,
    puntos: List<PuntoInteresEntity> = emptyList(),
    indicaciones: List<VinetaEntity> = emptyList(),
    mapboxToken: String = "",
    snapToRoadInitial: Boolean = true,
    vinetaSeleccionadaId: Long? = null,
    /** "PE" o "ENLACE": define el color de la línea, como la plataforma (revisión 15). */
    tipoTramo: String = "PE",
    modifier: Modifier = Modifier,
    onMapClick: ((Double, Double) -> Unit)? = null,
    onMapClickWithRoad: ((Double, Double, String) -> Unit)? = null,
    onMapClickWithRoadAndMode: ((Double, Double, String, Boolean) -> Unit)? = null,
    /** El mapa consiguió el camino entre la viñeta anterior y esta: se guarda para no volver a pedirlo. */
    onSegmentoCamino: ((vinetaId: Long, geometriaJson: String) -> Unit)? = null,
    /**
     * Ruta automática prendida: el toque va con el punto exacto (sin pegarlo antes al camino ni
     * como fuera de camino), igual que el doble clic de la plataforma.
     */
    rutaAutomatica: Boolean = false
) {
    val onSegmentoActual by rememberUpdatedState(onSegmentoCamino)
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }
    val roadSnappingService = remember { RoadSnappingService() }

    // Centrar en la indicación seleccionada si cambia
    LaunchedEffect(vinetaSeleccionadaId, isMapLoaded) {
        if (isMapLoaded && webViewRef != null && vinetaSeleccionadaId != null) {
            val v = indicaciones.find { it.id == vinetaSeleccionadaId }
            if (v != null && v.latitud != 0.0 && v.longitud != 0.0) {
                val script = "if (typeof focusIndicacion === 'function') { focusIndicacion(${v.latitud}, ${v.longitud}, ${v.numero}); }"
                webViewRef?.evaluateJavascript(script, null)
            }
        }
    }

    // Si las coordenadas están en 0.0, centramos por defecto en las sierras de Córdoba / Argentina
    val effectiveLat = if (latitud != 0.0) latitud else -31.4201
    val effectiveLng = if (longitud != 0.0) longitud else -64.1888

    // Actualizar posición del vehículo dinámicamente vía JavaScript
    LaunchedEffect(latitud, longitud, rumbo, isMapLoaded) {
        if (isMapLoaded && webViewRef != null) {
            val script = "if (typeof updateVehicle === 'function') { updateVehicle($effectiveLat, $effectiveLng, $rumbo); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    LaunchedEffect(rutaAutomatica, isMapLoaded) {
        if (isMapLoaded) webViewRef?.evaluateJavascript("RUTA_AUTO = ${if (rutaAutomatica) "true" else "false"};", null)
    }

    // Actualizar marcadores de POIs dinámicamente
    LaunchedEffect(puntos, isMapLoaded) {
        if (isMapLoaded && webViewRef != null) {
            val jsonPuntos = StringBuilder("[")
            puntos.forEachIndexed { index, p ->
                val coords = p.coordenadasGeoJson?.takeIf { it.trim().startsWith("[") } ?: "[]"
                jsonPuntos.append("""{"lat": ${p.latitud}, "lng": ${p.longitud}, "nombre": "${p.nombre.replace("\"", "'")}", "tipo": "${p.tipo}", "forma": "${p.forma}", "coords": $coords}""")
                if (index < puntos.size - 1) jsonPuntos.append(",")
            }
            jsonPuntos.append("]")
            val script = "if (typeof updatePOIs === 'function') { updatePOIs($jsonPuntos); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // Actualizar marcadores de Indicaciones y traza en tiempo real
    LaunchedEffect(indicaciones, isMapLoaded, tipoTramo) {
        if (isMapLoaded && webViewRef != null) {
            val jsonIndicaciones = StringBuilder("[")
            var anteriorConCoord: VinetaEntity? = null
            indicaciones.forEachIndexed { index, v ->
                val safeNota = v.informacion.replace("\"", "'").replace("\n", " ")
                // Camino guardado desde la anterior: solo si empieza y termina donde están hoy las dos viñetas
                val geo = anteriorConCoord?.let { a -> geometriaVigente(v.geometriaDesdeAnterior, a, v) } ?: "null"
                if (!(v.latitud == 0.0 && v.longitud == 0.0)) anteriorConCoord = v
                jsonIndicaciones.append("""{"id": ${v.id}, "num": ${v.numero}, "km": ${v.distanciaTotal}, "lat": ${v.latitud}, "lng": ${v.longitud}, "dibujo": "${v.tulipTipo}", "nota": "$safeNota", "esOffRoad": ${v.esOffRoad}, "geo": $geo}""")
                if (index < indicaciones.size - 1) jsonIndicaciones.append(",")
            }
            jsonIndicaciones.append("]")
            val tipo = if (tipoTramo.equals("PE", true)) "PE" else "ENLACE"
            val script = "TIPO_TRAMO = '$tipo'; if (typeof updateIndicaciones === 'function') { updateIndicaciones($jsonIndicaciones); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.databaseEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 FredianiRoadbook/1.0"

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.e("MAP_CONSOLE", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()})")
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.i("MAP_STATUS", "onPageFinished: $url")
                        isMapLoaded = true
                        view?.evaluateJavascript("if (typeof map !== 'undefined') { map.invalidateSize(); }", null)
                    }

                    override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        android.util.Log.e("MAP_STATUS", "Error: ${error?.description} on url: ${request?.url}")
                    }

                    override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        android.util.Log.e("MAP_STATUS", "Http Error: ${errorResponse?.statusCode} on url: ${request?.url}")
                    }

                    override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                        android.util.Log.e("MAP_STATUS", "SSL Error: $error")
                        handler?.proceed()
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onLocationClicked(lat: Double, lng: Double) {
                        post {
                            onMapClickWithRoad?.invoke(lat, lng, "")
                            onMapClick?.invoke(lat, lng)
                        }
                    }

                    @JavascriptInterface
                    fun onLocationClickedWithRoad(lat: Double, lng: Double, roadName: String) {
                        post {
                            onMapClickWithRoad?.invoke(lat, lng, roadName)
                            onMapClick?.invoke(lat, lng)
                        }
                    }

                    @JavascriptInterface
                    fun onLocationClickedWithRoadAndMode(lat: Double, lng: Double, roadName: String, isOffRoad: Boolean) {
                        post {
                            if (onMapClickWithRoadAndMode != null) {
                                onMapClickWithRoadAndMode.invoke(lat, lng, roadName, isOffRoad)
                            } else {
                                onMapClickWithRoad?.invoke(lat, lng, roadName)
                            }
                            onMapClick?.invoke(lat, lng)
                        }
                    }

                    @JavascriptInterface
                    fun guardarSegmento(vinetaId: String, geometriaJson: String) {
                        val id = vinetaId.toLongOrNull() ?: return
                        post { onSegmentoActual?.invoke(id, geometriaJson) }
                    }

                    @JavascriptInterface
                    fun openStreetView(lat: Double, lng: Double) {
                        post {
                            try {
                                val gmmIntentUri = Uri.parse("google.streetview:cbll=$lat,$lng")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                ctx.startActivity(mapIntent)
                            } catch (e: Exception) {
                                try {
                                    val webUri = Uri.parse("https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=$lat,$lng")
                                    val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    ctx.startActivity(webIntent)
                                } catch (e2: Exception) {
                                    android.util.Log.e("RallyMapView", "Error abriendo Street View", e2)
                                }
                            }
                        }
                    }

                    @JavascriptInterface
                    fun snapPointSync(lat: Double, lng: Double): String {
                        return try {
                            val res = kotlinx.coroutines.runBlocking {
                                roadSnappingService.snapPointToNearestRoad(lat, lng, mapboxToken.takeIf { it.isNotBlank() })
                            }
                            if (res != null) {
                                org.json.JSONObject().apply {
                                    put("success", true)
                                    put("lat", res.latitud)
                                    put("lng", res.longitud)
                                    put("roadName", res.nombreCamino)
                                    put("distOriginal", res.distanciaOriginalMetros)
                                }.toString()
                            } else {
                                org.json.JSONObject().apply {
                                    put("success", false)
                                    put("lat", lat)
                                    put("lng", lng)
                                }.toString()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RallyMapView", "Error en snapPointSync", e)
                            org.json.JSONObject().apply {
                                put("success", false)
                                put("lat", lat)
                                put("lng", lng)
                            }.toString()
                        }
                    }

                    @JavascriptInterface
                    fun getRouteBetweenSync(lat1: Double, lng1: Double, lat2: Double, lng2: Double): String {
                        return try {
                            val route = kotlinx.coroutines.runBlocking {
                                roadSnappingService.snapTraceToRoad(
                                    listOf(Pair(lat1, lng1), Pair(lat2, lng2)),
                                    mapboxToken.takeIf { it.isNotBlank() }
                                )
                            }
                            if (route != null && route.puntos.isNotEmpty()) {
                                val jsonArr = org.json.JSONArray()
                                route.puntos.forEach { (pLat, pLng) ->
                                    val pt = org.json.JSONArray()
                                    pt.put(pLat)
                                    pt.put(pLng)
                                    jsonArr.put(pt)
                                }
                                org.json.JSONObject().apply {
                                    put("success", true)
                                    put("points", jsonArr)
                                    put("distMeters", route.distanciaMetros)
                                }.toString()
                            } else {
                                org.json.JSONObject().apply {
                                    put("success", false)
                                }.toString()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RallyMapView", "Error en getRouteBetweenSync", e)
                            org.json.JSONObject().apply {
                                put("success", false)
                            }.toString()
                        }
                    }
                }, "AndroidBridge")

                loadDataWithBaseURL(
                    "https://unpkg.com",
                    generarHtmlMapa(effectiveLat, effectiveLng, rumbo, mapboxToken, snapToRoadInitial),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            webViewRef = webView
        }
    )
}

private fun generarHtmlMapa(
    lat: Double,
    lng: Double,
    heading: Float,
    mapboxToken: String,
    snapToRoadInitial: Boolean
): String {
    val safeToken = mapboxToken.trim()
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body {
                width: 100%;
                height: 100%;
                margin: 0;
                padding: 0;
                overflow: hidden;
                background-color: #0A0E17;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            #map {
                position: absolute;
                top: 0;
                bottom: 0;
                left: 0;
                right: 0;
                width: 100%;
                height: 100%;
                background-color: #0A0E17;
            }
            .car-marker {
                width: 28px;
                height: 28px;
                background: #00B4D8;
                border: 2.5px solid white;
                border-radius: 50%;
                box-shadow: 0 0 14px rgba(0, 180, 216, 0.95);
                display: flex;
                align-items: center;
                justify-content: center;
                transition: transform 0.2s linear;
            }
            .poi-marker {
                padding: 4px 8px;
                border-radius: 6px;
                color: white;
                font-size: 11px;
                font-weight: 800;
                box-shadow: 0 2px 6px rgba(0,0,0,0.7);
                white-space: nowrap;
            }
            .snap-btn {
                position: absolute;
                top: 80px;
                left: 12px;
                z-index: 1000;
                background: #0F172A;
                border: 2px solid #00B4D8;
                border-radius: 8px;
                padding: 6px 11px;
                font-size: 11px;
                font-weight: 800;
                color: #00B4D8;
                cursor: pointer;
                box-shadow: 0 2px 10px rgba(0,0,0,0.5);
                display: flex;
                align-items: center;
                gap: 5px;
                transition: all 0.2s ease-in-out;
            }
            .snap-btn.active {
                background: #00B4D8;
                color: #0A0E17;
                box-shadow: 0 0 14px rgba(0, 180, 216, 0.9);
            }
            .pegman-btn {
                position: absolute;
                top: 122px;
                left: 12px;
                z-index: 1000;
                background: white;
                border: 2px solid #0284C7;
                border-radius: 8px;
                padding: 6px 10px;
                font-size: 11px;
                font-weight: bold;
                color: #0F172A;
                cursor: pointer;
                box-shadow: 0 2px 8px rgba(0,0,0,0.3);
                display: flex;
                align-items: center;
                gap: 5px;
            }
            .pegman-btn.active {
                background: #0284C7;
                color: white;
            }
            /* Botón "mi ubicación" como el de Google Maps: cuadradito blanco con la mira */
            .center-btn {
                position: absolute;
                top: 86px;
                left: 10px;
                z-index: 1000;
                width: 34px;
                height: 34px;
                background: #FFFFFF;
                border: 2px solid rgba(0,0,0,0.2);
                border-radius: 4px;
                padding: 0;
                color: #5F6368;
                cursor: pointer;
                display: flex;
                align-items: center;
                justify-content: center;
                background-clip: padding-box;
            }
            .center-btn.active {
                color: #1A73E8;
            }
            .satellite-badge {
                position: absolute;
                bottom: 6px;
                left: 10px;
                z-index: 1000;
                background: rgba(15, 23, 42, 0.88);
                backdrop-filter: blur(4px);
                border: 1px solid rgba(0, 180, 216, 0.4);
                border-radius: 6px;
                padding: 4px 10px;
                font-size: 10px;
                font-weight: 700;
                color: #E2E8F0;
                pointer-events: none;
            }
            .toast-banner {
                position: absolute;
                top: 14px;
                left: 50%;
                transform: translateX(-50%);
                z-index: 1100;
                background: rgba(15, 23, 42, 0.94);
                border: 1.5px solid #00B4D8;
                color: #FFFFFF;
                padding: 6px 16px;
                border-radius: 20px;
                font-size: 11px;
                font-weight: 700;
                pointer-events: none;
                opacity: 0;
                transition: opacity 0.3s ease-in-out;
                box-shadow: 0 4px 14px rgba(0,0,0,0.6);
                white-space: nowrap;
            }
            .toast-banner.show {
                opacity: 1;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>

        <!-- BOTÓN SNAP TO ROAD -->
        <button id="snapToggle" style="display:none" class="snap-btn ${if (snapToRoadInitial) "active" else ""}" onclick="toggleSnapToRoad()">
            🛣️ Snap: ${if (snapToRoadInitial) "ACTIVO" else "OFF"}
        </button>

        <!-- STREET VIEW: ahora va adentro del selector de capas (este botón queda oculto) -->
        <button id="pegmanToggle" class="pegman-btn" style="display:none" onclick="togglePegman()">
            👤 Street View
        </button>

        <!-- BOTÓN CENTRAR EN EL VEHÍCULO -->
        <button id="centerToggle" class="center-btn" title="Centrar en mi ubicación" onclick="toggleAutoCenter()">
            <svg viewBox="0 0 24 24" width="20" height="20"><circle cx="12" cy="12" r="3.2" fill="currentColor"/><circle cx="12" cy="12" r="7" fill="none" stroke="currentColor" stroke-width="2"/><path d="M12 1.5v3.2M12 19.3v3.2M1.5 12h3.2M19.3 12h3.2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
        </button>

        <!-- BADGE INFORMATIVO -->
        <div id="satelliteDateBadge" class="satellite-badge">
            🛰️ Mapbox Satellite Streets HD
        </div>

        <!-- NOTIFICACIÓN TEMPORAL DE SNAP -->
        <div id="toastBanner" class="toast-banner">
            🛣️ Ajustado a la traza del camino
        </div>

        <script>
            // Colores de la plataforma (CAMBIOS_PARA_APPS revisión 15): prueba especial roja
            // (#DC2626) con banderita verde en la largada y a cuadros en la llegada; enlace blanco
            // con borde oscuro y punto negro al principio y al final.
            var TIPO_TRAMO = 'PE'; // lo actualiza la app junto con las viñetas
            var RUTA_AUTO = false; // ruta automática (Preferencias): el toque va con el punto exacto
            function lineaTramo(puntos, capa, provisoria) {
                var estilo = provisoria ? { weight: 2.5, dashArray: '3, 5', opacity: 0.6 } : { weight: 4.5, opacity: 0.95 };
                if (TIPO_TRAMO === 'PE') {
                    var l = L.polyline(puntos, Object.assign({ color: '#DC2626' }, estilo)).addTo(capa);
                    return [l];
                }
                var borde = L.polyline(puntos, Object.assign({}, estilo, { color: '#0F172A', weight: estilo.weight + 3 })).addTo(capa);
                var blanco = L.polyline(puntos, Object.assign({ color: '#FFFFFF' }, estilo)).addTo(capa);
                return [borde, blanco];
            }
            function marcasPuntas(primero, ultimo, capa) {
                if (TIPO_TRAMO === 'PE') {
                    var verde = L.divIcon({ className: '', iconSize: [22, 22], iconAnchor: [3, 21],
                        html: '<svg width="22" height="22" viewBox="0 0 22 22"><path d="M3 21 V2" stroke="#0F172A" stroke-width="2"/><path d="M4 2 H19 L15 7 L19 12 H4 Z" fill="#16A34A" stroke="#0F172A" stroke-width="1"/></svg>' });
                    var cuadros = L.divIcon({ className: '', iconSize: [22, 22], iconAnchor: [3, 21],
                        html: '<svg width="22" height="22" viewBox="0 0 22 22"><path d="M3 21 V2" stroke="#0F172A" stroke-width="2"/><rect x="4" y="2" width="15" height="10" fill="#FFF" stroke="#0F172A" stroke-width="1"/>' +
                              '<rect x="4" y="2" width="5" height="5" fill="#000"/><rect x="14" y="2" width="5" height="5" fill="#000"/><rect x="9" y="7" width="5" height="5" fill="#000"/></svg>' });
                    L.marker([primero.lat, primero.lng], { icon: verde, interactive: false }).addTo(capa);
                    L.marker([ultimo.lat, ultimo.lng], { icon: cuadros, interactive: false }).addTo(capa);
                } else {
                    [primero, ultimo].forEach(function(p) {
                        L.circleMarker([p.lat, p.lng], { radius: 5, color: '#000', fillColor: '#000', fillOpacity: 1, weight: 1 }).addTo(capa);
                    });
                }
            }
            var mapboxToken = '$safeToken';

            function showToast(text) {
                var tb = document.getElementById('toastBanner');
                if (tb) {
                    tb.innerText = text;
                    tb.classList.add('show');
                    setTimeout(function() {
                        tb.classList.remove('show');
                    }, 2600);
                }
            }

            // 1. MAPBOX SATELLITE STREETS
            var mapboxSat = L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/satellite-streets-v12/tiles/512/{z}/{x}/{y}@2x?access_token=' + (mapboxToken || 'pk.mapbox_token_default'), {
                maxZoom: 22,
                tileSize: 512,
                zoomOffset: -1,
                attribution: '© Mapbox Satellite Streets'
            });

            // 2. GOOGLE HYBRID SATELLITE (RESPALDO HD INMEDIATO)
            var googleHybrid = L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
                maxZoom: 20,
                attribution: 'Google Hybrid Satellite HD'
            });

            // 3. ESRI WORLD IMAGERY (MAXAR)
            var esriSat = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 19,
                attribution: 'Esri Satellite'
            });

            // 4. OPENSTREETMAP RUTAS
            var osmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: 'OpenStreetMap'
            });

            var isMapboxConfigured = (mapboxToken && mapboxToken.length > 20 && mapboxToken.indexOf('pk.') === 0);
            var defaultBaseLayer = isMapboxConfigured ? mapboxSat : googleHybrid;

            // Mapa base: MAPBOX si tiene token válido, GOOGLE HYBRID por defecto para visualización inmediata HD
            var map = L.map('map', {
                center: [$lat, $lng],
                zoom: 13,
                zoomControl: true,
                layers: [defaultBaseLayer]
            });

            var initialBadge = document.getElementById('satelliteDateBadge');
            if (initialBadge) {
                if (isMapboxConfigured) {
                    initialBadge.innerText = '🛰️ Mapbox Satellite Streets HD';
                } else {
                    initialBadge.innerText = '🛰️ Google Hybrid Satellite HD (Activo)';
                }
            }

            // Fallback elegante si Mapbox arroja 401/403 o token inválido
            var hasSwitchedToGoogle = false;
            mapboxSat.on('tileerror', function() {
                if (!hasSwitchedToGoogle && map.hasLayer(mapboxSat)) {
                    hasSwitchedToGoogle = true;
                    map.removeLayer(mapboxSat);
                    map.addLayer(googleHybrid);
                    showToast('🛰️ Mapbox requiere tu Token en Ajustes. Activado Google Hybrid HD.');
                    var badge = document.getElementById('satelliteDateBadge');
                    if (badge) {
                        badge.innerText = '🛰️ Google Hybrid HD (Configura Token Mapbox en Ajustes)';
                        badge.style.borderColor = '#F59E0B';
                    }
                }
            });

            setTimeout(function() { if (map) map.invalidateSize(); }, 250);
            setTimeout(function() { if (map) map.invalidateSize(); }, 1000);

            // Selector de capas: MAPBOX PRIMERO
            var baseMaps = {
                "🛰️ Mapbox Satellite Streets": mapboxSat,
                "🛰️ Google Satélite + Rutas (HD)": googleHybrid,
                "🛰️ Esri World Imagery (Maxar)": esriSat,
                "🗺️ OpenStreetMap Rutas": osmLayer
            };
            // Street View como una capa más: se tilda, se toca el mapa y abre Street View en ese punto
            var streetViewLayer = L.layerGroup();
            var overlayMaps = { "👤 Street View (tocá el mapa)": streetViewLayer };
            L.control.layers(baseMaps, overlayMaps, { position: 'topright' }).addTo(map);
            map.on('overlayadd', function(e) {
                if (e.layer === streetViewLayer) { pegmanActive = true; showToast('👤 Tocá el mapa donde querés ver Street View'); }
            });
            map.on('overlayremove', function(e) {
                if (e.layer === streetViewLayer) { pegmanActive = false; }
            });

            // Marcador del Auto
            var carIcon = L.divIcon({
                className: 'car-container',
                html: '<div id="vehiclePin" class="car-marker" style="transform: rotate(${heading}deg);"><span style="color:#0A0E17; font-size:13px; font-weight:900;">▲</span></div>',
                iconSize: [28, 28],
                iconAnchor: [14, 14]
            });

            var vehicleMarker = L.marker([$lat, $lng], { icon: carIcon }).addTo(map);
            var poiLayer = L.layerGroup().addTo(map);
            var indicacionesLayer = L.layerGroup().addTo(map);
            var trackSegmentsLayer = L.layerGroup().addTo(map);
            var segmentCache = {};

            // LÓGICA SNAP TO ROAD
            var snapToRoadActive = $snapToRoadInitial;

            function toggleSnapToRoad() {
                snapToRoadActive = !snapToRoadActive;
                var btn = document.getElementById('snapToggle');
                if (snapToRoadActive) {
                    btn.classList.add('active');
                    btn.innerHTML = '🛣️ Snap: ACTIVO';
                    showToast('🛣️ Modo SNAP: El próximo punto se ajustará a caminos');
                } else {
                    btn.classList.remove('active');
                    btn.innerHTML = '🏜️ Fuera de Pista';
                    showToast('🏜️ Modo OFF-ROAD: El próximo punto será campo traviesa');
                }
                // NO alterar las indicaciones ya taggueadas: cada segmento respeta cómo fue creado
            }

            var pegmanActive = false;
            function togglePegman() {
                // Tilda o destilda la capa "Street View" del selector (los eventos de arriba actualizan pegmanActive)
                if (map.hasLayer(streetViewLayer)) map.removeLayer(streetViewLayer);
                else map.addLayer(streetViewLayer);
            }

            // Consultar fecha exacta de captura satelital en Esri
            function querySatelliteDate(centerLat, centerLng) {
                var url = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/0/query?geometry=' + centerLng + ',' + centerLat + '&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=SRC_DATE2,NICE_NAME,RESOLUTION&f=json';
                fetch(url).then(function(r) { return r.json(); }).then(function(data) {
                    if (data && data.features && data.features.length > 0) {
                        var attr = data.features[0].attributes;
                        var date = attr.SRC_DATE2 || 'Actual';
                        var provider = attr.NICE_NAME || 'Maxar Vivid';
                        var badge = document.getElementById('satelliteDateBadge');
                        if (badge && !hasSwitchedToGoogle) {
                            badge.innerText = '🛰️ ' + provider + ' • Captura: ' + date;
                        }
                    }
                }).catch(function(err) {});
            }

            map.on('moveend', function() {
                var c = map.getCenter();
                querySatelliteDate(c.lat, c.lng);
            });
            querySatelliteDate($lat, $lng);

            // CONTROL DE CENTRADO MANUAL: El mapa no fuerza la vista al auto a menos que se active
            var autoCenterActive = false;
            var lastVehiclePos = [$lat, $lng];
            var lastVehicleHeading = $heading;

            function updateCenterButtonUi() {
                var btn = document.getElementById('centerToggle');
                if (!btn) return;
                // El ícono queda igual; siguiendo al auto se pone azul (como en Google Maps)
                if (autoCenterActive) btn.classList.add('active');
                else btn.classList.remove('active');
            }

            function toggleAutoCenter() {
                autoCenterActive = !autoCenterActive;
                updateCenterButtonUi();
                if (autoCenterActive) {
                    if (lastVehiclePos && lastVehiclePos[0] !== 0 && lastVehiclePos[1] !== 0) {
                        map.setView(lastVehiclePos, Math.max(map.getZoom(), 15));
                    }
                    showToast('🎯 Centrado en tu posición (Seguimiento activo)');
                } else {
                    showToast('🧭 Exploración libre: Mapa desvinculado del auto');
                }
            }

            // Desvincular el auto-centrado si el usuario toca o arrastra el mapa manualmente
            map.on('dragstart', function() {
                if (autoCenterActive) {
                    autoCenterActive = false;
                    updateCenterButtonUi();
                }
            });

            function updateVehicle(lat, lng, heading) {
                if (typeof vehicleMarker === 'undefined' || !vehicleMarker || !map) return;
                var newPos = [lat, lng];
                lastVehiclePos = newPos;
                lastVehicleHeading = heading;
                vehicleMarker.setLatLng(newPos);
                var pin = document.getElementById('vehiclePin');
                if (pin) {
                    pin.style.transform = 'rotate(' + heading + 'deg)';
                }
                // Solo centra si el usuario presionó el botón CENTRAR
                if (autoCenterActive) {
                    map.panTo(newPos);
                }
            }

            var currentSelectedLat = null;
            var currentSelectedLng = null;

            function focusIndicacion(lat, lng, num) {
                currentSelectedLat = lat;
                currentSelectedLng = lng;
                autoCenterActive = false;
                updateCenterButtonUi();
                if (map) {
                    map.flyTo([lat, lng], Math.min(Math.max(map.getZoom(), 14), 15), { animate: true, duration: 0.8 });
                }
            }

            function openFullStreetView() {
                if (currentSelectedLat !== null && currentSelectedLng !== null) {
                    if (window.AndroidBridge && typeof window.AndroidBridge.openStreetView === 'function') {
                        window.AndroidBridge.openStreetView(currentSelectedLat, currentSelectedLng);
                    }
                }
            }

            var lastIndicaciones = [];
            function updateIndicaciones(indicaciones) {
                lastIndicaciones = indicaciones;
                if (typeof indicacionesLayer === 'undefined' || !indicacionesLayer || !map) return;
                indicacionesLayer.clearLayers();

                indicaciones.forEach(function(ind) {
                    if (ind.lat !== 0 && ind.lng !== 0) {
                        var isOff = (ind.esOffRoad === true);
                        var indIcon = L.divIcon({
                            className: 'ind-custom',
                            html: '<div style="background:#FFB703; color:#0A0E17; font-weight:900; font-size:11px; border:2px solid white; border-radius:50%; width:24px; height:24px; display:flex; align-items:center; justify-content:center; box-shadow:0 0 10px rgba(0,0,0,0.85);">' + ind.num + '</div>',
                            iconAnchor: [12, 12]
                        });

                        var popupHtml = '<div style="font-family:sans-serif; min-width:180px; padding:2px;">' +
                            '<div style="font-size:13px; font-weight:900; color:#0F172A; margin-bottom:4px;">Indicación #' + ind.num + (isOff ? ' <span style="color:#D97706; font-size:10px;">[OFF-ROAD]</span>' : ' <span style="color:#0284C7; font-size:10px;">[CAMINO]</span>') + '</div>' +
                            '<div style="font-size:11px; color:#475569;">Km Total: <b>' + ind.km.toFixed(3) + ' km</b></div>' +
                            '<div style="font-size:11px; color:#475569;">Dibujo: <b>' + (ind.dibujo || '') + '</b></div>' +
                            (ind.nota ? '<div style="font-size:11px; color:#0F172A; margin-top:3px; background:#F1F5F9; padding:3px 6px; border-radius:4px;">' + ind.nota + '</div>' : '') +
                            '<button onclick="window.AndroidBridge && window.AndroidBridge.openStreetView(' + ind.lat + ',' + ind.lng + ')" style="margin-top:8px; width:100%; background:#0284C7; color:white; border:none; padding:6px 10px; border-radius:6px; font-weight:bold; font-size:11px; cursor:pointer; display:flex; align-items:center; justify-content:center; gap:4px;">👤 Abrir en Google Street View</button>' +
                            '</div>';

                        L.marker([ind.lat, ind.lng], { icon: indIcon })
                            .bindPopup(popupHtml)
                            .addTo(indicacionesLayer);
                    }
                });

                renderTrackSegments(indicaciones);
            }

            function fetchRoadSegment(prev, curr, onResult) {
                // 1. Intento nativo vía AndroidBridge (sin CORS, ultra-estable con multi-espejo)
                if (window.AndroidBridge && typeof window.AndroidBridge.getRouteBetweenSync === 'function') {
                    try {
                        var resStr = window.AndroidBridge.getRouteBetweenSync(prev.lat, prev.lng, curr.lat, curr.lng);
                        if (resStr && resStr.length > 5) {
                            var parsed = JSON.parse(resStr);
                            if (parsed && parsed.success && parsed.points && parsed.points.length > 0) {
                                onResult(parsed.points);
                                return;
                            }
                        }
                    } catch (e) {
                        console.error("AndroidBridge route error:", e);
                    }
                }

                // 2. Respaldo directo en Web
                var pairCoords = prev.lng.toFixed(6) + ',' + prev.lat.toFixed(6) + ';' + curr.lng.toFixed(6) + ',' + curr.lat.toFixed(6);

                function tryOsmDe() {
                    var osmDeUrl = 'https://routing.openstreetmap.de/routed-car/route/v1/driving/' + pairCoords + '?overview=full&geometries=geojson';
                    fetch(osmDeUrl)
                        .then(function(r) { return r.json(); })
                        .then(function(d) {
                            if (d && d.code === 'Ok' && d.routes && d.routes.length > 0) {
                                var pts = d.routes[0].geometry.coordinates.map(function(c) { return [c[1], c[0]]; });
                                onResult(pts);
                            }
                        })
                        .catch(function() {});
                }

                function tryProjectOsrm() {
                    var osrmUrl = 'https://router.project-osrm.org/route/v1/driving/' + pairCoords + '?overview=full&geometries=geojson';
                    fetch(osrmUrl)
                        .then(function(r) { return r.json(); })
                        .then(function(d) {
                            if (d && d.code === 'Ok' && d.routes && d.routes.length > 0) {
                                var pts = d.routes[0].geometry.coordinates.map(function(c) { return [c[1], c[0]]; });
                                onResult(pts);
                            } else {
                                tryOsmDe();
                            }
                        })
                        .catch(function() {
                            tryOsmDe();
                        });
                }

                if (isMapboxConfigured) {
                    var routeUrl = 'https://api.mapbox.com/directions/v5/mapbox/driving/' + pairCoords + '?geometries=geojson&overview=full&access_token=' + mapboxToken;
                    fetch(routeUrl)
                        .then(function(r) {
                            if (!r.ok) throw new Error("Mapbox HTTP " + r.status);
                            return r.json();
                        })
                        .then(function(data) {
                            if (data && data.code === 'Ok' && data.routes && data.routes.length > 0) {
                                var coords = data.routes[0].geometry.coordinates;
                                var roadPoints = coords.map(function(c) { return [c[1], c[0]]; });
                                onResult(roadPoints);
                            } else {
                                tryProjectOsrm();
                            }
                        })
                        .catch(function() {
                            tryProjectOsrm();
                        });
                } else {
                    tryProjectOsrm();
                }
            }

            function renderTrackSegments(indicaciones) {
                if (typeof trackSegmentsLayer === 'undefined' || !trackSegmentsLayer || !map) return;
                trackSegmentsLayer.clearLayers();

                var validInds = indicaciones.filter(function(i) { return i.lat !== 0 && i.lng !== 0; });
                if (validInds.length < 2) return;

                for (var i = 1; i < validInds.length; i++) {
                    (function(prev, curr) {
                        var isOffRoad = (curr.esOffRoad === true);
                        if (isOffRoad) {
                            // Fuera de camino: línea recta, punteada, con el color del tramo
                            lineaTramo([[prev.lat, prev.lng], [curr.lat, curr.lng]], trackSegmentsLayer, false)
                                .forEach(function(l) { l.setStyle({ dashArray: '8, 6' }); });
                        } else {
                            // Por camino (snap)
                            var cacheKey = prev.lat.toFixed(5) + ',' + prev.lng.toFixed(5) + '->' + curr.lat.toFixed(5) + ',' + curr.lng.toFixed(5);
                            if (curr.geo && curr.geo.length > 1) {
                                // Camino guardado en la tablet (sirve también sin señal)
                                lineaTramo(curr.geo, trackSegmentsLayer, false);
                            } else if (segmentCache[cacheKey]) {
                                lineaTramo(segmentCache[cacheKey], trackSegmentsLayer, false);
                            } else {
                                var provisorias = lineaTramo([[prev.lat, prev.lng], [curr.lat, curr.lng]], trackSegmentsLayer, true);
                                fetchRoadSegment(prev, curr, function(roadPoints) {
                                    segmentCache[cacheKey] = roadPoints;
                                    // Se guarda en la tablet, con las dos viñetas en las puntas
                                    if (curr.id && window.AndroidBridge && typeof window.AndroidBridge.guardarSegmento === 'function') {
                                        var conPuntas = [[prev.lat, prev.lng]].concat(roadPoints, [[curr.lat, curr.lng]]);
                                        try { window.AndroidBridge.guardarSegmento(String(curr.id), JSON.stringify(conPuntas)); } catch (e) {}
                                    }
                                    provisorias.forEach(function(l) { if (trackSegmentsLayer.hasLayer(l)) trackSegmentsLayer.removeLayer(l); });
                                    lineaTramo(roadPoints, trackSegmentsLayer, false);
                                });
                            }
                        }
                    })(validInds[i-1], validInds[i]);
                }
                marcasPuntas(validInds[0], validInds[validInds.length - 1], trackSegmentsLayer);
            }

            function updatePOIs(puntos) {
                if (typeof poiLayer === 'undefined' || !poiLayer || !map) return;
                poiLayer.clearLayers();
                // Colores de la plataforma (puntosInteres.ts)
                var COLORES = { PERMITIDA: '#06D6A0', PROHIBIDA: '#EF4444', ASISTENCIA: '#8B5CF6', ACCESO: '#0EA5E9',
                                LARGADA: '#16A34A', LLEGADA: '#DC2626', SANIDAD: '#F97316', INTERES: '#EAB308' };
                puntos.forEach(function(p) {
                    var color = COLORES[p.tipo] || '#00B4D8';
                    // Zona (polígono): como la plataforma, borde y relleno del color del tipo
                    if (p.forma === 'POLIGONO' && p.coords && p.coords.length >= 3) {
                        L.polygon(p.coords, { color: color, weight: 2.5, fillColor: color, fillOpacity: p.tipo === 'PROHIBIDA' ? 0.45 : 0.35 })
                            .bindTooltip('<b>' + p.nombre + '</b><br><span style="color:' + color + ';font-weight:bold;">' + p.tipo + '</span>', { sticky: true })
                            .addTo(poiLayer);
                        return;
                    }

                    var poiIcon = L.divIcon({
                        className: 'poi-custom',
                        html: '<div class="poi-marker" style="background:' + color + ';">' + p.nombre + '</div>',
                        iconAnchor: [15, 10]
                    });

                    var popupHtml = '<div style="font-family:sans-serif; min-width:160px;">' +
                        '<b>' + p.nombre + '</b><br><span style="color:#64748B;">Tipo: ' + p.tipo + '</span>' +
                        '<button onclick="window.AndroidBridge && window.AndroidBridge.openStreetView(' + p.lat + ',' + p.lng + ')" style="margin-top:6px; width:100%; background:#0284C7; color:white; border:none; padding:5px 8px; border-radius:6px; font-weight:bold; font-size:11px; cursor:pointer;">👤 Ver Street View</button>' +
                        '</div>';

                    L.marker([p.lat, p.lng], { icon: poiIcon })
                        .bindPopup(popupHtml)
                        .addTo(poiLayer);
                });
            }

            map.on('click', function(e) {
                if (pegmanActive) {
                    if (window.AndroidBridge && typeof window.AndroidBridge.openStreetView === 'function') {
                        window.AndroidBridge.openStreetView(e.latlng.lat, e.latlng.lng);
                    }
                    togglePegman();
                    return;
                }

                function sendLocationToBridge(lat, lng, name, isOff) {
                    if (window.AndroidBridge && typeof window.AndroidBridge.onLocationClickedWithRoadAndMode === 'function') {
                        window.AndroidBridge.onLocationClickedWithRoadAndMode(lat, lng, name, isOff);
                    } else if (window.AndroidBridge && typeof window.AndroidBridge.onLocationClickedWithRoad === 'function') {
                        window.AndroidBridge.onLocationClickedWithRoad(lat, lng, name);
                    } else if (window.AndroidBridge && typeof window.AndroidBridge.onLocationClicked === 'function') {
                        window.AndroidBridge.onLocationClicked(lat, lng);
                    }
                }

                // Ruta automática: el punto exacto del toque (la app arma el camino con las maniobras)
                if (RUTA_AUTO) {
                    sendLocationToBridge(e.latlng.lat, e.latlng.lng, '', false);
                    return;
                }

                if (snapToRoadActive) {
                    // 1. Intento nativo vía AndroidBridge (sin CORS, ultra-estable con multi-espejo)
                    if (window.AndroidBridge && typeof window.AndroidBridge.snapPointSync === 'function') {
                        try {
                            var sResultStr = window.AndroidBridge.snapPointSync(e.latlng.lat, e.latlng.lng);
                            if (sResultStr && sResultStr.length > 5) {
                                var sResult = JSON.parse(sResultStr);
                                if (sResult && sResult.success) {
                                    var sLat = sResult.lat;
                                    var sLng = sResult.lng;
                                    var sName = sResult.roadName || 'Camino detectado';
                                    showToast('🛣️ Ajustado a: ' + sName);
                                    sendLocationToBridge(sLat, sLng, sName, false);
                                    return;
                                }
                            }
                        } catch (err) {
                            console.error("Bridge snap error:", err);
                        }
                    }

                    // 2. Respaldo directo en Web
                    function fallbackOsmDeNearest() {
                        var nUrl = 'https://routing.openstreetmap.de/routed-car/nearest/v1/driving/' + e.latlng.lng + ',' + e.latlng.lat;
                        fetch(nUrl)
                            .then(function(r) { return r.json(); })
                            .then(function(oData) {
                                if (oData && oData.waypoints && oData.waypoints.length > 0) {
                                    var wp = oData.waypoints[0];
                                    var oName = wp.name || 'Camino rural';
                                    showToast('🛣️ Ajustado a ' + oName);
                                    sendLocationToBridge(wp.location[1], wp.location[0], oName, false);
                                } else {
                                    sendLocationToBridge(e.latlng.lat, e.latlng.lng, '', false);
                                }
                            })
                            .catch(function() {
                                sendLocationToBridge(e.latlng.lat, e.latlng.lng, '', false);
                            });
                    }

                    function fallbackOsrmNearest() {
                        var nearestUrl = 'https://router.project-osrm.org/nearest/v1/driving/' + e.latlng.lng + ',' + e.latlng.lat;
                        fetch(nearestUrl)
                            .then(function(or) { return or.json(); })
                            .then(function(oData) {
                                if (oData && oData.waypoints && oData.waypoints.length > 0) {
                                    var wp = oData.waypoints[0];
                                    var oName = wp.name || 'Camino / Pista';
                                    showToast('🛣️ Ajustado a ' + oName);
                                    sendLocationToBridge(wp.location[1], wp.location[0], oName, false);
                                } else {
                                    fallbackOsmDeNearest();
                                }
                            })
                            .catch(function() {
                                fallbackOsmDeNearest();
                            });
                    }

                    if (isMapboxConfigured) {
                        var lng1 = e.latlng.lng;
                        var lat1 = e.latlng.lat;
                        var lng2 = (lng1 + 0.00008).toFixed(6);
                        var lat2 = (lat1 + 0.00008).toFixed(6);
                        var mapboxMatchUrl = 'https://api.mapbox.com/matching/v5/mapbox/driving/' + lng1.toFixed(6) + ',' + lat1.toFixed(6) + ';' + lng2 + ',' + lat2 + '?radiuses=150;150&geometries=geojson&access_token=' + mapboxToken;

                        fetch(mapboxMatchUrl)
                            .then(function(r) {
                                if (!r.ok) throw new Error("Mapbox HTTP " + r.status);
                                return r.json();
                            })
                            .then(function(data) {
                                if (data && data.code === 'Ok' && data.tracepoints && data.tracepoints[0]) {
                                    var tp = data.tracepoints[0];
                                    var sLng = tp.location[0];
                                    var sLat = tp.location[1];
                                    var roadName = (tp.name && tp.name.trim().length > 0) ? tp.name : 'Camino detectado (Mapbox)';
                                    showToast('🛣️ Mapbox Snap: ' + roadName);
                                    sendLocationToBridge(sLat, sLng, roadName, false);
                                } else {
                                    fallbackOsrmNearest();
                                }
                            })
                            .catch(function() {
                                fallbackOsrmNearest();
                            });
                    } else {
                        fallbackOsrmNearest();
                    }
                } else {
                    sendLocationToBridge(e.latlng.lat, e.latlng.lng, 'Coordenada libre (Off-road)', true);
                }
            });
        </script>
    </body>
    </html>
    """.trimIndent()
}


/** Metros entre dos puntos (para ver si el camino guardado todavía empieza y termina en las viñetas). */
private fun metrosEntre(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).let { it * it } +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

/**
 * El camino guardado entre [anterior] y [actual], tal cual para el mapa, o null si no hay o si
 * alguna de las dos viñetas se movió (más de 30 m de las puntas): entonces se vuelve a pedir.
 */
internal fun geometriaVigente(json: String?, anterior: VinetaEntity, actual: VinetaEntity): String? {
    if (json.isNullOrBlank() || (actual.latitud == 0.0 && actual.longitud == 0.0)) return null
    if (actual.esOffRoad) return null // fuera de camino la línea va recta
    return try {
        val arr = com.google.gson.JsonParser.parseString(json).asJsonArray
        if (arr.size() < 2) return null
        val p0 = arr[0].asJsonArray
        val pn = arr[arr.size() - 1].asJsonArray
        val ok = metrosEntre(p0[0].asDouble, p0[1].asDouble, anterior.latitud, anterior.longitud) <= 30.0 &&
            metrosEntre(pn[0].asDouble, pn[1].asDouble, actual.latitud, actual.longitud) <= 30.0
        if (ok) json else null
    } catch (e: Exception) { null }
}
