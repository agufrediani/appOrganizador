package com.example.roadbookorganizador.data.local.dao

import androidx.room.*
import com.example.roadbookorganizador.data.local.entity.PuntoInteresEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoInteresDao {
    @Query("SELECT * FROM puntos_interes WHERE rallyId = :rallyId ORDER BY id DESC")
    fun getPuntosByRally(rallyId: Long): Flow<List<PuntoInteresEntity>>

    @Query("SELECT * FROM puntos_interes ORDER BY id DESC")
    fun getAllPuntos(): Flow<List<PuntoInteresEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPunto(punto: PuntoInteresEntity): Long

    @Delete
    suspend fun deletePunto(punto: PuntoInteresEntity)
}
