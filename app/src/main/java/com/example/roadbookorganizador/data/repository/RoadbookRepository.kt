package com.example.roadbookorganizador.data.repository

import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class RoadbookRepository(private val db: AppDatabase) {

    // --- RALLIES ---
    fun getAllRallies(): Flow<List<RallyEntity>> = db.rallyDao().getAllRallies()
    fun getActiveRally(): Flow<RallyEntity?> = db.rallyDao().getActiveRally()
    suspend fun getRallyById(id: Long): RallyEntity? = db.rallyDao().getRallyById(id)
    suspend fun getRallyByWebId(webId: Int): RallyEntity? = db.rallyDao().getRallyByWebId(webId)
    suspend fun insertRally(rally: RallyEntity): Long = db.rallyDao().insertRally(rally)
    suspend fun updateRally(rally: RallyEntity) = db.rallyDao().updateRally(rally)
    suspend fun deleteRally(rally: RallyEntity) = db.rallyDao().deleteRally(rally)

    // --- TRAMOS ---
    fun getTramosByRally(rallyId: Long): Flow<List<TramoEntity>> = db.tramoDao().getTramosByRally(rallyId)
    fun getTramoFlow(tramoId: Long): Flow<TramoEntity?> = db.tramoDao().getTramoFlowById(tramoId)
    suspend fun getTramoById(tramoId: Long): TramoEntity? = db.tramoDao().getTramoById(tramoId)
    suspend fun getTramoByRallyAndWebId(rallyId: Long, webId: Int): TramoEntity? = db.tramoDao().getTramoByRallyAndWebId(rallyId, webId)
    // Lo que se crea o cambia en la tablet queda marcado para subir a la plataforma
    suspend fun insertTramo(tramo: TramoEntity): Long = db.tramoDao().insertTramo(tramo.copy(pendienteSubir = true))
    suspend fun updateTramo(tramo: TramoEntity) = db.tramoDao().updateTramo(tramo.copy(pendienteSubir = true))
    /** Guarda lo que vino del servidor tal cual (no queda para subir). */
    suspend fun guardarTramoDesdeServidor(tramo: TramoEntity): Long =
        if (tramo.id == 0L) db.tramoDao().insertTramo(tramo.copy(pendienteSubir = false))
        else { db.tramoDao().updateTramo(tramo.copy(pendienteSubir = false)); tramo.id }
    suspend fun getTramoByUid(rallyId: Long, uid: String): TramoEntity? = db.tramoDao().getTramoByUid(rallyId, uid)
    suspend fun getTodosLosTramos(): List<TramoEntity> = db.tramoDao().getTodosSync()
    suspend fun marcarTramoSubido(id: Long, webId: Int?) = db.tramoDao().marcarSubido(id, webId)
    suspend fun guardarDatosServidorTramo(id: Long, datos: String?) = db.tramoDao().guardarDatosServidor(id, datos)
    suspend fun updateDistanciaYEstado(tramoId: Long, distancia: Double, estado: String) =
        db.tramoDao().updateDistanciaYEstado(tramoId, distancia, estado)
    suspend fun deleteTramo(tramo: TramoEntity) = db.tramoDao().deleteTramo(tramo)

    // --- VIÑETAS ---
    fun getVinetasByTramo(tramoId: Long): Flow<List<VinetaEntity>> = db.vinetaDao().getVinetasByTramo(tramoId)
    suspend fun getUltimaVineta(tramoId: Long): VinetaEntity? = db.vinetaDao().getUltimaVineta(tramoId)
    suspend fun getCantidadVinetas(tramoId: Long): Int = db.vinetaDao().getCantidadVinetas(tramoId)
    // Cada alta, baja o edición deja las viñetas numeradas 1, 2, 3… en orden de distancia,
    // sin números repetidos ni huecos.
    // Todo lo que se cambia en la tablet queda marcado para subir a la plataforma.
    suspend fun insertVineta(vineta: VinetaEntity): Long {
        val id = db.vinetaDao().insertVineta(vineta.copy(pendienteSubir = true))
        renumerarVinetas(vineta.tramoId)
        return id
    }
    suspend fun updateVineta(vineta: VinetaEntity) {
        db.vinetaDao().updateVineta(vineta.copy(pendienteSubir = true))
        renumerarVinetas(vineta.tramoId)
    }
    suspend fun deleteVineta(vineta: VinetaEntity) {
        // Si ya estaba en la plataforma, se anota para borrarla allá también
        vineta.webId?.let { db.vinetaDao().insertBaja(BajaPendienteEntity(tramoId = vineta.tramoId, webId = it, uid = vineta.uid)) }
        db.vinetaDao().deleteVineta(vineta)
        renumerarVinetas(vineta.tramoId)
    }

    // --- SINCRONIZACIÓN (sin marcar "falta subir") ---
    suspend fun getVinetasSync(tramoId: Long): List<VinetaEntity> = db.vinetaDao().getVinetasByTramoSync(tramoId)
    suspend fun getVinetasPendientes(tramoId: Long): List<VinetaEntity> = db.vinetaDao().getPendientes(tramoId)
    fun contarPendientes(tramoId: Long): Flow<Int> = db.vinetaDao().contarPendientes(tramoId)
    suspend fun marcarVinetaSubida(id: Long, webId: Int?) = db.vinetaDao().marcarSubida(id, webId)
    suspend fun getBajasPendientes(tramoId: Long): List<BajaPendienteEntity> = db.vinetaDao().getBajas(tramoId)
    suspend fun borrarBajaPendiente(id: Long) = db.vinetaDao().borrarBaja(id)
    /** Guarda lo que vino del servidor tal cual (no queda para subir). */
    suspend fun guardarDesdeServidor(vineta: VinetaEntity): Long =
        if (vineta.id == 0L) db.vinetaDao().insertVineta(vineta.copy(pendienteSubir = false))
        else { db.vinetaDao().updateVineta(vineta.copy(pendienteSubir = false)); vineta.id }
    suspend fun guardarGeometria(vinetaId: Long, geometria: String?) = db.vinetaDao().setGeometria(vinetaId, geometria)
    suspend fun getCaminosPendientes(tramoId: Long) = db.vinetaDao().getCaminosPendientes(tramoId)
    /** Cuántos cambios de la tablet faltan subir a la plataforma (tramos, viñetas y borradas). */
    suspend fun contarPendientesTodos(): Int = db.tramoDao().contarPendientesTodos() + db.vinetaDao().contarPendientesTodos()
    suspend fun contarCaminosPendientes(tramoId: Long) = db.vinetaDao().contarCaminosPendientes(tramoId)
    suspend fun marcarCaminoSubido(vinetaId: Long) = db.vinetaDao().marcarCaminoSubido(vinetaId)
    suspend fun borrarDesdeServidor(vineta: VinetaEntity) = db.vinetaDao().deleteVineta(vineta)

    suspend fun renumerarVinetas(tramoId: Long) {
        val dao = db.vinetaDao()
        dao.getVinetasByTramoSync(tramoId).forEachIndexed { i, v ->
            if (v.numero != i + 1) dao.setNumero(v.id, i + 1)
        }
    }

    suspend fun propagarDiferenciaKilometrica(tramoId: Long, desdeNumero: Int, deltaKm: Double) {
        val posteriores = db.vinetaDao().getVinetasPosteriores(tramoId, desdeNumero)
        if (posteriores.isNotEmpty()) {
            val actualizadas = posteriores.map { v ->
                v.copy(distanciaTotal = (v.distanciaTotal + deltaKm).coerceAtLeast(0.0))
            }
            db.vinetaDao().updateVinetas(actualizadas)
        }
        renumerarVinetas(tramoId)
    }

    // --- CALIBRACIÓN ---
    fun getCalibracionActiva(): Flow<CalibracionEntity?> = db.calibracionDao().getCalibracionActiva()
    suspend fun getFactorCalibracionActivo(): Double {
        val cal = db.calibracionDao().getCalibracionActivaSync()
        return cal?.factorCorreccion ?: 1.0
    }
    suspend fun guardarCalibracion(vehiculo: String, medidoMetros: Double, oficialMetros: Double = 1000.0): Long {
        val factor = if (medidoMetros > 0) oficialMetros / medidoMetros else 1.0
        val cal = CalibracionEntity(
            vehiculoNombre = vehiculo,
            distanciaOficialMetros = oficialMetros,
            distanciaMedidaMetros = medidoMetros,
            factorCorreccion = factor,
            activo = true
        )
        val id = db.calibracionDao().insertCalibracion(cal)
        db.calibracionDao().desactivarOtras(id)
        return id
    }

    // --- TRACKPOINTS ---
    fun getTrackPoints(tramoId: Long): Flow<List<TrackPointEntity>> = db.trackPointDao().getTrackPointsByTramo(tramoId)
    suspend fun getTrackPointsSync(tramoId: Long): List<TrackPointEntity> = db.trackPointDao().getTrackPointsByTramoSync(tramoId)
    suspend fun insertTrackPoint(point: TrackPointEntity) = db.trackPointDao().insertTrackPoint(point)

    // --- ACTIVAR RALLY ---
    suspend fun setRallyActivo(rallyId: Long) = db.rallyDao().setRallyActivo(rallyId)
    suspend fun deseleccionarTodosRallies() = db.rallyDao().deseleccionarTodosRallies()

    // --- PUNTOS DE INTERÉS (MAPA LIBRE) ---
    fun getPuntosInteres(rallyId: Long): Flow<List<PuntoInteresEntity>> = db.puntoInteresDao().getPuntosByRally(rallyId)
    fun getAllPuntosInteres(): Flow<List<PuntoInteresEntity>> = db.puntoInteresDao().getAllPuntos()
    suspend fun insertPuntoInteres(punto: PuntoInteresEntity) = db.puntoInteresDao().insertPunto(punto)
    suspend fun deletePuntoInteres(punto: PuntoInteresEntity) = db.puntoInteresDao().deletePunto(punto)
    suspend fun getPuntosInteresSync(rallyId: Long): List<PuntoInteresEntity> = db.puntoInteresDao().getPuntosSync(rallyId)
    suspend fun updatePuntoInteres(punto: PuntoInteresEntity) = db.puntoInteresDao().updatePunto(punto)
}
