package com.example.roadbookorganizador.data.local.dao

import androidx.room.*
import com.example.roadbookorganizador.data.local.entity.CalibracionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalibracionDao {
    @Query("SELECT * FROM calibraciones WHERE activo = 1 ORDER BY id DESC LIMIT 1")
    fun getCalibracionActiva(): Flow<CalibracionEntity?>

    @Query("SELECT * FROM calibraciones WHERE activo = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getCalibracionActivaSync(): CalibracionEntity?

    @Query("SELECT * FROM calibraciones ORDER BY fecha DESC")
    fun getAllCalibraciones(): Flow<List<CalibracionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibracion(calibracion: CalibracionEntity): Long

    @Query("UPDATE calibraciones SET activo = 0 WHERE id != :idActivo")
    suspend fun desactivarOtras(idActivo: Long)

    @Delete
    suspend fun deleteCalibracion(calibracion: CalibracionEntity)
}
