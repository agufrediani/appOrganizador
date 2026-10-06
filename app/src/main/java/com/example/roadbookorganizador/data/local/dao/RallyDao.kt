package com.example.roadbookorganizador.data.local.dao

import androidx.room.*
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RallyDao {
    @Query("SELECT * FROM rallies ORDER BY id DESC")
    fun getAllRallies(): Flow<List<RallyEntity>>

    @Query("SELECT * FROM rallies WHERE esActivo = 1 LIMIT 1")
    fun getActiveRally(): Flow<RallyEntity?>

    @Query("SELECT * FROM rallies WHERE id = :id")
    suspend fun getRallyById(id: Long): RallyEntity?

    @Query("SELECT * FROM rallies WHERE webId = :webId LIMIT 1")
    suspend fun getRallyByWebId(webId: Int): RallyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRally(rally: RallyEntity): Long

    @Update
    suspend fun updateRally(rally: RallyEntity)

    @Query("UPDATE rallies SET esActivo = CASE WHEN id = :rallyId THEN 1 ELSE 0 END")
    suspend fun setRallyActivo(rallyId: Long)

    @Query("UPDATE rallies SET esActivo = 0")
    suspend fun deseleccionarTodosRallies()

    @Delete
    suspend fun deleteRally(rally: RallyEntity)
}
