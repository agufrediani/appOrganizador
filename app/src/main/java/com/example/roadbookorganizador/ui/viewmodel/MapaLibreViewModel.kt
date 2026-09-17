package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.PuntoInteresEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.example.roadbookorganizador.service.OdometerEngine
import com.example.roadbookorganizador.service.OdometerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MapaLibreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoadbookRepository(AppDatabase.getInstance(application))
    val odoEngine = OdometerEngine()

    val odoState: StateFlow<OdometerState> = odoEngine.state

    private val _rallyIdActual = MutableStateFlow<Long>(1L)
    val puntos = _rallyIdActual.flatMapLatest { id ->
        repository.getPuntosInteres(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var simulacionJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getActiveRally().collect { rally ->
                if (rally != null) {
                    _rallyIdActual.value = rally.id
                }
            }
        }
        iniciarEscuchaGps(application)
    }

    @SuppressLint("MissingPermission")
    private fun iniciarEscuchaGps(context: Context) {
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) odoEngine.procesarNuevaUbicacion(loc)
            }
            val request = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .build()
            fused.requestLocationUpdates(request, object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(res: com.google.android.gms.location.LocationResult) {
                    for (l in res.locations) odoEngine.procesarNuevaUbicacion(l)
                }
            }, context.mainLooper)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = LocationListener { loc -> odoEngine.procesarNuevaUbicacion(loc) }
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, listener)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0.5f, listener)
            }
            val best = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (best != null) odoEngine.procesarNuevaUbicacion(best)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleSimulacion(velocidadKmh: Float = 60f) {
        if (simulacionJob != null && simulacionJob?.isActive == true) {
            simulacionJob?.cancel()
            simulacionJob = null
            odoEngine.setModoSimulacion(false)
        } else {
            odoEngine.setModoSimulacion(true)
            simulacionJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000L)
                    val mPorSeg = velocidadKmh / 3.6
                    odoEngine.simularPaso(deltaMetros = mPorSeg, velKmh = velocidadKmh)
                }
            }
        }
    }

    fun agregarPuntoInteres(nombre: String, tipo: String, descripcion: String, lat: Double? = null, lng: Double? = null) {
        val s = odoState.value
        val rId = _rallyIdActual.value
        viewModelScope.launch {
            val pLat = lat ?: (if (s.latitud != 0.0) s.latitud else -31.4201)
            val pLng = lng ?: (if (s.longitud != 0.0) s.longitud else -64.1888)
            val punto = PuntoInteresEntity(
                rallyId = rId,
                nombre = nombre.ifBlank { "Punto Relevado" },
                tipo = tipo,
                latitud = pLat,
                longitud = pLng,
                descripcion = descripcion
            )
            repository.insertPuntoInteres(punto)
        }
    }

    fun eliminarPunto(punto: PuntoInteresEntity) {
        viewModelScope.launch {
            repository.deletePuntoInteres(punto)
        }
    }
}
