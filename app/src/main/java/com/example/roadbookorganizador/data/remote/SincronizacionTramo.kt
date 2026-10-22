package com.example.roadbookorganizador.data.remote

import android.content.Context
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.example.roadbookorganizador.ui.vineta.BibliotecaVineta
import com.example.roadbookorganizador.ui.vineta.dibujoV1
import com.example.roadbookorganizador.vineta.VinetaDibujo
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/** Resultado de sincronizar un tramo. [enviadas] = viñetas que llegaron a la plataforma. */
data class ResultadoTramo(val ok: Boolean, val mensaje: String, val enviadas: Int = 0, val sinConexion: Boolean = false)

/**
 * Sincronización de UN tramo con la plataforma (ESPECIFICACION_TAQUEO puntos 2, 3 y 5).
 *
 * 1. Pide el detalle del tramo (`GET /api/tramos/{id}`) y busca cada viñeta por `uid`.
 * 2. Manda lo pendiente, en orden: las borradas (`DELETE /api/indicaciones/{id}`), las cambiadas
 *    (`PUT /api/indicaciones/{id}`) y las nuevas (`POST /api/tramos/{id}/indicaciones/insertar`,
 *    con el `uid` de la tablet: si se reintenta, el servidor no la duplica).
 * 3. Vuelve a pedir el detalle y lo guarda en la tablet.
 *
 * El PUT lleva la viñeta entera como vino del servidor, cambiando solo lo que se tocó en la
 * tablet (si no, el servidor borraría notas, iconoAlerta y los carteles de velocidad). La
 * distancia va como parcial y solo si cambió. Si la plataforma y la tablet cambiaron la misma
 * viñeta, la coordenada y la distancia son las de la tablet; lo demás, lo último que se guardó.
 */
class SincronizacionTramo(private val context: Context, private val repository: RoadbookRepository) {

    private val api = ApiCliente(context)

