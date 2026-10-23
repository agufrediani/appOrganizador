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
    val timestamp: Long = System.currentTimeMillis(),

    // ---- Zonas y puntos de interés de la plataforma (GET /api/rallies/{id}/zonas-espectadores) ----
    /** id en el servidor. null = creado en la tablet y todavía no subido. */
    val webId: Int? = null,
    /** "PUNTO" o "POLIGONO" (zona). */
    val forma: String = "PUNTO",
    /** Coordenadas como en la plataforma: [[lat, lng], ...] (un punto = un solo par). */
    val coordenadasGeoJson: String? = null,
    val servicios: String? = null,
    /** Tramo al que pertenece en la plataforma (null = todo el rally). */
    val tramoWebId: Int? = null,
    val tramoIdentificador: String? = null
)
