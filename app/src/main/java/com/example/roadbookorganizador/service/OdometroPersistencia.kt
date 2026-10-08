package com.example.roadbookorganizador.service

import android.content.Context

/** Último estado conocido del odómetro del tramo en trazado. */
data class OdometroGuardado(
    val tramoId: Long,
    val totalKm: Double,
    val parcialKm: Double,
    val guardadoMs: Long,
    /** true = el tramo seguía abierto en el cockpit (hay que retomarlo si la app se reinicia). */
    val activo: Boolean
)

/**
 * Guarda el odómetro del tramo activo en el almacenamiento de la app, para no perderlo
 * si Android cierra la app (batería, memoria) en medio de un trazado.
 */
class OdometroPersistencia(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("odometro_estado", Context.MODE_PRIVATE)

    fun guardar(tramoId: Long, totalKm: Double, parcialKm: Double) {
        prefs.edit()
            .putLong("tramo_id", tramoId)
            .putString("total_km", totalKm.toString())
            .putString("parcial_km", parcialKm.toString())
            .putLong("guardado_ms", System.currentTimeMillis())
            .putBoolean("activo", true)
            .apply()
    }

    /** Se salió del cockpit: se conserva el odómetro para retomarlo, pero no se reabre solo. */
    fun marcarInactivo() {
        prefs.edit().putBoolean("activo", false).apply()
    }

    fun leer(): OdometroGuardado? {
        if (!prefs.contains("tramo_id")) return null
        val total = prefs.getString("total_km", null)?.toDoubleOrNull() ?: return null
        val parcial = prefs.getString("parcial_km", null)?.toDoubleOrNull() ?: 0.0
        return OdometroGuardado(
            tramoId = prefs.getLong("tramo_id", -1L),
            totalKm = total,
            parcialKm = parcial,
            guardadoMs = prefs.getLong("guardado_ms", 0L),
            activo = prefs.getBoolean("activo", false)
        )
    }

    fun limpiar() {
        prefs.edit().clear().apply()
    }
}
