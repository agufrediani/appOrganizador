package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = TramoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tramoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tramoId")]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tramoId: Long,
    val latitud: Double,
    val longitud: Double,
    val altitud: Double = 0.0,
    val velocidadKmh: Float = 0.0f,
    val rumbo: Float = 0.0f,
    val distanciaAcumulada: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
