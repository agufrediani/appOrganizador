package com.example.roadbookorganizador.gps.racebox

import android.location.Location

/**
 * Modelo de datos de telemetría de 25 Hz decodificado según el protocolo binario
 * oficial de RaceBox Mini, RaceBox Mini S y RaceBox Micro (Revisión 9).
 */
data class RaceBoxTelemetry(
    val iTow: Long,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanoseconds: Int,
    val timeAccuracyNs: Long,
    val validityFlags: Int,
    val fixStatus: Int,             // 0: no fix, 2: 2D fix, 3: 3D fix
    val fixStatusFlags: Int,        // bit 0 = valid fix
    val satellitesCount: Int,       // Número de satélites activos (SVs)
    val latitude: Double,           // Coordenada WGS84
    val longitude: Double,          // Coordenada WGS84
    val wgsAltitudeMeters: Double,  // Altitud elipsoidal WGS84
    val mslAltitudeMeters: Double,  // Altitud sobre nivel del mar (MSL)
    val horizontalAccuracyMeters: Float,
    val verticalAccuracyMeters: Float,
    val speedKmh: Float,            // Velocidad en km/h
    val speedAccuracyKmh: Float,
    val headingDegrees: Float,      // Rumbo en grados (0° = Norte)
    val headingAccuracyDeg: Float,
    val pdop: Float,
    val isCharging: Boolean,        // RaceBox Mini / Mini S
    val batteryPercent: Int,        // 0..100%
    val isMicroVoltage: Boolean,    // true si el paquete proviene de RaceBox Micro
    val inputVoltageVolts: Float,   // Voltaje del auto si es RaceBox Micro
    val gForceX: Float,             // Aceleración longitudinal (frenado / tracción) en Gs
    val gForceY: Float,             // Aceleración lateral (curvas) en Gs
    val gForceZ: Float,             // Aceleración vertical (saltos / baches) en Gs
    val rotationRateX: Float,       // Balanceo (Roll) en grados/segundo
    val rotationRateY: Float,       // Cabeceo (Pitch) en grados/segundo
    val rotationRateZ: Float        // Guiñada (Yaw / Derrape) en grados/segundo
) {
    val hasValidFix: Boolean
        get() = (fixStatus >= 2) && ((fixStatusFlags and 0x01) != 0)

    /**
     * Convierte la telemetría binaria a un objeto Android Location estándar
     * para inyectar directamente en el motor de odometría y mapas.
     */
    fun toAndroidLocation(): Location {
        return Location("RaceBox_25Hz").apply {
            latitude = this@RaceBoxTelemetry.latitude
            longitude = this@RaceBoxTelemetry.longitude
            altitude = this@RaceBoxTelemetry.mslAltitudeMeters
            speed = this@RaceBoxTelemetry.speedKmh / 3.6f
            bearing = this@RaceBoxTelemetry.headingDegrees
            accuracy = this@RaceBoxTelemetry.horizontalAccuracyMeters
            time = System.currentTimeMillis()
        }
    }
}
