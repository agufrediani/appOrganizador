package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calibraciones")
data class CalibracionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehiculoNombre: String = "Vehículo Trazador 000",
    val distanciaOficialMetros: Double = 1000.0,
    val distanciaMedidaMetros: Double = 1000.0,
    val factorCorreccion: Double = 1.0, // factor k (Oficial / Medida)
    val fecha: Long = System.currentTimeMillis(),
    val activo: Boolean = true
)
