package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
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
import com.example.roadbookorganizador.util.paraPantalla
import com.example.roadbookorganizador.ui.vineta.BibliotecaVineta
import com.example.roadbookorganizador.vineta.Capa
import com.example.roadbookorganizador.vineta.Colocar
import com.example.roadbookorganizador.vineta.DibujoV1
import com.example.roadbookorganizador.vineta.VinetaDibujo
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
    val tulipaSeleccionada: String = "BLANCO", // código de flecha armada o "BLANCO" (sin dibujo)
    val nota: String = ""
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
    private val persistencia = com.example.roadbookorganizador.service.OdometroPersistencia(application)
    private var autoPcCallback: ((Double, Double, Double, String) -> Unit)? = null

    /** true solo en el ViewModel que abrió el tramo (no en el de la pantalla Exportar). */
    private var abrioTramo = false

    private val _tramoActivo = MutableStateFlow<TramoEntity?>(null)
    val tramoActivo: StateFlow<TramoEntity?> = _tramoActivo.asStateFlow()

    // Solo para pantalla: máximo 5 refrescos por segundo (el motor sigue a 25 Hz)
    val odoState: StateFlow<OdometerState> = odometerEngine.state.paraPantalla(viewModelScope)

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

    /** Avisos cortos para el usuario (igual que los alert de la plataforma). */
    private val _avisos = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val avisos: SharedFlow<String> = _avisos.asSharedFlow()

    /** Id de la viñeta recién creada desde el mapa (la pantalla la deja elegida, como la web). */
    private val _vinetaNueva = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val vinetaNueva: SharedFlow<Long> = _vinetaNueva.asSharedFlow()

    private var simulacionJob: Job? = null

    // ---- Sincronización del tramo con la plataforma (ESPECIFICACION_TAQUEO) ----
    private val sincroTramo = com.example.roadbookorganizador.data.remote.SincronizacionTramo(application, repository)
    private var sincroJob: Job? = null

    /** Cuántas viñetas tienen cambios que todavía no llegaron a la plataforma. */
    val pendientesSubir: StateFlow<Int> = _tramoActivo.flatMapLatest { t ->
        if (t != null) repository.contarPendientes(t.id) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _sincronizando = MutableStateFlow(false)
    val sincronizando: StateFlow<Boolean> = _sincronizando.asStateFlow()

    /** Sincroniza ahora el tramo abierto (botón). */
    fun sincronizarTramo() {
        val t = _tramoActivo.value ?: return
        viewModelScope.launch {
            _sincronizando.value = true
            val r = sincroTramo.sincronizar(t.id)
            _sincronizando.value = false
            repository.getTramoById(t.id)?.let { if (_tramoActivo.value?.id == it.id) _tramoActivo.value = it }
            _avisos.tryEmit(r.mensaje)
        }
    }

    /**
     * Al abrir un tramo que está en la plataforma: se baja lo último y, mientras el tramo esté
     * abierto, cada minuto se manda lo pendiente (si no hay señal, queda guardado y se reintenta).
     */
    private fun arrancarSincronizacion(tramo: TramoEntity) {
        sincroJob?.cancel()
        sincroJob = viewModelScope.launch {
            var primera = true
            var avisoSinConexion = false
            while (true) {
                if (primera || pendientesSubir.value > 0 || repository.contarPendientes(tramo.id).first() > 0) {
                    // Rally solo en la tablet: no hay nada que sincronizar
                    if (repository.getRallyById(tramo.rallyId)?.webId == null) return@launch
                    _sincronizando.value = true
                    val r = sincroTramo.sincronizar(tramo.id)
                    _sincronizando.value = false
                    // El tramo pudo haberse creado en la plataforma o cambiado allá
                    repository.getTramoById(tramo.id)?.let { if (_tramoActivo.value?.id == it.id) _tramoActivo.value = it }
                    when {
                        r.enviadas > 0 -> _avisos.tryEmit(r.mensaje)
                        !r.ok && r.sinConexion -> { if (!avisoSinConexion) _avisos.tryEmit(r.mensaje); avisoSinConexion = true }
                        !r.ok && primera -> _avisos.tryEmit(r.mensaje)
                    }
                    if (r.ok) avisoSinConexion = false
                    primera = false
                }
                kotlinx.coroutines.delay(60_000)
            }
        }
    }

    init {
        viewModelScope.launch {
            val factor = repository.getFactorCalibracionActivo()
            odometerEngine.setFactorCalibracion(factor)
        }
        // El estado del RaceBox y las posiciones GPS los maneja LocationTrackingService
        // (una sola fuente). Este ViewModel ya no registra listeners propios.
        autoPcCallback = { kmTotal, lat, lon, tipoPc ->
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
                        rumbo = 0f, // CAP: nunca se calcula solo (punto 12); el rumbo del GPS no se guarda en la viñeta
                        velocidadKmh = s.velocidadKmh,
                        tulipTipo = "CONTROL_HORARIO",
                        informacion = "WP Auto ($tipoPc)",
                        peligro = "",
                        esPuntoControl = true,
                        tipoPuntoControl = tipoPc,
                        origenCoordenada = "TABLET",
                        coordenadaFechaUtc = VinetaEntity.ahoraUtc(),
                        precisionGpsM = s.precisionMetros.takeIf { it > 0f }?.toDouble(),
                        verificadaTerreno = true
                    )
                    repository.insertVineta(entidad)
                    odometerEngine.resetParcial()
                }
            }
        }
    }

    override fun onCleared() {
        // Se salió del cockpit: dejar de grabar el track de este tramo y soltar el callback
        if (odometerEngine.onAutoPcTriggered === autoPcCallback) odometerEngine.onAutoPcTriggered = null
        if (abrioTramo) {
            val tId = _tramoActivo.value?.id
            val s = odometerEngine.state.value
            if (tId != null) persistencia.guardar(tId, s.odometroTotalKm, s.odometroParcialKm)
            persistencia.marcarInactivo()
            com.example.roadbookorganizador.service.LocationTrackingService.setTramoActivo(null)
        }
        super.onCleared()
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
        val servicio = com.example.roadbookorganizador.service.LocationTrackingService
        // Si este tramo ya está en trazado (volviendo de Calibración, o la app se reinició
        // y el servicio lo retomó), el odómetro sigue como está.
        val yaActivo = servicio.tramoActivoId == tramoId
        servicio.setTramoActivo(tramoId)
        abrioTramo = true
        // Los PCs automáticos los guarda solo el ViewModel del cockpit (no el de Exportar)
        odometerEngine.onAutoPcTriggered = autoPcCallback
        viewModelScope.launch {
            val tramo = repository.getTramoById(tramoId)
            _tramoActivo.value = tramo

            if (tramo != null) {
                arrancarSincronizacion(tramo)
                if (!yaActivo) {
                    val guardado = persistencia.leer()
                    if (guardado != null && guardado.tramoId == tramoId) {
                        // Retomar donde quedó el odómetro la última vez que se salió del tramo
                        odometerEngine.restaurar(guardado.totalKm, guardado.parcialKm)
                    } else {
                        // Primera vez: arrancar desde la última viñeta (o desde cero)
                        val ultima = repository.getUltimaVineta(tramoId)
                        odometerEngine.resetTotalYParcial(ultima?.distanciaTotal ?: 0.0)
                    }
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
            nota = ""
        )
    }

    fun actualizarTulipaSeleccionada(tulipa: String) {
        _dialogoMarcado.value = _dialogoMarcado.value.copy(tulipaSeleccionada = tulipa)
    }

    fun actualizarNota(nota: String) {
        _dialogoMarcado.value = _dialogoMarcado.value.copy(nota = nota)
    }

    fun cancelarMarcado() {
        odometerEngine.descongelar()
        _dialogoMarcado.value = DialogoMarcadoState(visible = false)
    }

    fun guardarVineta() {
        val tId = _tramoActivo.value?.id ?: return
        val dlg = _dialogoMarcado.value

        // El dibujo se guarda directo en la versión 1: la flecha armada elegida (o nada).
        val bib = BibliotecaVineta.obtener(getApplication<Application>())
        val armada = bib.flechasArmadas[dlg.tulipaSeleccionada]
        val (dibujoJson, peligroCalculado) = VinetaDibujo.paraGuardar(
            // La flecha armada se pone como un cuadrado de 29 mm en el recuadro de 50 × 45 (punto 8)
            if (armada != null) DibujoV1(dibujo = Capa(arrows = listOf(Colocar.armadaEnCaja(armada.flecha)))) else DibujoV1()
        )

        viewModelScope.launch {
            val entidad = VinetaEntity(
                tramoId = tId,
                numero = dlg.numero,
                distanciaTotal = dlg.distanciaTotalCongelada,
                distanciaParcial = dlg.distanciaParcialCongelada,
                latitud = dlg.latitud,
                longitud = dlg.longitud,
                altitud = dlg.altitud,
                rumbo = 0f, // CAP: nunca se calcula solo (punto 12); el rumbo del GPS no se guarda en la viñeta
                velocidadKmh = odometerEngine.state.value.velocidadKmh,
                tulipTipo = dlg.tulipaSeleccionada,
                dibujo = dibujoJson,
                informacion = dlg.nota,
                peligro = peligroCalculado,
                // Coordenada tomada con el GPS arriba del auto (taqueo)
                origenCoordenada = "TABLET",
                coordenadaFechaUtc = VinetaEntity.ahoraUtc(),
                precisionGpsM = odometerEngine.state.value.precisionMetros.takeIf { it > 0f }?.toDouble(),
                verificadaTerreno = true
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
                rumbo = 0f, // CAP: nunca se calcula solo (punto 12); el rumbo del GPS no se guarda en la viñeta
                velocidadKmh = s.velocidadKmh,
                tulipTipo = "CONTROL_HORARIO",
                informacion = "WP $tipo",
                peligro = "",
                esPuntoControl = true,
                tipoPuntoControl = tipo,
                origenCoordenada = "TABLET",
                coordenadaFechaUtc = VinetaEntity.ahoraUtc(),
                precisionGpsM = s.precisionMetros.takeIf { it > 0f }?.toDouble(),
                verificadaTerreno = true
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
        val totalKm = odometerEngine.state.value.odometroTotalKm
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

    /**
     * "+ Indicación" de la plataforma: viñeta vacía después de la elegida (o al final), parcial 0,
     * sin coordenada (se carga a mano o se taquea después).
     */
    fun insertarVinetaVacia(previaId: Long?) {
        val tId = _tramoActivo.value?.id ?: return
        val lista = vinetas.value
        val previa = lista.firstOrNull { it.id == previaId } ?: lista.lastOrNull()
        viewModelScope.launch {
            val idNueva = repository.insertVineta(
                VinetaEntity(
                    tramoId = tId,
                    numero = (previa?.numero ?: 0) + 1,
                    distanciaTotal = previa?.distanciaTotal ?: 0.0,
                    distanciaParcial = 0.0,
                    latitud = 0.0,
                    longitud = 0.0,
                    altitud = 0.0,
                    rumbo = 0f,
                    velocidadKmh = 0f
                )
            )
            _vinetaNueva.tryEmit(idNueva)
        }
    }

    /**
     * Duplicar (como la plataforma): copia completa de la viñeta, puesta justo después, con
     * parcial 0 y su propio uid. Las que siguen no se mueven.
     */
    fun duplicarVineta(v: VinetaEntity) {
        viewModelScope.launch {
            val idNueva = repository.insertVineta(
                v.copy(
                    id = 0,
                    uid = VinetaEntity.nuevoUid(),
                    numero = v.numero + 1,
                    distanciaParcial = 0.0,
                    // Es una viñeta nueva: todavía no está en la plataforma
                    webId = null,
                    datosServidor = null
                )
            )
            _vinetaNueva.tryEmit(idNueva)
        }
    }

    // =========================================================================
    // TAQUEO EN EL TERRENO (ESPECIFICACION_TAQUEO punto 3 y 4)
    // =========================================================================

    /** Parcial medido con el odómetro desde la viñeta anterior (km, 3 decimales). */
    private fun parcialMedido(previa: VinetaEntity?): Double {
        val total = odoState.value.odometroTotalKm
        return (Math.round((total - (previa?.distanciaTotal ?: 0.0)).coerceAtLeast(0.0) * 1000.0) / 1000.0)
    }

    private fun previaDe(v: VinetaEntity): VinetaEntity? =
        vinetas.value.let { l -> l.getOrNull(l.indexOfFirst { it.id == v.id } - 1) }

    private fun siguienteDe(v: VinetaEntity): VinetaEntity? =
        vinetas.value.let { l -> l.getOrNull(l.indexOfFirst { it.id == v.id } + 1) }

    /** Viñeta (que no sea [excluirId]) a menos de [radioM] metros del auto, para no repetir. */
    fun vinetaCercana(excluirId: Long?, radioM: Double = 30.0): VinetaEntity? {
        val o = odoState.value
        if (o.latitud == 0.0 && o.longitud == 0.0) return null
        return vinetas.value
            .filter { it.id != excluirId && !(it.latitud == 0.0 && it.longitud == 0.0) }
            .map { it to calcularDistanciaMetros(o.latitud, o.longitud, it.latitud, it.longitud) }
            .filter { it.second < radioM }
            .minByOrNull { it.second }?.first
    }

    /** Después de taquear: la distancia nueva corre las siguientes y el odómetro queda en la viñeta. */
    private suspend fun acomodarDistancias(v: VinetaEntity, parcialNuevo: Double) {
        val delta = parcialNuevo - v.distanciaParcial
        if (kotlin.math.abs(delta) >= 0.0005) repository.propagarDiferenciaKilometrica(v.tramoId, v.numero, delta)
        val totalNuevo = (previaDe(v)?.distanciaTotal ?: 0.0) + parcialNuevo
        odometerEngine.resetTotalYParcial(totalNuevo)
    }

    /** "Taquear acá": la coordenada del auto reemplaza a la de la plataforma (la anterior queda guardada). */
    fun taquearAca(v: VinetaEntity) {
        val o = odoState.value
        if (o.latitud == 0.0 && o.longitud == 0.0) { _avisos.tryEmit("Sin señal de GPS: no se puede taquear."); return }
        viewModelScope.launch {
            val parcial = parcialMedido(previaDe(v))
            val total = (previaDe(v)?.distanciaTotal ?: 0.0) + parcial
            acomodarDistancias(v, parcial) // primero se corren las siguientes, así no cambia el orden
            repository.updateVineta(
                v.copy(
                    latitudAnterior = if (v.latitud == 0.0 && v.longitud == 0.0) v.latitudAnterior else v.latitud,
                    longitudAnterior = if (v.latitud == 0.0 && v.longitud == 0.0) v.longitudAnterior else v.longitud,
                    latitud = Math.round(o.latitud * 1e7) / 1e7,
                    longitud = Math.round(o.longitud * 1e7) / 1e7,
                    origenCoordenada = "TABLET",
                    coordenadaFechaUtc = VinetaEntity.ahoraUtc(),
                    coordenadaPor = sessionManager.sesion.value.username.ifBlank { null },
                    precisionGpsM = o.precisionMetros.toDouble().takeIf { it > 0 },
                    verificadaTerreno = true,
                    distanciaParcial = parcial,
                    distanciaTotal = total
                )
            )
            siguienteDe(v)?.let { _vinetaNueva.tryEmit(it.id) }
        }
    }

    /** "Está bien": se deja la coordenada de la plataforma y se confirma en el terreno. */
    fun estaBien(v: VinetaEntity) {
        viewModelScope.launch {
            val parcial = parcialMedido(previaDe(v))
            val total = (previaDe(v)?.distanciaTotal ?: 0.0) + parcial
            acomodarDistancias(v, parcial)
            repository.updateVineta(v.copy(verificadaTerreno = true, distanciaParcial = parcial, distanciaTotal = total))
            siguienteDe(v)?.let { _vinetaNueva.tryEmit(it.id) }
        }
    }

    /** Volver a la coordenada anterior (la que había antes de la última). */
    fun volverCoordenadaAnterior(v: VinetaEntity) {
        val lat = v.latitudAnterior ?: return
        val lng = v.longitudAnterior ?: return
        viewModelScope.launch {
            repository.updateVineta(
                v.copy(
                    latitud = lat, longitud = lng,
                    latitudAnterior = v.latitud, longitudAnterior = v.longitud,
                    origenCoordenada = "MAPA",
                    coordenadaFechaUtc = VinetaEntity.ahoraUtc(),
                    precisionGpsM = null
                )
            )
        }
    }

    /**
     * "Nueva viñeta" en el terreno: va después de [previa] (la última que se pasó), en la
     * posición del auto, con el parcial del odómetro y un uid de la tablet.
     */
    fun nuevaVinetaTaqueo(previa: VinetaEntity?) {
        val tId = _tramoActivo.value?.id ?: return
        val o = odoState.value
        if (o.latitud == 0.0 && o.longitud == 0.0) { _avisos.tryEmit("Sin señal de GPS: no se puede marcar la viñeta."); return }
        viewModelScope.launch {
            val parcial = parcialMedido(previa)
            val total = (previa?.distanciaTotal ?: 0.0) + parcial
            if (previa != null && parcial > 0) repository.propagarDiferenciaKilometrica(tId, previa.numero, parcial)
            val idNueva = repository.insertVineta(
                VinetaEntity(
                    tramoId = tId,
                    numero = (previa?.numero ?: 0) + 1,
                    distanciaTotal = total,
                    distanciaParcial = parcial,
                    latitud = Math.round(o.latitud * 1e7) / 1e7,
                    longitud = Math.round(o.longitud * 1e7) / 1e7,
                    altitud = o.altitud,
                    rumbo = 0f, // CAP: nunca se calcula solo
                    velocidadKmh = o.velocidadKmh,
                    origenCoordenada = "TABLET",
                    coordenadaFechaUtc = VinetaEntity.ahoraUtc(),
                    coordenadaPor = sessionManager.sesion.value.username.ifBlank { null },
                    precisionGpsM = o.precisionMetros.toDouble().takeIf { it > 0 },
                    verificadaTerreno = true
                )
            )
            odometerEngine.resetTotalYParcial(total)
            _vinetaNueva.tryEmit(idNueva)
        }
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

    /**
     * Tocar el mapa crea la viñeta al instante, como el doble clic de la plataforma: sin cartel.
     * - Se inserta después de la viñeta elegida (si no hay ninguna elegida, al final).
     * - El parcial se mide desde ESA viñeta: por camino si se encuentra ruta; si no (o fuera de
     *   camino), en línea recta, y se avisa.
     * - Las viñetas que siguen se corren esa misma distancia (conservan su parcial).
     * - La viñeta nueva queda vacía (sin dibujo ni texto), con la coordenada marcada en el mapa.
     */
    fun insertarDesdeMapa(lat: Double, lng: Double, esOffRoad: Boolean = false, previaId: Long? = null) {
        val tId = _tramoActivo.value?.id ?: return
        val lista = vinetas.value
        val previa = lista.firstOrNull { it.id == previaId } ?: lista.lastOrNull()
        val latR = Math.round(lat * 1e6) / 1e6
        val lngR = Math.round(lng * 1e6) / 1e6
        viewModelScope.launch {
            var parcialKm = 0.0
            if (previa != null) {
                val conCoord = !(previa.latitud == 0.0 && previa.longitud == 0.0)
                if (!conCoord) {
                    _avisos.tryEmit("La viñeta anterior no tiene coordenadas: la nueva se inserta con parcial 0. Cargá la distancia a mano.")
                } else {
                    val recta = calcularDistanciaMetros(previa.latitud, previa.longitud, latR, lngR) / 1000.0
                    parcialKm = recta
                    if (!esOffRoad) {
                        val token = sessionManager.mapboxToken.value
                        val ruta = try {
                            roadSnappingService.snapTraceToRoad(
                                points = listOf(Pair(previa.latitud, previa.longitud), Pair(latR, lngR)),
                                mapboxToken = token.takeIf { it.isNotBlank() }
                            )
                        } catch (e: Exception) { null }
                        if (ruta != null && ruta.distanciaMetros > 0) parcialKm = ruta.distanciaMetros / 1000.0
                        else _avisos.tryEmit("No se encontró camino: el parcial (" +
                            String.format(java.util.Locale.US, "%.3f", recta).replace('.', ',') +
                            " km) se calculó en línea recta. Verificalo.")
                    }
                }
            }
            parcialKm = Math.round(parcialKm * 1000.0) / 1000.0
            val total = (previa?.distanciaTotal ?: 0.0) + parcialKm

            // Las que siguen a la elegida se corren el parcial nuevo (así el orden se mantiene)
            if (previa != null) repository.propagarDiferenciaKilometrica(tId, previa.numero, parcialKm)

            val idNueva = repository.insertVineta(
                VinetaEntity(
                    tramoId = tId,
                    numero = (previa?.numero ?: 0) + 1,
                    distanciaTotal = total,
                    distanciaParcial = parcialKm,
                    latitud = latR,
                    longitud = lngR,
                    altitud = 0.0,
                    rumbo = 0f, // CAP: nunca se calcula solo (punto 12)
                    velocidadKmh = 0f,
                    esOffRoad = esOffRoad,
                    // Punto marcado tocando el mapa: sin verificar en el terreno
                    origenCoordenada = "MAPA",
                    coordenadaFechaUtc = VinetaEntity.ahoraUtc()
                )
            )
            val ultimoTotal = repository.getUltimaVineta(tId)?.distanciaTotal ?: total
            repository.updateDistanciaYEstado(tId, ultimoTotal, "EN_TRAZADO")
            _vinetaNueva.tryEmit(idNueva)
        }
    }

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
                rumbo = 0f, // CAP: nunca se calcula solo (punto 12); el rumbo del GPS no se guarda en la viñeta
                velocidadKmh = 0f,
                tulipTipo = dlg.dibujoTipo,
                informacion = if (dlg.nota.isNotBlank()) "${dlg.tipoPunto}: ${dlg.nota}" else dlg.tipoPunto,
                peligro = dlg.peligro,
                esOffRoad = dlg.esOffRoad,
                // Punto marcado tocando el mapa: sin verificar en el terreno
                origenCoordenada = "MAPA",
                coordenadaFechaUtc = VinetaEntity.ahoraUtc()
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

