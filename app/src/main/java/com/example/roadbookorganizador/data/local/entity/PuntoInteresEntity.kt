package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puntos_interes")
data class PuntoInteresEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rallyId: Long,
    val nombre: String,
    val tipo: String, // ACCESO, PUBLICO, AMBULANCIA, RESCATE, HELIPUERTO, MOJON, PELIGRO
    val latitud: Double,
    val longitud: Double,
    val descripcion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
