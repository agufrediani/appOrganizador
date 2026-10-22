package com.example.roadbookorganizador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roadbookorganizador.data.local.AppDatabase
import com.example.roadbookorganizador.data.local.entity.RallyEntity
import com.example.roadbookorganizador.data.local.entity.TramoEntity
import com.example.roadbookorganizador.data.repository.RoadbookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TramosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RoadbookRepository(AppDatabase.getInstance(application))

    val rallies: StateFlow<List<RallyEntity>> = repository.getAllRallies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rallyActivo: StateFlow<RallyEntity?> = repository.getActiveRally()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calibracionActiva = MutableStateFlow<Double>(1.0)

    /** Avisos para el usuario. */
    private val _avisos = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val avisos: SharedFlow<String> = _avisos.asSharedFlow()

    private val _selectedRallyId = MutableStateFlow<Long?>(null)
    val selectedRallyId: StateFlow<Long?> = _selectedRallyId.asStateFlow()

    val tramos: StateFlow<List<TramoEntity>> = _selectedRallyId.flatMapLatest { id ->
        if (id != null) repository.getTramosByRally(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {

        // Vincular el ID del rally activo. No se crean tramos ni viñetas de ejemplo:
        // los tramos se cargan desde la web o con "+ NUEVO TRAMO".
        viewModelScope.launch {
            repository.getActiveRally().collect { rally ->
                if (rally != null) _selectedRallyId.value = rally.id
            }
        }

        viewModelScope.launch {
            repository.getCalibracionActiva().collect { cal ->
                calibracionActiva.value = cal?.factorCorreccion ?: 1.0
            }
        }
    }

    fun seleccionarRally(id: Long) {
        _selectedRallyId.value = id
    }

    fun crearTramo(
        tipo: String,
        identificador: String,
        nombre: String,
        numeroSector: Int,
        chInicio: String,
        chFin: String,
        distanciaEstimada: Double,
        tiempoOtorgado: String,
        atrasoMaximo: String,
        horaPrimerAuto: String
    ) {
        val rId = _selectedRallyId.value ?: return
        viewModelScope.launch {
            // Los tramos van siempre adentro de un rally de la plataforma (acordado el 21/10/2026)
            if (repository.getRallyById(rId)?.webId == null) {
                _avisos.tryEmit("Este rally está solo en la tablet. Los rallies se crean en la plataforma: sincronizá y elegí uno de allá.")
                return@launch
            }
            repository.insertTramo(
                TramoEntity(
                    rallyId = rId,
                    tipo = tipo,
                    identificador = identificador,
                    nombre = nombre,
                    numeroSector = numeroSector,
                    chInicio = chInicio,
                    chFin = chFin,
                    distanciaTotalEstimada = distanciaEstimada,
                    tiempoOtorgado = tiempoOtorgado,
                    atrasoMaximo = atrasoMaximo,
                    horaPrimerAuto = horaPrimerAuto,
                    ordenSecuencia = (tramos.value.size + 1)
                )
            )
        }
    }

    // ---- Importar un tramo de un archivo (de otra tablet o de la SD) ----

    private val _paqueteImportado = MutableStateFlow<com.example.roadbookorganizador.util.PaqueteTramo?>(null)
    val paqueteImportado: StateFlow<com.example.roadbookorganizador.util.PaqueteTramo?> = _paqueteImportado.asStateFlow()

    /** Lee el archivo elegido. Si sirve, queda en [paqueteImportado] para confirmar. */
    fun leerArchivo(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val texto = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } ?: throw IllegalArgumentException("No se pudo abrir el archivo.")
                _paqueteImportado.value = com.example.roadbookorganizador.util.ArchivoTramo.leer(texto)
            } catch (e: IllegalArgumentException) {
                _avisos.tryEmit(e.message ?: "El archivo no sirve.")
            } catch (e: Exception) {
                _avisos.tryEmit("No se pudo leer el archivo: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun cancelarImportacion() { _paqueteImportado.value = null }

    /** Un tramo igual (tipo, identificador y nombre) que ya está en el rally, o null. */
    fun tramoRepetido(tipo: String, identificador: String, nombre: String): TramoEntity? =
        tramos.value.firstOrNull {
            it.tipo.equals(tipo, true) && it.identificador.trim().equals(identificador.trim(), true) && it.nombre.trim().equals(nombre.trim(), true)
        }

    /**
     * Guarda el tramo importado en el rally elegido, como una copia (uid nuevos, punto 0 ter).
     * Queda para subir: al sincronizar se crea en la plataforma (POST /api/tramos) y se suben
     * sus viñetas una por una.
     */
    fun confirmarImportacion(tipo: String, identificador: String, nombre: String) {
        val paquete = _paqueteImportado.value ?: return
        val rId = _selectedRallyId.value ?: return
        viewModelScope.launch {
            if (repository.getRallyById(rId)?.webId == null) {
                _avisos.tryEmit("Este rally está solo en la tablet. Los rallies se crean en la plataforma: sincronizá y elegí uno de allá.")
                return@launch
            }
            if (tramoRepetido(tipo, identificador, nombre) != null) {
                _avisos.tryEmit("Ya hay un tramo $identificador · $nombre en este rally. Cambiale el identificador o el nombre.")
                return@launch
            }
            val tramoId = repository.insertTramo(
                paquete.tramo.copy(
                    rallyId = rId,
                    tipo = tipo,
                    identificador = identificador.trim().uppercase(),
                    nombre = nombre.trim().uppercase(),
                    ordenSecuencia = tramos.value.size + 1
                )
            )
            paquete.vinetas.forEach { v -> repository.insertVineta(v.copy(tramoId = tramoId)) }
            _paqueteImportado.value = null
            _avisos.tryEmit("Tramo importado con ${paquete.vinetas.size} viñetas. Se sube a la plataforma al sincronizar.")
        }
    }

    fun eliminarTramo(tramo: TramoEntity) {
        // Un tramo que ya está en la plataforma se borra desde la web (si no, volvería al sincronizar)
        if (tramo.webId != null) {
            _avisos.tryEmit("${tramo.identificador} está en la plataforma: borralo desde la web.")
            return
        }
        viewModelScope.launch {
            repository.deleteTramo(tramo)
        }
    }

    fun actualizarTramo(
        tramoOriginal: TramoEntity,
        tipo: String,
        identificador: String,
        nombre: String,
        numeroSector: Int,
        chInicio: String,
        chFin: String,
        distanciaEstimada: Double,
        tiempoOtorgado: String,
        atrasoMaximo: String,
        horaPrimerAuto: String
    ) {
        viewModelScope.launch {
            val tramoModificado = tramoOriginal.copy(
                tipo = tipo,
                identificador = identificador,
                nombre = nombre,
                numeroSector = numeroSector,
                chInicio = chInicio,
                chFin = chFin,
                distanciaTotalEstimada = distanciaEstimada,
                tiempoOtorgado = tiempoOtorgado,
                atrasoMaximo = atrasoMaximo,
                horaPrimerAuto = horaPrimerAuto,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTramo(tramoModificado)
        }
    }
}
