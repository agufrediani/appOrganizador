package com.example.roadbookorganizador.data.local.dao

import androidx.room.*
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VinetaDao {
    @Query("SELECT * FROM vinetas WHERE tramoId = :tramoId ORDER BY distanciaTotal ASC, numero ASC")
    fun getVinetasByTramo(tramoId: Long): Flow<List<VinetaEntity>>

    @Query("SELECT * FROM vinetas WHERE tramoId = :tramoId ORDER BY distanciaTotal ASC, numero ASC, id ASC")
    suspend fun getVinetasByTramoSync(tramoId: Long): List<VinetaEntity>

    @Query("UPDATE vinetas SET numero = :numero WHERE id = :id")
    suspend fun setNumero(id: Long, numero: Int)

    @Query("SELECT * FROM vinetas WHERE tramoId = :tramoId ORDER BY distanciaTotal DESC, numero DESC LIMIT 1")
    suspend fun getUltimaVineta(tramoId: Long): VinetaEntity?

    @Query("SELECT COUNT(*) FROM vinetas WHERE tramoId = :tramoId")
    suspend fun getCantidadVinetas(tramoId: Long): Int

    @Query("SELECT * FROM vinetas WHERE id = :id")
    suspend fun getVinetaById(id: Long): VinetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVineta(vineta: VinetaEntity): Long

    @Update
    suspend fun updateVineta(vineta: VinetaEntity)

    @Update
    suspend fun updateVinetas(vinetas: List<VinetaEntity>)

    @Query("SELECT * FROM vinetas WHERE tramoId = :tramoId AND numero > :numero ORDER BY numero ASC")
    suspend fun getVinetasPosteriores(tramoId: Long, numero: Int): List<VinetaEntity>

    @Delete
    suspend fun deleteVineta(vineta: VinetaEntity)

    @Query("DELETE FROM vinetas WHERE tramoId = :tramoId")
    suspend fun clearVinetasByTramo(tramoId: Long)

    // ---- Sincronización con la plataforma ----
    @Query("SELECT * FROM vinetas WHERE tramoId = :tramoId AND pendienteSubir = 1 ORDER BY distanciaTotal ASC, numero ASC, id ASC")
    suspend fun getPendientes(tramoId: Long): List<VinetaEntity>

    @Query("SELECT COUNT(*) FROM vinetas WHERE tramoId = :tramoId AND pendienteSubir = 1")
    fun contarPendientes(tramoId: Long): Flow<Int>

    /**
     * Guarda el camino desde la anterior sin marcar la viñeta como cambiada (es solo para el
     * mapa); queda marcado para subirlo aparte con `PUT /api/indicaciones/{id}/camino`.
     */
    @Query("UPDATE vinetas SET geometriaDesdeAnterior = :geometria, caminoPendiente = 1 WHERE id = :id")
    suspend fun setGeometria(id: Long, geometria: String?)

    @Query("SELECT * FROM vinetas WHERE tramoId = :tramoId AND caminoPendiente = 1 AND webId IS NOT NULL ORDER BY distanciaTotal ASC, numero ASC")
    suspend fun getCaminosPendientes(tramoId: Long): List<VinetaEntity>

    @Query("SELECT COUNT(*) FROM vinetas WHERE tramoId = :tramoId AND caminoPendiente = 1")
    suspend fun contarCaminosPendientes(tramoId: Long): Int

    /** Todo lo que falta subir en la tablet: viñetas cambiadas y viñetas borradas. */
    @Query("SELECT (SELECT COUNT(*) FROM vinetas WHERE pendienteSubir = 1) + (SELECT COUNT(*) FROM bajas_pendientes)")
    suspend fun contarPendientesTodos(): Int

    @Query("UPDATE vinetas SET caminoPendiente = 0 WHERE id = :id")
    suspend fun marcarCaminoSubido(id: Long)

    @Query("UPDATE vinetas SET pendienteSubir = 0, webId = :webId WHERE id = :id")
    suspend fun marcarSubida(id: Long, webId: Int?)

    @Insert
    suspend fun insertBaja(baja: com.example.roadbookorganizador.data.local.entity.BajaPendienteEntity)

    @Query("SELECT * FROM bajas_pendientes WHERE tramoId = :tramoId ORDER BY id ASC")
    suspend fun getBajas(tramoId: Long): List<com.example.roadbookorganizador.data.local.entity.BajaPendienteEntity>

    @Query("DELETE FROM bajas_pendientes WHERE id = :id")
    suspend fun borrarBaja(id: Long)
}
