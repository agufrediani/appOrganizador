package com.example.roadbookorganizador.data.remote

import android.content.Context
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.google.gson.JsonElement
import com.google.gson.JsonObject

data class SyncResult(
    val success: Boolean,
    val message: String,
    val ralliesRecibidos: Int = 0,
    val tramosRecibidos: Int = 0
)

/**
 * Sincronización con la plataforma, con la forma nueva del servidor (ESPECIFICACION_TAQUEO
 * punto 0). Las direcciones viejas (`datos_completos`, `sincronizar_indicaciones`,
 * `crear_rapido`) ya no existen.
 *
 * Por ahora solo BAJA: `GET /api/rallies` (los rallies del organizador, cada uno con la lista
 * liviana de sus tramos) y los guarda en la tablet. Lo que ya está en la tablet no se borra.
 * La subida (taqueo viñeta por viñeta con `PUT /api/indicaciones/{id}`) va aparte.
 */
class WebSyncService(private val contexto: Context, private val repository: RoadbookRepository) {

    private val api = ApiCliente(contexto)

    private suspend fun sincronizarZonas(rallyWebId: Int, rallyId: Long): Int =
        sincronizarZonasInterno(api, repository, rallyWebId, rallyId)

    suspend fun sincronizarConServidor(): SyncResult {
        // 1) Subida: cada tramo con algo de la tablet sin subir, tramo por tramo y viñeta por viñeta
        var avisoSubida: String? = null
        var subidos = 0
        val sincro = SincronizacionTramo(contexto, repository)
        for (t in repository.getTodosLosTramos()) {
            val rally = repository.getRallyById(t.rallyId) ?: continue
            if (rally.webId == null) continue // rally solo en la tablet: no se sube
            val hayAlgo = t.webId == null || t.pendienteSubir ||
                repository.getVinetasPendientes(t.id).isNotEmpty() || repository.getBajasPendientes(t.id).isNotEmpty() ||
                repository.contarCaminosPendientes(t.id) > 0
            if (!hayAlgo) continue
            val r = sincro.sincronizar(t.id)
            if (r.ok) subidos += r.enviadas else if (avisoSubida == null) avisoSubida = r.mensaje
            if (r.sinConexion) break
        }
        // 2) Bajada: los rallies y la lista de tramos
        return try {
            val lista = api.get("/api/rallies")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: return SyncResult(false, "El servidor no devolvió la lista de rallies.")
            var nuevosRallies = 0
            var zonas = 0
            var nuevosTramos = 0
            for (el in lista) {
                val r = el.asJsonObject
                val webId = r.entero("id") ?: continue
                val localidad = r.texto("localidad")
                val provincia = r.texto("provincia")
                val existente = repository.getRallyByWebId(webId)
                val datos = (existente ?: RallyEntity(nombre = "", esActivo = false)).copy(
                    webId = webId,
                    nombre = r.texto("nombre").ifBlank { existente?.nombre ?: "Rally #$webId" },
                    campeonato = r.texto("federacion"),
                    organizadorClub = r.texto("organizadorClub"),
                    sede = listOf(localidad, provincia).filter { it.isNotBlank() }.joinToString(", "),
                    fecha = r.texto("fechaInicio"),
                    fechaFin = r.texto("fechaFin"),
                    fiscalizador = r.texto("fiscalizador"),
                    estadoRally = r.texto("estado").ifBlank { existente?.estadoRally ?: "ACTIVO" },
                    areaKm2 = r.decimal("areaKm2") ?: existente?.areaKm2 ?: 350.0,
                    descripcion = r.texto("descripcionPeriodistica"),
                    sincronizado = true
                )
                val rallyId = if (existente == null) {
                    nuevosRallies++
                    repository.insertRally(datos)
                } else {
                    repository.updateRally(datos)
                    existente.id
                }

                // Los tramos son del rally; los datos viejos los traen dentro de las etapas
                val tramos = r.get("tramos")?.takeIf { it.isJsonArray }?.asJsonArray?.toList()
                    ?: r.get("etapas")?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.flatMap { e -> e.asJsonObject.get("tramos")?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty() }
                    ?: emptyList()
                tramos.forEachIndexed { i, tEl ->
                    val t = tEl.asJsonObject
                    val tWebId = t.entero("id") ?: return@forEachIndexed
                    val tUid = t.texto("uid")
                    val tEx = (if (tUid.isNotBlank()) repository.getTramoByUid(rallyId, tUid) else null)
                        ?: repository.getTramoByRallyAndWebId(rallyId, tWebId)
                    when {
                        tEx == null -> {
                            repository.guardarTramoDesdeServidor(
                                SincronizacionTramo.tramoDeServidor(t, rallyId, null).copy(ordenSecuencia = i + 1)
                            )
                            nuevosTramos++
                        }
                        // Con cambios de la tablet sin subir: no se pisan (se mandan al sincronizar el tramo)
                        tEx.pendienteSubir -> Unit
                        else -> {
                            // La lista de rallies trae el tramo liviano: se completan los datos que trae
                            // y se conserva lo demás de la tablet (lo medido, el estado del trazado).
                            repository.guardarTramoDesdeServidor(mezclarLiviano(t, tEx))
                        }
                    }
                }

                // Zonas y puntos de interés del rally (los de la tablet que faltan se suben primero)
                try { zonas += sincronizarZonas(webId, rallyId) } catch (e: SesionVencida) { throw e } catch (e: Exception) { }
                // Portada y chapa oficial del rally (como se ven en la plataforma)
                com.example.roadbookorganizador.util.ImagenesRally.bajar(contexto, api, webId,
                    com.example.roadbookorganizador.util.ImagenesRally.PORTADA, r.texto("fotoPortadaUrl"))
                com.example.roadbookorganizador.util.ImagenesRally.bajar(contexto, api, webId,
                    com.example.roadbookorganizador.util.ImagenesRally.CHAPA, r.texto("chapaOficialUrl"))
            }
            val msg = buildString {
                append("Sincronizado con la plataforma: ${lista.size()} rallies.")
                if (nuevosRallies > 0) append(" $nuevosRallies nuevos.")
                if (nuevosTramos > 0) append(" $nuevosTramos tramos nuevos.")
                if (zonas > 0) append(" $zonas zonas y puntos de interés.")
                if (subidos > 0) append(" $subidos cambios de la tablet enviados.")
                avisoSubida?.let { append("\n").append(it) }
            }
            SyncResult(true, msg, nuevosRallies, nuevosTramos)
        } catch (e: SesionVencida) {
            SyncResult(false, e.message ?: "La sesión venció.")
        } catch (e: ErrorServidor) {
            SyncResult(false, e.message ?: "Error del servidor.")
        } catch (e: Exception) {
            SyncResult(false, "No se pudo conectar con el servidor. Revisá la conexión y la dirección del servidor.")
        }
    }
}

