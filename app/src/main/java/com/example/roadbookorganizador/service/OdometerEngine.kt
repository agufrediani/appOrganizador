package com.example.roadbookorganizador.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max

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
    val distanciaIntervaloPcsKm: Double = 0.350,
    // Diagnóstico de calidad de señal
    val posicionesDescartadasPrecision: Int = 0,
    val saltosDescartados: Int = 0
)

/**
 * Motor de odometría. No depende de Android: recibe [GpsFix] y se puede probar en la JVM.
 *
 * Reglas:
 * - Una sola fuente cuenta distancia: el RaceBox tiene prioridad; la tablet solo cuenta si no
 *   hubo RaceBox o si el usuario aceptó pasar a la tablet tras una desconexión.
 * - Se descartan posiciones con precisión peor que [precisionMaximaMetros].
 * - Un salto entre dos posiciones se acepta solo si es coherente con la velocidad y el tiempo
 *   transcurrido. Así un corte de señal o de Bluetooth no pierde distancia, y un salto falso
 *   (multitrayecto) no la suma. Tras [MAX_SALTOS_SEGUIDOS] rechazos seguidos se toma la
 *   posición nueva como referencia sin sumar distancia, para no quedar trabado.
 *
 * Es seguro llamarlo desde varios hilos (callbacks de GPS y de Bluetooth).
 */
class OdometerEngine {

    companion object {
        const val PRECISION_MAXIMA_METROS_DEFAULT = 15f
        const val MAX_SALTOS_SEGUIDOS = 5
        private const val VELOCIDAD_MAXIMA_PLAUSIBLE_MS = 250.0 / 3.6
        private const val MARGEN_SALTO_METROS = 5.0
        private const val TOLERANCIA_SALTO = 1.5
    }

    private val _state = MutableStateFlow(OdometerState())
    val state: StateFlow<OdometerState> = _state.asStateFlow()

    var precisionMaximaMetros: Float = PRECISION_MAXIMA_METROS_DEFAULT
        @Synchronized get
        @Synchronized set

    private var ultimoFix: GpsFix? = null
    private var saltosSeguidos = 0
    private var distanciaTotalMetros: Double = 0.0
    private var distanciaParcialMetros: Double = 0.0
    private var factorCalibracion: Double = 1.0

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

    @Synchronized
    fun setFactorCalibracion(factor: Double) {
        factorCalibracion = if (factor > 0.1) factor else 1.0
        _state.value = _state.value.copy(factorCalibracion = factorCalibracion)
    }

    @Synchronized
    fun toggleModoReverso(): Boolean {
        modoReverso = !modoReverso
        _state.value = _state.value.copy(modoReverso = modoReverso)
        return modoReverso
    }

    @Synchronized
    fun setVelocidadMinimaFiltro(kmh: Float) {
        velocidadMinimaFiltroKmh = kmh.coerceAtLeast(0.0f)
        _state.value = _state.value.copy(velocidadMinimaFiltroKmh = velocidadMinimaFiltroKmh)
    }

    @Synchronized
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

    @Synchronized
    fun toggleAutoPcs() {
        setAutoPcsHabilitado(!autoPcsHabilitado, distanciaPrimerPcKm, distanciaIntervaloPcsKm)
    }

