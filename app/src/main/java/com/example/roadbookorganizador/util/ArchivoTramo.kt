package com.example.roadbookorganizador.util

import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

/** Un tramo leído de un archivo, listo para guardar en la tablet. */
data class PaqueteTramo(
    val tramo: TramoEntity,        // sin rally todavía (rallyId = 0)
    val vinetas: List<VinetaEntity> // sin tramo todavía (tramoId = 0)
)

/**
 * Archivo de un tramo para pasarlo de tablet a tablet o desde la SD.
 *
 * Usa el formato "Frediani 2.1", el mismo que exporta la plataforma
 * (`GET /api/tramos/{id}/exportar/json`) y que recibe `importar-paquete`. Al leer acepta
 * también los nombres en camelCase de la API y el archivo viejo de esta app.
 *
 * Regla de ESPECIFICACION_TAQUEO punto 0 ter: el que recibe un tramo lo guarda como una COPIA,
 * con uid nuevos (del tramo y de cada viñeta). Así es un tramo independiente.
 */
object ArchivoTramo {

    // ------------------------------------------------------------------ LEER

    /** Lee el texto del archivo. Tira IllegalArgumentException con el motivo si no sirve. */
    fun leer(texto: String): PaqueteTramo {
        val raiz = try { JsonParser.parseString(texto) } catch (e: Exception) {
            throw IllegalArgumentException("El archivo no es un JSON válido.")
        }
        if (!raiz.isJsonObject) throw IllegalArgumentException("El archivo no es un tramo de Frediani Roadbook.")
        val r = raiz.asJsonObject
        // El tramo puede venir en "tramo" (Frediani 2.1) o en la raíz (API / archivo viejo de la app)
        val t = r.obj("tramo") ?: r
        val lista = (r.arr("indicaciones") ?: r.arr("vinetas"))
            ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
            ?: throw IllegalArgumentException("El archivo no trae viñetas.")
        if (lista.isEmpty()) throw IllegalArgumentException("El archivo no trae viñetas.")

        val tramo = TramoEntity(
            rallyId = 0,
            uid = VinetaEntity.nuevoUid(), // copia: uid nuevo
            tipo = t.txt("tipo").ifBlank { "PE" }.uppercase().let { if (it == "PE") "PE" else "ENLACE" },
            identificador = t.txt("identificador", "tramo_identificador").trim(),
            nombre = t.txt("nombre", "tramo_nombre").trim(),
            numeroSector = t.ent("numero_sector", "numeroSector") ?: 1,
            chInicio = t.txt("ch_inicio", "chInicio"),
            chFin = t.txt("ch_fin", "chFin"),
            distanciaTotalEstimada = t.dec("distancia_total_km", "distanciaTotalKm", "distancia_medida_km") ?: 0.0,
            tiempoOtorgado = t.txt("tiempo_otorgado", "tiempoOtorgado"),
            atrasoMaximo = t.txt("atraso_maximo", "atrasoMaximo"),
            horaPrimerAuto = t.txt("hora_primer_auto", "horaPrimerAuto"),
            velocidadPromedioObjetivoKmh = t.dec("velocidad_promedio_general", "velocidadPromedioGeneralKmH", "velocidad_promedio_objetivo_kmh") ?: 0.0,
            velocidadMaximaPermitidaKmh = t.dec("velocidad_maxima_etapa", "velocidadMaximaEtapaKmH", "velocidad_maxima_permitida_kmh") ?: 0.0,
            estadoTrazado = "BORRADOR"
        )

        // En orden; el parcial se deduce del acumulado si no viene (igual que importar-paquete)
        val ordenadas = lista.withIndex()
            .sortedWith(compareBy({ it.value.ent("numero_orden", "numeroOrden", "orden", "numero") ?: (it.index + 1) }, { it.index }))
            .map { it.value }
        var acum = 0.0
        val vinetas = ordenadas.mapIndexed { i, e ->
            val totalArchivo = e.dec("distancia_total", "distanciaTotal", "distanciaAcumuladaKm", "distancia_acumulada")
            val parcial = (e.dec("distancia_parcial", "distanciaParcial", "distanciaParcialKm")
                ?: totalArchivo?.let { (it - acum).coerceAtLeast(0.0) } ?: 0.0).coerceAtLeast(0.0)
            acum += parcial
            val esArchivoViejo = e.has("tulip_tipo") || e.has("tulip_svg_data")
            val tipoWpt = e.txt("tipo_waypoint", "tipoWaypoint")
            val notas = e.txt("notas")
            VinetaEntity(
                tramoId = 0,
                uid = VinetaEntity.nuevoUid(), // copia: uid nuevo
                numero = i + 1,
                distanciaParcial = Math.round(parcial * 1000.0) / 1000.0,
                distanciaTotal = Math.round(acum * 1000.0) / 1000.0,
                latitud = e.dec("lat", "latitud") ?: 0.0,
                longitud = e.dec("lon", "longitud") ?: 0.0,
                altitud = e.dec("altitud") ?: 0.0,
                rumbo = e.dec("rumbo_cap", "rumboCap")?.toFloat() ?: 0f,
                // Frediani 2.1: el dibujo viene en "tulipa_tipo". Archivo viejo de la app: se convierte al abrirlo.
                dibujo = if (esArchivoViejo) null else e.txt("tulipa_tipo", "tulipaTipo", "dibujo"),
                tulipTipo = if (esArchivoViejo) e.txt("tulip_tipo").ifBlank { "BLANCO" } else "BLANCO",
                tulipSvgData = e.txt("tulip_svg_data"),
                notasSvgData = e.txt("notas_svg_data"),
                informacion = e.txt("informacion", "descripcion"),
                peligro = e.txt("icono_alerta", "iconoAlerta", "peligro"),
                esOffRoad = e.txt("tipo_trazado", "tipoTrazado").equals("OFFROAD", true) || e.bool("es_off_road"),
                velocidadPromedioSectorKmh = e.dec("velocidad_promedio", "velocidadPromedio", "velocidadPromedioKmH", "velocidad_promedio_sector_kmh") ?: 0.0,
                velocidadMaximaSectorKmh = e.dec("velocidad_maxima_dz", "velocidadMaximaDz", "velocidadMaximaKmH", "velocidad_maxima_sector_kmh") ?: 0.0,
                tiempoIdealSegundos = e.dec("tiempo_sector_segundos", "tiempoSectorSegundos", "tiempo_ideal_segundos") ?: 0.0,
                radioCapturaMetros = e.ent("radio_captura_metros") ?: 30,
                esReinicioCero = e.bool("reset_odometro", "resetOdometro"),
                distanciaOculta = e.bool("distancia_oculta", "distanciaOculta"),
                esOcultaOrg = e.bool("oculta_organizacion", "ocultaOrganizacion"),
                mostrarWpt = e.bool("coordenada_gps_explicita", "coordenadaGpsExplicita"),
                esTierra = e.bool("es_tierra", "esTierra"),
                tipoPuntoControl = tipoWpt,
                esPuntoControl = tipoWpt.isNotBlank(),
                origenCoordenada = e.txt("origen_coordenada", "origenCoordenada").ifBlank { null },
                coordenadaFechaUtc = e.txt("coordenada_fecha_utc", "coordenadaFechaUtc").ifBlank { null },
                coordenadaPor = e.txt("coordenada_por", "coordenadaPor").ifBlank { null },
                precisionGpsM = e.dec("precision_gps_m", "precisionGpsM"),
                verificadaTerreno = e.bool("verificada_terreno", "verificadaTerreno"),
                // Las notas (segundo texto de la plataforma) no se editan en la tablet: se guardan
                // para mandarlas tal cual al subir el tramo (nunca se pierde un dato)
                datosServidor = if (notas.isNotBlank()) JsonObject().apply { addProperty("notas", notas) }.toString() else null
            )
        }
        return PaqueteTramo(tramo, vinetas)
    }

