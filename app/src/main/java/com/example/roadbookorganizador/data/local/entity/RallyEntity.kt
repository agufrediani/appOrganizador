package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rallies")
data class RallyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val webId: Int? = null,
    val nombre: String,
    val campeonato: String = "",
    val organizadorClub: String = "",
    val sede: String = "",
    val fecha: String = "", // Fecha de inicio
    val fechaFin: String = "", // Fecha de finalización
    val fiscalizador: String = "",
    val estadoRally: String = "ACTIVO",
    val areaKm2: Double = 350.0,
    val descripcion: String = "",
    val esActivo: Boolean = true,
    val sincronizado: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
