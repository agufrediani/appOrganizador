package com.example.roadbookorganizador.service

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class OdometerState(
    val odometroTotalKm: Double = 0.0,
    val odometroParcialKm: Double = 0.0,
    val velocidadKmh: Float = 0.0f,
    val rumbo: Float = 0.0f,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val altitud: Double = 0.0,
    val precisionMetros: Float = 0.0f,
    val satelitesConectados: Boolean = false,
    val enMovimiento: Boolean = false,
    val factorCalibracion: Double = 1.0,
    val congelado: Boolean = false,
    val odometroTotalCongeladoKm: Double = 0.0,
    val odometroParcialCongeladoKm: Double = 0.0,
    val modoSimulacion: Boolean = false,
    // Telemetría GPS Externo 25Hz
    val fuenteGps: String = "GPS Tablet",
    val raceBoxConectado: Boolean = false,
    val raceBoxTieneFix: Boolean = false,
    val raceBoxBateriaPct: Int? = null,
    val raceBoxSatelites: Int? = null,
    val raceBoxInputVoltage: Float? = null,
    val raceBoxGForceX: Float = 0.0f,
    val raceBoxGForceY: Float = 0.0f,
    val raceBoxGForceZ: Float = 0.0f,
    val preguntarCambioATablet: Boolean = false,
    val modoReverso: Boolean = false,
    val velocidadMinimaFiltroKmh: Float = 2.0f,
    val autoPcsHabilitado: Boolean = false,
    val distanciaPrimerPcKm: Double = 0.100,
    val distanciaIntervaloPcsKm: Double = 0.350
)

class OdometerEngine {

    private val _state = MutableStateFlow(OdometerState())
    val state: StateFlow<OdometerState> = _state.asStateFlow()

    private var ultimaUbicacion: Location? = null
    private var distanciaTotalMetros: Double = 0.0
    private var distanciaParcialMetros: Double = 0.0
    private var factorCalibracion: Double = 1.0
    private var ultimoTimestampRaceBoxMs: Long = 0L

    private var raceBoxBleConectado: Boolean = false
    private var algunaVezConectoExterno: Boolean = false
    private var permitirFallbackTablet: Boolean = false

    private var modoReverso: Boolean = false
    private var velocidadMinimaFiltroKmh: Float = 2.0f
    private var autoPcsHabilitado: Boolean = false
    private var distanciaPrimerPcKm: Double = 0.100
    private var distanciaIntervaloPcsKm: Double = 0.350
    private var ultimoPcGeneradoKm: Double = 0.0
    private var primerPcGenerado: Boolean = false

    var onAutoPcTriggered: ((distanciaKm: Double, lat: Double, lon: Double, tipoPc: String) -> Unit)? = null

    fun setFactorCalibracion(factor: Double) {
        factorCalibracion = if (factor > 0.1) factor else 1.0
        _state.value = _state.value.copy(factorCalibracion = factorCalibracion)
    }

    fun toggleModoReverso(): Boolean {
        modoReverso = !modoReverso
        _state.value = _state.value.copy(modoReverso = modoReverso)
        return modoReverso
    }

    fun setVelocidadMinimaFiltro(kmh: Float) {
        velocidadMinimaFiltroKmh = kmh.coerceAtLeast(0.0f)
        _state.value = _state.value.copy(velocidadMinimaFiltroKmh = velocidadMinimaFiltroKmh)
    }

    fun setAutoPcsHabilitado(habilitado: Boolean, primerPcKm: Double = 0.100, intervaloKm: Double = 0.350) {
        autoPcsHabilitado = habilitado
        distanciaPrimerPcKm = primerPcKm
        distanciaIntervaloPcsKm = intervaloKm
        primerPcGenerado = false
        ultimoPcGeneradoKm = _state.value.odometroTotalKm
        _state.value = _state.value.copy(
            autoPcsHabilitado = habilitado,
            distanciaPrimerPcKm = primerPcKm,
            distanciaIntervaloPcsKm = intervaloKm
        )
    }

    fun toggleAutoPcs() {
        setAutoPcsHabilitado(!autoPcsHabilitado, distanciaPrimerPcKm, distanciaIntervaloPcsKm)
    }