    @Synchronized
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
     * Estado del enlace BLE del GPS Externo. Si se conecta, toma prioridad absoluta.
     * Si se desconecta habiendo estado conectado, se pide confirmación antes de usar la tablet.
     */
    @Synchronized
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
            val s = _state.value
            _state.value = s.copy(
                raceBoxConectado = false,
                satelitesConectados = false,
                // Una segunda notificación de "desconectado" no debe cancelar la pregunta pendiente
                preguntarCambioATablet = s.preguntarCambioATablet || (estabaConectado && !permitirFallbackTablet)
            )
        }
    }

    @Synchronized
    fun aceptarFallbackTablet() {
        permitirFallbackTablet = true
        _state.value = _state.value.copy(
            fuenteGps = "GPS Tablet",
            preguntarCambioATablet = false
        )
    }

    @Synchronized
    fun cancelarPreguntaCambioATablet() {
        // El usuario prefiere esperar al GPS Externo sin pasar a la tablet
        permitirFallbackTablet = false
        _state.value = _state.value.copy(preguntarCambioATablet = false)
    }

    /** true si en este momento una posición de [fuente] cuenta distancia. */
    @Synchronized
    fun fuenteActiva(fuente: FuenteGps): Boolean = when (fuente) {
        FuenteGps.RACEBOX -> true
        FuenteGps.TABLET -> !raceBoxBleConectado && (!algunaVezConectoExterno || permitirFallbackTablet)
    }

    /**
     * Telemetría a 25 Hz del GPS Externo. Actualiza el estado del equipo y, si hay fix válido,
     * procesa la posición. Devuelve el [GpsFix] usado, o null si no hubo posición válida.
     */
    @Synchronized
    fun procesarTelemetriaRaceBox(telemetry: com.example.roadbookorganizador.gps.racebox.RaceBoxTelemetry): GpsFix? {
        raceBoxBleConectado = true
        algunaVezConectoExterno = true
        permitirFallbackTablet = false

        val accuracy = if (telemetry.horizontalAccuracyMeters > 0) telemetry.horizontalAccuracyMeters else _state.value.precisionMetros
        _state.value = _state.value.copy(
            fuenteGps = "GPS Externo (25Hz)",
            raceBoxConectado = true,
            raceBoxTieneFix = telemetry.hasValidFix,
            precisionMetros = accuracy,
            raceBoxBateriaPct = telemetry.batteryPercent,
            raceBoxSatelites = telemetry.satellitesCount,
            raceBoxInputVoltage = if (telemetry.isMicroVoltage) telemetry.inputVoltageVolts else null,
            raceBoxGForceX = telemetry.gForceX,
            raceBoxGForceY = telemetry.gForceY,
            raceBoxGForceZ = telemetry.gForceZ
        )

        if (!telemetry.hasValidFix) {
            // Aún buscando satélites o fix 3D (ej. interiores)
            _state.value = _state.value.copy(satelitesConectados = false, enMovimiento = false)
            return null
        }

        val fix = telemetry.toGpsFix(System.currentTimeMillis())
        return if (procesarFix(fix)) fix else null
    }

    /**
     * Procesa una posición. Devuelve true si la posición fue aceptada (fuente activa y precisión
     * suficiente); en ese caso conviene guardarla en el track.
     */
    @Synchronized
    fun procesarFix(fix: GpsFix): Boolean {
        if (!fuenteActiva(fix.fuente)) return false

        val precision = fix.precisionMetros
        if (precision != null && precision > precisionMaximaMetros) {
            _state.value = _state.value.copy(
                precisionMetros = precision,
                posicionesDescartadasPrecision = _state.value.posicionesDescartadasPrecision + 1
            )
            return false
        }

        val prev = ultimoFix
        var deltaMetros = 0.0
        var velKmh = fix.velocidadKmh ?: 0f

        if (prev != null) {
            val dist = Geo.distanciaMetros(prev.latitud, prev.longitud, fix.latitud, fix.longitud)
            val dtSeg = segundosEntre(prev, fix)
            if (fix.velocidadKmh == null && dtSeg > 0) {
                velKmh = ((dist / dtSeg) * 3.6).toFloat()
            }

            if (!saltoPlausible(prev, fix, dist, dtSeg)) {
                saltosSeguidos++
                if (saltosSeguidos < MAX_SALTOS_SEGUIDOS) {
                    // Salto falso: se ignora la posición y se mantiene la referencia anterior
                    _state.value = _state.value.copy(saltosDescartados = _state.value.saltosDescartados + 1)
                    return false
                }
                // Demasiados rechazos seguidos: la referencia era la mala. Se re-ancla sin sumar.
                saltosSeguidos = 0
                ultimoFix = fix
                _state.value = _state.value.copy(saltosDescartados = _state.value.saltosDescartados + 1)
                publicarPosicion(fix, velKmh, 0.0)
                return true
            }
            saltosSeguidos = 0

            val umbralQuieto = if (fix.fuente == FuenteGps.RACEBOX) 0.35 else 1.2
            val umbralMinimo = if (fix.fuente == FuenteGps.RACEBOX) 0.04 else 0.5
            if (velKmh < velocidadMinimaFiltroKmh && dist < umbralQuieto) {
                deltaMetros = 0.0 // vehículo detenido: se ignora el jitter
            } else if (dist >= umbralMinimo) {
                val dirSign = if (modoReverso) -1.0 else 1.0
                deltaMetros = dist * factorCalibracion * dirSign
            }
        }

        ultimoFix = fix
        distanciaTotalMetros = (distanciaTotalMetros + deltaMetros).coerceAtLeast(0.0)
        distanciaParcialMetros = (distanciaParcialMetros + deltaMetros).coerceAtLeast(0.0)

        val totalKm = distanciaTotalMetros / 1000.0
        checkAutoPcTrigger(totalKm, fix.latitud, fix.longitud)
        publicarPosicion(fix, velKmh, deltaMetros)
        return true
    }

    private fun segundosEntre(prev: GpsFix, fix: GpsFix): Double {
        val gpsPrev = prev.gpsTimeMs
        val gpsNow = fix.gpsTimeMs
        if (gpsPrev != null && gpsNow != null) {
            val d = gpsNow - gpsPrev
            if (d in 1..3_600_000) return d / 1000.0
        }
        val d = fix.timeMs - prev.timeMs
        return if (d > 0) d / 1000.0 else 0.0
    }

    /**
     * Un salto es plausible si no supera lo que el vehículo pudo recorrer en ese tiempo a la
     * mayor de las dos velocidades informadas (con tolerancia), más el error de posición.
     */
    private fun saltoPlausible(prev: GpsFix, fix: GpsFix, dist: Double, dtSeg: Double): Boolean {
        val margen = MARGEN_SALTO_METROS + (prev.precisionMetros ?: 5f) + (fix.precisionMetros ?: 5f)
        if (dist <= margen) return true
        if (dtSeg <= 0.0) return false
        val vPrev = (prev.velocidadKmh ?: 0f) / 3.6
        val vNow = (fix.velocidadKmh ?: 0f) / 3.6
        val vRef = when {
            prev.velocidadKmh == null && fix.velocidadKmh == null -> VELOCIDAD_MAXIMA_PLAUSIBLE_MS
            else -> max(vPrev, vNow)
        }
        if (dist / dtSeg > VELOCIDAD_MAXIMA_PLAUSIBLE_MS) return false
        return dist <= vRef * dtSeg * TOLERANCIA_SALTO + margen
    }

    private fun publicarPosicion(fix: GpsFix, velKmh: Float, deltaMetros: Double) {
        _state.value = _state.value.copy(
            odometroTotalKm = distanciaTotalMetros / 1000.0,
            odometroParcialKm = distanciaParcialMetros / 1000.0,
            velocidadKmh = velKmh,
            rumbo = fix.rumbo ?: _state.value.rumbo,
            latitud = fix.latitud,
            longitud = fix.longitud,
            altitud = fix.altitud,
            precisionMetros = fix.precisionMetros ?: 5.0f,
            satelitesConectados = true,
            enMovimiento = velKmh > 1.0f || abs(deltaMetros) > 0.08,
            fuenteGps = if (fix.fuente == FuenteGps.RACEBOX) "GPS Externo (25Hz)" else "GPS Tablet",
            raceBoxConectado = fix.fuente == FuenteGps.RACEBOX
        )
    }

    /**
     * Congela instantáneamente las lecturas para registrar una viñeta exacta
     * sin detener la acumulación interna del odómetro de fondo.
     */
    @Synchronized
    fun congelarParaMarcado(): Pair<Double, Double> {
        val s = _state.value
        _state.value = s.copy(
            congelado = true,
            odometroTotalCongeladoKm = s.odometroTotalKm,
            odometroParcialCongeladoKm = s.odometroParcialKm
        )
        return Pair(s.odometroTotalKm, s.odometroParcialKm)
    }

    @Synchronized
    fun descongelar() {
        _state.value = _state.value.copy(congelado = false)
    }

    /** Resetea el odómetro parcial a cero (usado al marcar una viñeta o punto de referencia). */
    @Synchronized
    fun resetParcial() {
        distanciaParcialMetros = 0.0
        _state.value = _state.value.copy(odometroParcialKm = 0.0)
    }

    /** Resetea ambos odómetros (al iniciar un tramo desde la largada o reanudarlo). */
    @Synchronized
    fun resetTotalYParcial(inicioKm: Double = 0.0) {
        distanciaTotalMetros = inicioKm * 1000.0
        distanciaParcialMetros = 0.0
        ultimoFix = null
        saltosSeguidos = 0
        _state.value = _state.value.copy(
            odometroTotalKm = inicioKm,
            odometroParcialKm = 0.0,
            congelado = false
        )
    }

    /**
     * Restaura total y parcial guardados (por ejemplo, tras un cierre de la app por Android).
     * La próxima posición se toma como nueva referencia, sin sumar el tramo intermedio.
     */
    @Synchronized
    fun restaurar(totalKm: Double, parcialKm: Double) {
        distanciaTotalMetros = (totalKm * 1000.0).coerceAtLeast(0.0)
        distanciaParcialMetros = (parcialKm * 1000.0).coerceAtLeast(0.0)
        ultimoFix = null
        saltosSeguidos = 0
        _state.value = _state.value.copy(
            odometroTotalKm = distanciaTotalMetros / 1000.0,
            odometroParcialKm = distanciaParcialMetros / 1000.0,
            congelado = false
        )
    }

    /** Ajuste fino manual (+/- metros) para sincronizar con jalones de ruta. */
    @Synchronized
    fun ajustarMetros(deltaMetros: Double) {
        distanciaTotalMetros = maxOf(0.0, distanciaTotalMetros + deltaMetros)
        distanciaParcialMetros = maxOf(0.0, distanciaParcialMetros + deltaMetros)
        _state.value = _state.value.copy(
            odometroTotalKm = distanciaTotalMetros / 1000.0,
            odometroParcialKm = distanciaParcialMetros / 1000.0
        )
    }

    /** Simula avance en terreno (útil para pruebas en mesa/taller o tablets sin GPS). */
    @Synchronized
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

    @Synchronized
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
