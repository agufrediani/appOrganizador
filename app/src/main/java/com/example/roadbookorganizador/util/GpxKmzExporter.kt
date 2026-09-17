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
            sb.appendLine("""  <wpt lat="${v.latitud}" lon="${v.longitud}">""")
            sb.appendLine("""    <ele>${v.altitud}</ele>""")
            sb.appendLine("""    <time>${isoFormat.format(Date(v.timestamp))}</time>""")
            sb.appendLine("""    <name>V${v.numero} - ${String.format(Locale.US, "%.3f", v.distanciaTotal)} km</name>""")
            sb.appendLine("""    <desc>Tulipa: ${v.tulipTipo} | Parcial: ${String.format(Locale.US, "%.3f", v.distanciaParcial)} km | Notas: ${v.informacion} ${v.peligro}</desc>""")
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
            "distancia_medida_km" to tramo.distanciaMedidaReal,
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
                    "informacion" to v.informacion,
                    "peligro" to v.peligro
                )
            }
        )

        val gson = GsonBuilder().setPrettyPrinting().create()
        file.writeText(gson.toJson(payload))
        return file
    }
}