    // ---------------------------------------------------------------- ESCRIBIR

    /**
     * Arma el archivo "Frediani 2.1" de un tramo (mismos nombres que la exportación de la
     * plataforma). [dibujoDe] da el dibujo de cada viñeta en formato v3.
     */
    fun escribir(rally: String?, tramo: TramoEntity, vinetas: List<VinetaEntity>, dibujoDe: (VinetaEntity) -> String): String {
        fun JsonObject.p(k: String, v: Any?) {
            when (v) {
                null -> add(k, JsonNull.INSTANCE)
                is Number -> addProperty(k, v)
                is Boolean -> addProperty(k, v)
                else -> addProperty(k, v.toString())
            }
        }
        val raiz = JsonObject()
        raiz.p("version_frediani", "2.1")
        raiz.p("ecosistema", "FREDIANI ROADBOOK DIGITAL")
        raiz.p("generado_en", java.time.Instant.now().toString())
        raiz.p("rally", rally)
        raiz.add("tramo", JsonObject().apply {
            p("identificador", tramo.identificador)
            p("nombre", tramo.nombre)
            p("tipo", tramo.tipo)
            p("numero_sector", tramo.numeroSector)
            p("ch_inicio", tramo.chInicio)
            p("ch_fin", tramo.chFin)
            p("distancia_total_km", if (tramo.distanciaMedidaReal > 0) tramo.distanciaMedidaReal else tramo.distanciaTotalEstimada)
            p("tiempo_otorgado", tramo.tiempoOtorgado)
            p("atraso_maximo", tramo.atrasoMaximo)
            p("velocidad_promedio_general", tramo.velocidadPromedioObjetivoKmh.takeIf { it > 0 })
            p("velocidad_maxima_etapa", tramo.velocidadMaximaPermitidaKmh.takeIf { it > 0 })
            p("hora_primer_auto", tramo.horaPrimerAuto.ifBlank { null })
        })
        val inds = JsonArray()
        vinetas.sortedBy { it.numero }.forEach { v ->
            val hayCoord = !(v.latitud == 0.0 && v.longitud == 0.0)
            val notas = v.datosServidor?.let { runCatching { JsonParser.parseString(it).asJsonObject.txt("notas") }.getOrNull() }
            inds.add(JsonObject().apply {
                p("numero_orden", v.numero)
                p("distancia_total", v.distanciaTotal)
                p("distancia_parcial", v.distanciaParcial)
                p("tulipa_tipo", dibujoDe(v))
                p("informacion", v.informacion)
                p("notas", notas?.ifBlank { null })
                p("icono_alerta", v.peligro.ifBlank { null })
                p("lat", if (hayCoord) v.latitud else null)
                p("lon", if (hayCoord) v.longitud else null)
                p("rumbo_cap", if (v.rumbo != 0f) v.rumbo.toInt() else null)
                p("velocidad_promedio", v.velocidadPromedioSectorKmh.takeIf { it > 0 })
                p("tiempo_sector_segundos", v.tiempoIdealSegundos.takeIf { it > 0 }?.toInt())
                p("velocidad_maxima_dz", v.velocidadMaximaSectorKmh.takeIf { it > 0 }?.toInt())
                p("tipo_trazado", if (v.esOffRoad) "OFFROAD" else "SNAP")
                p("reset_odometro", v.esReinicioCero)
                p("distancia_oculta", v.distanciaOculta)
                p("oculta_organizacion", v.esOcultaOrg)
                p("coordenada_gps_explicita", v.mostrarWpt)
                p("es_tierra", v.esTierra)
                p("tipo_waypoint", v.tipoPuntoControl.ifBlank { null })
                p("uid", v.uid)
                p("origen_coordenada", v.origenCoordenada)
                p("coordenada_fecha_utc", v.coordenadaFechaUtc)
                p("coordenada_por", v.coordenadaPor)
                p("precision_gps_m", v.precisionGpsM)
                p("verificada_terreno", v.verificadaTerreno)
            })
        }
        raiz.add("indicaciones", inds)
        return GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(raiz)
    }
}