    fun setIntervaloAutoPcs(intervaloKm: Double) {
        distanciaIntervaloPcsKm = intervaloKm.coerceAtLeast(0.05)
        _state.value = _state.value.copy(distanciaIntervaloPcsKm = distanciaIntervaloPcsKm)
    }

    private fun checkAutoPcTrigger(totalKm: Double, lat: Double, lon: Double) {
        if (!autoPcsHabilitado) return
        if (!primerPcGenerado) {
            if (totalKm >= distanciaPrimerPcKm) {
                primerPcGenerado = true
                ultimoPcGeneradoKm = totalKm
                onAutoPcTriggered?.invoke(totalKm, lat, lon, "PASO")
            }
        } else {
            val delta = totalKm - ultimoPcGeneradoKm
            if (delta >= distanciaIntervaloPcsKm) {
                ultimoPcGeneradoKm = totalKm
                onAutoPcTriggered?.invoke(totalKm, lat, lon, "REGULARIDAD")
            }
        }
    }

    /**
     * Notifica el estado de conexión del enlace BLE del GPS Externo.
     * Si se conecta, el GPS Externo toma prioridad absoluta.
     * Si se desconecta habiendo estado conectado, se solicita confirmación al usuario antes de usar la tablet.
     */
    fun setRaceBoxBleConectado(conectado: Boolean) {
        if (conectado) {
            raceBoxBleConectado = true
            algunaVezConectoExterno = true
            permitirFallbackTablet = false
            _state.value = _state.value.copy(
                raceBoxConectado = true,
                fuenteGps = "GPS Externo (25Hz)",
                preguntarCambioATablet = false
            )
        } else {
            val estabaConectado = raceBoxBleConectado
            raceBoxBleConectado = false
            _state.value = _state.value.copy(
                raceBoxConectado = false,
                satelitesConectados = false,
                preguntarCambioATablet = estabaConectado && !permitirFallbackTablet
            )
        }
    }

    fun aceptarFallbackTablet() {
        permitirFallbackTablet = true
        _state.value = _state.value.copy(
            fuenteGps = "GPS Tablet",
            preguntarCambioATablet = false
        )
    }

    fun cancelarPreguntaCambioATablet() {
        // El usuario prefiere esperar al GPS Externo sin pasar a la tablet
        permitirFallbackTablet = false
        _state.value = _state.value.copy(
            preguntarCambioATablet = false
        )
    }

