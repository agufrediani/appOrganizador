package com.example.roadbookorganizador.data.local.dao

import androidx.room.*
import com.example.roadbookorganizador.data.local.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE tramoId = :tramoId ORDER BY timestamp ASC")
    fun getTrackPointsByTramo(tramoId: Long): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE tramoId = :tramoId ORDER BY timestamp ASC")
    suspend fun getTrackPointsByTramoSync(tramoId: Long): List<TrackPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(point: TrackPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)

    @Query("DELETE FROM track_points WHERE tramoId = :tramoId")
    suspend fun clearTrackPointsByTramo(tramoId: Long)
}
