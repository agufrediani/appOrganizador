package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tramos",
    foreignKeys = [
        ForeignKey(
            entity = RallyEntity::class,
            parentColumns = ["id"],
            childColumns = ["rallyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rallyId")]
)
data class TramoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rallyId: Long,
    val webId: Int? = null,
    val tipo: String = "PE", // PE (Prueba Especial) o ENLACE
    val identificador: String, // ej: "PE 1", "ENLACE 1"
    val nombre: String, // ej: "Calamuchita - San Agustín"
    val numeroSector: Int = 1,
    val chInicio: String = "CH 1",
    val chFin: String = "CH 2",
    val distanciaTotalEstimada: Double = 0.0,
    val distanciaMedidaReal: Double = 0.0,
    val tiempoOtorgado: String = "25'",
    val atrasoMaximo: String = "10'",
    val horaPrimerAuto: String = "",
    val estadoTrazado: String = "BORRADOR", // BORRADOR, EN_TRAZADO, COMPLETADO, SINCRONIZADO
    val ordenSecuencia: Int = 1,
    val fechaTrazado: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
