package com.example.roadbookorganizador.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.TrackPointEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.example.roadbookorganizador.service.OdometerEngine
import com.example.roadbookorganizador.service.OdometerState
import com.example.roadbookorganizador.util.GpxKmzExporter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class DialogoMarcadoState(
    val visible: Boolean = false,
    val numero: Int = 1,
    val distanciaTotalCongelada: Double = 0.0,
    val distanciaParcialCongelada: Double = 0.0,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val altitud: Double = 0.0,
    val rumbo: Float = 0.0f,
    val tulipaSeleccionada: String = "RECTA",
    val nota: String = "",
    val peligro: String = ""
)

class CockpitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoadbookRepository = RoadbookRepository(AppDatabase.getInstance(application))
    val odometerEngine = com.example.roadbookorganizador.service.LocationTrackingService.sharedOdometerEngine

    private val _tramoActivo = MutableStateFlow<TramoEntity?>(null)
    val tramoActivo: StateFlow<TramoEntity?> = _tramoActivo.asStateFlow()

    val odoState: StateFlow<OdometerState> = odometerEngine.state

    val vinetas = _tramoActivo.flatMapLatest { tramo ->
        if (tramo != null) repository.getVinetasByTramo(tramo.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dialogoMarcado = MutableStateFlow(DialogoMarcadoState())
    val dialogoMarcado: StateFlow<DialogoMarcadoState> = _dialogoMarcado.asStateFlow()

    private val _exportResult = MutableSharedFlow<String>()
    val exportResult: SharedFlow<String> = _exportResult.asSharedFlow()

    private var simulacionJob: Job? = null

    init {
        viewModelScope.launch {
            val factor = repository.getFactorCalibracionActivo()
            odometerEngine.setFactorCalibracion(factor)
        }
        iniciarEscuchaGps(application)
    }

    @SuppressLint("MissingPermission")
    private fun iniciarEscuchaGps(context: Context) {
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) odometerEngine.procesarNuevaUbicacion(loc)
            }
            val request = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .build()
            fused.requestLocationUpdates(request, object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(res: com.google.android.gms.location.LocationResult) {
                    for (l in res.locations) odometerEngine.procesarNuevaUbicacion(l)
                }
            }, context.mainLooper)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = LocationListener { loc -> odometerEngine.procesarNuevaUbicacion(loc) }
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0.5f, listener)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0.5f, listener)
            }
            val best = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (best != null) odometerEngine.procesarNuevaUbicacion(best)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleSimulacion(velocidadKmh: Float = 60f) {
        if (simulacionJob != null && simulacionJob?.isActive == true) {
            simulacionJob?.cancel()
            simulacionJob = null
            odometerEngine.setModoSimulacion(false)
        } else {
            odometerEngine.setModoSimulacion(true)
            simulacionJob = viewModelScope.launch {
                while (isActive) {
                    delay(1000L)
                    val mPorSeg = velocidadKmh / 3.6
                    odometerEngine.simularPaso(deltaMetros = mPorSeg, velKmh = velocidadKmh)
                }
            }
        }
    }

    fun cargarTramo(tramoId: Long) {
        com.example.roadbookorganizador.service.LocationTrackingService.instance?.currentTramoId = tramoId
        viewModelScope.launch {
            val tramo = repository.getTramoById(tramoId)
            _tramoActivo.value = tramo

            if (tramo != null) {
                // Si el tramo ya tiene viñetas, reanudar odómetro desde la última distancia
                val ultima = repository.getUltimaVineta(tramoId)
                if (ultima != null) {
                    odometerEngine.resetTotalYParcial(ultima.distanciaTotal)
                } else {
                    odometerEngine.resetTotalYParcial(0.0)
                }
                repository.updateDistanciaYEstado(tramoId, tramo.distanciaMedidaReal, "EN_TRAZADO")
            }
        }
    }

    /**
     * 1-CLICK: Congela instantáneamente el kilometraje y abre el modal de confirmación rápida
     */
    fun presionarMarcarVineta() {
        val s = odometerEngine.state.value
        val (tot, par) = odometerEngine.congelarParaMarcado()
        val numProxima = vinetas.value.size + 1

        _dialogoMarcado.value = DialogoMarcadoState(
            visible = true,
            numero = numProxima,
            distanciaTotalCongelada = tot,
            distanciaParcialCongelada = par,
            latitud = s.latitud,
            longitud = s.longitud,
            altitud = s.altitud,
            rumbo = s.rumbo,
            tulipaSeleccionada = "RECTA",
            nota = "",
            peligro = ""
        )
    }

    fun actualizarTulipaSeleccionada(tulipa: String) {
        _dialogoMarcado.value = _dialogoMarcado.value.copy(tulipaSeleccionada = tulipa)
    }

    fun actualizarNota(nota: String) {
        _dialogoMarcado.value = _dialogoMarcado.value.copy(nota = nota)
    }

    fun actualizarPeligro(peligro: String) {
        _dialogoMarcado.value = _dialogoMarcado.value.copy(peligro = peligro)
    }

    fun cancelarMarcado() {
        odometerEngine.descongelar()
        _dialogoMarcado.value = DialogoMarcadoState(visible = false)
    }

    fun guardarVineta() {
        val tId = _tramoActivo.value?.id ?: return
        val dlg = _dialogoMarcado.value

        viewModelScope.launch {
            val entidad = VinetaEntity(
                tramoId = tId,
                numero = dlg.numero,
                distanciaTotal = dlg.distanciaTotalCongelada,
                distanciaParcial = dlg.distanciaParcialCongelada,
                latitud = dlg.latitud,
                longitud = dlg.longitud,
                altitud = dlg.altitud,
                rumbo = dlg.rumbo,
                velocidadKmh = odoState.value.velocidadKmh,
                tulipTipo = dlg.tulipaSeleccionada,
                informacion = dlg.nota,
                peligro = dlg.peligro
            )

            repository.insertVineta(entidad)
            // Resetea parcial a 0 para medir hasta la próxima viñeta
            odometerEngine.resetParcial()
            odometerEngine.descongelar()

            // Actualizar distancia medida en el tramo
            repository.updateDistanciaYEstado(tId, dlg.distanciaTotalCongelada, "EN_TRAZADO")

            _dialogoMarcado.value = DialogoMarcadoState(visible = false)
        }
    }

    fun resetParcialManual() {
        odometerEngine.resetParcial()
    }

    fun ajustarMetros(delta: Double) {
        odometerEngine.ajustarMetros(delta)
    }

    fun finalizarTramo() {
        val tramo = _tramoActivo.value ?: return
        val totalKm = odoState.value.odometroTotalKm
        viewModelScope.launch {
            repository.updateDistanciaYEstado(tramo.id, totalKm, "COMPLETADO")
            _tramoActivo.value = repository.getTramoById(tramo.id)
        }
    }

    fun exportarGpx(): File? {
        val tramo = _tramoActivo.value ?: return null
        val context = getApplication<Application>()
        val vList = vinetas.value
        val ptList = runCatching {
            // Obtener trackpoints síncronos o vacíos
            emptyList<TrackPointEntity>()
        }.getOrDefault(emptyList())

        val file = GpxKmzExporter.exportarGpx(context, tramo, vList, ptList)
        viewModelScope.launch {
            _exportResult.emit("GPX exportado con éxito en: ${file.name}")
        }
        return file
    }

    fun exportarJson(): File? {
        val tramo = _tramoActivo.value ?: return null
        val context = getApplication<Application>()
        val file = GpxKmzExporter.exportarJsonPlataforma(context, tramo, vinetas.value)
        viewModelScope.launch {
            _exportResult.emit("JSON Web exportado en: ${file.name}")
        }
        return file
    }

    fun eliminarVineta(vineta: VinetaEntity) {
        viewModelScope.launch {
            repository.deleteVineta(vineta)
        }
    }

    fun actualizarVineta(vineta: VinetaEntity) {
        viewModelScope.launch {
            repository.updateVineta(vineta)
        }
    }
}
