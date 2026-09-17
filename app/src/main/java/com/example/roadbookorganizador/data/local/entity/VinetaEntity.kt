package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vinetas",
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
data class VinetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tramoId: Long,
    val numero: Int,
    val distanciaTotal: Double,       // Km acumulados con 3 decimales
    val distanciaParcial: Double,     // Km desde la viñeta previa
    val latitud: Double,
    val longitud: Double,
    val altitud: Double = 0.0,        // Metros sobre el nivel del mar
    val rumbo: Float = 0.0f,          // Heading 0-360 grados
    val velocidadKmh: Float = 0.0f,   // Velocidad instantánea al marcar
    val tulipTipo: String = "RECTA",  // Tipo de maniobra/ícono
    val tulipSvgData: String = "",    // Coordenadas o dibujo vectorial opcional
    val informacion: String = "",     // Notas: "Firme", "Puente angosto"
    val peligro: String = "",         // "", "!", "!!", "!!!"
    val fotoUri: String? = null,      // Foto capturada in situ
    val audioUri: String? = null,     // Nota de voz rápida
    val timestamp: Long = System.currentTimeMillis()
)