    /**
     * Procesa la telemetría ultra-precisa a 25 Hz proveniente del GPS Externo por BLE.
     */
    fun procesarTelemetriaRaceBox(telemetry: com.example.roadbookorganizador.gps.racebox.RaceBoxTelemetry) {
        ultimoTimestampRaceBoxMs = System.currentTimeMillis()
        raceBoxBleConectado = true
        algunaVezConectoExterno = true
        permitirFallbackTablet = false

        val velKmh = telemetry.speedKmh
        val accuracy = if (telemetry.horizontalAccuracyMeters > 0) telemetry.horizontalAccuracyMeters else _state.value.precisionMetros

        if (!telemetry.hasValidFix) {
            // Aún buscando satélites o fix 3D (ej. interiores):
            _state.value = _state.value.copy(
                fuenteGps = "GPS Externo (25Hz)",
                raceBoxConectado = true,
                raceBoxTieneFix = false,
                satelitesConectados = false,
                enMovimiento = false,
                precisionMetros = accuracy,
                raceBoxBateriaPct = telemetry.batteryPercent,
                raceBoxSatelites = telemetry.satellitesCount,
                raceBoxInputVoltage = if (telemetry.isMicroVoltage) telemetry.inputVoltageVolts else null,
                raceBoxGForceX = telemetry.gForceX,
                raceBoxGForceY = telemetry.gForceY,
                raceBoxGForceZ = telemetry.gForceZ
            )
            return
        }

        // Tiene fix válido:
        val location = telemetry.toAndroidLocation()
        val prevLoc = ultimaUbicacion
        ultimaUbicacion = location

        var deltaMetros = 0.0

        if (prevLoc != null) {
            val dist = prevLoc.distanceTo(location).toDouble()
            // Filtro de velocidad mínima (< 2 km/h con auto detenido ignora jitter)
            if (velKmh < velocidadMinimaFiltroKmh && dist < 0.35) {
                deltaMetros = 0.0
            } else if (dist >= 0.04 && dist < 60.0) {
                val dirSign = if (modoReverso) -1.0 else 1.0
                deltaMetros = dist * factorCalibracion * dirSign
            }
        }

        distanciaTotalMetros = (distanciaTotalMetros + deltaMetros).coerceAtLeast(0.0)
        distanciaParcialMetros = (distanciaParcialMetros + deltaMetros).coerceAtLeast(0.0)

        val totalKm = (distanciaTotalMetros / 1000.0)
        val parcialKm = (distanciaParcialMetros / 1000.0)

        checkAutoPcTrigger(totalKm, location.latitude, location.longitude)

        _state.value = _state.value.copy(
            odometroTotalKm = totalKm,
            odometroParcialKm = parcialKm,
            velocidadKmh = velKmh,
            rumbo = telemetry.headingDegrees,
            latitud = telemetry.latitude,
            longitud = telemetry.longitude,
            altitud = telemetry.mslAltitudeMeters,
            precisionMetros = accuracy,
            satelitesConectados = true,
            enMovimiento = velKmh > 0.8f || Math.abs(deltaMetros) > 0.08,
            fuenteGps = "GPS Externo (25Hz)",
            raceBoxConectado = true,
            raceBoxTieneFix = true,
            raceBoxBateriaPct = telemetry.batteryPercent,
            raceBoxSatelites = telemetry.satellitesCount,
            raceBoxInputVoltage = if (telemetry.isMicroVoltage) telemetry.inputVoltageVolts else null,
            raceBoxGForceX = telemetry.gForceX,
            raceBoxGForceY = telemetry.gForceY,
            raceBoxGForceZ = telemetry.gForceZ
        )
    }

    /**
     * Procesa ubicación del GPS de la Tablet.
     * Si el GPS Externo está conectado o el usuario no autorizó el cambio, se ignora la tablet.
     */
    fun procesarNuevaUbicacion(location: Location) {
        if (raceBoxBleConectado || (algunaVezConectoExterno && !permitirFallbackTablet)) {
            // El GPS Externo tiene prioridad absoluta. La tablet se mantiene en silencio.
            return
        }

        val prevLoc = ultimaUbicacion
        ultimaUbicacion = location

        var deltaMetros = 0.0
        var velKmh = if (location.hasSpeed()) location.speed * 3.6f else 0.0f

        if (prevLoc != null) {
            val dist = prevLoc.distanceTo(location).toDouble()
            val tiempoSegundos = if (location.time > prevLoc.time) (location.time - prevLoc.time) / 1000.0 else 1.0

            if (!location.hasSpeed() && tiempoSegundos > 0) {
                velKmh = ((dist / tiempoSegundos) * 3.6).toFloat()
            }

            // Filtro de velocidad mínima en tablet (< 2 km/h ignora jitter)
            if (velKmh < velocidadMinimaFiltroKmh && dist < 1.2) {
                deltaMetros = 0.0
            } else if (dist >= 0.5 && dist < 500.0) {
                val dirSign = if (modoReverso) -1.0 else 1.0
                deltaMetros = dist * factorCalibracion * dirSign
            }
        }

        distanciaTotalMetros = (distanciaTotalMetros + deltaMetros).coerceAtLeast(0.0)
        distanciaParcialMetros = (distanciaParcialMetros + deltaMetros).coerceAtLeast(0.0)

        val rumbo = if (location.hasBearing()) location.bearing else _state.value.rumbo

        val totalKm = (distanciaTotalMetros / 1000.0)
        val parcialKm = (distanciaParcialMetros / 1000.0)

        checkAutoPcTrigger(totalKm, location.latitude, location.longitude)

        _state.value = _state.value.copy(
            odometroTotalKm = totalKm,
            odometroParcialKm = parcialKm,
            velocidadKmh = velKmh,
            rumbo = rumbo,
            latitud = location.latitude,
            longitud = location.longitude,
            altitud = location.altitude,
            precisionMetros = if (location.hasAccuracy()) location.accuracy else 5.0f,
            satelitesConectados = true,
            enMovimiento = velKmh > 1.0f || Math.abs(deltaMetros) > 0.5,
            fuenteGps = "GPS Tablet",
            raceBoxConectado = false
        )
    }