    suspend fun sincronizar(tramoLocalId: Long): ResultadoTramo = candado.withLock {
        val tramo = repository.getTramoById(tramoLocalId) ?: return ResultadoTramo(false, "No se encontró el tramo.")
        val rally = repository.getRallyById(tramo.rallyId)
        try {
            // 0) El tramo: si se creó en la tablet, se da de alta en la plataforma (con su uid,
            //    así un reintento no lo duplica); si se cambiaron sus datos, se mandan.
            var tramoWebId = tramo.webId
            var recienCreado = false
            var cambiosTramo = 0
            if (tramoWebId == null) {
                val rallyWebId = rally?.webId
                    ?: return ResultadoTramo(false, "${tramo.identificador}: el rally está solo en la tablet. Los rallies se crean en la plataforma.")
                val r = api.post("/api/tramos", cuerpoAltaTramo(tramo, rallyWebId))
                tramoWebId = r?.takeIf { it.isJsonObject }?.asJsonObject?.entero("id")
                    ?: throw ErrorServidor(0, "el servidor no devolvió el id del tramo nuevo")
                repository.marcarTramoSubido(tramo.id, tramoWebId)
                recienCreado = true
                cambiosTramo++
            }
            var datosTramo = detalleTramo(tramoWebId)
            if (tramo.pendienteSubir && !recienCreado) {
                val base = tramo.datosServidor?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
                api.put("/api/tramos/$tramoWebId", cuerpoTramo(tramo, base, datosTramo))
                repository.marcarTramoSubido(tramo.id, tramoWebId)
                cambiosTramo++
                datosTramo = detalleTramo(tramoWebId)
            }
            var lista = indicacionesDe(datosTramo)
            val locales = repository.getVinetasSync(tramoLocalId)

            // Viñetas de antes de esta versión: no tienen uid de la plataforma y no se pueden
            // emparejar. Si la plataforma no tiene ninguna, se suben; si tiene, no se mezclan.
            val viejas = locales.filter { it.webId == null && !it.pendienteSubir }
            if (viejas.isNotEmpty() && locales.none { it.webId != null }) {
                if (lista.isEmpty()) {
                    viejas.forEach { repository.updateVineta(it) } // quedan para subir
                } else {
                    return ResultadoTramo(
                        false,
                        "${tramo.identificador}: la tablet tiene ${viejas.size} viñetas de antes que no se pueden emparejar con " +
                            "las ${lista.size} de la plataforma. No se mezcló nada."
                    )
                }
            }

            var enviadas = cambiosTramo

            // 1) Las borradas en la tablet
            for (b in repository.getBajasPendientes(tramoLocalId)) {
                try {
                    api.delete("/api/indicaciones/${b.webId}")
                } catch (e: ErrorServidor) {
                    if (e.codigo != 404) throw e // si ya no estaba, listo
                }
                repository.borrarBajaPendiente(b.id)
                enviadas++
            }

            // 2) Las cambiadas y las nuevas, en el orden del tramo
            val bib = BibliotecaVineta.obtener(context)
            val pendientes = repository.getVinetasPendientes(tramoLocalId)
            for (v in pendientes) {
                val todas = repository.getVinetasSync(tramoLocalId)
                val srv = buscar(lista, v)
                val local = camposLocales(v, v.dibujo ?: VinetaDibujo.paraGuardar(v.dibujoV1(bib)).first)
                val base = v.datosServidor?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
                if (srv != null) {
                    val idSrv = srv.entero("id") ?: continue
                    api.put("/api/indicaciones/$idSrv", cuerpoPut(local, base, srv, v, conDistancia = true))
                    repository.marcarVinetaSubida(v.id, idSrv)
                } else {
                    // Nueva (o borrada en la plataforma mientras se cambiaba en la tablet: no se pierde)
                    val i = todas.indexOfFirst { it.id == v.id }
                    val previa = if (i > 0) todas[i - 1] else null
                    val despuesDeOrden = previa?.let { p -> buscar(lista, p)?.entero("orden") } ?: 0
                    val cuerpo = cuerpoPut(local, null, null, v, conDistancia = true).apply {
                        addProperty("despuesDeOrden", despuesDeOrden)
                        addProperty("uid", v.uid)
                        if (!has("origenCoordenada") || get("origenCoordenada").isJsonNull) addProperty("origenCoordenada", v.origenCoordenada ?: "TABLET")
                        // Notas guardadas (por ejemplo, de un tramo importado de un archivo)
                        base?.get("notas")?.takeIf { !it.isJsonNull }?.let { add("notas", it) }
                    }
                    val resp = api.post("/api/tramos/$tramoWebId/indicaciones/insertar", cuerpo)
                    lista = detalle(tramoWebId)
                    val creada = buscar(lista, v)
                    val idSrv = creada?.entero("id") ?: idDeRespuesta(resp)
                    // Por si el alta no toma el dibujo y los textos: se completan con un PUT (sin la distancia)
                    if (idSrv != null) {
                        val cuerpoCompleto = cuerpoPut(local, null, creada, v, conDistancia = false)
                        if (cuerpoCompleto.get("notas")?.isJsonNull != false) base?.get("notas")?.takeIf { !it.isJsonNull }?.let { cuerpoCompleto.add("notas", it) }
                        api.put("/api/indicaciones/$idSrv", cuerpoCompleto)
                    }
                    repository.marcarVinetaSubida(v.id, idSrv)
                }
                enviadas++
            }

            // 3) Lo que quedó en la plataforma, a la tablet (el tramo y sus viñetas)
            if (enviadas > 0) { datosTramo = detalleTramo(tramoWebId); lista = indicacionesDe(datosTramo) }
            repository.getTramoById(tramoLocalId)?.let { actual ->
                if (!actual.pendienteSubir) repository.guardarTramoDesdeServidor(tramoDeServidor(datosTramo, actual.rallyId, actual))
            }
            guardarDetalle(tramoLocalId, lista)
            return ResultadoTramo(
                true,
                if (enviadas > 0) "${tramo.identificador}: $enviadas cambios enviados a la plataforma." else "${tramo.identificador}: al día con la plataforma.",
                enviadas
            )
        } catch (e: SesionVencida) {
            return ResultadoTramo(false, e.message ?: "La sesión venció.")
        } catch (e: ErrorServidor) {
            return ResultadoTramo(false, "${tramo.identificador}: ${e.message}")
        } catch (e: Exception) {
            return ResultadoTramo(false, "Sin conexión con la plataforma: los cambios quedan guardados en la tablet y se mandan cuando haya señal.", sinConexion = true)
        }
    }

    private suspend fun detalleTramo(tramoWebId: Int): JsonObject =
        api.get("/api/tramos/$tramoWebId")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: throw ErrorServidor(0, "el servidor no devolvió el tramo")

