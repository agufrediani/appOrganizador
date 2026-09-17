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
    val modoSimulacion: Boolean = false
)

class OdometerEngine {

    private val _state = MutableStateFlow(OdometerState())
    val state: StateFlow<OdometerState> = _state.asStateFlow()

    private var ultimaUbicacion: Location? = null
    private var distanciaTotalMetros: Double = 0.0
    private var distanciaParcialMetros: Double = 0.0
    private var factorCalibracion: Double = 1.0

    fun setFactorCalibracion(factor: Double) {
        factorCalibracion = if (factor > 0.1) factor else 1.0
        _state.value = _state.value.copy(factorCalibracion = factorCalibracion)
    }

    fun procesarNuevaUbicacion(location: Location) {
        val prevLoc = ultimaUbicacion
        ultimaUbicacion = location

        var deltaMetros = 0.0
        var velKmh = if (location.hasSpeed()) location.speed * 3.6f else 0.0f

        if (prevLoc != null) {
            val dist = prevLoc.distanceTo(location).toDouble()
            val tiempoSegundos = if (location.time > prevLoc.time) (location.time - prevLoc.time) / 1000.0 else 1.0

            // Si el chip GPS no entrega velocidad calculada, calcularla por desplazamiento
            if (!location.hasSpeed() && tiempoSegundos > 0) {
                velKmh = ((dist / tiempoSegundos) * 3.6).toFloat()
            }

            // Aceptar desplazamiento si es mayor a 0.5 metros (sensible para pruebas reales)
            if (dist >= 0.5 && dist < 500.0) { // Menor a 500m para evitar saltos locos de triangulación
                deltaMetros = dist * factorCalibracion
            }
        }

        distanciaTotalMetros += deltaMetros
        distanciaParcialMetros += deltaMetros

        val rumbo = if (location.hasBearing()) location.bearing else _state.value.rumbo

        val totalKm = (distanciaTotalMetros / 1000.0)
        val parcialKm = (distanciaParcialMetros / 1000.0)

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
            enMovimiento = velKmh > 1.0f || deltaMetros > 0.5
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
