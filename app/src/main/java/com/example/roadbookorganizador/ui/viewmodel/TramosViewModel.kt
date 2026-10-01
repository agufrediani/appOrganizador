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

    private val _selectedRallyId = MutableStateFlow<Long?>(null)
    val selectedRallyId: StateFlow<Long?> = _selectedRallyId.asStateFlow()

    val tramos: StateFlow<List<TramoEntity>> = _selectedRallyId.flatMapLatest { id ->
        if (id != null) repository.getTramosByRally(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {

        // Vincular el ID del rally activo y sembrar tramo inicial si no existe ninguno
        viewModelScope.launch {
            try {
                repository.getActiveRally().collect { rally ->
                    if (rally != null) {
                        _selectedRallyId.value = rally.id
                        val tramosActuales = repository.getTramosByRally(rally.id).firstOrNull() ?: emptyList()
                        if (tramosActuales.isEmpty()) {
                            val tramoId = repository.insertTramo(
                                TramoEntity(
                                    rallyId = rally.id,
                                    tipo = "PE",
                                    identificador = "P.E. 1",
                                    nombre = "PARQUE GIORGI",
                                    numeroSector = 1,
                                    chInicio = "CH 01",
                                    chFin = "CH 02",
                                    distanciaTotalEstimada = 14.85,
                                    distanciaMedidaReal = 14.85,
                                    tiempoOtorgado = "25'",
                                    atrasoMaximo = "10'",
                                    horaPrimerAuto = "09:30",
                                    ordenSecuencia = 1,
                                    estadoTrazado = "LISTO"
                                )
                            )
                            // Viñetas iniciales según estándar oficial de hoja de ruta
                            repository.insertVineta(
                                com.example.roadbookorganizador.data.local.entity.VinetaEntity(
                                    tramoId = tramoId,
                                    numero = 1,
                                    distanciaTotal = 0.00,
                                    distanciaParcial = 0.00,
                                    latitud = -33.0450,
                                    longitud = -61.1650,
                                    tulipTipo = "LARGADA",
                                    informacion = "Largada oficial sobre asfalto. Precaución curva a 200m.",
                                    peligro = ""
                                )
                            )
                            repository.insertVineta(
                                com.example.roadbookorganizador.data.local.entity.VinetaEntity(
                                    tramoId = tramoId,
                                    numero = 2,
                                    distanciaTotal = 2.45,
                                    distanciaParcial = 2.45,
                                    latitud = -33.0520,
                                    longitud = -61.1710,
                                    tulipTipo = "CRUCE DER",
                                    informacion = "Cruce en T a la derecha entre arboleda. Calzada angosta.",
                                    peligro = "!"
                                )
                            )
                            repository.insertVineta(
                                com.example.roadbookorganizador.data.local.entity.VinetaEntity(
                                    tramoId = tramoId,
                                    numero = 3,
                                    distanciaTotal = 5.80,
                                    distanciaParcial = 3.35,
                                    latitud = -33.0610,
                                    longitud = -61.1850,
                                    tulipTipo = "VADO",
                                    informacion = "Vado con agua y barro profundo. Bajar ritmo.",
                                    peligro = "!!"
                                )
                            )
                            repository.insertVineta(
                                com.example.roadbookorganizador.data.local.entity.VinetaEntity(
                                    tramoId = tramoId,
                                    numero = 4,
                                    distanciaTotal = 14.85,
                                    distanciaParcial = 9.05,
                                    latitud = -33.0780,
                                    longitud = -61.1990,
                                    tulipTipo = "STOP",
                                    informacion = "Fin de Prueba Especial. Mesa de control stop.",
                                    peligro = ""
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    fun eliminarTramo(tramo: TramoEntity) {
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