    private fun indicacionesDe(d: JsonObject): List<JsonObject> =
        d.get("indicaciones")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }.orEmpty()

    private suspend fun detalle(tramoWebId: Int): List<JsonObject> = indicacionesDe(detalleTramo(tramoWebId))

    private fun buscar(lista: List<JsonObject>, v: VinetaEntity): JsonObject? =
        lista.firstOrNull { it.texto("uid").equals(v.uid, ignoreCase = true) }
            ?: v.webId?.let { w -> lista.firstOrNull { it.entero("id") == w && it.texto("uid").isBlank() } }

    private fun idDeRespuesta(r: JsonElement?): Int? {
        val o = r?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        return o.entero("id") ?: o.get("indicacion")?.takeIf { it.isJsonObject }?.asJsonObject?.entero("id")
    }

    /** Guarda en la tablet lo que hay en la plataforma, sin pisar lo que falta subir. */
    private suspend fun guardarDetalle(tramoLocalId: Long, lista: List<JsonObject>) {
        val locales = repository.getVinetasSync(tramoLocalId)
        val bajas = repository.getBajasPendientes(tramoLocalId).map { it.uid.lowercase() }.toSet()
        val enServidor = HashSet<String>()
        for (o in lista) {
            val uid = o.texto("uid").lowercase()
            if (uid.isNotBlank()) enServidor += uid
            if (uid in bajas) continue
            val existente = locales.firstOrNull { it.uid.equals(uid, true) && uid.isNotBlank() }
                ?: locales.firstOrNull { it.webId != null && it.webId == o.entero("id") }
            if (existente?.pendienteSubir == true) continue
            repository.guardarDesdeServidor(deServidor(o, tramoLocalId, existente))
        }
        // Las que la plataforma ya no tiene (borradas allá) y no tienen cambios de la tablet
        val idsServidor = lista.mapNotNull { it.entero("id") }.toSet()
        for (v in locales) {
            if (!v.pendienteSubir && v.webId != null && v.uid.lowercase() !in enServidor && v.webId !in idsServidor) {
                repository.borrarDesdeServidor(v)
            }
        }
        repository.renumerarVinetas(tramoLocalId)
    }

    companion object {
        private val candado = Mutex()

        /** Los campos que manda la plataforma en el PUT (App.tsx, enviarIndicacion). */
        private val CAMPOS = listOf(
            "dibujo", "descripcion", "iconoAlerta", "latitud", "longitud", "rumboCap",
            "velocidadPromedioKmH", "tiempoSectorSegundos", "velocidadMaximaKmH", "tipoTrazado",
            "resetOdometro", "distanciaOculta", "ocultaOrganizacion", "coordenadaGpsExplicita",
            "esTierra", "tipoWaypoint"
        )

        /** Tramo del servidor → tramo de la tablet. Los datos van tal cual (sin valores inventados). */
        fun tramoDeServidor(o: JsonObject, rallyId: Long, existente: TramoEntity?): TramoEntity {
            val base = existente ?: TramoEntity(rallyId = rallyId, identificador = "", nombre = "")
            return base.copy(
                rallyId = rallyId,
                webId = o.entero("id") ?: base.webId,
                uid = o.texto("uid").ifBlank { base.uid },
                tipo = o.texto("tipo").ifBlank { "PE" },
                identificador = o.texto("identificador"),
                nombre = o.texto("nombre"),
                numeroSector = o.entero("numeroSector") ?: base.numeroSector,
                chInicio = o.texto("chInicio"),
                chFin = o.texto("chFin"),
                distanciaTotalEstimada = o.decimal("distanciaTotalKm") ?: o.decimal("distanciaEstimadaKm") ?: 0.0,
                tiempoOtorgado = o.texto("tiempoOtorgado"),
                atrasoMaximo = o.texto("atrasoMaximo"),
                horaPrimerAuto = o.texto("horaPrimerAuto"),
                velocidadPromedioObjetivoKmh = o.decimal("velocidadPromedioGeneralKmH") ?: 0.0,
                velocidadMaximaPermitidaKmh = o.decimal("velocidadMaximaEtapaKmH") ?: 0.0,
                datosServidor = o.toString(),
                pendienteSubir = false,
                updatedAt = System.currentTimeMillis()
            )
        }

        /** Datos del tramo de la tablet con los nombres del servidor (los del PUT de la plataforma). */
        private fun camposTramo(t: TramoEntity): JsonObject = JsonObject().apply {
            addProperty("tipo", t.tipo)
            addProperty("identificador", t.identificador.uppercase())
            addProperty("nombre", t.nombre.uppercase())
            addProperty("numeroSector", t.numeroSector)
            addProperty("chInicio", t.chInicio.uppercase())
            addProperty("chFin", t.chFin.uppercase())
            addProperty("distanciaEstimadaKm", t.distanciaTotalEstimada)
            addProperty("tiempoOtorgado", t.tiempoOtorgado.uppercase())
            addProperty("atrasoMaximo", t.atrasoMaximo.uppercase())
            addProperty("horaPrimerAuto", t.horaPrimerAuto)
            add("velocidadPromedioGeneralKmH", t.velocidadPromedioObjetivoKmh.takeIf { it > 0 }?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
            add("velocidadMaximaEtapaKmH", t.velocidadMaximaPermitidaKmh.takeIf { it > 0 }?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
        }

        /** En el GET la distancia del tramo se llama distanciaTotalKm; en el PUT, distanciaEstimadaKm. */
        private fun claveGet(k: String) = if (k == "distanciaEstimadaKm") "distanciaTotalKm" else k

        /**
         * PUT del tramo (revisión 19): el tramo ENTERO, como está en la plataforma ([actual], recién
         * bajado), cambiando solo lo que se tocó en la tablet respecto de cómo vino ([base]).
         * Van todos los campos, porque el servidor deja vacíos chInicio, chFin, tiempoOtorgado,
         * atrasoMaximo, los dos promedios y tiempoTotalEstimado si no vienen.
         * No se mandan el uid ni el rally (no se cambian con este pedido). distanciaEstimadaKm
         * solo cuenta mientras el tramo no tiene viñetas.
         */
        fun cuerpoTramo(t: TramoEntity, base: JsonObject?, actual: JsonObject): JsonObject {
            val local = camposTramo(t)
            val cuerpo = JsonObject()
            fun deServidor(k: String): JsonElement =
                actual.get(claveGet(k))?.takeIf { !it.isJsonNull } ?: base?.get(claveGet(k))?.takeIf { !it.isJsonNull } ?: JsonNull.INSTANCE
            // Lo que la tablet no maneja va como está en la plataforma
            cuerpo.add("etapaId", actual.get("etapaId")?.takeIf { !it.isJsonNull } ?: JsonPrimitive(0))
            cuerpo.add("tiempoTotalEstimado", deServidor("tiempoTotalEstimado"))
            for ((k, l) in local.entrySet()) {
                val cambio = base == null || !igual(k, l, base.get(claveGet(k)))
                cuerpo.add(k, if (cambio) l else deServidor(k))
            }
            return cuerpo
        }

        /** Alta del tramo creado en la tablet: lo obligatorio, el uid y lo que se haya cargado. */
        fun cuerpoAltaTramo(t: TramoEntity, rallyWebId: Int): JsonObject = JsonObject().apply {
            addProperty("rallyId", rallyWebId)
            addProperty("tipo", t.tipo.ifBlank { "PE" })
            addProperty("identificador", t.identificador.uppercase())
            addProperty("nombre", t.nombre.uppercase())
            addProperty("uid", t.uid)
            addProperty("numeroSector", t.numeroSector)
            if (t.chInicio.isNotBlank()) addProperty("chInicio", t.chInicio.uppercase())
            if (t.chFin.isNotBlank()) addProperty("chFin", t.chFin.uppercase())
            if (t.distanciaTotalEstimada > 0) addProperty("distanciaEstimadaKm", t.distanciaTotalEstimada)
            if (t.tiempoOtorgado.isNotBlank()) addProperty("tiempoOtorgado", t.tiempoOtorgado.uppercase())
            if (t.atrasoMaximo.isNotBlank()) addProperty("atrasoMaximo", t.atrasoMaximo.uppercase())
            if (t.horaPrimerAuto.isNotBlank()) addProperty("horaPrimerAuto", t.horaPrimerAuto)
            if (t.velocidadPromedioObjetivoKmh > 0) addProperty("velocidadPromedioGeneralKmH", t.velocidadPromedioObjetivoKmh)
            if (t.velocidadMaximaPermitidaKmh > 0) addProperty("velocidadMaximaEtapaKmH", t.velocidadMaximaPermitidaKmh)
        }

        /** Viñeta del servidor → viñeta de la tablet (lo que es solo de la tablet se conserva). */
        fun deServidor(o: JsonObject, tramoId: Long, existente: VinetaEntity?): VinetaEntity {
            val base = existente ?: VinetaEntity(tramoId = tramoId, numero = 0, distanciaTotal = 0.0, distanciaParcial = 0.0, latitud = 0.0, longitud = 0.0)
            val tipoWpt = o.texto("tipoWaypoint")
            return base.copy(
                tramoId = tramoId,
                numero = o.entero("orden") ?: base.numero,
                distanciaTotal = o.decimal("distanciaAcumuladaKm") ?: base.distanciaTotal,
                distanciaParcial = o.decimal("distanciaParcialKm") ?: base.distanciaParcial,
                latitud = o.decimal("latitud") ?: 0.0,
                longitud = o.decimal("longitud") ?: 0.0,
                rumbo = o.decimal("rumboCap")?.toFloat() ?: 0f,
                dibujo = o.texto("dibujo"),
                informacion = o.texto("descripcion"),
                peligro = o.texto("iconoAlerta"),
                velocidadPromedioSectorKmh = o.decimal("velocidadPromedioKmH") ?: 0.0,
                tiempoIdealSegundos = o.decimal("tiempoSectorSegundos") ?: 0.0,
                velocidadMaximaSectorKmh = o.decimal("velocidadMaximaKmH") ?: 0.0,
                esOffRoad = o.texto("tipoTrazado").equals("OFFROAD", true),
                esReinicioCero = o.booleano("resetOdometro"),
                distanciaOculta = o.booleano("distanciaOculta"),
                esOcultaOrg = o.booleano("ocultaOrganizacion"),
                mostrarWpt = o.booleano("coordenadaGpsExplicita"),
                esTierra = o.booleano("esTierra"),
                tipoPuntoControl = tipoWpt,
                esPuntoControl = tipoWpt.isNotBlank(),
                uid = o.texto("uid").ifBlank { base.uid },
                origenCoordenada = o.texto("origenCoordenada").ifBlank { null },
                coordenadaFechaUtc = o.texto("coordenadaFechaUtc").ifBlank { null },
                coordenadaPor = o.texto("coordenadaPor").ifBlank { null },
                precisionGpsM = o.decimal("precisionGpsM"),
                verificadaTerreno = o.booleano("verificadaTerreno"),
                latitudAnterior = o.decimal("latitudAnterior"),
                longitudAnterior = o.decimal("longitudAnterior"),
                webId = o.entero("id"),
                datosServidor = o.toString(),
                pendienteSubir = false
            )
        }

        /** Los valores de la tablet con los nombres del servidor. */
        fun camposLocales(v: VinetaEntity, dibujo: String): JsonObject = JsonObject().apply {
            val hayCoord = !(v.latitud == 0.0 && v.longitud == 0.0)
            addProperty("dibujo", dibujo)
            addProperty("descripcion", v.informacion.uppercase())
            add("iconoAlerta", v.peligro.ifBlank { null }?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
            add("latitud", if (hayCoord) JsonPrimitive(v.latitud) else JsonNull.INSTANCE)
            add("longitud", if (hayCoord) JsonPrimitive(v.longitud) else JsonNull.INSTANCE)
            add("rumboCap", if (v.rumbo != 0f) JsonPrimitive(v.rumbo.toDouble()) else JsonNull.INSTANCE)
            add("velocidadPromedioKmH", v.velocidadPromedioSectorKmh.takeIf { it > 0 }?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
            add("tiempoSectorSegundos", v.tiempoIdealSegundos.takeIf { it > 0 }?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
            add("velocidadMaximaKmH", v.velocidadMaximaSectorKmh.takeIf { it > 0 }?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
            addProperty("tipoTrazado", if (v.esOffRoad) "OFFROAD" else "SNAP")
            addProperty("resetOdometro", v.esReinicioCero)
            addProperty("distanciaOculta", v.distanciaOculta)
            addProperty("ocultaOrganizacion", v.esOcultaOrg)
            addProperty("coordenadaGpsExplicita", v.mostrarWpt)
            addProperty("esTierra", v.esTierra)
            add("tipoWaypoint", v.tipoPuntoControl.ifBlank { null }?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE)
        }

        /**
         * Cuerpo del PUT: la viñeta como está en la plataforma ([actual]) con lo que cambió en la
         * tablet respecto de cómo vino ([base]). Sin [base] (viñeta nueva), va todo lo de la tablet.
         */
        fun cuerpoPut(local: JsonObject, base: JsonObject?, actual: JsonObject?, v: VinetaEntity, conDistancia: Boolean): JsonObject {
            val cuerpo = JsonObject()
            for (k in CAMPOS) {
                val l = local.get(k)
                val cambio = base == null || !igual(k, l, base.get(k))
                cuerpo.add(k, if (cambio || actual == null) (l ?: JsonNull.INSTANCE) else (actual.get(k) ?: JsonNull.INSTANCE))
            }
            // Las notas no se editan en la tablet: van como están en la plataforma
            cuerpo.add("notas", actual?.get("notas") ?: base?.get("notas") ?: JsonNull.INSTANCE)

            // Coordenada: si la tablet la cambió, manda la de la tablet con su origen
            val cambioCoord = base == null || !igual("latitud", local.get("latitud"), base.get("latitud")) ||
                !igual("longitud", local.get("longitud"), base.get("longitud"))
            if (cambioCoord && !local.get("latitud").isJsonNull) {
                cuerpo.add("latitud", local.get("latitud"))
                cuerpo.add("longitud", local.get("longitud"))
                cuerpo.addProperty("origenCoordenada", v.origenCoordenada ?: "TABLET")
                v.precisionGpsM?.let { cuerpo.addProperty("precisionGpsM", it) }
            }
            // "Está bien": la tablet confirmó la coordenada en el terreno
            if (v.verificadaTerreno && (base == null || !base.booleano("verificadaTerreno"))) {
                cuerpo.addProperty("verificadaTerreno", true)
            }
            // Distancia: el parcial, solo si cambió (el servidor corre las siguientes)
            if (conDistancia) {
                val antes = base?.decimal("distanciaParcialKm")
                if (antes == null || abs(v.distanciaParcial - antes) >= 0.0005) {
                    cuerpo.addProperty("distanciaParcialKm", Math.round(v.distanciaParcial * 1000.0) / 1000.0)
                }
            }
            return cuerpo
        }

        /** Mismo valor (null, "" y false/0 en los campos que la plataforma manda vacíos cuentan igual). */
        private fun igual(campo: String, a: JsonElement?, b: JsonElement?): Boolean {
            fun vacio(e: JsonElement?) = e == null || e.isJsonNull || (e.isJsonPrimitive && (
                (e.asJsonPrimitive.isString && e.asString.isEmpty()) ||
                    (e.asJsonPrimitive.isBoolean && !e.asBoolean) ||
                    (e.asJsonPrimitive.isNumber && e.asDouble == 0.0)))
            if (vacio(a) && vacio(b)) return true
            if (a == null || b == null || a.isJsonNull || b.isJsonNull) return false
            val pa = a.asJsonPrimitive
            val pb = b.asJsonPrimitive
            return when {
                pa.isNumber || pb.isNumber -> runCatching { abs(a.asDouble - b.asDouble) < 1e-7 }.getOrDefault(false)
                pa.isBoolean || pb.isBoolean -> runCatching { a.asBoolean == b.asBoolean }.getOrDefault(false)
                campo in setOf("descripcion", "identificador", "nombre", "chInicio", "chFin", "tiempoOtorgado", "atrasoMaximo") ->
                    a.asString.equals(b.asString, ignoreCase = true)
                campo == "tipoTrazado" -> a.asString.ifBlank { "SNAP" }.equals(b.asString.ifBlank { "SNAP" }, true)
                else -> a.asString == b.asString
            }
        }
    }
}

private fun JsonObject.valor(k: String): JsonElement? = get(k)?.takeIf { !it.isJsonNull }
private fun JsonObject.texto(k: String): String = try { valor(k)?.asString ?: "" } catch (e: Exception) { "" }
private fun JsonObject.entero(k: String): Int? = try { valor(k)?.asInt } catch (e: Exception) { null }
private fun JsonObject.decimal(k: String): Double? = try { valor(k)?.asDouble } catch (e: Exception) { null }
private fun JsonObject.booleano(k: String): Boolean = try { valor(k)?.asBoolean ?: false } catch (e: Exception) { false }
