package com.example.roadbookorganizador.util

import android.content.Context
import com.example.roadbookorganizador.data.local.entity.TrackPointEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object GpxKmzExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Genera un archivo GPX estándar con waypoints (viñetas) y track continuo (trackpoints).
     */
    fun exportarGpx(
        context: Context,
        tramo: TramoEntity,
        vinetas: List<VinetaEntity>,
        trackPoints: List<TrackPointEntity>
    ): File {
        val fileName = "Tramo_${tramo.identificador.replace(" ", "_")}_${System.currentTimeMillis()}.gpx"
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, fileName)

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="Frediani Roadbook Organizador" xmlns="http://www.topografix.com/GPX/1/1">""")
        sb.appendLine("""  <metadata>""")
        sb.appendLine("""    <name>${tramo.identificador} - ${tramo.nombre}</name>""")
        sb.appendLine("""    <time>${isoFormat.format(Date())}</time>""")
        sb.appendLine("""  </metadata>""")

        // Waypoints (Viñetas con sus datos de odómetro y tulipán)
        for (v in vinetas) {
            val modoStr = if (v.esOffRoad) "OFF-ROAD" else "CAMINO"
            val regStr = if (v.velocidadPromedioSectorKmh > 0) " | VelProm: ${v.velocidadPromedioSectorKmh.toInt()} km/h" else ""
            val maxStr = if (v.velocidadMaximaSectorKmh > 0) " | MaxDZ: ${v.velocidadMaximaSectorKmh.toInt()} km/h" else ""
            sb.appendLine("""  <wpt lat="${v.latitud}" lon="${v.longitud}">""")
            sb.appendLine("""    <ele>${v.altitud}</ele>""")
            sb.appendLine("""    <time>${isoFormat.format(Date(v.timestamp))}</time>""")
            sb.appendLine("""    <name>V${v.numero} - ${String.format(Locale.US, "%.3f", v.distanciaTotal)} km</name>""")
            sb.appendLine("""    <desc>Maniobra: ${v.tulipTipo} | Parcial: ${String.format(Locale.US, "%.3f", v.distanciaParcial)} km | Modo: $modoStr$regStr$maxStr | Notas: ${v.informacion} ${v.peligro}</desc>""")
            sb.appendLine("""    <sym>${v.tulipTipo}</sym>""")
            sb.appendLine("""  </wpt>""")
        }

        // Track continuo de recorrido
        sb.appendLine("""  <trk>""")
        sb.appendLine("""    <name>Recorrido ${tramo.identificador}</name>""")
        sb.appendLine("""    <trkseg>""")
        for (pt in trackPoints) {
            sb.appendLine("""      <trkpt lat="${pt.latitud}" lon="${pt.longitud}">""")
            sb.appendLine("""        <ele>${pt.altitud}</ele>""")
            sb.appendLine("""        <time>${isoFormat.format(Date(pt.timestamp))}</time>""")
            sb.appendLine("""      </trkpt>""")
        }
        sb.appendLine("""    </trkseg>""")
        sb.appendLine("""  </trk>""")
        sb.appendLine("""</gpx>""")

        file.writeText(sb.toString())
        return file
    }

    /**
     * Genera un archivo JSON completo compatible con la Plataforma Web Frediani Roadbook.
     */
    fun exportarJsonPlataforma(
        context: Context,
        tramo: TramoEntity,
        vinetas: List<VinetaEntity>
    ): File {
        val fileName = "Export_Web_${tramo.identificador.replace(" ", "_")}.json"
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, fileName)

        val payload = mapOf(
            "tramo_identificador" to tramo.identificador,
            "tramo_nombre" to tramo.nombre,
            "tipo" to tramo.tipo,
            "distancia_medida_km" to tramo.distanciaMedidaReal,
            "velocidad_promedio_objetivo_kmh" to tramo.velocidadPromedioObjetivoKmh,
            "velocidad_maxima_permitida_kmh" to tramo.velocidadMaximaPermitidaKmh,
            "total_vinetas" to vinetas.size,
            "vinetas" to vinetas.map { v ->
                mapOf(
                    "numero" to v.numero,
                    "distancia_total" to v.distanciaTotal,
                    "distancia_parcial" to v.distanciaParcial,
                    "latitud" to v.latitud,
                    "longitud" to v.longitud,
                    "altitud" to v.altitud,
                    "rumbo" to v.rumbo,
                    "tulip_tipo" to v.tulipTipo,
                    "tulip_svg_data" to v.tulipSvgData,
                    "notas_svg_data" to v.notasSvgData,
                    "informacion" to v.informacion,
                    "peligro" to v.peligro,
                    "es_off_road" to v.esOffRoad,
                    "velocidad_promedio_sector_kmh" to v.velocidadPromedioSectorKmh,
                    "velocidad_maxima_sector_kmh" to v.velocidadMaximaSectorKmh,
                    "tiempo_ideal_segundos" to v.tiempoIdealSegundos,
                    "radio_captura_metros" to v.radioCapturaMetros
                )
            }
        )

        val gson = GsonBuilder().setPrettyPrinting().create()
        file.writeText(gson.toJson(payload))
        return file
    }

    /**
     * Genera un archivo KML estándar para Google Earth con trazado 3D, pines coloreados por tipo de viñeta
     * y globos descriptivos con odometría y notas.
     */
    fun exportarKml(
        context: Context,
        tramo: TramoEntity,
        vinetas: List<VinetaEntity>,
        trackPoints: List<TrackPointEntity>
    ): File {
        val fileName = "GoogleEarth_${tramo.identificador.replace(" ", "_")}_${System.currentTimeMillis()}.kml"
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, fileName)

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
        sb.appendLine("""  <Document>""")
        sb.appendLine("""    <name>${tramo.identificador} - ${tramo.nombre}</name>""")
        sb.appendLine("""    <description>Roadbook Digital Frediani Competición - Distancia: ${String.format(Locale.US, "%.2f", tramo.distanciaMedidaReal)} km</description>""")

        sb.appendLine("""    <Style id="lineaRuta">""")
        sb.appendLine("""      <LineStyle>""")
        sb.appendLine("""        <color>ff0055ff</color>""")
        sb.appendLine("""        <width>6</width>""")
        sb.appendLine("""      </LineStyle>""")
        sb.appendLine("""    </Style>""")

        sb.appendLine("""    <Folder><name>Viñetas de Roadbook</name>""")
        for (v in vinetas) {
            val pinColor = when {
                v.peligro.contains("PELIGRO") || v.peligro.contains("!!!") -> "red"
                v.peligro.contains("ATENCIÓN") || v.peligro.contains("!") -> "ylw"
                v.peligro.contains("LARGADA") || v.numero == 1 -> "grn"
                else -> "blu"
            }
            val regKml = if (v.velocidadPromedioSectorKmh > 0) "<br/><b>Vel. Promedio:</b> ${v.velocidadPromedioSectorKmh.toInt()} km/h" else ""
            val maxKml = if (v.velocidadMaximaSectorKmh > 0) "<br/><b>Zona DZ Máx:</b> <font color=\"red\">${v.velocidadMaximaSectorKmh.toInt()} km/h</font>" else ""
            val offKml = if (v.esOffRoad) "<br/><b>Terreno:</b> <font color=\"#d97706\">OFF-ROAD (Fuera de pista)</font>" else "<br/><b>Terreno:</b> Camino oficial"

            sb.appendLine("""      <Placemark>""")
            sb.appendLine("""        <name>#${v.numero} - ${String.format(Locale.US, "%.3f", v.distanciaTotal)} km</name>""")
            sb.appendLine("""        <description><![CDATA[""")
            sb.appendLine("""          <h3>Viñeta #${v.numero}</h3>""")
            sb.appendLine("""          <p><b>Odómetro Total:</b> ${String.format(Locale.US, "%.3f", v.distanciaTotal)} km<br/>""")
            sb.appendLine("""          <b>Parcial:</b> ${String.format(Locale.US, "%.3f", v.distanciaParcial)} km<br/>""")
            sb.appendLine("""          <b>Rumbo (CAP):</b> ${String.format(Locale.US, "%.0f°", v.rumbo)}<br/>""")
            sb.appendLine("""          <b>Maniobra:</b> ${v.tulipTipo}$offKml$regKml$maxKml<br/>""")
            if (v.peligro.isNotBlank()) sb.appendLine("""          <b>Alerta:</b> <font color="red">${v.peligro}</font><br/>""")
            if (v.informacion.isNotBlank()) sb.appendLine("""          <b>Notas:</b> ${v.informacion}<br/>""")
            sb.appendLine("""          <b>Coordenadas:</b> ${v.latitud}, ${v.longitud}</p>""")
            sb.appendLine("""        ]]></description>""")
            sb.appendLine("""        <Style><IconStyle><Icon><href>http://maps.google.com/mapfiles/kml/paddle/${pinColor}-circle.png</href></Icon></IconStyle></Style>""")
            sb.appendLine("""        <Point><coordinates>${v.longitud},${v.latitud},${v.altitud}</coordinates></Point>""")
            sb.appendLine("""      </Placemark>""")
        }
        sb.appendLine("""    </Folder>""")

        sb.appendLine("""    <Placemark>""")
        sb.appendLine("""      <name>Trazado del Tramo</name>""")
        sb.appendLine("""      <styleUrl>#lineaRuta</styleUrl>""")
        sb.appendLine("""      <LineString>""")
        sb.appendLine("""        <tessellate>1</tessellate>""")
        sb.appendLine("""        <altitudeMode>clampToGround</altitudeMode>""")
        sb.appendLine("""        <coordinates>""")

        if (trackPoints.isNotEmpty()) {
            for (pt in trackPoints) {
                sb.append("${pt.longitud},${pt.latitud},${pt.altitud} ")
            }
        } else {
            for (v in vinetas) {
                sb.append("${v.longitud},${v.latitud},${v.altitud} ")
            }
        }
        sb.appendLine("""        </coordinates>""")
        sb.appendLine("""      </LineString>""")
        sb.appendLine("""    </Placemark>""")

        sb.appendLine("""  </Document>""")
        sb.appendLine("""</kml>""")

        file.writeText(sb.toString())
        return file
    }
}
