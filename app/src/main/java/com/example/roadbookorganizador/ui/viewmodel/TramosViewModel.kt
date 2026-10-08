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
