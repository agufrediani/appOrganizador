package com.example.roadbookorganizador.data.local.dao

import androidx.room.*
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TramoDao {
    @Query("SELECT * FROM tramos WHERE rallyId = :rallyId ORDER BY ordenSecuencia ASC")
    fun getTramosByRally(rallyId: Long): Flow<List<TramoEntity>>

    @Query("SELECT * FROM tramos WHERE id = :id")
    suspend fun getTramoById(id: Long): TramoEntity?

    @Query("SELECT * FROM tramos WHERE rallyId = :rallyId AND webId = :webId LIMIT 1")
    suspend fun getTramoByRallyAndWebId(rallyId: Long, webId: Int): TramoEntity?

    @Query("SELECT * FROM tramos WHERE id = :id")
    fun getTramoFlowById(id: Long): Flow<TramoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTramo(tramo: TramoEntity): Long

    @Update
    suspend fun updateTramo(tramo: TramoEntity)

    @Query("UPDATE tramos SET distanciaMedidaReal = :distancia, estadoTrazado = :estado, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateDistanciaYEstado(id: Long, distancia: Double, estado: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTramo(tramo: TramoEntity)
}
