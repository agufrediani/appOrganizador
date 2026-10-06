package com.example.roadbookorganizador.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.PuntoInteresEntity
import com.example.roadbookorganizador.data.local.entity.TrackPointEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.local.entity.VinetaEntity
import com.example.roadbookorganizador.data.local.SessionManager
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import com.example.roadbookorganizador.service.OdometerEngine
import com.example.roadbookorganizador.service.OdometerState
import com.example.roadbookorganizador.service.RoadSnappingService
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
    val tulipaSeleccionada: String = "BLANCO",
    val nota: String = "",
    val peligro: String = ""
)

data class DialogoPuntoManualState(
    val visible: Boolean = false,
    val numero: Int = 1,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val distanciaTotal: Double = 0.0,
    val distanciaParcial: Double = 0.0,
    val tipoPunto: String = "INICIO",
    val nombrePunto: String = "",
    val nota: String = "",
    val dibujoTipo: String = "RECTA",
    val peligro: String = "",
    val nombreCamino: String = "",
    val esOffRoad: Boolean = false
)


class CockpitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoadbookRepository = RoadbookRepository(AppDatabase.getInstance(application))
    private val roadSnappingService = RoadSnappingService()
    private val sessionManager = SessionManager(application)
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

    private val _dialogoPuntoManual = MutableStateFlow(DialogoPuntoManualState())
    val dialogoPuntoManual: StateFlow<DialogoPuntoManualState> = _dialogoPuntoManual.asStateFlow()

    private val _exportResult = MutableSharedFlow<String>()
    val exportResult: SharedFlow<String> = _exportResult.asSharedFlow()

    private var simulacionJob: Job? = null

    init {
        viewModelScope.launch {
            val factor = repository.getFactorCalibracionActivo()
            odometerEngine.setFactorCalibracion(factor)
        }
        viewModelScope.launch {
            val bleManager = com.example.roadbookorganizador.gps.racebox.RaceBoxBleManager.getInstance(application)
            bleManager.connectionStatus.collect { status ->
                val conectado = (status == com.example.roadbookorganizador.gps.racebox.RaceBoxConnectionStatus.CONNECTED)
                odometerEngine.setRaceBoxBleConectado(conectado)
            }
        }
        odometerEngine.onAutoPcTriggered = { kmTotal, lat, lon, tipoPc ->
            val tId = _tramoActivo.value?.id
            if (tId != null) {
                viewModelScope.launch {
                    val s = odometerEngine.state.value
                    val numProxima = vinetas.value.size + 1
                    val entidad = VinetaEntity(
                        tramoId = tId,
                        numero = numProxima,
                        distanciaTotal = kmTotal,
                        distanciaParcial = s.odometroParcialKm,
                        latitud = lat,
                        longitud = lon,
                        altitud = s.altitud,
                        rumbo = s.rumbo,
                        velocidadKmh = s.velocidadKmh,
                        tulipTipo = "CONTROL_HORARIO",
                        informacion = "WP Auto ($tipoPc)",
                        peligro = "",
                        esPuntoControl = true,
                        tipoPuntoControl = tipoPc
                    )
                    repository.insertVineta(entidad)
                    odometerEngine.resetParcial()
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
            tulipaSeleccionada = "BLANCO",
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

    fun resetTotalManual(nuevoKm: Double = 0.0) {
        odometerEngine.resetTotalYParcial(nuevoKm)
    }

    fun ajustarMetros(delta: Double) {
        odometerEngine.ajustarMetros(delta)
    }

    fun toggleModoReverso() {
        odometerEngine.toggleModoReverso()
    }

    fun toggleAutoPcs() {
        odometerEngine.toggleAutoPcs()
    }

    fun setAutoPcsIntervalo(metros: Int) {
        odometerEngine.setIntervaloAutoPcs((metros / 1000.0).coerceAtLeast(0.05))
    }

    fun insertarPuntoControlManual(tipo: String = "REGULARIDAD") {
        val tId = _tramoActivo.value?.id ?: return
        val s = odometerEngine.state.value
        val numProxima = vinetas.value.size + 1
        viewModelScope.launch {
            val entidad = VinetaEntity(
                tramoId = tId,
                numero = numProxima,
                distanciaTotal = s.odometroTotalKm,
                distanciaParcial = s.odometroParcialKm,
                latitud = s.latitud,
                longitud = s.longitud,
                altitud = s.altitud,
                rumbo = s.rumbo,
                velocidadKmh = s.velocidadKmh,
                tulipTipo = "CONTROL_HORARIO",
                informacion = "WP $tipo",
                peligro = "",
                esPuntoControl = true,
                tipoPuntoControl = tipo
            )
            repository.insertVineta(entidad)
            odometerEngine.resetParcial()
            repository.updateDistanciaYEstado(tId, s.odometroTotalKm, "EN_TRAZADO")
        }
    }

    fun propagarDiferenciaKilometrica(desdeNumero: Int, deltaKm: Double) {
        val tId = _tramoActivo.value?.id ?: return
        viewModelScope.launch {
            repository.propagarDiferenciaKilometrica(tId, desdeNumero, deltaKm)
            val ult = repository.getUltimaVineta(tId)
            if (ult != null) {
                odometerEngine.resetTotalYParcial(ult.distanciaTotal)
                repository.updateDistanciaYEstado(tId, ult.distanciaTotal, "EN_TRAZADO")
            }
        }
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

    fun exportarKml(): File? {
        val tramo = _tramoActivo.value ?: return null
        val context = getApplication<Application>()
        val vList = vinetas.value
        val ptList = runCatching {
            emptyList<TrackPointEntity>()
        }.getOrDefault(emptyList())

        val file = GpxKmzExporter.exportarKml(context, tramo, vList, ptList)
        viewModelScope.launch {
            _exportResult.emit("Google Earth KML exportado en: ${file.name}")
        }
        return file
    }

    fun eliminarVineta(vineta: VinetaEntity) {
        viewModelScope.launch {
            repository.deleteVineta(vineta)
        }
    }

    fun eliminarVinetasPorIds(ids: Set<Long>) {
        viewModelScope.launch {
            val toDelete = vinetas.value.filter { it.id in ids }
            toDelete.forEach { repository.deleteVineta(it) }
        }
    }

    fun actualizarVineta(vineta: VinetaEntity) {
        viewModelScope.launch {
            repository.updateVineta(vineta)
        }
    }

    fun actualizarTramo(tramo: TramoEntity) {
        viewModelScope.launch {
            repository.updateTramo(tramo)
            _tramoActivo.value = tramo
        }
    }

    // =========================================================================
    // TRACKEO MANUAL: CREACIÓN DE PUNTOS DE PASO POR CLIC EN MAPA
    // =========================================================================

    fun iniciarCreacionPuntoManual(lat: Double, lng: Double, roadName: String = "", esOffRoad: Boolean = false) {
        val lista = vinetas.value
        val numProximo = lista.size + 1
        val esPrimero = lista.isEmpty()

        // REGLA SOLICITADA POR EL USUARIO:
        // Si es el 1ro -> preseleccionado INICIO
        // Luego (2do, 3ro, etc.) -> preseleccionado WAYPOINT
        val tipoDefecto = if (esPrimero) "INICIO" else "WAYPOINT"
        val dibujoDefecto = if (esPrimero) "LARGADA" else "RECTA"

        val (totKm, parKm) = if (esPrimero) {
            Pair(0.0, 0.0)
        } else {
            val ult = lista.last()
            val deltaMetros = calcularDistanciaMetros(ult.latitud, ult.longitud, lat, lng)
            val deltaKm = deltaMetros / 1000.0
            val tot = ult.distanciaTotal + deltaKm
            Pair(tot, deltaKm)
        }

        _dialogoPuntoManual.value = DialogoPuntoManualState(
            visible = true,
            numero = numProximo,
            latitud = lat,
            longitud = lng,
            distanciaTotal = totKm,
            distanciaParcial = parKm,
            tipoPunto = tipoDefecto,
            dibujoTipo = dibujoDefecto,
            nombreCamino = roadName,
            nota = when (tipoDefecto) {
                "INICIO" -> "Largada Oficial PE"
                else -> if (roadName.isNotBlank()) roadName else ""
            },
            esOffRoad = esOffRoad
        )

        // Si es camino (no off-road) y ya existe un punto previo, calcular la distancia REAL de calzada con curvas
        if (!esOffRoad && !esPrimero) {
            val ult = lista.last()
            viewModelScope.launch {
                val token = sessionManager.mapboxToken.value
                val pointsList = listOf(Pair(ult.latitud, ult.longitud), Pair(lat, lng))
                val route = roadSnappingService.snapTraceToRoad(
                    points = pointsList,
                    mapboxToken = token.takeIf { it.isNotBlank() }
                )
                if (route != null && route.distanciaMetros > 0) {
                    val roadDeltaKm = route.distanciaMetros / 1000.0
                    val cur = _dialogoPuntoManual.value
                    if (cur.visible && cur.latitud == lat && cur.longitud == lng) {
                        _dialogoPuntoManual.value = cur.copy(
                            distanciaParcial = roadDeltaKm,
                            distanciaTotal = ult.distanciaTotal + roadDeltaKm
                        )
                    }
                }
            }
        }
    }

    fun actualizarTipoPuntoManual(tipo: String) {
        val current = _dialogoPuntoManual.value
        val dibujo = when (tipo) {
            "INICIO" -> "LARGADA"
            "FINAL" -> "LLEGADA"
            "WAYPOINT" -> "RECTA"
            "AMBULANCIA" -> "CRUZ_ROJA"
            "HIDRATACIÓN" -> "RECTA"
            "BOMBEROS" -> "PRECAUCION"
            "CONTROL HORARIO (CH)" -> "CONTROL_HORARIO"
            "RESCATE 4X4" -> "PRECAUCION"
            "HELIPUERTO" -> "PRECAUCION"
            "PELIGRO (!)" -> "PRECAUCION"
            "ZONA DE ESPECTADORES" -> "PRECAUCION"
            "PARQUE DE ASISTENCIA" -> "PRECAUCION"
            else -> "RECTA"
        }
        val peligro = if (tipo == "PELIGRO (!)") "!" else ""
        val notaSugerida = when (tipo) {
            "INICIO" -> "Largada Oficial PE"
            "FINAL" -> "Llegada / Stop PE"
            "AMBULANCIA" -> "Puesto Médico / Ambulancia"
            "HIDRATACIÓN" -> "Puesto de Hidratación"
            "BOMBEROS" -> "Dotación Bomberos y Rescate"
            "CONTROL HORARIO (CH)" -> "CH - Control Horario"
            "RESCATE 4X4" -> "Unidad de Rescate 4x4"
            "HELIPUERTO" -> "Punto Evacuación / Helipuerto"
            "PELIGRO (!)" -> "Precaución / Peligro 1"
            "ZONA DE ESPECTADORES" -> "Zona Espectadores"
            "PARQUE DE ASISTENCIA" -> "Parque de Asistencia"
            else -> if (current.nombreCamino.isNotBlank()) current.nombreCamino else ""
        }

        _dialogoPuntoManual.value = current.copy(
            tipoPunto = tipo,
            dibujoTipo = dibujo,
            peligro = peligro,
            nota = if (current.nota.isBlank() || current.nota.startsWith("Largada") || current.nota.startsWith("Llegada") || current.nota.startsWith("Puesto") || current.nota.startsWith("CH") || current.nota.startsWith("Dotación") || current.nota.startsWith("Zona") || current.nota.startsWith("Parque") || current.nota.startsWith("Unidad") || current.nota.startsWith("Precaución")) notaSugerida else current.nota
        )
    }

    fun actualizarNotaPuntoManual(nota: String) {
        _dialogoPuntoManual.value = _dialogoPuntoManual.value.copy(nota = nota)
    }

    fun cancelarPuntoManual() {
        _dialogoPuntoManual.value = DialogoPuntoManualState(visible = false)
    }

    fun confirmarPuntoManual() {
        val tId = _tramoActivo.value?.id ?: return
        val dlg = _dialogoPuntoManual.value
        if (!dlg.visible) return

        viewModelScope.launch {
            val entidad = VinetaEntity(
                tramoId = tId,
                numero = dlg.numero,
                distanciaTotal = dlg.distanciaTotal,
                distanciaParcial = dlg.distanciaParcial,
                latitud = dlg.latitud,
                longitud = dlg.longitud,
                altitud = 0.0,
                rumbo = odoState.value.rumbo,
                velocidadKmh = 0f,
                tulipTipo = dlg.dibujoTipo,
                informacion = if (dlg.nota.isNotBlank()) "${dlg.tipoPunto}: ${dlg.nota}" else dlg.tipoPunto,
                peligro = dlg.peligro,
                esOffRoad = dlg.esOffRoad
            )

            repository.insertVineta(entidad)
            repository.updateDistanciaYEstado(tId, dlg.distanciaTotal, "EN_TRAZADO")
            odometerEngine.resetTotalYParcial(dlg.distanciaTotal)

            val tiposPoi = listOf("AMBULANCIA", "BOMBEROS", "ZONA DE ESPECTADORES", "PARQUE DE ASISTENCIA", "HIDRATACIÓN", "HELIPUERTO", "RESCATE 4X4")
            if (dlg.tipoPunto in tiposPoi) {
                val rId = _tramoActivo.value?.rallyId ?: 1L
                repository.insertPuntoInteres(
                    PuntoInteresEntity(
                        rallyId = rId,
                        nombre = "${dlg.tipoPunto} #${dlg.numero}",
                        tipo = when (dlg.tipoPunto) {
                            "AMBULANCIA" -> "AMBULANCIA"
                            "BOMBEROS", "RESCATE 4X4" -> "RESCATE"
                            "ZONA DE ESPECTADORES" -> "PUBLICO"
                            "HELIPUERTO" -> "HELIPUERTO"
                            else -> "ACCESO"
                        },
                        latitud = dlg.latitud,
                        longitud = dlg.longitud,
                        descripcion = dlg.nota
                    )
                )
            }

            _dialogoPuntoManual.value = DialogoPuntoManualState(visible = false)
        }
    }

    private fun calcularDistanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