/**
 * Zonas y puntos de interés de un rally (`GET /api/rallies/{id}/zonas-espectadores`), en la
 * misma tabla que la plataforma: Forma = POLIGONO (zona) o PUNTO. Los puntos marcados en la
 * tablet se suben con `POST /api/rallies/{id}/zonas-espectadores`. Devuelve cuántas hay.
 */
private suspend fun WebSyncService.sincronizarZonasInterno(api: ApiCliente, repository: RoadbookRepository, rallyWebId: Int, rallyId: Long): Int {
    // 1) Subir los de la tablet
    for (p in repository.getPuntosInteresSync(rallyId).filter { it.webId == null }) {
        val cuerpo = com.google.gson.JsonObject().apply {
            addProperty("nombre", p.nombre)
            addProperty("tipo", p.tipo)
            addProperty("forma", p.forma)
            addProperty("descripcionAcceso", p.descripcion)
            addProperty("coordenadasGeoJson", p.coordenadasGeoJson ?: "[[${p.latitud},${p.longitud}]]")
            p.servicios?.let { addProperty("servicios", it) }
            p.tramoWebId?.let { addProperty("tramoId", it) }
        }
        val r = api.post("/api/rallies/$rallyWebId/zonas-espectadores", cuerpo)?.takeIf { it.isJsonObject }?.asJsonObject
        r?.entero("id")?.let { repository.updatePuntoInteres(p.copy(webId = it)) }
    }
    // 2) Bajar lo que hay en la plataforma
    val lista = api.get("/api/rallies/$rallyWebId/zonas-espectadores")?.takeIf { it.isJsonArray }?.asJsonArray ?: return 0
    val locales = repository.getPuntosInteresSync(rallyId)
    val vistos = HashSet<Int>()
    for (el in lista) {
        val z = el.takeIf { it.isJsonObject }?.asJsonObject ?: continue
        val id = z.entero("id") ?: continue
        vistos += id
        val coords = z.texto("coordenadasGeoJson")
        val pares = try {
            com.google.gson.JsonParser.parseString(coords.ifBlank { "[]" }).asJsonArray.mapNotNull { p ->
                p.takeIf { it.isJsonArray && it.asJsonArray.size() >= 2 }?.asJsonArray?.let { it[0].asDouble to it[1].asDouble }
            }
        } catch (e: Exception) { emptyList() }
        // Para la lista y el mapa: el punto, o el centro de la zona
        val lat = if (pares.isEmpty()) 0.0 else pares.sumOf { it.first } / pares.size
        val lng = if (pares.isEmpty()) 0.0 else pares.sumOf { it.second } / pares.size
        val forma = z.texto("forma").ifBlank { if (pares.size == 1) "PUNTO" else "POLIGONO" }
        val ex = locales.firstOrNull { it.webId == id }
        val nuevo = (ex ?: com.example.roadbookorganizador.data.local.entity.PuntoInteresEntity(
            rallyId = rallyId, nombre = "", tipo = "", latitud = 0.0, longitud = 0.0
        )).copy(
            rallyId = rallyId,
            webId = id,
            nombre = z.texto("nombre"),
            tipo = z.texto("tipo"),
            forma = forma,
            coordenadasGeoJson = coords.ifBlank { null },
            latitud = lat,
            longitud = lng,
            descripcion = z.texto("descripcionAcceso"),
            servicios = z.texto("servicios").ifBlank { null },
            tramoWebId = z.entero("tramoId"),
            tramoIdentificador = z.texto("tramoIdentificador").ifBlank { null }
        )
        if (ex == null) repository.insertPuntoInteres(nuevo) else repository.updatePuntoInteres(nuevo)
    }
    // 3) Las que se borraron en la plataforma
    locales.filter { it.webId != null && it.webId !in vistos }.forEach { repository.deletePuntoInteres(it) }
    return vistos.size
}

