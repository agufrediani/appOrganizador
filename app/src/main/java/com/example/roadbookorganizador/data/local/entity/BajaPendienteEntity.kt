package com.example.roadbookorganizador.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Viñeta borrada en la tablet que todavía hay que borrar en la plataforma
 * (`DELETE /api/indicaciones/{webId}`). Se manda con la próxima sincronización del tramo.
 */
@Entity(tableName = "bajas_pendientes")
data class BajaPendienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tramoId: Long,
    val webId: Int,
    val uid: String,
    val creadoMs: Long = System.currentTimeMillis()
)