    /**
     * Congela instantáneamente las lecturas para registrar una viñeta exacta
     * sin detener la acumulación interna del odómetro de fondo.
     */
    fun congelarParaMarcado(): Pair<Double, Double> {
        val s = _state.value
        _state.value = s.copy(
            congelado = true,
            odometroTotalCongeladoKm = s.odometroTotalKm,
            odometroParcialCongeladoKm = s.odometroParcialKm
        )
        return Pair(s.odometroTotalKm, s.odometroParcialKm)
    }

    fun descongelar() {
        _state.value = _state.value.copy(congelado = false)
    }

    /**
     * Resetea el odómetro parcial a cero (usado al marcar una viñeta o punto de referencia)
     */
    fun resetParcial() {
        distanciaParcialMetros = 0.0
        _state.value = _state.value.copy(odometroParcialKm = 0.0)
    }

    /**
     * Resetea ambos odómetros a cero (al iniciar un tramo desde la largada)
     */
    fun resetTotalYParcial(inicioKm: Double = 0.0) {
        distanciaTotalMetros = inicioKm * 1000.0
        distanciaParcialMetros = 0.0
        ultimaUbicacion = null
        _state.value = _state.value.copy(
            odometroTotalKm = inicioKm,
            odometroParcialKm = 0.0,
            congelado = false
        )
    }

    /**
     * Ajuste fino manual (+/- metros) para sincronizar con jalones de ruta
     */
    fun ajustarMetros(deltaMetros: Double) {
        distanciaTotalMetros = maxOf(0.0, distanciaTotalMetros + deltaMetros)
        distanciaParcialMetros = maxOf(0.0, distanciaParcialMetros + deltaMetros)
        _state.value = _state.value.copy(
            odometroTotalKm = distanciaTotalMetros / 1000.0,
            odometroParcialKm = distanciaParcialMetros / 1000.0
        )
    }

    /**
     * Simula avance en terreno (útil para pruebas en mesa/taller o tablets sin GPS)
     */
    fun simularPaso(deltaMetros: Double = 16.6, velKmh: Float = 60.0f) {
        val s = _state.value
        distanciaTotalMetros += (deltaMetros * factorCalibracion)
        distanciaParcialMetros += (deltaMetros * factorCalibracion)

        val rumboRad = Math.toRadians(s.rumbo.toDouble())
        val deltaLat = (deltaMetros * Math.cos(rumboRad)) / 111111.0
        val baseLat = if (s.latitud == 0.0) -31.4201 else s.latitud
        val baseLng = if (s.longitud == 0.0) -64.1888 else s.longitud
        val deltaLng = (deltaMetros * Math.sin(rumboRad)) / (111111.0 * Math.cos(Math.toRadians(baseLat)))

        val nuevoRumbo = (s.rumbo + 1.2f) % 360f

        _state.value = s.copy(
            odometroTotalKm = distanciaTotalMetros / 1000.0,
            odometroParcialKm = distanciaParcialMetros / 1000.0,
            velocidadKmh = velKmh,
            rumbo = nuevoRumbo,
            latitud = baseLat + deltaLat,
            longitud = baseLng + deltaLng,
            altitud = 650.0,
            precisionMetros = 2.0f,
            satelitesConectados = true,
            enMovimiento = true,
            modoSimulacion = true
        )
    }

    fun setModoSimulacion(activo: Boolean) {
        if (!activo) {
            _state.value = _state.value.copy(modoSimulacion = false, velocidadKmh = 0f, enMovimiento = false)
        } else {
            val baseLat = if (_state.value.latitud == 0.0) -31.4201 else _state.value.latitud
            val baseLng = if (_state.value.longitud == 0.0) -64.1888 else _state.value.longitud
            _state.value = _state.value.copy(
                latitud = baseLat,
                longitud = baseLng,
                rumbo = if (_state.value.rumbo == 0f) 45f else _state.value.rumbo,
                satelitesConectados = true,
                modoSimulacion = true
            )
        }
    }
}
