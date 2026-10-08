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
}