/** Tramo de la lista liviana: solo se cambian los datos que vienen (lo demás queda como está). */
private fun mezclarLiviano(t: JsonObject, ex: TramoEntity): TramoEntity {
    fun tiene(k: String) = t.has(k) && !t.get(k).isJsonNull
    return ex.copy(
        webId = t.entero("id") ?: ex.webId,
        uid = t.texto("uid").ifBlank { ex.uid },
        tipo = if (tiene("tipo")) t.texto("tipo") else ex.tipo,
        identificador = if (tiene("identificador")) t.texto("identificador") else ex.identificador,
        nombre = if (tiene("nombre")) t.texto("nombre") else ex.nombre,
        numeroSector = t.entero("numeroSector") ?: ex.numeroSector,
        chInicio = if (tiene("chInicio")) t.texto("chInicio") else ex.chInicio,
        chFin = if (tiene("chFin")) t.texto("chFin") else ex.chFin,
        distanciaTotalEstimada = t.decimal("distanciaTotalKm") ?: ex.distanciaTotalEstimada,
        tiempoOtorgado = if (tiene("tiempoOtorgado")) t.texto("tiempoOtorgado") else ex.tiempoOtorgado,
        atrasoMaximo = if (tiene("atrasoMaximo")) t.texto("atrasoMaximo") else ex.atrasoMaximo,
        horaPrimerAuto = if (tiene("horaPrimerAuto")) t.texto("horaPrimerAuto") else ex.horaPrimerAuto,
        velocidadPromedioObjetivoKmh = t.decimal("velocidadPromedioGeneralKmH") ?: ex.velocidadPromedioObjetivoKmh,
        velocidadMaximaPermitidaKmh = t.decimal("velocidadMaximaEtapaKmH") ?: ex.velocidadMaximaPermitidaKmh,
        datosServidor = ex.datosServidor ?: t.toString(),
        updatedAt = System.currentTimeMillis()
    )
}

private fun JsonObject.valor(k: String): JsonElement? = get(k)?.takeIf { !it.isJsonNull }
private fun JsonObject.texto(k: String): String = try { valor(k)?.asString ?: "" } catch (e: Exception) { "" }
private fun JsonObject.entero(k: String): Int? = try { valor(k)?.asInt } catch (e: Exception) { null }
private fun JsonObject.decimal(k: String): Double? = try { valor(k)?.asDouble } catch (e: Exception) { null }
