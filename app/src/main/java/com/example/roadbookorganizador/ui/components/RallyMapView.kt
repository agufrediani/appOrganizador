package com.example.roadbookorganizador.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.roadbookorganizador.data.local.entity.PuntoInteresEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RallyMapView(
    latitud: Double,
    longitud: Double,
    rumbo: Float,
    puntos: List<PuntoInteresEntity> = emptyList(),
    indicaciones: List<VinetaEntity> = emptyList(),
    modifier: Modifier = Modifier,
    onMapClick: ((Double, Double) -> Unit)? = null
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

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

    // Actualizar marcadores de POIs dinámicamente
    LaunchedEffect(puntos, isMapLoaded) {
        if (isMapLoaded && webViewRef != null) {
            val jsonPuntos = StringBuilder("[")
            puntos.forEachIndexed { index, p ->
                jsonPuntos.append("""{"lat": ${p.latitud}, "lng": ${p.longitud}, "nombre": "${p.nombre.replace("\"", "'")}", "tipo": "${p.tipo}"}""")
                if (index < puntos.size - 1) jsonPuntos.append(",")
            }
            jsonPuntos.append("]")
            val script = "if (typeof updatePOIs === 'function') { updatePOIs($jsonPuntos); }"
            webViewRef?.evaluateJavascript(script, null)
        }
    }

    // Actualizar marcadores de Indicaciones y traza en tiempo real
    LaunchedEffect(indicaciones, isMapLoaded) {
        if (isMapLoaded && webViewRef != null) {
            val jsonIndicaciones = StringBuilder("[")
            indicaciones.forEachIndexed { index, v ->
                jsonIndicaciones.append("""{"num": ${v.numero}, "km": ${v.distanciaTotal}, "lat": ${v.latitud}, "lng": ${v.longitud}, "tulipa": "${v.tulipTipo}", "nota": "${v.informacion.replace("\"", "'")}"}""")
                if (index < indicaciones.size - 1) jsonIndicaciones.append(",")
            }
            jsonIndicaciones.append("]")
            val script = "if (typeof updateIndicaciones === 'function') { updateIndicaciones($jsonIndicaciones); }"
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

                if (onMapClick != null) {
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onLocationClicked(lat: Double, lng: Double) {
                            post { onMapClick(lat, lng) }
                        }
                    }, "AndroidBridge")
                }

                loadDataWithBaseURL(
                    "https://unpkg.com",
                    generarHtmlMapa(effectiveLat, effectiveLng, rumbo),
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

private fun generarHtmlMapa(lat: Double, lng: Double, heading: Float): String {
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
                width: 26px;
                height: 26px;
                background: #00B4D8;
                border: 2.5px solid white;
                border-radius: 50%;
                box-shadow: 0 0 12px rgba(0, 180, 216, 0.9);
                display: flex;
                align-items: center;
                justify-content: center;
                transition: transform 0.2s linear;
            }
            .poi-marker {
                padding: 3px 7px;
                border-radius: 6px;
                color: white;
                font-family: sans-serif;
                font-size: 11px;
                font-weight: bold;
                box-shadow: 0 2px 6px rgba(0,0,0,0.6);
                white-space: nowrap;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            // Mapa base con OpenStreetMap Topográfico y Satelital Esri
            var osmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '© OpenStreetMap'
            });

            var satLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 19,
                attribution: 'Esri Satellite'
            });

            var map = L.map('map', {
                center: [$lat, $lng],
                zoom: 14,
                zoomControl: true,
                layers: [osmLayer]
            });

            setTimeout(function() { if (map) map.invalidateSize(); }, 250);
            setTimeout(function() { if (map) map.invalidateSize(); }, 1000);

            var baseMaps = {
                "Mapa Terreno / Rutas": osmLayer,
                "Satelital Esri": satLayer
            };
            L.control.layers(baseMaps).addTo(map);

            // Marcador del Auto
            var carIcon = L.divIcon({
                className: 'car-container',
                html: '<div id="vehiclePin" class="car-marker" style="transform: rotate(${heading}deg);"><span style="color:#0A0E17; font-size:12px; font-weight:900;">▲</span></div>',
                iconSize: [26, 26],
                iconAnchor: [13, 13]
            });

            var vehicleMarker = L.marker([$lat, $lng], { icon: carIcon }).addTo(map);
            var poiLayer = L.layerGroup().addTo(map);
            var indicacionesLayer = L.layerGroup().addTo(map);
            var trackPolyline = L.polyline([], { color: '#00B4D8', weight: 4, opacity: 0.85 }).addTo(map);

            function updateVehicle(lat, lng, heading) {
                var newPos = [lat, lng];
                vehicleMarker.setLatLng(newPos);
                var pin = document.getElementById('vehiclePin');
                if (pin) {
                    pin.style.transform = 'rotate(' + heading + 'deg)';
                }
                map.panTo(newPos);
            }

            function updateIndicaciones(indicaciones) {
                indicacionesLayer.clearLayers();
                var latlngs = [];
                indicaciones.forEach(function(ind) {
                    if (ind.lat !== 0 && ind.lng !== 0) {
                        latlngs.push([ind.lat, ind.lng]);
                        var indIcon = L.divIcon({
                            className: 'ind-custom',
                            html: '<div style="background:#FFB703; color:#0A0E17; font-weight:900; font-size:11px; border:2px solid white; border-radius:50%; width:22px; height:22px; display:flex; align-items:center; justify-content:center; box-shadow:0 0 8px rgba(0,0,0,0.8);">' + ind.num + '</div>',
                            iconAnchor: [11, 11]
                        });
                        L.marker([ind.lat, ind.lng], { icon: indIcon })
                            .bindPopup('<b>Indicación #' + ind.num + '</b><br>Km: ' + ind.km.toFixed(3) + '<br>Tulipa: ' + ind.tulipa + '<br>Nota: ' + ind.nota)
                            .addTo(indicacionesLayer);
                    }
                });
                if (latlngs.length > 1) {
                    trackPolyline.setLatLngs(latlngs);
                }
            }

            function updatePOIs(puntos) {
                poiLayer.clearLayers();
                puntos.forEach(function(p) {
                    var color = '#00B4D8';
                    if (p.tipo === 'AMBULANCIA' || p.tipo === 'RESCATE') color = '#EF476F';
                    else if (p.tipo === 'ACCESO') color = '#06D6A0';
                    else if (p.tipo === 'PUBLICO') color = '#FFB703';
                    else if (p.tipo === 'HELIPUERTO') color = '#FB8500';

                    var poiIcon = L.divIcon({
                        className: 'poi-custom',
                        html: '<div class="poi-marker" style="background:' + color + ';">' + p.nombre + '</div>',
                        iconAnchor: [15, 10]
                    });

                    L.marker([p.lat, p.lng], { icon: poiIcon })
                        .bindPopup('<b>' + p.nombre + '</b><br>Tipo: ' + p.tipo)
                        .addTo(poiLayer);
                });
            }

            map.on('click', function(e) {
                if (window.AndroidBridge && typeof window.AndroidBridge.onLocationClicked === 'function') {
                    window.AndroidBridge.onLocationClicked(e.latlng.lat, e.latlng.lng);
                }
            });
        </script>
    </body>
    </html>
    """.trimIndent()
}