// --- Lectura tolerante: el primer nombre que venga, con o sin comillas ---
private fun JsonObject.v(vararg k: String): JsonElement? = k.asSequence().mapNotNull { get(it)?.takeIf { e -> !e.isJsonNull } }.firstOrNull()
private fun JsonObject.obj(k: String): JsonObject? = get(k)?.takeIf { it.isJsonObject }?.asJsonObject
private fun JsonObject.arr(k: String): JsonArray? = get(k)?.takeIf { it.isJsonArray }?.asJsonArray
private fun JsonObject.txt(vararg k: String): String = try { v(*k)?.let { if (it.isJsonPrimitive) it.asString else it.toString() } ?: "" } catch (e: Exception) { "" }
private fun JsonObject.dec(vararg k: String): Double? = try {
    v(*k)?.let { if (it.isJsonPrimitive && (it as JsonPrimitive).isString) it.asString.replace(',', '.').toDoubleOrNull() else it.asDouble }
} catch (e: Exception) { null }
private fun JsonObject.ent(vararg k: String): Int? = dec(*k)?.toInt()
private fun JsonObject.bool(vararg k: String): Boolean = try {
    v(*k)?.let { if ((it as? JsonPrimitive)?.isString == true) it.asString.equals("true", true) else it.asBoolean } ?: false
} catch (e: Exception) { false }
