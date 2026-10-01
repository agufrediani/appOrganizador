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
    suspend fun insertTramo(tramo: TramoEntity): Long = db.tramoDao().insertTramo(tramo)
    suspend fun updateTramo(tramo: TramoEntity) = db.tramoDao().updateTramo(tramo)
    suspend fun updateDistanciaYEstado(tramoId: Long, distancia: Double, estado: String) =
        db.tramoDao().updateDistanciaYEstado(tramoId, distancia, estado)
    suspend fun deleteTramo(tramo: TramoEntity) = db.tramoDao().deleteTramo(tramo)

    // --- VIÑETAS ---
    fun getVinetasByTramo(tramoId: Long): Flow<List<VinetaEntity>> = db.vinetaDao().getVinetasByTramo(tramoId)
    suspend fun getUltimaVineta(tramoId: Long): VinetaEntity? = db.vinetaDao().getUltimaVineta(tramoId)
    suspend fun getCantidadVinetas(tramoId: Long): Int = db.vinetaDao().getCantidadVinetas(tramoId)
    suspend fun insertVineta(vineta: VinetaEntity): Long = db.vinetaDao().insertVineta(vineta)
    suspend fun updateVineta(vineta: VinetaEntity) = db.vinetaDao().updateVineta(vineta)
    suspend fun deleteVineta(vineta: VinetaEntity) = db.vinetaDao().deleteVineta(vineta)

    suspend fun propagarDiferenciaKilometrica(tramoId: Long, desdeNumero: Int, deltaKm: Double) {
        val posteriores = db.vinetaDao().getVinetasPosteriores(tramoId, desdeNumero)
        if (posteriores.isNotEmpty()) {
            val actualizadas = posteriores.map { v ->
                v.copy(distanciaTotal = (v.distanciaTotal + deltaKm).coerceAtLeast(0.0))
            }
            db.vinetaDao().updateVinetas(actualizadas)
        }
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

    // --- PUNTOS DE INTERÉS (MAPA LIBRE) ---
    fun getPuntosInteres(rallyId: Long): Flow<List<PuntoInteresEntity>> = db.puntoInteresDao().getPuntosByRally(rallyId)
    fun getAllPuntosInteres(): Flow<List<PuntoInteresEntity>> = db.puntoInteresDao().getAllPuntos()
    suspend fun insertPuntoInteres(punto: PuntoInteresEntity) = db.puntoInteresDao().insertPunto(punto)
    suspend fun deletePuntoInteres(punto: PuntoInteresEntity) = db.puntoInteresDao().deletePunto(punto)
}
